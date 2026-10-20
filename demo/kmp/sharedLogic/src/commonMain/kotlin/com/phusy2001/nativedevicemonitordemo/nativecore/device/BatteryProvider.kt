package com.phusy2001.nativedevicemonitordemo.nativecore.device

data class BatteryInfo(
    val level: Int,
    val isCharging: Boolean
)

interface BatteryProvider {
    fun getBatteryLevel(): Int
    fun isCharging(): Boolean
    fun getBatteryInfo(): BatteryInfo
}
