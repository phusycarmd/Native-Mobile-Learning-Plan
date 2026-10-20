package com.phusy2001.nativedevicemonitordemo.nativecore.bluetooth

data class BleDevice(
    val id: String,
    val name: String,
    val rssi: Int,
    val isConnectable: Boolean
) {
    val address: String get() = id
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING
}

data class BleService(
    val uuid: String,
    val characteristics: List<String> = emptyList(),
    val readableCharacteristics: List<String> = emptyList(),
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
    fun write(characteristicUuid: String, data: ByteArray, onResult: (String?) -> Unit)
    fun subscribe(characteristicUuid: String, onNotification: (ByteArray) -> Unit)
    fun unsubscribe(characteristicUuid: String)
    fun getConnectionState(): ConnectionState
}
