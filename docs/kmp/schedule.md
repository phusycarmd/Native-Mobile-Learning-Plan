# Lộ trình học KMP chi tiết (Schedule)

Lộ trình đào tạo **Kotlin Multiplatform (KMP)** được thiết kế kéo dài khoảng **4 tuần** (~56 giờ thực hành), tập trung vào mục tiêu chuyển hóa toàn bộ kiến trúc **Native Device Monitor** sang nền tảng chia sẻ mã nguồn KMP theo mô hình **Shared Logic Only & Native UI**.

---

## 1. Phân bổ thời lượng theo tuần

| Tuần | Giai đoạn | Thời lượng | Cột mốc bàn giao (Milestone) |
| :---: | :--- | :---: | :--- |
| **Tuần 1** | **Fundamentals & Native UI Integration** | ~14 giờ | Thiết lập cấu trúc KMP `sharedLogic`; tích hợp App Shell Native trên Android (Jetpack Compose) và iOS (SwiftUI); quản lý State qua `StateFlow`. |
| **Tuần 2** | **Network & Local File Logging** | ~14 giờ | Tích hợp Ktor Client (OkHttp/Darwin) gọi API GET/POST; ghi log sự kiện JSONL cục bộ bằng `okio`. |
| **Tuần 3** | **Platform Providers & Permissions** | ~14 giờ | Hoàn thành `BatteryProvider`, `NetworkProvider` và `PermissionManager` với Kotlin/Native interop trên iOS. |
| **Tuần 4** | **BLE 9 Bước & Background Monitor** | ~14 giờ | Trọn vẹn luồng BLE 9 bước trên KMP; chạy Background Monitor định kỳ; hoàn tất kiểm thử End-to-End. |
| **Tổng** | **Toàn khóa học KMP** | **~56 giờ** | **Ứng dụng KMP Device Monitor hoàn chỉnh chạy native mượt mà trên Android & iOS.** |

---

## 2. Bảng phân cấp ưu tiên (Priority Order)

<ProgressTracker prefix="kmp-p0" title="Tiến độ 9 mục tiêu cốt lõi (P0)" :total="9" />

### <span class="badge-p0">P0 — Bắt buộc phải hoàn thành (Must Complete)</span>
Các tính năng sống còn quyết định sự thành công của kiến trúc KMP:

<CheckTask id="kmp-p0-1" tag="P0">1. Cấu hình KMP Gradle & Version Catalog (libs.versions.toml, SourceSets commonMain, androidMain, iosMain)</CheckTask>
<CheckTask id="kmp-p0-2" tag="P0">2. Tích hợp Shared Logic vào Base UI Shell Native (Jetpack Compose trên Android & SwiftUI trên iOS)</CheckTask>
<CheckTask id="kmp-p0-3" tag="P0">3. Quản lý trạng thái chia sẻ (StateFlow trong sharedLogic kết hợp ViewModel/State Native)</CheckTask>
<CheckTask id="kmp-p0-4" tag="P0">4. Thiết kế hệ thống Service Interfaces thuần khiết trong commonMain</CheckTask>
<CheckTask id="kmp-p0-5" tag="P0">5. Triển khai BatteryProvider (Android BatteryManager & iOS UIDevice.batteryLevel)</CheckTask>
<CheckTask id="kmp-p0-6" tag="P0">6. Triển khai NetworkProvider (Android ConnectivityManager & iOS NWPathMonitor)</CheckTask>
<CheckTask id="kmp-p0-7" tag="P0">7. Xử lý quyền hệ thống PermissionManager đa nền tảng</CheckTask>
<CheckTask id="kmp-p0-8" tag="P0">8. Trọn vẹn luồng Bluetooth BLE 9 bước trên KMP (Scan, Connect, Discover, Notify, Read, Disconnect)</CheckTask>
<CheckTask id="kmp-p0-9" tag="P0">9. Dịch vụ giám sát nền Background Monitor và ma trận 5 trạng thái OS</CheckTask>

---

### <span class="badge-p1">P1 — Nên hoàn thành (Should Complete)</span>
Các tính năng mở rộng cần thiết cho bức tranh tổng thể:

<CheckTask id="kmp-p1-10" tag="P1">10. Tích hợp Ktor Client cho HTTP API GET & POST kèm kotlinx.serialization (OkHttp cho Android, Darwin cho iOS)</CheckTask>
<CheckTask id="kmp-p1-11" tag="P1">11. Ghi và đọc log file cục bộ theo định dạng JSONL bằng thư viện okio.FileSystem</CheckTask>
<CheckTask id="kmp-p1-12" tag="P1">12. Tích hợp Camera & Media Picker trên từng nền tảng Native</CheckTask>

---

### <span class="badge-p2">P2 — Có thể tinh giản nếu thiếu thời gian (Can Simplify)</span>
Các cải tiến kiến trúc và công cụ nâng cao:

<CheckTask id="kmp-p2-13" tag="P2">13. Cài đặt Dependency Injection tập trung với Koin Multiplatform</CheckTask>
<CheckTask id="kmp-p2-14" tag="P2">14. Viết Unit Test cho Shared Logic bằng kotlin.test và kotlinx-coroutines-test</CheckTask>
<CheckTask id="kmp-p2-15" tag="P2">15. Đóng gói và tối ưu kích thước XCFramework cho ứng dụng iOS</CheckTask>

---

## 3. Kế hoạch chi tiết từng ngày

### Tuần 1: Fundamentals & Tích hợp Shared Logic vào Native UI

- **Ngày 1 (Setup & Gradle Architecture):**
  - Khảo sát module `demo/kmp/sharedLogic`, cấu hình Source Sets `commonMain`, `androidMain`, `iosMain`.
  - Thiết lập Version Catalog (`libs.versions.toml`) cho Kotlin 2.4.20 và Android Gradle Plugin 9.1.1.
  - Kiểm tra lệnh chạy trên Android (`./gradlew :androidApp:assembleDebug`) và mở `demo/kmp/iosApp` trên Xcode.
- **Ngày 2 (Android Jetpack Compose Native UI):**
  - Xây dựng giao diện Native Android trong `androidApp`: `ModalNavigationDrawer`, `AppTopBar` (với `statusBarsPadding()`), `Card`.
  - Ánh xạ trực tiếp từ `demo/android/NativeDeviceMonitorDemo`.
- **Ngày 3 (iOS SwiftUI Native UI):**
  - Xây dựng giao diện Native iOS trong `iosApp`: `AppRoot`, `SideMenu` trượt từ cạnh trái, `AppHeader` phủ Safe Area.
  - Ánh xạ trực tiếp từ `demo/ios/NativeDeviceMonitorDemo`.
- **Ngày 4 (Swift/Kotlin Interop & StateFlow):**
  - Liên kết `SharedLogic.framework` vào project Xcode.
  - Tìm hiểu cơ chế gọi Kotlin interfaces, classes, và quan sát `StateFlow` từ Swift/SwiftUI.
- **Ngày 5 (Review & Milestone 1):**
  - Bàn giao UI App Shell hoàn chỉnh, đồng nhất về tính năng và trải nghiệm Native gốc trên cả Android và iOS.

---

### Tuần 2: Shared Network (Ktor) & Local File Logging (Okio)

- **Ngày 6 (Ktor Client Setup):**
  - Khai báo dependencies Ktor trong `libs.versions.toml`: `ktor-client-core`, `ktor-client-okhttp`, `ktor-client-darwin`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`.
  - Cấu hình HttpEngine riêng cho từng target trong `sharedLogic`.
- **Ngày 7 (ApiClient Implementation):**
  - Triển khai `ApiClient` với hàm `getDeviceStatus()` và `postDeviceLogs()`.
  - Xử lý timeout, network exceptions, status code validation.
- **Ngày 8 (Local Storage với Okio):**
  - Tích hợp `com.squareup.okio:okio` vào `sharedLogic/commonMain`.
  - Lấy đường dẫn thư mục Documents an toàn thông qua `expect/actual`:
    - Android: `context.filesDir.absolutePath`
    - iOS: `NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)`
- **Ngày 9 (FileLogger JSONL):**
  - Xây dựng `FileLogger` hỗ trợ ghi append từng dòng JSONL.
  - Viết hàm đọc lại danh sách log và hàm xóa log file.
- **Ngày 10 (Review & Milestone 2):**
  - Màn hình ApiTestScreen và LogViewerScreen hoạt động đồng bộ trên cả 2 nền tảng.

---

### Tuần 3: Platform Device Providers & Permissions

- **Ngày 11 (Common Provider Interfaces):**
  - Định nghĩa `BatteryProvider`, `NetworkProvider` trong `sharedLogic/commonMain`.
  - Ánh xạ dữ liệu sang `StateFlow<BatteryInfo>` và `StateFlow<NetworkType>`.
- **Ngày 12 (Android Providers):**
  - Triển khai `AndroidBatteryProvider` sử dụng `BroadcastReceiver` với `ACTION_BATTERY_CHANGED`.
  - Triển khai `AndroidNetworkProvider` sử dụng `ConnectivityManager.registerDefaultNetworkCallback`.
- **Ngày 13 (iOS Providers với Kotlin/Native Interop):**
  - Viết trực tiếp mã Kotlin trong `iosMain` gọi Objective-C/Apple APIs:
    - Bật `UIDevice.currentDevice.batteryMonitoringEnabled = true`.
    - Lắng nghe sự kiện qua `NSNotificationCenter`.
    - Khởi tạo `NWPathMonitor` và hàng đợi `dispatch_queue_create`.
- **Ngày 14 (PermissionManager Multiplatform):**
  - Cơ chế xin quyền Bluetooth, Location, Camera trên từng OS.
  - Xử lý callback kết quả quyền và cập nhật UI.
- **Ngày 15 (Review & Milestone 3):**
  - Dashboard hiển thị mức pin và loại kết nối mạng trực tiếp theo thời gian thực từ cảm biến thật của Android và iPhone.

---

### Tuần 4: Bluetooth BLE 9 Bước & Background Monitoring

- **Ngày 16 (BLE Common Domain & State Machine):**
  - Thiết kế State Machine: `BleState` (Disconnected, Scanning, Connecting, Discovering, Subscribed, Error).
  - Định nghĩa interface `BluetoothManager` với 9 bước theo chuẩn của dự án.
- **Ngày 17 (BLE Platform Implementations):**
  - Triển khai `BluetoothLeScanner` trên Android (xử lý `ScanCallback`, `BluetoothGattCallback`).
  - Triển khai `CBCentralManager` và `CBPeripheral` trên iOS trong `iosMain`.
  - Kỹ thuật chuyển đổi delegate callback thành Kotlin Coroutine `Flow` / `suspendCancellableCoroutine`.
- **Ngày 18 (Background Processing Architecture):**
  - Sự khác biệt về vòng đời tiến trình giữa Android và iOS.
  - Android: `ForegroundService` kết hợp thông báo cố định (Ongoing Notification).
  - iOS: Cấu hình `UIBackgroundModes` (`bluetooth-central`, `fetch`, `processing`) và đăng ký `BGTaskScheduler`.
- **Ngày 19 (End-to-End Integration & Matrix Testing):**
  - Kiểm thử ma trận 5 trạng thái:
    1. *Foreground active*
    2. *Background standby*
    3. *Return to foreground*
    4. *System terminated under memory pressure*
    5. *Force killed by user*
- **Ngày 20 (Dọn dẹp mã nguồn & Final Milestone):**
  - Tối ưu hóa code, xóa bỏ TODO, hoàn tất checklist bàn giao demo KMP hoàn chỉnh.
