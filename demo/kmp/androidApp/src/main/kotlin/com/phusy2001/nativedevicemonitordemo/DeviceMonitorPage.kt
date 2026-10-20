package com.phusy2001.nativedevicemonitordemo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.phusy2001.nativedevicemonitordemo.nativecore.background.BackgroundMonitor
import com.phusy2001.nativedevicemonitordemo.nativecore.background.BackgroundMonitorImpl
import com.phusy2001.nativedevicemonitordemo.nativecore.device.BatteryProviderImpl
import com.phusy2001.nativedevicemonitordemo.nativecore.device.DeviceSnapshot
import com.phusy2001.nativedevicemonitordemo.nativecore.device.NetworkProviderImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val PrimaryBlue = Color(0xFF2563EB)
private val DarkBlue = Color(0xFF1D4ED8)
private val LightBlue = Color(0xFFEFF6FF)
private val SuccessGreen = Color(0xFF16A34A)
private val DangerRed = Color(0xFFDC2626)
private val BgGray = Color(0xFFF9FAFB)
private val CardBorder = Color(0xFFE5E7EB)
private val TextPrimary = Color(0xFF111827)
private val TextSecondary = Color(0xFF4B5563)
private val TextMuted = Color(0xFF9CA3AF)

@Composable
fun DeviceMonitorPage(
    onMenuClick: () -> Unit,
    backgroundMonitor: BackgroundMonitor? = null
) {
    val context = LocalContext.current
    val monitor = backgroundMonitor ?: remember { BackgroundMonitorImpl(context) }
    val scope = rememberCoroutineScope()

    val isMonitoring by monitor.isMonitoring.collectAsState()
    val latestSnapshot by monitor.latestSnapshot.collectAsState()
    val logCount by monitor.logCount.collectAsState()

    var selectedInterval by remember { mutableLongStateOf(5L) }
    val logEntries = remember { mutableStateListOf<DeviceSnapshot>() }
    var fileSizeBytes by remember { mutableStateOf(0L) }
    var showClearDialog by remember { mutableStateOf(false) }
    var selectedRawJson by remember { mutableStateOf<String?>(null) }

    // Launcher xin quyền Notification trên Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            monitor.startMonitoring(selectedInterval)
            Toast.makeText(context, "Đã bật giám sát thiết bị nền", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Cần cấp quyền Thông báo để chạy Foreground Service", Toast.LENGTH_LONG).show()
        }
    }

    fun refreshLogs() {
        scope.launch(Dispatchers.IO) {
            val all = monitor.readLogEntries()
            val size = (monitor as? BackgroundMonitorImpl)?.fileLogger?.getFileSizeBytes() ?: 0L
            withContext(Dispatchers.Main) {
                logEntries.clear()
                logEntries.addAll(all.reversed())
                fileSizeBytes = size
            }
        }
    }

    LaunchedEffect(logCount) {
        refreshLogs()
    }

    fun handleToggleMonitoring() {
        if (isMonitoring) {
            monitor.stopMonitoring()
            Toast.makeText(context, "Đã dừng giám sát", Toast.LENGTH_SHORT).show()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasNotifPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasNotifPermission) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    return
                }
            }
            monitor.startMonitoring(selectedInterval)
            Toast.makeText(context, "Bắt đầu giám sát (${selectedInterval}s/lần)", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleManualSnapshot() {
        scope.launch(Dispatchers.IO) {
            val snap = monitor.triggerManualSnapshot()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Đã ghi 1 snapshot: Pin ${snap.battery}%", Toast.LENGTH_SHORT).show()
                refreshLogs()
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onMenuClick) {
                        HamburgerIcon(tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Device Monitor",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Hardware metrics & JSONL Logger",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        },
        containerColor = BgGray
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card 1: Live Hardware Metrics
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TRẠNG THÁI PHẦN CỨNG (LIVE)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMuted,
                                letterSpacing = 0.5.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = LightBlue
                            ) {
                                Text(
                                    text = if (latestSnapshot != null) "Cập nhật realtime" else "Chờ dữ liệu",
                                    fontSize = 11.sp,
                                    color = PrimaryBlue,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Grid 3 thông số: Battery, Network, BLE
                        val snap = latestSnapshot
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricBox(
                                label = "Pin (Battery)",
                                value = if (snap != null) "${snap.battery}%" else "--",
                                subtext = if (snap != null) "Mức pin hiện tại" else "Chưa đo",
                                badgeColor = if ((snap?.battery ?: 0) > 20) SuccessGreen else DangerRed,
                                modifier = Modifier.weight(1f)
                            )
                            MetricBox(
                                label = "Wi-Fi",
                                value = if (snap != null) (if (snap.wifiConnected) "KẾT NỐI" else "NGẮT") else "--",
                                subtext = if (snap?.cellularAvailable == true) "+ 4G/Cellular" else "Chỉ Wi-Fi",
                                badgeColor = if (snap?.wifiConnected == true) SuccessGreen else TextMuted,
                                modifier = Modifier.weight(1f)
                            )
                            MetricBox(
                                label = "Bluetooth",
                                value = if (snap != null) (if (snap.bluetoothEnabled) "BẬT" else "TẮT") else "--",
                                subtext = if (snap?.bluetoothConnected == true) "Đã kết nối" else "Chưa nối",
                                badgeColor = if (snap?.bluetoothEnabled == true) PrimaryBlue else DangerRed,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (snap != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Thời gian ghi nhận: ${snap.timestamp}",
                                fontSize = 11.sp,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Card 2: Background Monitor Control
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DỊCH VỤ CHẠY NỀN (BACKGROUND MONITOR)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMuted,
                                letterSpacing = 0.5.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isMonitoring) SuccessGreen else DangerRed)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isMonitoring) "RUNNING" else "STOPPED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMonitoring) SuccessGreen else DangerRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Chu kỳ lấy mẫu (Interval):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(2L, 5L, 10L).forEach { sec ->
                                FilterChip(
                                    selected = selectedInterval == sec,
                                    onClick = { if (!isMonitoring) selectedInterval = sec },
                                    label = { Text("${sec} giây") },
                                    enabled = !isMonitoring
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { handleToggleMonitoring() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isMonitoring) DangerRed else PrimaryBlue
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (isMonitoring) "Dừng Giám Sát" else "Bắt Đầu Giám Sát",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            OutlinedButton(
                                onClick = { handleManualSnapshot() },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Ghi 1 Log Ngay")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Educational alert
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF3F4F6),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "📌 Hành vi hệ điều hành Android (Ma trận 5 trạng thái):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Khi thu nhỏ app (Background), Foreground Service duy trì Notification treo giúp chu kỳ ${selectedInterval}s tiếp tục chạy liên tục và không bị Doze Mode hoãn lại.",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            // Card 3: JSONL Storage & Log Entries
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "NHẬT KÝ FILE JSONL",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMuted,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "monitor_logs.jsonl (${logEntries.size} bản ghi · ${(fileSizeBytes / 1024.0).let { String.format("%.1f KB", it) }})",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = { refreshLogs() }) {
                                    Text("Làm mới", fontSize = 12.sp)
                                }
                                TextButton(
                                    onClick = { showClearDialog = true },
                                    colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
                                ) {
                                    Text("Xóa file", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (logEntries.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Chưa có bản ghi nào. Bấm 'Bắt Đầu Giám Sát' hoặc 'Ghi 1 Log Ngay' để tạo dữ liệu.",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // List of Log Entries
            itemsIndexed(logEntries) { index, entry ->
                LogItemCard(
                    index = logEntries.size - index,
                    entry = entry,
                    onClick = { selectedRawJson = entry.toJson() }
                )
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Xác nhận xóa nhật ký") },
            text = { Text("Bạn có chắc chắn muốn xóa toàn bộ file monitor_logs.jsonl không?") },
            confirmButton = {
                Button(
                    onClick = {
                        monitor.clearLogs()
                        refreshLogs()
                        showClearDialog = false
                        Toast.makeText(context, "Đã xóa toàn bộ file log", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (selectedRawJson != null) {
        AlertDialog(
            onDismissRequest = { selectedRawJson = null },
            title = { Text("Chi tiết bản ghi JSON") },
            text = {
                Text(
                    text = selectedRawJson ?: "",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(onClick = { selectedRawJson = null }) {
                    Text("Đóng")
                }
            }
        )
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    subtext: String,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = BgGray,
        border = BorderStroke(1.dp, CardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = label, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtext, fontSize = 10.sp, color = TextMuted)
        }
    }
}

@Composable
private fun LogItemCard(
    index: Int,
    entry: DeviceSnapshot,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#$index · ${entry.timestamp}",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = "JSONL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusTag(
                    text = "Pin: ${entry.battery}%",
                    color = if (entry.battery > 20) SuccessGreen else DangerRed
                )
                StatusTag(
                    text = if (entry.wifiConnected) "Wi-Fi: ON" else "Wi-Fi: OFF",
                    color = if (entry.wifiConnected) PrimaryBlue else TextMuted
                )
                StatusTag(
                    text = if (entry.cellularAvailable) "4G: YES" else "4G: NO",
                    color = if (entry.cellularAvailable) PrimaryBlue else TextMuted
                )
                StatusTag(
                    text = if (entry.bluetoothEnabled) "BLE: ON" else "BLE: OFF",
                    color = if (entry.bluetoothEnabled) PrimaryBlue else DangerRed
                )
            }
        }
    }
}

@Composable
private fun StatusTag(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun HamburgerIcon(tint: Color) {
    Column(
        modifier = Modifier.size(24.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(tint, RoundedCornerShape(1.dp))
            )
        }
    }
}
