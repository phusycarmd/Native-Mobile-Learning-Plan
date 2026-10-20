package com.phusy2001.nativedevicemonitordemo.nativecore.tool

import com.phusy2001.nativedevicemonitordemo.nativecore.bluetooth.BleService
import com.phusy2001.nativedevicemonitordemo.nativecore.bluetooth.BluetoothManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Giao tiếp với CarMD dongle qua giao thức lệnh riêng
 * (tham chiếu: carmd-connect-app/libs/ble-tool và tài liệu Bluetooth_CommandSet).
 */
class CarMdToolClient private constructor(
    private val bluetoothManager: BluetoothManager,
    private val writeUuid: String,
    val notifyUuid: String,
    private val onRawNotification: ((ByteArray) -> Unit)?
) {
    companion object {
        private const val REQUEST_HEADER = 0xAD
        private const val RESPONSE_POSITIVE = 0xDA
        private const val RESPONSE_STATUS = 0xDD

        private const val CMD_GET_TOOL_SETTING = 0xE1
        private const val CMD_GET_DEVICE_GUID = 0xE5
        private const val CMD_GET_USB_PRODUCT_ID = 0xE7

        private const val COMMAND_TIMEOUT_MS = 5_000L
        private const val NOTIFY_SETUP_DELAY_MS = 500L

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

        internal fun parseToolSetting(body: ByteArray): Pair<String, String>? {
            if (body.size < 32) return null
            return body.asciiString(0, 15) to body.asciiString(16, 31)
        }

        internal fun parseGuid(body: ByteArray): String? {
            if (body.size != 16) return null
            if (body.all { it == 0x00.toByte() } || body.all { it == 0xFF.toByte() }) return null
            val bytes = body.copyOf()
            bytes.reverse(0, 4)
            bytes.reverse(4, 6)
            bytes.reverse(6, 8)
            val hex = bytes.joinToString("") { byteToHex(it) }
            return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-" +
                    "${hex.substring(16, 20)}-${hex.substring(20)}"
        }

        internal fun parseUsbProductId(body: ByteArray): Int? {
            if (body.size != 2) return null
            return (body[0].toInt() and 0xFF) or ((body[1].toInt() and 0xFF) shl 8)
        }

        private fun byteToHex(b: Byte): String {
            val v = b.toInt() and 0xFF
            val hexChars = "0123456789abcdef"
            return "${hexChars[v shr 4]}${hexChars[v and 0x0F]}"
        }

        private fun ByteArray.reverse(fromIndex: Int, toIndex: Int) {
            var i = fromIndex
            var j = toIndex - 1
            while (i < j) {
                val temp = this[i]
                this[i] = this[j]
                this[j] = temp
                i++
                j--
            }
        }

        private fun ByteArray.asciiString(from: Int, to: Int): String {
            val filtered = copyOfRange(from, to).filter { it.toInt() in 1..127 }.toByteArray()
            return filtered.decodeToString()
        }
    }

    internal class Response(val frame: ByteArray) {
        val isPositive: Boolean get() = (frame[0].toInt() and 0xFF) == RESPONSE_POSITIVE
        val commandId: Int get() = ((frame[1].toInt() and 0xFF) - 1) and 0xFF
        val body: ByteArray get() = frame.copyOfRange(4, frame.size - 1)
    }

    internal class PendingCommand(
        val commandId: Int,
        val onResult: (Response?, String?) -> Unit
    )

    private val scope = CoroutineScope(Dispatchers.Main)
    private var receiveBuffer = ByteArray(0)
    private var pendingCommand: PendingCommand? = null
    private var isNotifying = false
    private var timeoutJob: Job? = null

    fun getToolInfo(onResult: (ToolInfo?, String?) -> Unit) {
        ensureNotifying {
            sendCommand(CMD_GET_TOOL_SETTING) { resE1, errE1 ->
                if (errE1 != null || resE1 == null || !resE1.isPositive) {
                    onResult(null, errE1 ?: "Lệnh E1 bị từ chối")
                    return@sendCommand
                }
                val (bootloader, firmware) = parseToolSetting(resE1.body) ?: (null to null)

                sendCommand(CMD_GET_DEVICE_GUID) { resE5, _ ->
                    val guid = resE5?.takeIf { it.isPositive }?.let { parseGuid(it.body) }

                    sendCommand(CMD_GET_USB_PRODUCT_ID) { resE7, _ ->
                        val usbId = resE7?.takeIf { it.isPositive }?.let { parseUsbProductId(it.body) }
                        onResult(
                            ToolInfo(
                                bootloaderVersion = bootloader,
                                firmwareVersion = firmware,
                                guid = guid,
                                usbProductId = usbId
                            ),
                            null
                        )
                    }
                }
            }
        }
    }

    internal fun sendCommand(
        commandId: Int,
        data: ByteArray = ByteArray(0),
        onResult: (Response?, String?) -> Unit
    ) {
        if (pendingCommand != null) {
            onResult(null, "Có lệnh khác đang chờ phản hồi.")
            return
        }

        val request = buildRequest(commandId, data)
        pendingCommand = PendingCommand(commandId, onResult)
        timeoutJob = scope.launch {
            delay(COMMAND_TIMEOUT_MS)
            receiveBuffer = ByteArray(0)
            finishPendingCommand(null, "Hết thời gian chờ phản hồi từ tool.")
        }

        bluetoothManager.write(writeUuid, request) { err ->
            if (err != null) {
                timeoutJob?.cancel()
                finishPendingCommand(null, "Ghi lệnh thất bại: $err")
            }
        }
    }

    private fun ensureNotifying(onReady: () -> Unit) {
        if (isNotifying) {
            onReady()
            return
        }
        bluetoothManager.subscribe(notifyUuid) { chunk ->
            onNotification(chunk)
        }
        isNotifying = true
        scope.launch {
            delay(NOTIFY_SETUP_DELAY_MS)
            onReady()
        }
    }

    private fun onNotification(chunk: ByteArray) {
        onRawNotification?.invoke(chunk)
        receiveBuffer += chunk
        processBuffer()
    }

    private fun processBuffer() {
        while (receiveBuffer.size >= 5) {
            val header = receiveBuffer[0].toInt() and 0xFF
            if (header != RESPONSE_POSITIVE && header != RESPONSE_STATUS) {
                receiveBuffer = receiveBuffer.copyOfRange(1, receiveBuffer.size)
                continue
            }

            val bodyLen = (receiveBuffer[2].toInt() and 0xFF) or
                    ((receiveBuffer[3].toInt() and 0xFF) shl 8)
            val expectedTotal = 4 + bodyLen + 1
            if (receiveBuffer.size < expectedTotal) {
                return
            }

            val frame = receiveBuffer.copyOfRange(0, expectedTotal)
            receiveBuffer = receiveBuffer.copyOfRange(expectedTotal, receiveBuffer.size)

            val calculatedCs = checksum(frame, frame.size - 1)
            val receivedCs = frame[frame.size - 1].toInt() and 0xFF
            if (calculatedCs != receivedCs) {
                finishPendingCommand(null, "Lỗi checksum phản hồi từ tool.")
                return
            }

            val response = Response(frame)
            val pending = pendingCommand
            if (pending != null && response.commandId == pending.commandId) {
                timeoutJob?.cancel()
                finishPendingCommand(response, null)
            }
        }
    }

    private fun finishPendingCommand(response: Response?, error: String?) {
        val callback = pendingCommand?.onResult
        pendingCommand = null
        callback?.invoke(response, error)
    }

    fun close() {
        timeoutJob?.cancel()
        if (isNotifying) {
            bluetoothManager.unsubscribe(notifyUuid)
            isNotifying = false
        }
        finishPendingCommand(null, "Đã đóng kết nối CarMD tool.")
        receiveBuffer = ByteArray(0)
    }
}
