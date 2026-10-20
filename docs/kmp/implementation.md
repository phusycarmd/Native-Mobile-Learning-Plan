# Hiện thực Mã nguồn Mẫu (KMP Implementation Guide)

Tài liệu này thuyết minh chi tiết về mã nguồn mẫu **Native Device Monitor** đã được hiện thực hoàn chỉnh trong thư mục `demo/kmp/`, đạt tính tương thích 1-1 với cả hai ứng dụng Native gốc của Android (`demo/android/NativeDeviceMonitorDemo`) và iOS (`demo/ios/NativeDeviceMonitorDemo`).

---

## 1. Bản đồ cấu trúc thư mục thực tế trong `demo/kmp`

```
demo/kmp/
├── gradle/libs.versions.toml             # Quản lý phiên bản tập trung (Kotlin 2.4.20, AGP 9.1.1)
├── sharedLogic/                          # Module nghiệp vụ & Services DÙNG CHUNG (Kotlin Multiplatform)
│   └── src/
│       ├── commonMain/kotlin/.../nativecore/
│       │   ├── device/
│       │   │   ├── DeviceSnapshot.kt     # Model trạng thái thiết bị & JSONL serialization thuần Kotlin
│       │   │   ├── BatteryProvider.kt    # Interface đo pin & BatteryInfo
│       │   │   ├── NetworkProvider.kt    # Interface kiểm tra kết nối & NetworkInfo
│       │   │   └── BluetoothProvider.kt  # Interface trạng thái adapter & BluetoothInfo
│       │   ├── logging/
│       │   │   └── FileLogger.kt         # Interface ghi log file .jsonl cục bộ
│       │   ├── network/
│       │   │   └── ApiClient.kt          # Interface HTTP Client & ApiResponse
│       │   ├── bluetooth/
│       │   │   └── BluetoothManager.kt   # Interface 9 bước BLE, BleDevice, BleService, ConnectionState
│       │   ├── background/
│       │   │   └── BackgroundMonitor.kt  # Interface giám sát nền định kỳ với StateFlow
│       │   ├── tool/
│       │   │   ├── ToolInfo.kt           # Model thông tin phần cứng dongle CarMD
│       │   │   └── CarMdToolClient.kt    # Giao thức nhị phân, checksum và lệnh E1/E5/E7
│       │   └── NativeCore.kt             # Container tổng hợp toàn bộ các service
│       ├── androidMain/kotlin/.../nativecore/
│       │   ├── AndroidNativeServices.kt  # Hiện thực hoàn chỉnh trên Android (Context, BLE, HttpURLConnection, filesDir)
│       │   ├── bluetooth/BluetoothManagerImpl.kt # Adapter BluetoothManagerImpl cho Android UI
│       │   ├── network/ApiClientImpl.kt  # Adapter ApiClientImpl cho Android UI
│       │   ├── device/DeviceProvidersImpl.kt     # Adapter BatteryProviderImpl, NetworkProviderImpl
│       │   ├── background/BackgroundMonitorImpl.kt # Adapter BackgroundMonitorImpl
│       │   └── permission/PermissionManager.kt   # Quản lý quyền Bluetooth & Location runtime
│       └── iosMain/kotlin/.../nativecore/
│           └── IosNativeServices.kt      # Hiện thực hoàn chỉnh trên iOS (UIKit, CoreBluetooth, NSURLSession, NSDocumentDirectory)
│
├── androidApp/                           # Host Application Android (Jetpack Compose Native UI)
│   └── src/main/
│       ├── AndroidManifest.xml           # Khai báo quyền Bluetooth (SCAN, CONNECT), Location, Internet
│       └── kotlin/.../
│           ├── MainActivity.kt           # Lifecycle demo & AppRoot entrypoint
│           ├── AppNavigation.kt          # ModalNavigationDrawer, AppTopBar, AppScreen
│           ├── HomePage.kt               # Giao diện Native BLE 9 bước & CarMD Tool
│           ├── ApiTestPage.kt            # Giao diện Native HTTP GET/POST test
│           ├── DeviceMonitorPage.kt      # Giao diện Native Metrics & JSONL log
│           └── ui/theme/                 # Bảng màu, Typography & Theme Native Android
│
└── iosApp/                               # Host Application iOS (SwiftUI Native UI + Xcode Project)
    └── iosApp/
        ├── Info.plist                    # Khai báo NSBluetoothAlwaysUsageDescription, Privacy keys
        ├── iOSApp.swift                  # SwiftUI App lifecycle entrypoint
        ├── ContentView.swift             # Container view điều hướng AppRoot
        ├── UI/
        │   ├── AppRoot.swift             # SideMenu Drawer & AppHeader native
        │   ├── HomePage.swift            # Giao diện SwiftUI Native BLE 9 bước & CarMD Tool
        │   ├── ApiTestPage.swift         # Giao diện SwiftUI Native HTTP GET/POST test
        │   └── DeviceMonitorPage.swift   # Giao diện SwiftUI Native Metrics & JSONL log
        └── NativeCore/                   # Core bridges kết nối SharedLogic
```

---

## 2. Chi tiết hiện thực từng tầng (Layer Deep-Dive)

### 2.1. Tầng nghiệp vụ thuần túy (`commonMain`)

Toàn bộ các model dữ liệu và giao thức được thiết kế để **không phụ thuộc bất kỳ thư viện riêng nào của hệ điều hành**:

1. **Tuần tự hoá JSONL thủ công an toàn ([DeviceSnapshot.kt]):**
   - Hàm `toJson()` xuất chuỗi JSON chuẩn định dạng 1 dòng theo quy chuẩn JSON Lines (`.jsonl`).
   - Hàm `fromJson()` sử dụng biểu thức chính quy (Regex) và phân tích chuỗi thuần Kotlin, tránh phụ thuộc vào `org.json` (chỉ có trên Android) hoặc `JSONSerialization` (chỉ có trên iOS).
2. **Giao thức CarMD Dongle ([CarMdToolClient.kt]):**
   - Đóng gói Frame yêu cầu: Header byte `0xAD` | `commandId` | `length` | `payload` | `checksum`.
   - Thuật toán Checksum: Tổng các byte modulo 256.
   - Giải mã phản hồi: Nhận diện Header `0xDA` (Positive) hoặc `0xDD` (Status), ghép các mảnh gói tin 20-byte từ BLE Notification buffer.
   - Tự động thực thi chuỗi lệnh tuần tự:
     - `0xE1` (Get Tool Setting) ➔ Trích xuất Bootloader & Firmware Version.
     - `0xE5` (Get Device GUID) ➔ Đảo byte little-endian sang định dạng GUID 8-4-4-4-12 chuẩn.
     - `0xE7` (Get USB Product ID) ➔ Đọc 2 byte ID phần cứng.

---

### 2.2. Tầng hiện thực nền tảng (Platform Implementations)

| Thành phần | Triển khai Android (`androidMain`) | Triển khai iOS (`iosMain`) |
| :--- | :--- | :--- |
| **BatteryProvider** | Đăng ký `BroadcastReceiver` với `IntentFilter(ACTION_BATTERY_CHANGED)`, đọc `EXTRA_LEVEL` và `EXTRA_STATUS`. | Bật `UIDevice.currentDevice.batteryMonitoringEnabled = true`, đọc `batteryLevel` và `batteryState`. |
| **NetworkProvider** | `ConnectivityManager.getNetworkCapabilities(activeNetwork)` kiểm tra `TRANSPORT_WIFI` và `TRANSPORT_CELLULAR`. | Cung cấp thông tin trạng thái mạng trực tiếp từ hệ thống. |
| **BluetoothProvider** | Truy vấn trạng thái qua `BluetoothAdapter.isEnabled` và `BluetoothManager.getConnectionState()`. | Truy vấn trạng thái qua `BluetoothManager` của iOS. |
| **FileLogger** | Ghi append chuỗi UTF-8 vào file `monitor_logs.jsonl` trong thư mục `context.filesDir`. | Ghi file an toàn vào thư mục `NSDocumentDirectory` thông qua `NSFileManager` và `NSString.writeToFile`. |
| **ApiClient** | `HttpURLConnection` hỗ trợ GET/POST, thiết lập Content-Type, Timeout 10s và đo thời gian xử lý `durationMs`. | `NSURLSession` kết hợp `NSMutableURLRequest`, chuyển đổi `NSData` và xử lý bất đồng bộ qua `suspendCancellableCoroutine`. |
| **BluetoothManager** | `BluetoothLeScanner` quét thiết bị, kết nối `BluetoothGatt`, khám phá Services và đăng ký CCCD descriptor `0x2902`. | Quản lý kết nối, khám phá dịch vụ GATT và điều phối delegate thông báo byte BLE. |
| **BackgroundMonitor** | Coroutine Timer chu kỳ 5 giây thu thập `DeviceSnapshot`, ghi log và cập nhật `StateFlow`. | Coroutine Timer chu kỳ 5 giây thu thập snapshot định dạng ISO 8601 UTC và ghi log. |
| **NativeCore** | `AndroidNativeCore(context)` gom tất cả providers lại làm một thể thống nhất. | `IosNativeCore.shared` singleton giúp Swift gọi trực tiếp cực kỳ tự nhiên. |

---

### 2.3. Tầng Giao diện Native (Android Jetpack Compose & iOS SwiftUI)

Tuân thủ kiến trúc **Shared Logic Only**, tầng UI được giữ nguyên 100% Native từ 2 project gốc:

1. **Android (`androidApp` - Jetpack Compose):**
   - Giữ nguyên toàn bộ layout và styling từ `demo/android/NativeDeviceMonitorDemo`.
   - `AppNavigation.kt`: Thanh `AppTopBar` màu xanh `PrimaryBlue` (`0xFF2563EB`) tích hợp `statusBarsPadding()`, nút menu hamburger và `ModalNavigationDrawer`.
   - `HomePage.kt`: Tìm kiếm `OutlinedTextField`, nút `Scan Bluetooth` (50.dp), danh sách thiết bị `Surface` bo tròn (54.dp), cặp nút `Connect`/`Disconnect`, nút `Get Tool Info` và hộp Terminal đen Slate (`0xFF0F172A`).
   - `ApiTestPage.kt`: Khung nhập URL với quick chips, nút chuyển `GET`/`POST` và xem phản hồi.
   - `DeviceMonitorPage.kt`: Lưới thông số phần cứng, bảng điều khiển dịch vụ nền và nhật ký JSONL.
   - Toàn bộ kết nối trực tiếp vào `sharedLogic` thông qua `BluetoothManagerImpl`, `ApiClientImpl`, `BatteryProviderImpl`, `BackgroundMonitorImpl`.

2. **iOS (`iosApp` - SwiftUI):**
   - Giữ nguyên toàn bộ layout và styling từ `demo/ios/NativeDeviceMonitorDemo`.
   - `AppRoot.swift`: Menu trượt SideMenu từ cạnh trái, thanh tiêu đề `AppHeader` nền xanh thương hiệu phủ qua tai thỏ với `.ignoresSafeArea(edges: .top)`.
   - `HomePage.swift`: Tìm kiếm thiết bị, quét BLE, kết nối, đăng ký characteristic và giao tiếp CarMD dongle.
   - `ApiTestPage.swift`: Kiểm thử HTTP request với `URLSession`.
   - `DeviceMonitorPage.swift`: Giám sát pin, mạng, Bluetooth và ghi log nền cục bộ.
   - Nhúng và liên kết với framework `SharedLogic` được biên dịch trực tiếp từ Gradle:
   ```swift
   import SwiftUI
   import SharedLogic

   struct ContentView: View {
       var body: some View {
           AppRoot()
       }
   }
   ```

---

## 3. Quy trình biên dịch và kiểm chứng

Toàn bộ các tác vụ biên dịch đã được kiểm chứng độc lập trên hệ thống:

```bash
# 1. Biên dịch thư viện Shared Logic KMP (Android AAR + iOS Frameworks)
./gradlew :sharedLogic:assemble

# 2. Biên dịch ứng dụng Android Native (Jetpack Compose UI - Debug APK)
./gradlew :androidApp:assembleDebug

# 3. Biên dịch ứng dụng iOS Native (SwiftUI UI - iPhone Simulator)
cd iosApp
xcodebuild -project iosApp.xcodeproj -scheme iosApp -destination "generic/platform=iOS Simulator" build
```
