# Bài tập thực hành KMP (Hands-on Exercises)

Tài liệu này tổng hợp các bài tập thực hành theo từng tuần dành cho dự án **Kotlin Multiplatform (KMP)**. Tất cả các bài tập đều được thiết kế để bạn tự tay viết code và kiểm chứng trực tiếp trên thiết bị Android & iOS.

::: tip 💡 ĐÃ CÓ MÃ NGUỒN MẪU THAM KHẢO HOÀN CHỈNH
Toàn bộ các bài tập dưới đây đều đã được hiện thực mẫu hoàn chỉnh trong dự án `demo/kmp/`:
- **Giao diện & Navigation Native:**
  - **Android:** Native Jetpack Compose trong `demo/kmp/androidApp/src/main/java/.../ui/` (`AppNavigation.kt`, `HomePage.kt`, `ApiTestPage.kt`, `DeviceMonitorPage.kt`).
  - **iOS:** Native SwiftUI trong `demo/kmp/iosApp/iosApp/UI/` (`AppRoot.swift`, `HomePage.swift`, `ApiTestPage.swift`, `DeviceMonitorPage.swift`).
- **Nghiệp vụ & Services:** `sharedLogic/src/commonMain/kotlin/.../` (`BluetoothManager`, `ApiClient`, `BatteryProvider`, `NetworkProvider`, `FileLogger`, `BackgroundMonitor`) kèm triển khai thực tế trên Android SDK (`androidMain`) và Apple iOS SDK (`iosMain`).
- **CarMD Protocol:** Thuật toán đóng gói nhị phân và đọc thông tin dongle trong `CarMdToolClient.kt` (`sharedLogic/commonMain`).
- **Host Apps:** Android `MainActivity.kt` kết nối trực tiếp `sharedLogic` và iOS `ContentView.swift` nhúng `AppRoot()` kết nối `SharedLogic.framework`.

Chi tiết thuyết minh xem tại: [**Hiện thực Mã nguồn Mẫu (KMP Implementation Guide)**](/kmp/implementation).
:::

---

## 🛠️ Tuần 1: Fundamentals & Kiến trúc Shared Logic với Native UI

### Bài tập 1.1: Thiết lập Shared Logic và tích hợp Native UI (Jetpack Compose & SwiftUI)
- **Mục tiêu:** Xây dựng cấu trúc KMP chia sẻ logic, kết nối giao diện Native 100% trên cả Android và iOS.
- **Thư mục làm việc:** `demo/kmp/sharedLogic`, `demo/kmp/androidApp`, `demo/kmp/iosApp`
- **Yêu cầu:**
  1. Cấu hình Gradle KMP với `sharedLogic` chứa các Source Sets: `commonMain`, `androidMain`, `iosMain`.
  2. Trên Android: Xây dựng UI Native bằng Jetpack Compose với `ModalNavigationDrawer`, thanh `AppTopBar` màu xanh `PrimaryBlue` (`0xFF2563EB`) tích hợp `statusBarsPadding()`.
  3. Trên iOS: Xây dựng UI Native bằng SwiftUI với `AppRoot`, menu trượt `SideMenu` từ cạnh trái, thanh tiêu đề `AppHeader` phủ qua Safe Area.
  4. Kiểm chứng biên dịch thành công độc lập: `./gradlew :androidApp:assembleDebug` và `xcodebuild` trên iOS.

### Bài tập 1.2: Quản lý trạng thái chia sẻ với StateFlow
- **Mục tiêu:** Sử dụng `StateFlow` trong `sharedLogic` để phát dữ liệu trạng thái và tiêu thụ trên UI Native của từng nền tảng.
- **Yêu cầu:**
  1. Khai báo data class trạng thái trong `sharedLogic/commonMain`:
     ```kotlin
     data class DashboardUiState(
         val platformName: String = "",
         val batteryLevel: Int = 100,
         val isCharging: Boolean = false,
         val networkType: String = "Unknown",
         val isBleScanning: Boolean = false
     )
     ```
  2. Phát dữ liệu qua `StateFlow<DashboardUiState>` trong service hoặc ViewModel.
  3. Lắng nghe trạng thái:
     - Trên Android: sử dụng `collectAsState()` trong Composable.
     - Trên iOS: quan sát luồng từ Swift kết hợp SwiftUI State.

---

## 🌐 Tuần 2: Shared Network & File Logging

### Bài tập 2.1: Cấu hình Ktor Client đa nền tảng
- **Mục tiêu:** Tạo HTTP Client duy nhất trong `sharedLogic` với HttpEngine phù hợp từng OS.
- **Thư mục làm việc:** `demo/kmp/sharedLogic/src/commonMain/kotlin`
- **Yêu cầu:**
  1. Khai báo hàm factory `createHttpClient()` bằng `expect/actual`:
     - Android engine: `OkHttp` (cấu hình connection timeout 10s).
     - iOS engine: `Darwin` (cấu hình NSURLSessionConfiguration).
  2. Cài đặt plugin `ContentNegotiation` với `kotlinx.serialization.json`.
  3. Viết hàm gọi HTTP GET đến `https://httpbin.org/get` và HTTP POST đến `https://httpbin.org/post` kèm JSON body.

### Bài tập 2.2: Ghi và đọc file JSONL với Okio
- **Mục tiêu:** Lưu trữ lịch sử sự kiện phần cứng dưới dạng các dòng JSON (JSON Lines).
- **Yêu cầu:**
  1. Sử dụng thư viện `okio.FileSystem` trong `commonMain`.
  2. Viết class `FileLogger`:
     - `fun appendLog(event: LogEntry)`: ghi thêm 1 dòng JSON vào cuối file `device_monitor.jsonl`.
     - `fun readAllLogs(): List<LogEntry>`: đọc toàn bộ file và parse thành danh sách objects.
     - `fun clearLogs()`: xóa sạch nội dung file.
  3. Hiển thị danh sách log lên giao diện `LogViewerScreen` với LazyColumn.

---

## 🔋 Tuần 3: Platform Device Providers & Permissions

### Bài tập 3.1: Triển khai BatteryProvider (Android vs iOS)
- **Mục tiêu:** Đọc trạng thái pin thật của thiết bị và phát dữ liệu dạng Reactive Flow.
- **Yêu cầu:**
  1. Trong `sharedLogic/commonMain`, định nghĩa interface `BatteryProvider`:
     ```kotlin
     interface BatteryProvider {
         val batteryLevel: StateFlow<Int>
         val isCharging: StateFlow<Boolean>
     }
     ```
  2. Trong `androidMain`: sử dụng `BroadcastReceiver` đăng ký lắng nghe `Intent.ACTION_BATTERY_CHANGED`.
  3. Trong `iosMain`: bật `UIDevice.currentDevice.batteryMonitoringEnabled = true`, đọc `batteryLevel` và `batteryState`.

### Bài tập 3.2: Triển khai NetworkProvider với Flow
- **Mục tiêu:** Phát hiện kết nối Wi-Fi, 4G/5G hoặc Mất mạng theo thời gian thực.
- **Yêu cầu:**
  1. Android: Đăng ký `ConnectivityManager.NetworkCallback`.
  2. iOS: Khởi tạo `NWPathMonitor` và đăng ký callback trên `dispatch_get_main_queue()`.
  3. Đẩy thông tin trạng thái mạng vào `StateFlow<NetworkStatus>`.

---

## 📡 Tuần 4: Bluetooth BLE 9 Bước & Background Monitor

### Bài tập 4.1: Xây dựng BluetoothManager 9 bước trên KMP
- **Mục tiêu:** Thực hiện kết nối BLE hoàn chỉnh từ quét thiết bị đến đọc Characteristic.
- **Yêu cầu hoàn thành 9 bước:**
  1. **Step 1 — Check & Request Permission:** Kiểm tra quyền Bluetooth.
  2. **Step 2 — Verify Adapter State:** Đảm bảo Bluetooth đang bật (`PoweredOn` / `STATE_ON`).
  3. **Step 3 — Start Scanning:** Quét tìm các thiết bị lân cận và hiển thị tên, RSSI.
  4. **Step 4 — Stop Scanning:** Dừng quét khi người dùng chọn thiết bị hoặc sau 10s timeout.
  5. **Step 5 — Connect Device:** Khởi tạo kết nối GATT.
  6. **Step 6 — Discover Services:** Khám phá danh sách Services và Characteristics.
  7. **Step 7 — Subscribe Notification:** Đăng ký nhận notify từ Characteristic mong muốn.
  8. **Step 8 — Read Data:** Đọc và giải mã mảng byte thành dữ liệu text/số.
  9. **Step 9 — Disconnect & Cleanup:** Ngắt kết nối sạch sẽ, giải phóng tài nguyên.

### Bài tập 4.2: Tích hợp Background Monitor & Kiểm thử 5 trạng thái
- **Mục tiêu:** Giữ tiến trình giám sát thiết bị chạy nền định kỳ mỗi 60 giây và ghi log JSONL.
- **Yêu cầu:**
  1. Android: Cấu hình `ForegroundService` với Persistent Notification.
  2. iOS: Cấu hình `BGTaskScheduler` và quyền `bluetooth-central` background mode trong `Info.plist`.
  3. Thực hiện kiểm thử và ghi lại kết quả theo ma trận:
     - Trạng thái 1: Ứng dụng đang mở (Foreground) -> Ghi log đều đặn.
     - Trạng thái 2: Nhấn Home xuống nền (Background) -> Ghi log tiếp tục.
     - Trạng thái 3: Mở lại ứng dụng -> Dữ liệu UI đồng bộ ngay lập tức.
     - Trạng thái 4: Chờ OS thu hồi bộ nhớ (Memory Pressure) -> Tự phục hồi khi mở lại.
     - Trạng thái 5: Người dùng vuốt tắt ứng dụng (Force Kill) -> Ghi nhận mốc kết thúc sạch.
