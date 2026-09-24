package com.phusy2001.nativedevicemonitordemo.nativecore.bluetooth

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING
}

data class BleService(
    val uuid: String,
    val characteristics: List<String> = emptyList(),
    // Các characteristic hỗ trợ Read
    val readableCharacteristics: List<String> = emptyList(),
    // Các characteristic hỗ trợ Notify/Indicate (có thể subscribe)
    val notifiableCharacteristics: List<String> = emptyList()
)

interface BluetoothManager {
    fun isAvailable(): Boolean
    fun isEnabled(): Boolean
    fun startScan(onDeviceFound: (BleDevice) -> Unit, onError: (String) -> Unit)
    fun stopScan()
    fun connect(deviceAddress: String, onStateChange: (ConnectionState) -> Unit)
    fun disconnect()
    fun discoverServices(onSuccess: (List<BleService>) -> Unit, onError: (String) -> Unit)
    fun read(characteristicUuid: String, onResult: (ByteArray?, String?) -> Unit)
    // onResult(null) khi ghi thành công, ngược lại là thông báo lỗi
    fun write(characteristicUuid: String, data: ByteArray, onResult: (String?) -> Unit)
    fun subscribe(characteristicUuid: String, onNotification: (ByteArray) -> Unit)
    fun unsubscribe(characteristicUuid: String)
    fun getConnectionState(): ConnectionState
}
