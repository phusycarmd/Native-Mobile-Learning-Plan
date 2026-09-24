package com.phusy2001.nativedevicemonitordemo

import android.Manifest
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.phusy2001.nativedevicemonitordemo.nativecore.permission.PermissionManager
import com.phusy2001.nativedevicemonitordemo.nativecore.permission.PermissionManagerImpl
import com.phusy2001.nativedevicemonitordemo.ui.theme.NativeDeviceMonitorDemoTheme

data class BluetoothDeviceItem(
    val id: String,
    val name: String,
    val rssi: String
)

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
    val statusGreen = Color(0xFF10B981)

    val context = LocalContext.current
    val permissionManager: PermissionManager = remember(context) {
        PermissionManagerImpl(context.applicationContext)
    }
    val bluetoothManager: BluetoothManager = remember(context) {
        BluetoothManagerImpl(context.applicationContext)
    }

    // Interactive UI state
    var searchQuery by remember { mutableStateOf("") }
    var selectedDeviceId by remember { mutableStateOf("device_a") }
    var isConnected by remember { mutableStateOf(true) }
    var isScanning by remember { mutableStateOf(false) }
    var receivedData by remember { mutableStateOf("01 02 03 04") }

    var discoveredDevices by remember {
        mutableStateOf(
            listOf(
                BluetoothDeviceItem(id = "device_a", name = "Device A", rssi = "-58 dBm"),
                BluetoothDeviceItem(id = "device_b", name = "Device B", rssi = "-63 dBm"),
                BluetoothDeviceItem(id = "device_c", name = "Device C", rssi = "-77 dBm")
            )
        )
    }

    val filteredDevices = remember(discoveredDevices, searchQuery) {
        if (searchQuery.isBlank()) {
            discoveredDevices
        } else {
            discoveredDevices.filter {
                it.name.contains(searchQuery, ignoreCase = true) || it.id.contains(
                    searchQuery,
                    ignoreCase = true
                )
            }
        }
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



    // Tự động dừng quét khi Composable bị hủy
    DisposableEffect(Unit) {
        onDispose {
            bluetoothManager.stopScan()
        }
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

                                    isScanning = true
                                    Toast.makeText(
                                        context,
                                        "Đang quét thiết bị BLE xung quanh...",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    bluetoothManager.startScan(
                                        onDeviceFound = { bleDevice ->
                                            val displayName = bleDevice.name
                                                ?: "Thiết bị (${bleDevice.address.takeLast(5)})"
                                            val rssiText = "${bleDevice.rssi} dBm"
                                            val newItem = BluetoothDeviceItem(
                                                id = bleDevice.address,
                                                name = displayName,
                                                rssi = rssiText
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
                                    Toast.makeText(
                                        context,
                                        "Cần cấp quyền Bluetooth để quét thiết bị.",
                                        Toast.LENGTH_SHORT
                                    ).show()
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
                    containerColor = primaryBlue,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Scan Bluetooth",
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
                    Column(
                        modifier = Modifier.clip(RoundedCornerShape(14.dp))
                    ) {
                        filteredDevices.forEachIndexed { index, device ->
                            val isSelected = device.id == selectedDeviceId

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) lightBlueBg else Color.White)
                                    .clickable { selectedDeviceId = device.id }
                                    .padding(horizontal = 16.dp, vertical = 15.dp),
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
                                    text = device.rssi,
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
                    onClick = { isConnected = true },
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
                    onClick = { isConnected = false },
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
                            color = if (isConnected) statusGreen else Color.Gray,
                            shape = CircleShape
                        )
                )
                Text(
                    text = if (isConnected) "Status: Connected" else "Status: Disconnected",
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
                    Text(
                        text = receivedData,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = terminalText
                    )
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

private fun Context.findActivity(): ComponentActivity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is ComponentActivity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}