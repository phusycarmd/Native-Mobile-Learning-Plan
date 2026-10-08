package com.phusy2001.nativedevicemonitordemo.nativecore.tool

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.phusy2001.nativedevicemonitordemo.nativecore.bluetooth.BleService
import com.phusy2001.nativedevicemonitordemo.nativecore.bluetooth.BluetoothManager

data class ToolInfo(
    val bootloaderVersion: String?,
    val firmwareVersion: String?,
    val guid: String?,
    val usbProductId: Int?
)

/**
 * Giao tiếp với CarMD dongle qua giao thức lệnh riêng
 * (tham chiếu: carmd-connect-app/libs/ble-tool và tài liệu Bluetooth_CommandSet).
 *
 * Request:  AD | cmdId | len | data... | CS
 * Response: DA (positive) hoặc DD (status) | cmdId + 1 | lenLo | lenHi | data... | CS
 * CS = tổng các byte phía trước mod 256.
 *
 * Lệnh được ghi vào characteristic Write, phản hồi về qua Notify (có thể chia thành nhiều gói 20 byte).
 */
class CarMdToolClient private constructor(
    private val bluetoothManager: BluetoothManager,
    private val writeUuid: String,
    val notifyUuid: String,
    private val onRawNotification: ((ByteArray) -> Unit)?
) {

    companion object {
        private const val TAG = "CarMdToolClient"

        private const val REQUEST_HEADER = 0xAD
        private const val RESPONSE_POSITIVE = 0xDA
        private const val RESPONSE_STATUS = 0xDD

        private const val CMD_GET_TOOL_SETTING = 0xE1
        private const val CMD_GET_DEVICE_GUID = 0xE5
        private const val CMD_GET_USB_PRODUCT_ID = 0xE7

        private const val COMMAND_TIMEOUT_MS = 5_000L

        // Chờ CCCD được ghi xong trước khi gửi lệnh đầu tiên (app gốc chờ 400 ms sau khi kết nối)
        private const val NOTIFY_SETUP_DELAY_MS = 500L

        // Cặp (Write, Notify) theo loại module BLE bên trong dongle
        private val CHARACTERISTIC_PAIRS = listOf(
            sigUuid("fff2") to sigUuid("fff1"), // FSC-BT646 / 826E / 825 (CarMD)
            sigUuid("ffe9") to sigUuid("ffe4")  // RF-BMS02A / ESP32
        )

        private fun sigUuid(short: String) = "0000$short-0000-1000-8000-00805f9b34fb"

        /** Trả về null nếu thiết bị không có characteristic của CarMD dongle. */
        fun create(
            bluetoothManager: BluetoothManager,
            services: List<BleService>,
            onRawNotification: ((ByteArray) -> Unit)? = null
        ): CarMdToolClient? {
            val available = services.flatMap { it.characteristics }.map { it.lowercase() }.toSet()
            val (writeUuid, notifyUuid) = CHARACTERISTIC_PAIRS
                .firstOrNull { (write, notify) -> write in available && notify in available }
                ?: return null
            return CarMdToolClient(bluetoothManager, writeUuid, notifyUuid, onRawNotification)
        }

        fun buildRequest(commandId: Int, data: ByteArray = ByteArray(0)): ByteArray {
            val frame = ByteArray(data.size + 4)
            frame[0] = REQUEST_HEADER.toByte()
            frame[1] = commandId.toByte()
            frame[2] = data.size.toByte()
            data.copyInto(frame, destinationOffset = 3)
            frame[frame.size - 1] = checksum(frame, frame.size - 1).toByte()
            return frame
        }

        private fun checksum(bytes: ByteArray, length: Int): Int =
            (0 until length).sumOf { bytes[it].toInt() and 0xFF } % 256

        // E1: bootloader = body[0..14], firmware = body[16..30] (ASCII, bỏ NUL)
        internal fun parseToolSetting(body: ByteArray): Pair<String, String>? {
            if (body.size < 32) return null
            return body.asciiString(0, 15) to body.asciiString(16, 31)
        }

        // E5: 16 byte theo thứ tự GUID của .NET -> đảo byte 0-3, 4-5, 6-7 rồi định dạng 8-4-4-4-12
        internal fun parseGuid(body: ByteArray): String? {
            if (body.size != 16) return null
            if (body.all { it == 0x00.toByte() } || body.all { it == 0xFF.toByte() }) return null
            val bytes = body.copyOf()
            bytes.reverse(0, 4)
            bytes.reverse(4, 6)
            bytes.reverse(6, 8)
            val hex = bytes.joinToString("") { "%02x".format(it) }
            return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-" +
                    "${hex.substring(16, 20)}-${hex.substring(20)}"
        }

        // E7: 2 byte little-endian
        internal fun parseUsbProductId(body: ByteArray): Int? {
            if (body.size != 2) return null
            return (body[0].toInt() and 0xFF) or ((body[1].toInt() and 0xFF) shl 8)
        }

        private fun ByteArray.asciiString(from: Int, to: Int): String =
            copyOfRange(from, to)
                .filter { it.toInt() in 1..127 }
                .toByteArray()
                .toString(Charsets.US_ASCII)
    }

    private class Response(val frame: ByteArray) {
        val isPositive: Boolean get() = (frame[0].toInt() and 0xFF) == RESPONSE_POSITIVE
        val commandId: Int get() = ((frame[1].toInt() and 0xFF) - 1) and 0xFF
        val body: ByteArray get() = frame.copyOfRange(4, frame.size - 1)
    }

    private class PendingCommand(
        val commandId: Int,
        val onResult: (Response?, String?) -> Unit
    )

    // Chạy toàn bộ trên main thread (BluetoothManagerImpl trả callback về main thread)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var receiveBuffer = ByteArray(0)
    private var pendingCommand: PendingCommand? = null
    private var isNotifying = false

    private val commandTimeout = Runnable {
        Log.w(TAG, "Lệnh 0x%02X hết thời gian chờ".format(pendingCommand?.commandId ?: 0))
        receiveBuffer = ByteArray(0)
        finishPendingCommand(null, "Hết thời gian chờ phản hồi từ tool.")
    }

    /** Đọc tuần tự E1 (phiên bản), E5 (GUID), E7 (USB product id). */
    fun getToolInfo(onResult: (ToolInfo?, String?) -> Unit) {
        ensureNotifying {
            sendCommand(CMD_GET_TOOL_SETTING, byteArrayOf(0x00)) { settingBody, settingError ->
                sendCommand(CMD_GET_DEVICE_GUID) { guidBody, _ ->
                    sendCommand(CMD_GET_USB_PRODUCT_ID) { productIdBody, _ ->
                        val versions = settingBody?.let { parseToolSetting(it) }
                        val info = ToolInfo(
                            bootloaderVersion = versions?.first,
                            firmwareVersion = versions?.second,
                            guid = guidBody?.let { parseGuid(it) },
                            usbProductId = productIdBody?.let { parseUsbProductId(it) }
                        )
                        if (versions == null && info.guid == null && info.usbProductId == null) {
                            onResult(null, settingError ?: "Không đọc được thông tin tool.")
                        } else {
                            onResult(info, null)
                        }
                    }
                }
            }
        }
    }

    private fun ensureNotifying(action: () -> Unit) {
        if (isNotifying) {
            action()
            return
        }
        bluetoothManager.subscribe(notifyUuid) { bytes -> handleNotification(bytes) }
        isNotifying = true
        mainHandler.postDelayed(action, NOTIFY_SETUP_DELAY_MS)
    }

    // onResult nhận body của phản hồi DA, hoặc null kèm thông báo lỗi
    private fun sendCommand(
        commandId: Int,
        data: ByteArray = ByteArray(0),
        onResult: (ByteArray?, String?) -> Unit
    ) {
        if (pendingCommand != null) {
            onResult(null, "Tool đang xử lý lệnh khác.")
            return
        }

        val command = PendingCommand(commandId) { response, error ->
            when {
                response == null -> onResult(null, error)
                response.isPositive -> onResult(response.body, null)
                else -> {
                    // Frame DD: body[0] = C1 (thành công) / C2 (lỗi), body[1] = mã lỗi
                    val errorCode = response.body.getOrNull(1)?.let { "0x%02X".format(it) } ?: "?"
                    onResult(null, "Tool từ chối lệnh 0x%02X (mã lỗi $errorCode).".format(commandId))
                }
            }
        }
        pendingCommand = command
        mainHandler.postDelayed(commandTimeout, COMMAND_TIMEOUT_MS)

        val frame = buildRequest(commandId, data)
        Log.d(TAG, "TX ${frame.toHex()}")
        bluetoothManager.write(writeUuid, frame) { error ->
            if (error != null && pendingCommand === command) {
                finishPendingCommand(null, "Gửi lệnh thất bại: $error")
            }
        }
    }

    private fun handleNotification(bytes: ByteArray) {
        onRawNotification?.invoke(bytes)
        receiveBuffer += bytes

        // Ghép các gói notification thành frame hoàn chỉnh dựa trên trường độ dài
        while (true) {
            val start = receiveBuffer.indexOfFirst {
                val b = it.toInt() and 0xFF
                b == RESPONSE_POSITIVE || b == RESPONSE_STATUS
            }
            if (start < 0) {
                receiveBuffer = ByteArray(0)
                return
            }
            if (start > 0) receiveBuffer = receiveBuffer.copyOfRange(start, receiveBuffer.size)
            if (receiveBuffer.size < 4) return

            val frameLength = ((receiveBuffer[2].toInt() and 0xFF) or
                    ((receiveBuffer[3].toInt() and 0xFF) shl 8)) + 5
            if (receiveBuffer.size < frameLength) return

            val frame = receiveBuffer.copyOfRange(0, frameLength)
            receiveBuffer = receiveBuffer.copyOfRange(frameLength, receiveBuffer.size)
            handleFrame(frame)
        }
    }

    private fun handleFrame(frame: ByteArray) {
        Log.d(TAG, "RX ${frame.toHex()}")
        if (checksum(frame, frame.size - 1) != (frame.last().toInt() and 0xFF)) {
            // App gốc không kiểm tra checksum phản hồi, nên chỉ cảnh báo
            Log.w(TAG, "Checksum phản hồi không khớp: ${frame.toHex()}")
        }

        val response = Response(frame)
        if (response.commandId == pendingCommand?.commandId) {
            finishPendingCommand(response, null)
        } else {
            // Frame tool tự gửi (vd: DA C6 ... báo pin yếu) hoặc phản hồi trễ của lệnh đã timeout
            Log.d(TAG, "Bỏ qua frame của lệnh 0x%02X".format(response.commandId))
        }
    }

    private fun finishPendingCommand(response: Response?, error: String?) {
        mainHandler.removeCallbacks(commandTimeout)
        val command = pendingCommand ?: return
        pendingCommand = null
        command.onResult(response, error)
    }

    private fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }
}
