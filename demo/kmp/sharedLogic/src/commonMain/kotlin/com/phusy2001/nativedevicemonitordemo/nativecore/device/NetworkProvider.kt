package com.phusy2001.nativedevicemonitordemo.nativecore.device

data class NetworkInfo(
    val isConnected: Boolean,
    val isWifiConnected: Boolean,
    val isCellularAvailable: Boolean
)

interface NetworkProvider {
    fun isConnected(): Boolean
    fun isWifiConnected(): Boolean
    fun isCellularAvailable(): Boolean
    fun getNetworkInfo(): NetworkInfo
}
