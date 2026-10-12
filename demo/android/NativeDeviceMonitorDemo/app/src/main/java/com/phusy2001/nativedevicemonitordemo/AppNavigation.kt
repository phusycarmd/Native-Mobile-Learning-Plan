package com.phusy2001.nativedevicemonitordemo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val PrimaryBlue = Color(0xFF2563EB)
private val LightBlueBg = Color(0xFFEFF4FF)
private val TextPrimary = Color(0xFF1F2937)
private val TextSecondary = Color(0xFF6B7280)

// Các màn hình có trong Side Menu. Thêm màn mới: thêm 1 entry ở đây và 1 nhánh trong AppRoot
enum class AppScreen(val title: String, val subtitle: String) {
    BLUETOOTH("Bluetooth BLE", "Scan, connect, notify"),
    API_CLIENT("API Client", "HTTP GET / POST")
}

@Composable
fun AppRoot() {
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.BLUETOOTH) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    fun openDrawer() {
        // Ẩn bàn phím trước khi mở menu
        focusManager.clearFocus()
        scope.launch { drawerState.open() }
    }

    // ModalNavigationDrawer tự xử lý Back khi menu đang mở.
    // Khi menu đóng: Back ở màn phụ thì quay về màn Bluetooth thay vì thoát app.
    BackHandler(enabled = drawerState.isClosed && currentScreen != AppScreen.BLUETOOTH) {
        currentScreen = AppScreen.BLUETOOTH
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                currentScreen = currentScreen,
                onScreenSelected = { screen ->
                    currentScreen = screen
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Màn Bluetooth luôn nằm trong composition, chỉ bị ẩn khi sang màn khác.
            // Nếu bị huỷ, onDispose của HomePage sẽ ngắt BLE và mất toàn bộ state (thiết bị, dữ liệu notify)
            HomePage(
                modifier = Modifier.hiddenUnless(currentScreen == AppScreen.BLUETOOTH),
                onMenuClick = { openDrawer() }
            )
            if (currentScreen == AppScreen.API_CLIENT) {
                ApiTestPage(onMenuClick = { openDrawer() })
            }
        }
    }
}

// Vẫn giữ trong composition (state và effect tiếp tục chạy), nhưng không được đặt lên màn hình
// nên không vẽ và không nhận chạm
private fun Modifier.hiddenUnless(visible: Boolean): Modifier =
    if (visible) {
        this
    } else {
        layout { measurable, constraints ->
            measurable.measure(constraints)
            layout(0, 0) {}
        }
    }

@Composable
private fun AppDrawerContent(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = Color.White,
        // Header màu xanh tự lo phần status bar, nên bỏ inset mặc định của sheet
        windowInsets = WindowInsets(0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryBlue)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Native Device Monitor",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = "Android · NativeCore Demo",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        AppScreen.entries.forEach { screen ->
            NavigationDrawerItem(
                label = {
                    Column {
                        Text(text = screen.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(text = screen.subtitle, fontSize = 12.sp, color = TextSecondary)
                    }
                },
                selected = screen == currentScreen,
                onClick = { onScreenSelected(screen) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                shape = RoundedCornerShape(12.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = LightBlueBg,
                    unselectedContainerColor = Color.Transparent,
                    selectedTextColor = PrimaryBlue,
                    unselectedTextColor = TextPrimary
                )
            )
        }
    }
}

// Thanh tiêu đề dùng chung cho mọi màn hình: nút mở menu + tiêu đề
@Composable
fun AppTopBar(title: String, onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryBlue)
            .statusBarsPadding()
            .padding(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.semantics { contentDescription = "Mở menu" }
        ) {
            // Vẽ icon hamburger bằng 3 thanh ngang, không cần thêm thư viện material-icons
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(width = 20.dp, height = 2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color.White)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
    }
}
