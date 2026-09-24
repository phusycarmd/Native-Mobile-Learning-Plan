# Bảng đối chiếu song song Android vs iOS

Bảng tra cứu đối chiếu 1-1 giúp lập trình viên nhanh chóng chuyển đổi tư duy và cú pháp giữa **Android (Kotlin)** và **iOS (Swift)** trong quá trình thực hiện dự án.

---

## 1. Ngôn ngữ & Khái niệm cốt lõi

| Khái niệm | <span class="badge-android">Android (Kotlin)</span> | <span class="badge-ios">iOS (Swift)</span> |
| :--- | :--- | :--- |
| **Biến không đổi (Hằng số)** | [`val`](https://kotlinlang.org/docs/basic-syntax.html#variables) `x = 10` | [`let`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/thebasics/#Constants-and-Variables) `x = 10` |
| **Biến có thể gán lại** | [`var`](https://kotlinlang.org/docs/basic-syntax.html#variables) `y = 20` | [`var`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/thebasics/#Constants-and-Variables) `y = 20` |
| **An toàn Null / Optional** | [`String?`](https://kotlinlang.org/docs/null-safety.html) (gọi an toàn: `name?.length`, gán mặc định: [`?:`](https://kotlinlang.org/docs/null-safety.html#elvis-operator) `"N/A"`) | [`String?`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/thebasics/#Optionals) (mở gói an toàn: [`if let`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/thebasics/#Optional-Binding) `name = name`, gán mặc định: [`??`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/basicoperators/#Nil-Coalescing-Operator) `"N/A"`) |
| **Khai báo Interface / Protocol** | [`interface`](https://kotlinlang.org/docs/interfaces.html) `OnDeviceSelectedListener` | [`protocol`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/protocols/) `DeviceSelectedDelegate: AnyObject` |
| **Hàm ẩn danh / Closure** | [Lambda](https://kotlinlang.org/docs/lambdas.html): `{ device -> println(device) }` | [Closure](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/closures/): `{ device in print(device) }` |
| **Lớp dữ liệu thuần túy** | [`data class`](https://kotlinlang.org/docs/data-classes.html) `Device(val name: String)` | [`struct`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/classesandstructures/) `Device { let name: String }` |
| **Kế thừa & Triển khai** | [`class`](https://kotlinlang.org/docs/classes.html) `BleManager : BaseManager(), BleInterface` | [`class`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/classesandstructures/) `BleManager: BaseManager, BleProtocol` |

---

## 2. Vòng đời Hệ thống (Host / OS) & Vòng đời Màn hình Native (Declarative UI)

::: tip 💡 PHÂN BIỆT RÕ: VÒNG ĐỜI HỆ ĐIỀU HÀNH VS VÒNG ĐỜI MÀN HÌNH NATIVE
Trong kiến trúc Native hiện đại (**Single-Activity** trên Android và **SwiftUI App** trên iOS):
- **Activity (`onCreate`) / UIViewController (`viewDidLoad`)**: Là **Vòng đời Cấp Bộ Vỏ Hệ Điều Hành (Platform Host)**, chỉ khởi tạo **đúng 1 lần duy nhất** khi mở ứng dụng để làm "sân khấu" chứa giao diện.
- **Màn hình giao diện thực tế**: Là các hàm `@Composable` (Android) hoặc `View` struct (iOS). Chúng **không có hàm `onCreate` hay `viewDidLoad` riêng**, mà tuân theo chu trình **Enter Composition ➔ Recomposition ➔ Leave Composition**.
:::

### 2.1. Vòng đời Cấp Bộ vỏ Hệ điều hành (Host / OS Window Lifecycle)
Dùng để quản lý tiến trình, tài nguyên hệ thống khi ứng dụng bật, xoay màn hình, thu nhỏ ra màn hình chính (Background) hoặc tắt hẳn:

| Giai đoạn hệ thống | <span class="badge-android">Android ComponentActivity</span> | <span class="badge-ios">iOS UIViewController / Scene</span> | Ý nghĩa & Hành vi hệ thống |
| :--- | :--- | :--- | :--- |
| **Khởi tạo ứng dụng / Cửa sổ** | [`onCreate(savedInstanceState)`](https://developer.android.com/reference/android/app/Activity#onCreate(android.os.Bundle)) | [`viewDidLoad()`](https://developer.apple.com/documentation/uikit/uiviewcontroller/1621495-viewdidload) | Chạy 1 lần duy nhất khi mở app để gắn `setContent { }` hoặc `UIHostingController`. |
| **Ứng dụng sẵn sàng hiển thị** | [`onStart()`](https://developer.android.com/reference/android/app/Activity#onStart()) | [`viewWillAppear(animated)`](https://developer.apple.com/documentation/uikit/uiviewcontroller/1621485-viewwillappear) | Cửa sổ UI bắt đầu hiện ra trước mắt người dùng. |
| **Ứng dụng đang tương tác (Foreground)** | [`onResume()`](https://developer.android.com/reference/android/app/Activity#onResume()) | [`viewDidAppear(animated)`](https://developer.apple.com/documentation/uikit/uiviewcontroller/1621443-viewdidappear) / Scene `.active` | Ứng dụng ở trạng thái tích cực nhất (chạy animation, quét BLE, camera preview). |
| **Người dùng rời khỏi app (Ấn Home / Đa nhiệm)** | [`onPause()`](https://developer.android.com/reference/android/app/Activity#onPause()) | [`viewWillDisappear(animated)`](https://developer.apple.com/documentation/uikit/uiviewcontroller/1621485-viewwilldisappear) / Scene `.inactive` | Tạm dừng các tác vụ nặng, hạ tần suất quét BLE để tiết kiệm pin. |
| **Ứng dụng bị che khuất hoàn toàn (Chạy nền)** | [`onStop()`](https://developer.android.com/reference/android/app/Activity#onStop()) | [`viewDidDisappear(animated)`](https://developer.apple.com/documentation/uikit/uiviewcontroller/1621477-viewdiddisappear) / Scene `.background` | App đã xuống nền; hệ điều hành có thể thu hồi tài nguyên nếu thiếu RAM. |
| **Ứng dụng bị đóng / Tiêu hủy** | [`onDestroy()`](https://developer.android.com/reference/android/app/Activity#onDestroy()) | [`deinit`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/deinitialization/) | Người dùng vuốt tắt app khỏi đa nhiệm; giải phóng toàn bộ tài nguyên. |

### 2.2. Vòng đời Màn hình thực tế trong Declarative UI (Jetpack Compose vs SwiftUI)
Dùng trong từng màn hình cụ thể (`LoginScreen`, `HomeScreen`, `DetailScreen`...) khi chuyển trang hoặc điều hướng:

| Tình huống vòng đời màn hình | <span class="badge-android">Jetpack Compose (@Composable)</span> | <span class="badge-ios">SwiftUI (View struct)</span> | Cách xử lý chuẩn |
| :--- | :--- | :--- | :--- |
| **Vào màn hình ➔ Chạy tác vụ async 1 lần** | [`LaunchedEffect(Unit) { ... }`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary#LaunchedEffect(kotlin.Any,kotlin.coroutines.SuspendFunction1)) | [`.task { await ... }`](https://developer.apple.com/documentation/swiftui/view/task(priority:_:)) | Tự động chạy khi vào màn hình và **tự động cancel** nếu người dùng bấm Back trước khi xong. |
| **Vào màn hình ➔ Kích hoạt giao diện** | `LifecycleEventEffect(Lifecycle.Event.ON_RESUME)` | [`.onAppear { ... }`](https://developer.apple.com/documentation/swiftui/view/onappear(perform:)) | Kích hoạt hiệu ứng hoặc log analytics xem màn hình. |
| **Rời màn hình ➔ Dọn dẹp tài nguyên (Bắt buộc)** | [`DisposableEffect(Unit) { onDispose { } }`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary#DisposableEffect(kotlin.Any,kotlin.Function1)) | [`.onDisappear { ... }`](https://developer.apple.com/documentation/swiftui/view/ondisappear(perform:)) | Ngắt kết nối BLE, unregister Broadcast Receiver, đóng file handle. |
| **Lắng nghe App ra ngoài Home / Quay lại** | `LifecycleResumeEffect` / `LifecycleStartEffect` | `@Environment(\.scenePhase) private var phase` | Tự động tạm dừng / khôi phục quét thiết bị theo trạng thái app. |
| **Đọc dữ liệu Flow an toàn theo vòng đời** | `.collectAsStateWithLifecycle()` | `@Observable` / `@StateObject` + `.task` | Dừng lắng nghe khi app xuống nền để tránh lãng phí CPU và pin. |

---

## 3. Quản lý tác vụ bất đồng bộ & UI Thread

| Tính năng | <span class="badge-android">Android (Kotlin Coroutines)</span> | <span class="badge-ios">iOS (Swift Concurrency)</span> |
| :--- | :--- | :--- |
| **Bắt đầu tác vụ async** | [`lifecycleScope.launch`](https://developer.android.com/topic/libraries/architecture/coroutines#lifecyclescope) | [`Task { ... }`](https://developer.apple.com/documentation/swift/task) |
| **Chạy ở luồng nền (IO / Background)** | [`withContext(Dispatchers.IO)`](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-dispatchers/-i-o.html) | Chạy trong hàm [`async`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/concurrency/) hoặc detached [`Task`](https://developer.apple.com/documentation/swift/task) |
| **Chuyển về Main Thread cập nhật UI** | [`withContext(Dispatchers.Main)`](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-dispatchers/-main.html)<br>hoặc [`runOnUiThread { ... }`](https://developer.android.com/reference/android/app/Activity#runOnUiThread(java.lang.Runnable)) | [`await MainActor.run { ... }`](https://developer.apple.com/documentation/swift/mainactor)<br>hoặc [`DispatchQueue.main.async`](https://developer.apple.com/documentation/dispatch/dispatchqueue/1781006-main) |
| **Tạm dừng không block luồng** | [`delay(1000)`](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/delay.html) | [`try await Task.sleep(...)`](https://developer.apple.com/documentation/swift/task/sleep(nanoseconds:)) |

---

## 4. Quyền hạn (Permissions) & File cấu hình

| Mục tiêu xin quyền | <span class="badge-android">Android</span> (Khai báo Manifest & Runtime API) | <span class="badge-ios">iOS</span> (Khai báo Info.plist & API) |
| :--- | :--- | :--- |
| **File cấu hình gốc** | [`AndroidManifest.xml`](https://developer.android.com/guide/topics/manifest/manifest-intro) | [`Info.plist`](https://developer.apple.com/documentation/bundleresources/information_property_list) |
| **Quét Bluetooth** | [`<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />`](https://developer.android.com/reference/android/Manifest.permission#BLUETOOTH_SCAN) | [`<key>NSBluetoothAlwaysUsageDescription</key>`](https://developer.apple.com/documentation/bundleresources/information_property_list/nsbluetoothalwaysusagedescription) |
| **Kết nối Bluetooth** | [`<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />`](https://developer.android.com/reference/android/Manifest.permission#BLUETOOTH_CONNECT) | [`<key>NSBluetoothAlwaysUsageDescription</key>`](https://developer.apple.com/documentation/bundleresources/information_property_list/nsbluetoothalwaysusagedescription) |
| **Vị trí (BLE cũ < Android 12)** | [`ACCESS_FINE_LOCATION`](https://developer.android.com/reference/android/Manifest.permission#ACCESS_FINE_LOCATION) | [`NSLocationWhenInUseUsageDescription`](https://developer.apple.com/documentation/bundleresources/information_property_list/nslocationwheninuseusagedescription) (iOS không yêu cầu quyền vị trí khi chỉ dùng BLE) |
| **Camera** | [`<uses-permission android:name="android.permission.CAMERA" />`](https://developer.android.com/reference/android/Manifest.permission#CAMERA) | [`<key>NSCameraUsageDescription</key>`](https://developer.apple.com/documentation/bundleresources/information_property_list/nscamerausagedescription) |
| **Thư viện ảnh** | [`READ_MEDIA_IMAGES`](https://developer.android.com/reference/android/Manifest.permission#READ_MEDIA_IMAGES) (hoặc dùng [`PhotoPicker`](https://developer.android.com/training/data-storage/shared/photopicker) không cần quyền) | [`<key>NSPhotoLibraryUsageDescription</key>`](https://developer.apple.com/documentation/bundleresources/information_property_list/nsphotolibraryusagedescription) (hoặc dùng [`PHPicker`](https://developer.apple.com/documentation/photokit/phpickerviewcontroller) không cần quyền) |
| **Mạng Internet** | [`<uses-permission android:name="android.permission.INTERNET" />`](https://developer.android.com/reference/android/Manifest.permission#INTERNET) | Tự động cho phép HTTPS (chỉ cấu hình [`NSAppTransportSecurity`](https://developer.apple.com/documentation/bundleresources/information_property_list/nsapptransportsecurity) nếu dùng HTTP) |

---

## 5. Các thành phần phần cứng & Dịch vụ chính

| Khối chức năng | <span class="badge-android">Android APIs</span> | <span class="badge-ios">iOS APIs</span> |
| :--- | :--- | :--- |
| **BLE Scanner** | [`BluetoothLeScanner.startScan(callback)`](https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner) | [`CBCentralManager.scanForPeripherals(...)`](https://developer.apple.com/documentation/corebluetooth/cbcentralmanager/1518986-scanforperipherals) |
| **BLE GATT Client** | [`device.connectGatt(context, false, callback)`](https://developer.android.com/reference/android/bluetooth/BluetoothDevice#connectGatt(android.content.Context,boolean,android.bluetooth.BluetoothGattCallback)) | [`centralManager.connect(peripheral, options:)`](https://developer.apple.com/documentation/corebluetooth/cbcentralmanager/1518765-connect) |
| **BLE Notify** | [`gatt.setCharacteristicNotification(char, true)`](https://developer.android.com/reference/android/bluetooth/BluetoothGatt#setCharacteristicNotification(android.bluetooth.BluetoothGattCharacteristic,%20boolean)) + ghi CCCD [`0x2902`](https://developer.android.com/reference/android/bluetooth/BluetoothGattDescriptor#ENABLE_NOTIFICATION_VALUE) | [`peripheral.setNotifyValue(true, for: char)`](https://developer.apple.com/documentation/corebluetooth/cbperipheral/1518949-setnotifyvalue) |
| **Chụp ảnh Camera** | [`ActivityResultContracts.TakePicturePreview()`](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.TakePicturePreview) | [`UIImagePickerController(sourceType: .camera)`](https://developer.apple.com/documentation/uikit/uiimagepickercontroller) |
| **Chọn ảnh Thư viện** | [`ActivityResultContracts.PickVisualMedia()`](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.PickVisualMedia) | [`PHPickerViewController`](https://developer.apple.com/documentation/photokit/phpickerviewcontroller) (PhotosUI) |
| **Gọi REST API** | [`OkHttpClient`](https://square.github.io/okhttp/) hoặc [`HttpURLConnection`](https://developer.android.com/reference/java/net/HttpURLConnection) | [`URLSession.shared.data(for: request)`](https://developer.apple.com/documentation/foundation/urlsession) |
| **Đọc mức pin** | Nhận Intent [`Intent.ACTION_BATTERY_CHANGED`](https://developer.android.com/reference/android/content/Intent#ACTION_BATTERY_CHANGED) | [`UIDevice.current.batteryLevel`](https://developer.apple.com/documentation/uikit/uidevice) |
| **Đọc trạng thái mạng** | [`ConnectivityManager`](https://developer.android.com/reference/android/net/ConnectivityManager) + [`NetworkCapabilities`](https://developer.android.com/reference/android/net/NetworkCapabilities) | [`Network.framework`](https://developer.apple.com/documentation/network) ([`NWPathMonitor`](https://developer.apple.com/documentation/network/nwpathmonitor)) |
| **Lưu trữ file JSONL** | [`File`](https://developer.android.com/reference/java/io/File)(context.[`filesDir`](https://developer.android.com/reference/android/content/Context#getFilesDir()), "log.jsonl").[`appendText(...)`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.io/append-text.html) | [`FileManager`](https://developer.apple.com/documentation/foundation/filemanager).default.urls(for: .[`documentDirectory`](https://developer.apple.com/documentation/foundation/filemanager/searchpathdirectory/documentdirectory), ...) |
| **Tiến trình nền** | [ForegroundService](https://developer.android.com/develop/background-work/services/foreground-services) + Notification liên tục | [`BGTaskScheduler`](https://developer.apple.com/documentation/backgroundtasks/bgtaskscheduler) (không đảm bảo chu kỳ cố định) |
