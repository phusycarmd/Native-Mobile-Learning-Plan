package com.phusy2001.nativedevicemonitordemo.nativecore.device

data class BluetoothInfo(
    val isEnabled: Boolean,
    val isConnected: Boolean
)

interface BluetoothProvider {
    fun isBluetoothEnabled(): Boolean
    fun isBluetoothConnected(): Boolean
    fun getBluetoothInfo(): BluetoothInfo
}
