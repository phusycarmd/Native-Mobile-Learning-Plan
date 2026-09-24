package com.phusy2001.nativedevicemonitordemo.nativecore.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.UUID

class BluetoothManagerImpl(
    private val context: Context
) : BluetoothManager {

    companion object {
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val androidBluetoothManager: android.bluetooth.BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager

    private val adapter: BluetoothAdapter?
        get() = androidBluetoothManager?.adapter

    private var activeScanCallback: ScanCallback? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectionState: ConnectionState = ConnectionState.DISCONNECTED
    private val mainHandler = Handler(Looper.getMainLooper())

    private var onNotificationCallback: ((ByteArray) -> Unit)? = null
    private var onReadResultCallback: ((ByteArray?, String?) -> Unit)? = null
    private var onServicesDiscoveredSuccess: ((List<BleService>) -> Unit)? = null
    private var onServicesDiscoveredError: ((String) -> Unit)? = null

    override fun isAvailable(): Boolean {
        return adapter != null
    }

    override fun isEnabled(): Boolean {
        return adapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    override fun startScan(onDeviceFound: (BleDevice) -> Unit, onError: (String) -> Unit) {
        val bluetoothAdapter = adapter
        if (bluetoothAdapter == null) {
            onError("Thiết bị không hỗ trợ Bluetooth.")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            onError("Bluetooth đang tắt. Vui lòng bật Bluetooth để quét.")
            return
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            onError("Không thể khởi tạo Bluetooth LE Scanner.")
            return
        }

        // Dừng quét trước đó nếu còn đang chạy
        stopScan()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result ?: return
                val device = result.device
                val name = device.name ?: result.scanRecord?.deviceName
                val address = device.address
                val rssi = result.rssi

                mainHandler.post {
                    onDeviceFound(
                        BleDevice(
                            name = name,
                            address = address,
                            rssi = rssi
                        )
                    )
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                results?.forEach { onScanResult(0, it) }
            }

            override fun onScanFailed(errorCode: Int) {
                mainHandler.post {
                    onError("Quét Bluetooth thất bại (mã lỗi: $errorCode).")
                }
            }
        }

        activeScanCallback = callback
        try {
            scanner.startScan(callback)
        } catch (e: Exception) {
            activeScanCallback = null
            onError("Lỗi khi bắt đầu quét: ${e.localizedMessage}")
        }
    }

    @SuppressLint("MissingPermission")
    override fun stopScan() {
        val scanner = adapter?.bluetoothLeScanner
        activeScanCallback?.let { callback ->
            try {
                scanner?.stopScan(callback)
            } catch (_: Exception) {
            }
            activeScanCallback = null
        }
    }

    @SuppressLint("MissingPermission")
    override fun connect(deviceAddress: String, onStateChange: (ConnectionState) -> Unit) {
        val bluetoothAdapter = adapter ?: return
        val device = try {
            bluetoothAdapter.getRemoteDevice(deviceAddress)
        } catch (_: Exception) {
            onStateChange(ConnectionState.DISCONNECTED)
            return
        }

        disconnect()
        connectionState = ConnectionState.CONNECTING
        onStateChange(connectionState)

        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        connectionState = ConnectionState.CONNECTED
                        mainHandler.post { onStateChange(connectionState) }
                        gatt?.discoverServices()
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        connectionState = ConnectionState.DISCONNECTED
                        mainHandler.post { onStateChange(connectionState) }
                        disconnect()
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                    val serviceList = gatt.services.map { service ->
                        BleService(
                            uuid = service.uuid.toString(),
                            characteristics = service.characteristics.map { it.uuid.toString() }
                        )
                    }
                    mainHandler.post {
                        onServicesDiscoveredSuccess?.invoke(serviceList)
                    }
                } else {
                    mainHandler.post {
                        onServicesDiscoveredError?.invoke("Khám phá services thất bại với status: $status")
                    }
                }
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    mainHandler.post { onReadResultCallback?.invoke(value, null) }
                } else {
                    mainHandler.post {
                        onReadResultCallback?.invoke(
                            null,
                            "Lỗi đọc dữ liệu status: $status"
                        )
                    }
                }
            }

            @Suppress("DEPRECATION")
            @Deprecated("Deprecated for SDK 33+")
            override fun onCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null) {
                    val value = characteristic.value
                    mainHandler.post { onReadResultCallback?.invoke(value, null) }
                } else {
                    mainHandler.post {
                        onReadResultCallback?.invoke(
                            null,
                            "Lỗi đọc dữ liệu status: $status"
                        )
                    }
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                mainHandler.post { onNotificationCallback?.invoke(value) }
            }

            @Suppress("DEPRECATION")
            @Deprecated("Deprecated for SDK 33+")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?
            ) {
                characteristic?.value?.let { value ->
                    mainHandler.post { onNotificationCallback?.invoke(value) }
                }
            }
        }

        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    @SuppressLint("MissingPermission")
    override fun disconnect() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (_: Exception) {
        }
        bluetoothGatt = null
        connectionState = ConnectionState.DISCONNECTED
    }

    @SuppressLint("MissingPermission")
    override fun discoverServices(
        onSuccess: (List<BleService>) -> Unit,
        onError: (String) -> Unit
    ) {
        val gatt = bluetoothGatt
        if (gatt == null || connectionState != ConnectionState.CONNECTED) {
            onError("Chưa kết nối tới thiết bị.")
            return
        }
        onServicesDiscoveredSuccess = onSuccess
        onServicesDiscoveredError = onError
        gatt.discoverServices()
    }

    @SuppressLint("MissingPermission")
    override fun read(characteristicUuid: String, onResult: (ByteArray?, String?) -> Unit) {
        val gatt = bluetoothGatt
        if (gatt == null || connectionState != ConnectionState.CONNECTED) {
            onResult(null, "Chưa kết nối tới thiết bị.")
            return
        }

        val targetCharacteristic = findCharacteristic(characteristicUuid)
        if (targetCharacteristic == null) {
            onResult(null, "Không tìm thấy Characteristic với UUID: $characteristicUuid")
            return
        }

        onReadResultCallback = onResult
        gatt.readCharacteristic(targetCharacteristic)
    }

    @SuppressLint("MissingPermission")
    override fun subscribe(characteristicUuid: String, onNotification: (ByteArray) -> Unit) {
        val gatt = bluetoothGatt ?: return
        val targetCharacteristic = findCharacteristic(characteristicUuid) ?: return

        this.onNotificationCallback = onNotification
        gatt.setCharacteristicNotification(targetCharacteristic, true)

        val descriptor = targetCharacteristic.getDescriptor(CCCD_UUID)
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }
    }

    @SuppressLint("MissingPermission")
    override fun unsubscribe(characteristicUuid: String) {
        val gatt = bluetoothGatt ?: return
        val targetCharacteristic = findCharacteristic(characteristicUuid) ?: return

        gatt.setCharacteristicNotification(targetCharacteristic, false)
        val descriptor = targetCharacteristic.getDescriptor(CCCD_UUID)
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }
        this.onNotificationCallback = null
    }

    override fun getConnectionState(): ConnectionState {
        return connectionState
    }

    private fun findCharacteristic(uuidStr: String): BluetoothGattCharacteristic? {
        val gatt = bluetoothGatt ?: return null
        val targetUuid = try {
            UUID.fromString(uuidStr)
        } catch (_: Exception) {
            return null
        }

        for (service in gatt.services) {
            for (characteristic in service.characteristics) {
                if (characteristic.uuid == targetUuid) {
                    return characteristic
                }
            }
        }
        return null
    }
}
