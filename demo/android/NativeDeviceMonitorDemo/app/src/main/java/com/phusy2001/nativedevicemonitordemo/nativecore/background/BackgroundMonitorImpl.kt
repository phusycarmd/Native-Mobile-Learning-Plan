package com.phusy2001.nativedevicemonitordemo.nativecore.background

import android.content.Context
import android.content.Intent
import android.os.Build
import com.phusy2001.nativedevicemonitordemo.nativecore.bluetooth.BluetoothManager
import com.phusy2001.nativedevicemonitordemo.nativecore.device.BatteryProviderImpl
import com.phusy2001.nativedevicemonitordemo.nativecore.device.BluetoothProviderImpl
import com.phusy2001.nativedevicemonitordemo.nativecore.device.DeviceSnapshot
import com.phusy2001.nativedevicemonitordemo.nativecore.device.DeviceSnapshotProvider
import com.phusy2001.nativedevicemonitordemo.nativecore.device.DeviceSnapshotProviderImpl
import com.phusy2001.nativedevicemonitordemo.nativecore.device.NetworkProviderImpl
import com.phusy2001.nativedevicemonitordemo.nativecore.logging.FileLogger
import com.phusy2001.nativedevicemonitordemo.nativecore.logging.FileLoggerImpl
import kotlinx.coroutines.flow.StateFlow

class BackgroundMonitorImpl(
    private val context: Context,
    bluetoothManager: BluetoothManager? = null,
    val fileLogger: FileLogger = FileLoggerImpl(context),
    val snapshotProvider: DeviceSnapshotProvider = DeviceSnapshotProviderImpl(
        BatteryProviderImpl(context),
        NetworkProviderImpl(context),
        BluetoothProviderImpl(context, bluetoothManager)
    )
) : BackgroundMonitor {

    override val isMonitoring: StateFlow<Boolean> = DeviceMonitorService.isServiceRunning
    override val latestSnapshot: StateFlow<DeviceSnapshot?> = DeviceMonitorService.latestSnapshot
    override val logCount: StateFlow<Int> = DeviceMonitorService.logCount

    init {
        // Đồng bộ số lượng log ban đầu
        DeviceMonitorService.updateLogCount(fileLogger.getLogCount())
        val all = fileLogger.readAll()
        if (all.isNotEmpty()) {
            DeviceMonitorService.updateLatestSnapshot(all.last())
        }
    }

    override fun startMonitoring(intervalSeconds: Long) {
        val intent = Intent(context, DeviceMonitorService::class.java).apply {
            action = DeviceMonitorService.ACTION_START
            putExtra(DeviceMonitorService.EXTRA_INTERVAL_SECONDS, intervalSeconds)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    override fun stopMonitoring() {
        val intent = Intent(context, DeviceMonitorService::class.java).apply {
            action = DeviceMonitorService.ACTION_STOP
        }
        context.startService(intent)
    }

    override fun getLatestSnapshot(): DeviceSnapshot? {
        return latestSnapshot.value ?: fileLogger.readAll().lastOrNull()
    }

    override fun readLogEntries(): List<DeviceSnapshot> {
        return fileLogger.readAll()
    }

    override fun triggerManualSnapshot(): DeviceSnapshot {
        val snapshot = snapshotProvider.takeSnapshot()
        fileLogger.append(snapshot)
        DeviceMonitorService.updateLatestSnapshot(snapshot)
        DeviceMonitorService.updateLogCount(fileLogger.getLogCount())
        return snapshot
    }

    override fun clearLogs() {
        fileLogger.clear()
        DeviceMonitorService.updateLogCount(0)
    }
}
