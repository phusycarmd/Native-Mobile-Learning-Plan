package com.phusy2001.nativedevicemonitordemo.nativecore.background

import com.phusy2001.nativedevicemonitordemo.nativecore.device.DeviceSnapshot
import kotlinx.coroutines.flow.StateFlow

interface BackgroundMonitor {
    val isMonitoring: StateFlow<Boolean>
    val latestSnapshot: StateFlow<DeviceSnapshot?>
    val logCount: StateFlow<Int>

    fun startMonitoring(intervalSeconds: Long = 5L)
    fun stopMonitoring()
    fun getLatestSnapshot(): DeviceSnapshot?
    fun readLogEntries(): List<DeviceSnapshot>
    fun triggerManualSnapshot(): DeviceSnapshot
    fun clearLogs(): Boolean
}
