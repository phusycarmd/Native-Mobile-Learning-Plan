package com.phusy2001.nativedevicemonitordemo

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phusy2001.nativedevicemonitordemo.nativecore.bluetooth.BluetoothManager
import com.phusy2001.nativedevicemonitordemo.nativecore.bluetooth.BluetoothManagerImpl
import com.phusy2001.nativedevicemonitordemo.nativecore.bluetooth.ConnectionState
import com.phusy2001.nativedevicemonitordemo.nativecore.permission.PermissionManager
import com.phusy2001.nativedevicemonitordemo.nativecore.permission.PermissionManagerImpl
import com.phusy2001.nativedevicemonitordemo.nativecore.tool.CarMdToolClient
import com.phusy2001.nativedevicemonitordemo.nativecore.tool.ToolInfo
import com.phusy2001.nativedevicemonitordemo.ui.theme.NativeDeviceMonitorDemoTheme

data class BluetoothDeviceItem(
    val id: String,
    val name: String,
    val rssi: Int,
    val isConnectable: Boolean
)

private val DeviceRowHeight = 54.dp
private const val MAX_VISIBLE_DEVICES = 5

// Một dòng trong ô Received Data: giá trị mới nhất của một characteristic
data class CharacteristicRow(
    val uuid: String,
    val canRead: Boolean,
    val canNotify: Boolean,
    val value: String = "...",
    val notificationCount: Int = 0
)

// Tín hiệu yếu hơn ngưỡng này coi là quá xa, kết nối không ổn định
private const val MIN_RSSI = -90

@Composable
fun HomePage(
    modifier: Modifier = Modifier
) {
    // Colors matching the design
    val primaryBlue = Color(0xFF2563EB)
    val lightBlueBg = Color(0xFFEFF4FF)
    val textPrimary = Color(0xFF1F2937)
    val textSecondary = Color(0xFF4B5563)
    val textLabel = Color(0xFF374151)
    val textRssi = Color(0xFF6B7280)
    val borderColor = Color(0xFFE5E7EB)
    val dividerColor = Color(0xFFF3F4F6)
    val buttonGrayBg = Color(0xFFF3F4F6)
    val terminalBg = Color(0xFF0F172A)
    val terminalText = Color(0xFF4ADE80)
    val terminalLabel = Color(0xFF94A3B8)
    val statusGreen = Color(0xFF10B981)
    val stopRed = Color(0xFFDC2626)

    val context = LocalContext.current
    val permissionManager: PermissionManager = remember(context) {
        PermissionManagerImpl(context.applicationContext)
    }
    val bluetoothManager: BluetoothManager = remember(context) {
        BluetoothManagerImpl(context.applicationContext)
    }

    // Interactive UI state
    var searchQuery by remember { mutableStateOf("") }
    var selectedDeviceId by remember { mutableStateOf("") }
    var connectionState by remember { mutableStateOf(ConnectionState.DISCONNECTED) }
    var isScanning by remember { mutableStateOf(false) }
    var receivedData by remember { mutableStateOf("--") }
    val characteristicRows = remember { mutableStateListOf<CharacteristicRow>() }
    // Chỉ có giá trị khi thiết bị đang kết nối là CarMD dongle
    var toolClient by remember { mutableStateOf<CarMdToolClient?>(null) }
    var toolInfoText by remember { mutableStateOf<String?>(null) }
    var isReadingToolInfo by remember { mutableStateOf(false) }
    // Quyền bị khoá ("Don't ask again") -> phải hướng người dùng vào Settings, giống iOS
    var showPermissionDialog by remember { mutableStateOf(false) }

    var discoveredDevices by remember { mutableStateOf(emptyList<BluetoothDeviceItem>()) }

    // Lọc theo từ khoá, sau đó sắp xếp tín hiệu mạnh nhất (dBm lớn nhất) lên đầu
    val filteredDevices = remember(discoveredDevices, searchQuery) {
        // Ẩn thiết bị nhiễu: không kết nối được hoặc tín hiệu quá yếu
        val cleanDevices = discoveredDevices.filter {
            it.isConnectable && it.rssi >= MIN_RSSI
        }
        val list = if (searchQuery.isBlank()) {
            cleanDevices
        } else {
            cleanDevices.filter {
                it.name.contains(searchQuery, ignoreCase = true) || it.id.contains(
                    searchQuery,
                    ignoreCase = true
                )
            }
        }
        list.sortedByDescending { it.rssi }
    }

    val bluetoothPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    LaunchedEffect(Unit) {

    }



    // Tự động dừng quét và ngắt kết nối khi Composable bị hủy
    DisposableEffect(Unit) {
        onDispose {
            bluetoothManager.stopScan()
            bluetoothManager.disconnect()
        }
    }

    // Bước 4 -> 7: khám phá services, chọn characteristic Notify/Indicate đầu tiên và subscribe
    fun updateRow(uuid: String, transform: (CharacteristicRow) -> CharacteristicRow) {
        val index = characteristicRows.indexOfFirst { it.uuid == uuid }
        if (index >= 0) characteristicRows[index] = transform(characteristicRows[index])
    }

    // Khám phá services, sau đó đọc mọi characteristic Read và subscribe mọi characteristic Notify/Indicate.
    // Các lệnh được BluetoothManagerImpl xếp hàng và gửi lần lượt từng lệnh một.
    fun discoverReadAndSubscribeAll() {
        bluetoothManager.discoverServices(
            onSuccess = { services ->
                characteristicRows.clear()
                // Tool client nhận lại notification của characteristic phản hồi, nên chuyển tiếp để dòng đó vẫn cập nhật
                toolClient = CarMdToolClient.create(bluetoothManager, services) { bytes ->
                    toolClient?.let { client ->
                        updateRow(client.notifyUuid) {
                            it.copy(value = bytes.toDisplayString(), notificationCount = it.notificationCount + 1)
                        }
                    }
                }
                for (service in services) {
                    for (uuid in service.characteristics) {
                        val canRead = uuid in service.readableCharacteristics
                        val canNotify = uuid in service.notifiableCharacteristics
                        // Bỏ qua characteristic chỉ Write, và UUID trùng ở nhiều service
                        if (!canRead && !canNotify) continue
                        if (characteristicRows.any { it.uuid == uuid }) continue

                        characteristicRows.add(CharacteristicRow(uuid, canRead, canNotify))
                        if (canRead) {
                            bluetoothManager.read(uuid) { bytes, error ->
                                updateRow(uuid) {
                                    // Notification có thể tới trước kết quả read, khi đó giữ giá trị mới hơn
                                    if (it.notificationCount > 0) it
                                    else it.copy(value = error ?: bytes?.toDisplayString() ?: "--")
                                }
                            }
                        }
                        if (canNotify) {
                            bluetoothManager.subscribe(uuid) { bytes ->
                                updateRow(uuid) {
                                    it.copy(
                                        value = bytes.toDisplayString(),
                                        notificationCount = it.notificationCount + 1
                                    )
                                }
                            }
                        }
                    }
                }
                receivedData = if (characteristicRows.isEmpty()) {
                    "Không có characteristic Read/Notify"
                } else {
                    "--"
                }
            },
            onError = { error -> receivedData = error }
        )
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Cần quyền Bluetooth") },
            text = {
                Text("Quyền Bluetooth đã bị từ chối. Vui lòng bật lại trong Cài đặt để quét thiết bị xung quanh.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    permissionManager.openAppSettings()
                }) {
                    Text("Mở Cài đặt", color = primaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Huỷ", color = textSecondary)
                }
            },
            containerColor = Color.White
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(primaryBlue)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Native Device Monitor",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // --- Section: Device Name ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Device Name",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = textLabel
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search device...",
                            color = Color(0xFF9CA3AF),
                            fontSize = 15.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryBlue,
                        unfocusedBorderColor = borderColor,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = primaryBlue
                    )
                )
            }

            // --- Button: Scan Bluetooth ---
            Button(
                onClick = {
                    if (isScanning) {
                        bluetoothManager.stopScan()
                        isScanning = false
                        Toast.makeText(context, "Đã dừng quét Bluetooth.", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        val activity = context.findActivity()
                        if (activity != null) {
                            permissionManager.requestMultiplePermissions(
                                activity,
                                bluetoothPermissions
                            ) { results ->
                                val allGranted = results.values.all { it }
                                if (allGranted) {
                                    if (!bluetoothManager.isEnabled()) {
                                        Toast.makeText(
                                            context,
                                            "Bluetooth đang tắt! Vui lòng bật Bluetooth trên thiết bị.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@requestMultiplePermissions
                                    }

                                    // Quét lại: xoá danh sách cũ
                                    discoveredDevices = emptyList()
                                    selectedDeviceId = ""
                                    isScanning = true
                                    Toast.makeText(
                                        context,
                                        "Đang quét thiết bị BLE xung quanh...",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    bluetoothManager.startScan(
                                        onDeviceFound = { bleDevice ->
                                            // Thiết bị không tên là nhiễu, bỏ qua
                                            val name = bleDevice.name
                                                ?.takeIf { it.isNotBlank() }
                                                ?: return@startScan
                                            val newItem = BluetoothDeviceItem(
                                                id = bleDevice.address,
                                                name = name,
                                                rssi = bleDevice.rssi,
                                                isConnectable = bleDevice.isConnectable
                                            )

                                            // Cập nhật danh sách: nếu đã tồn tại thì cập nhật thông tin mới nhất, nếu chưa có thì thêm vào
                                            val index =
                                                discoveredDevices.indexOfFirst { it.id == bleDevice.address }
                                            discoveredDevices = if (index >= 0) {
                                                discoveredDevices.toMutableList()
                                                    .apply { set(index, newItem) }
                                            } else {
                                                discoveredDevices + newItem
                                            }
                                        },
                                        onError = { error ->
                                            isScanning = false
                                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                } else {
                                    val permanentlyDenied = results.filterValues { !it }.keys
                                        .any { permissionManager.isPermanentlyDenied(activity, it) }
                                    if (permanentlyDenied) {
                                        showPermissionDialog = true
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Cần cấp quyền Bluetooth để quét thiết bị.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning) stopRed else primaryBlue,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (isScanning) "Stop Scan" else "Scan Bluetooth",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // --- Section: Discovered Devices ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Discovered Devices",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = textLabel
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, borderColor),
                    color = Color.White
                ) {
                    if (filteredDevices.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(DeviceRowHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isScanning) "Đang quét thiết bị xung quanh..." else "Chưa có thiết bị nào",
                                fontSize = 14.sp,
                                color = textRssi
                            )
                        }
                    }

                    // Hiển thị tối đa 5 thiết bị, nhiều hơn thì cuộn trong khung
                    LazyColumn(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .heightIn(max = DeviceRowHeight * MAX_VISIBLE_DEVICES + 1.dp * (MAX_VISIBLE_DEVICES - 1))
                    ) {
                        itemsIndexed(filteredDevices, key = { _, device -> device.id }) { index, device ->
                            val isSelected = device.id == selectedDeviceId

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(DeviceRowHeight)
                                    .background(if (isSelected) lightBlueBg else Color.White)
                                    .clickable { selectedDeviceId = device.id }
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = device.name,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 16.sp,
                                    color = if (isSelected) primaryBlue else textPrimary
                                )

                                Text(
                                    text = "${device.rssi} dBm",
                                    fontSize = 15.sp,
                                    color = textRssi
                                )
                            }

                            if (index < filteredDevices.size - 1) {
                                HorizontalDivider(
                                    color = dividerColor,
                                    thickness = 1.dp
                                )
                            }
                        }
                    }
                }
            }

            // --- Action Buttons: Connect & Disconnect ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Button(
                    onClick = {
                        if (!BluetoothAdapter.checkBluetoothAddress(selectedDeviceId)) {
                            Toast.makeText(context, "Hãy chọn một thiết bị trong danh sách.", Toast.LENGTH_SHORT)
                                .show()
                            return@Button
                        }
                        bluetoothManager.stopScan()
                        isScanning = false
                        characteristicRows.clear()
                        toolClient = null
                        toolInfoText = null
                        receivedData = "Đang kết nối..."

                        bluetoothManager.connect(selectedDeviceId) { state ->
                            val wasConnected = connectionState == ConnectionState.CONNECTED
                            connectionState = state
                            when (state) {
                                ConnectionState.CONNECTED -> discoverReadAndSubscribeAll()
                                ConnectionState.DISCONNECTED -> {
                                    characteristicRows.clear()
                                    toolClient = null
                                    isReadingToolInfo = false
                                    // Không đi qua DISCONNECTING nghĩa là không phải người dùng bấm Disconnect
                                    if (wasConnected) {
                                        receivedData = "Mất kết nối đột ngột!"
                                        Toast.makeText(context, "Mất kết nối với thiết bị.", Toast.LENGTH_LONG)
                                            .show()
                                    }
                                }

                                else -> Unit
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = lightBlueBg,
                        contentColor = primaryBlue
                    )
                ) {
                    Text(
                        text = "Connect",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Button(
                    onClick = {
                        // disconnect() tự huỷ mọi subscription và lệnh GATT đang chờ
                        characteristicRows.clear()
                        toolClient = null
                        toolInfoText = null
                        bluetoothManager.disconnect()
                        receivedData = "Đã ngắt kết nối"
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonGrayBg,
                        contentColor = textSecondary
                    )
                ) {
                    Text(
                        text = "Disconnect",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            // --- Button: Get Tool Info (CarMD dongle) ---
            Button(
                onClick = {
                    val client = toolClient
                    if (client == null) {
                        Toast.makeText(context, "Thiết bị đang kết nối không phải CarMD tool.", Toast.LENGTH_SHORT)
                            .show()
                        return@Button
                    }
                    isReadingToolInfo = true
                    toolInfoText = "Đang đọc thông tin tool..."
                    client.getToolInfo { info, error ->
                        isReadingToolInfo = false
                        toolInfoText = error ?: info?.toDisplayText()
                    }
                },
                enabled = connectionState == ConnectionState.CONNECTED && !isReadingToolInfo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = lightBlueBg,
                    contentColor = primaryBlue
                )
            ) {
                Text(
                    text = "Get Tool Info",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // --- Section: Status Indicator ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = when (connectionState) {
                                ConnectionState.CONNECTED -> statusGreen
                                ConnectionState.CONNECTING, ConnectionState.DISCONNECTING -> Color(0xFFF59E0B)
                                ConnectionState.DISCONNECTED -> Color.Gray
                            },
                            shape = CircleShape
                        )
                )
                Text(
                    text = when (connectionState) {
                        ConnectionState.CONNECTED -> "Status: Connected"
                        ConnectionState.CONNECTING -> "Status: Connecting..."
                        ConnectionState.DISCONNECTING -> "Status: Disconnecting..."
                        ConnectionState.DISCONNECTED -> "Status: Disconnected"
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = textLabel
                )
            }

            // --- Section: Received Data ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Received Data",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = textLabel
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(terminalBg)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    if (characteristicRows.isEmpty()) {
                        Text(
                            text = receivedData,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = terminalText
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            characteristicRows.forEach { row ->
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = row.label(),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = terminalLabel
                                    )
                                    Text(
                                        text = row.value,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = terminalText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Section: Tool Info ---
            toolInfoText?.let { text ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tool Info",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = textLabel
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(terminalBg)
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = text,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = terminalText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun HomePagePreview() {
    NativeDeviceMonitorDemoTheme {
        HomePage()
    }
}

@Preview(name = "Full Phone Preview", showSystemUi = true, showBackground = true)
@Composable
fun HomePageSystemUiPreview() {
    NativeDeviceMonitorDemoTheme {
        HomePage()
    }
}

private fun ToolInfo.toDisplayText(): String = listOf(
    "Bootloader: ${bootloaderVersion ?: "--"}",
    "Firmware:   ${firmwareVersion ?: "--"}",
    "GUID:       ${guid ?: "--"}",
    "USB PID:    ${usbProductId?.let { "0x%04X (%d)".format(it, it) } ?: "--"}"
).joinToString("\n")

private fun ByteArray.toHexString(): String =
    joinToString(" ") { "%02X".format(it) }

// Hex, kèm chuỗi ASCII nếu mọi byte đều in được (vd: tên nhà sản xuất, số serial)
private fun ByteArray.toDisplayString(): String {
    if (isEmpty()) return "(rỗng)"
    val hex = toHexString()
    val isPrintable = all { it.toInt() in 0x20..0x7E }
    return if (isPrintable) "$hex  \"${String(this, Charsets.US_ASCII)}\"" else hex
}

// UUID chuẩn Bluetooth SIG (0000xxxx-0000-1000-8000-00805f9b34fb) rút gọn thành 4 ký tự
private fun String.shortUuid(): String {
    val lower = lowercase()
    return if (lower.startsWith("0000") && lower.endsWith("-0000-1000-8000-00805f9b34fb")) {
        lower.substring(4, 8).uppercase()
    } else {
        lower
    }
}

private fun CharacteristicRow.label(): String {
    val modes = listOfNotNull("Read".takeIf { canRead }, "Notify".takeIf { canNotify })
        .joinToString("/")
    val count = if (notificationCount > 0) "  #$notificationCount" else ""
    return "${uuid.shortUuid()}  [$modes]$count"
}

private fun Context.findActivity(): ComponentActivity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is ComponentActivity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}