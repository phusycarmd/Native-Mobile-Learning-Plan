package com.phusy2001.nativedevicemonitordemo.nativecore.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.phusy2001.nativedevicemonitordemo.MainActivity
import com.phusy2001.nativedevicemonitordemo.R
import com.phusy2001.nativedevicemonitordemo.nativecore.device.BatteryProviderImpl
import com.phusy2001.nativedevicemonitordemo.nativecore.device.BluetoothProviderImpl
import com.phusy2001.nativedevicemonitordemo.nativecore.device.DeviceSnapshot
import com.phusy2001.nativedevicemonitordemo.nativecore.device.DeviceSnapshotProviderImpl
import com.phusy2001.nativedevicemonitordemo.nativecore.device.NetworkProviderImpl
import com.phusy2001.nativedevicemonitordemo.nativecore.logging.FileLoggerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Android Foreground Service phục vụ ghi nhận trạng thái thiết bị định kỳ.
 * Đáp ứng yêu cầu Tuần 3 - Bài tập 3.4 (Hoạt động bền vững ngay cả khi ứng dụng bị thu nhỏ).
 */
class DeviceMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var monitorJob: Job? = null

    private lateinit var snapshotProvider: DeviceSnapshotProviderImpl
    private lateinit var fileLogger: FileLoggerImpl
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        val battery = BatteryProviderImpl(applicationContext)
        val network = NetworkProviderImpl(applicationContext)
        val bluetooth = BluetoothProviderImpl(applicationContext)
        snapshotProvider = DeviceSnapshotProviderImpl(battery, network, bluetooth)
        fileLogger = FileLoggerImpl(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        when (action) {
            ACTION_STOP -> {
                stopMonitoringLoop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val interval = intent?.getLongExtra(EXTRA_INTERVAL_SECONDS, 5L) ?: 5L
                startForegroundWithNotification()
                startMonitoringLoop(interval)
                return START_STICKY
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification("Khởi động dịch vụ giám sát thiết bị...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        _isServiceRunning.value = true
    }

    private fun startMonitoringLoop(intervalSeconds: Long) {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (isActive) {
                val snapshot = snapshotProvider.takeSnapshot()
                fileLogger.append(snapshot)
                _latestSnapshot.value = snapshot
                _logCount.value = fileLogger.getLogCount()

                updateNotification(snapshot)
                delay(intervalSeconds * 1000L)
            }
        }
    }

    private fun stopMonitoringLoop() {
        monitorJob?.cancel()
        monitorJob = null
        _isServiceRunning.value = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Device Monitor Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hiển thị trạng thái hoạt động của tiến trình giám sát thiết bị"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, DeviceMonitorService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Native Device Monitor: Đang ghi log")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Dừng", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(snapshot: DeviceSnapshot) {
        val text = "Pin: ${snapshot.battery}% | Wi-Fi: ${if (snapshot.wifiConnected) "ON" else "OFF"} | Logs: ${_logCount.value}"
        val notification = buildNotification(text)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        stopMonitoringLoop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "device_monitor_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.phusy2001.ACTION_START_MONITOR"
        const val ACTION_STOP = "com.phusy2001.ACTION_STOP_MONITOR"
        const val EXTRA_INTERVAL_SECONDS = "extra_interval_seconds"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _latestSnapshot = MutableStateFlow<DeviceSnapshot?>(null)
        val latestSnapshot: StateFlow<DeviceSnapshot?> = _latestSnapshot.asStateFlow()

        private val _logCount = MutableStateFlow(0)
        val logCount: StateFlow<Int> = _logCount.asStateFlow()

        fun updateLogCount(count: Int) {
            _logCount.value = count
        }

        fun updateLatestSnapshot(snapshot: DeviceSnapshot) {
            _latestSnapshot.value = snapshot
        }
    }
}
