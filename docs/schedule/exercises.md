# 📝 Tổng hợp 17 Bài tập Thực hành theo Lộ trình 3.5 Tuần

Trang này tổng hợp chi tiết toàn bộ **17 bài tập thực hành theo ngày** xuyên suốt lộ trình đào tạo 3.5 tuần (~56 giờ làm việc). Mỗi bài tập đều được thiết kế sát thực tế sản phẩm, có mục tiêu rõ ràng, mã khung gợi ý (**Starter Code**) cho cả Android (Kotlin) & iOS (Swift), và kết quả mẫu cần đạt được (**Expected Output**).

::: tip 🔒 CHẾ ĐỘ THỰC HÀNH TỰ LỰC (SELF-SOLVE)
Mọi đoạn code mẫu, gợi ý kỹ thuật và kết quả đầu ra đều được **ẩn mặc định**. Hãy tự suy nghĩ và viết code dựa theo **Mục tiêu** và **Đề bài**. Khi hoàn thành hoặc nếu gặp bế tắc, hãy nhấn vào thanh **"💡 Gợi ý & Mã nguồn mẫu (Click để mở)"** để đối chiếu và tự kiểm tra.
:::

::: danger NGUYÊN TẮC THỰC HÀNH CỐT LÕI
**Tự tay viết từng dòng code** trên Android Studio và Xcode để thấu hiểu tường tận cơ chế luồng, sự kiện và vòng đời native. Tuyệt đối không copy-paste code máy móc mà không hiểu bản chất.
:::

---

## Tuần 1: Fundamentals & Native UI

### Bài tập 1.1 (Day 1) — Lọc mảng & Null-Safety trong Kotlin
- **Mục tiêu**: Làm quen cú pháp Kotlin, [`data class`](https://kotlinlang.org/docs/data-classes.html), biểu thức [`lambda`](https://kotlinlang.org/docs/lambdas.html) và toán tử an toàn [`?.let`](https://kotlinlang.org/docs/scope-functions.html#let), [`?:`](https://kotlinlang.org/docs/null-safety.html#elvis-operator).
- **Đề bài**: Tạo `data class Device(val name: String, val rssi: Int?)`. Khởi tạo danh sách 4 thiết bị (có phần tử mang `rssi = null`). Lọc các thiết bị có tín hiệu mạnh hơn `-75 dBm` và in ra màn hình.

:::: details 💡 Gợi ý & Mã nguồn mẫu (Click để mở)

::: code-group
```kotlin [Kotlin (Android - Starter)]
data class Device(val name: String, val rssi: Int?)

fun main() {
    val devices = listOf(
        Device("ESP32_Sensor", -65),
        Device("OBD_Scanner", null),
        Device("Polar_H10", -82),
        Device("Smart_Lock", -70)
    )
    
    // TODO: Dùng filter lọc rssi != null và rssi > -75
    val strongDevices = devices.filter { (it.rssi ?: -999) > -75 }
    
    strongDevices.forEach { dev ->
        println("Thiết bị: ${dev.name}, RSSI: ${dev.rssi} dBm")
    }
}
```

```swift [Swift (iOS - Đối chiếu)]
struct Device {
    let name: String
    let rssi: Int?
}

let devices = [
    Device(name: "ESP32_Sensor", rssi: -65),
    Device(name: "OBD_Scanner", rssi: nil),
    Device(name: "Polar_H10", rssi: -82),
    Device(name: "Smart_Lock", rssi: -70)
]

let strongDevices = devices.compactMap { dev -> Device? in
    guard let rssi = dev.rssi, rssi > -75 else { return nil }
    return dev
}

for dev in strongDevices {
    print("Thiết bị: \(dev.name), RSSI: \(dev.rssi!) dBm")
}
```
:::

- **Kết quả mẫu (Expected Output)**:
```text
Thiết bị: ESP32_Sensor, RSSI: -65 dBm
Thiết bị: Smart_Lock, RSSI: -70 dBm
```

::::

<CheckTask id="w1-d1" tag="Android">Hoàn thành bài tập 1.1: Viết và chạy chương trình Kotlin lọc mảng null-safe.</CheckTask>

---

### Bài tập 1.2 (Day 2) — Mở gói Optional & Closure trong Swift
- **Mục tiêu**: Nắm vững [`struct`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/classesandstructures/) (Value Type), cơ chế [`Optional`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/thebasics/#Optionals), mở gói an toàn bằng [`guard let`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/controlflow/#Early-Exit) / [`if let`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/thebasics/#Optional-Binding), biến đổi mảng với [`compactMap`](https://developer.apple.com/documentation/swift/sequence/compactmap(_:)).
- **Đề bài**: Tạo `struct Device` gồm `name: String` và `rssi: Int?`. Viết hàm tính giá trị trung bình RSSI của các thiết bị hợp lệ (bỏ qua `nil`).

:::: details 💡 Gợi ý & Mã nguồn mẫu (Click để mở)

::: code-group
```swift [Swift (iOS - Starter)]
struct Device {
    let name: String
    let rssi: Int?
}

func calculateAverageRSSI(devices: [Device]) -> Double? {
    // TODO: Dùng compactMap trích xuất mảng [Int] không chứa nil
    let validRssiList = devices.compactMap { $0.rssi }
    guard !validRssiList.isEmpty else { return nil }
    
    let sum = validRssiList.reduce(0, +)
    return Double(sum) / Double(validRssiList.count)
}

let testDevices = [
    Device(name: "Sensor_A", rssi: -60),
    Device(name: "Sensor_B", rssi: nil),
    Device(name: "Sensor_C", rssi: -80)
]

if let avg = calculateAverageRSSI(devices: testDevices) {
    print("RSSI trung bình: \(avg) dBm")
}
```

```kotlin [Kotlin (Android - Đối chiếu)]
data class Device(val name: String, val rssi: Int?)

fun calculateAverageRSSI(devices: List<Device>): Double? {
    val validRssiList = devices.mapNotNull { it.rssi }
    if (validRssiList.isEmpty()) return null
    return validRssiList.average()
}

fun main() {
    val testDevices = listOf(
        Device("Sensor_A", -60),
        Device("Sensor_B", null),
        Device("Sensor_C", -80)
    )
    val avg = calculateAverageRSSI(testDevices)
    println("RSSI trung bình: $avg dBm")
}
```
:::

- **Kết quả mẫu (Expected Output)**:
```text
RSSI trung bình: -70.0 dBm
```

::::

<CheckTask id="w1-d2" tag="iOS">Hoàn thành bài tập 1.2: Viết Swift Playground mở gói Optional an toàn với guard let.</CheckTask>

---

### Bài tập 1.3 (Day 3) — Quan sát Vòng đời Activity (Android) & Đọc hiểu Java
- **Mục tiêu**: Hiểu thứ tự kích hoạt của các callback vòng đời Android: [`onCreate`](https://developer.android.com/reference/android/app/Activity#onCreate(android.os.Bundle)), [`onStart`](https://developer.android.com/reference/android/app/Activity#onStart()), [`onResume`](https://developer.android.com/reference/android/app/Activity#onResume()), [`onPause`](https://developer.android.com/reference/android/app/Activity#onPause()), [`onStop`](https://developer.android.com/reference/android/app/Activity#onStop()), [`onDestroy`](https://developer.android.com/reference/android/app/Activity#onDestroy()).
- **Đề bài**: Thêm các lệnh ghi log vào Activity chính để quan sát 3 kịch bản:
  1. Mở ứng dụng lần đầu.
  2. Bấm nút Home đưa ứng dụng xuống nền (Background).
  3. Mở lại ứng dụng từ danh sách đa nhiệm.

:::: details 💡 Gợi ý & Mã nguồn mẫu (Click để mở)

::: code-group
```kotlin [Kotlin (Android - ComponentActivity + Compose)]
class MainActivity : ComponentActivity() {
    private val TAG = "LifecycleDemo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "--> onCreate: Khởi tạo ComponentActivity")
        
        // Chuẩn Jetpack Compose: Dùng setContent thay thế hoàn toàn setContentView XML
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Native Device Monitor (Jetpack Compose)",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "--> onStart: Màn hình bắt đầu hiển thị")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "--> onResume: Sẵn sàng tương tác với người dùng")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "--> onPause: Tạm dừng tương tác")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "--> onStop: Màn hình bị ẩn hoàn toàn")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "--> onDestroy: Giải phóng tài nguyên")
    }
}
```

```java [Java (Android - Đối chiếu legacy)]
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "LifecycleDemo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Cách viết Java cũ truyền thống:
        setContentView(R.layout.activity_main);
        Log.d(TAG, "--> onCreate: Java syntax");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "--> onDestroy: Java syntax");
    }
}
```
:::

- **Kết quả mẫu trên Logcat (Android Studio)**:
```text
D/LifecycleDemo: --> onCreate: Khởi tạo ComponentActivity
D/LifecycleDemo: --> onStart: Màn hình bắt đầu hiển thị
D/LifecycleDemo: --> onResume: Sẵn sàng tương tác với người dùng
(Bấm Home)
D/LifecycleDemo: --> onPause: Tạm dừng tương tác
D/LifecycleDemo: --> onStop: Màn hình bị ẩn hoàn toàn
```

::::

<CheckTask id="w1-d3" tag="Android">Hoàn thành bài tập 1.3: Chạy ứng dụng và ghi chép lại thứ tự log vòng đời.</CheckTask>

---

### Bài tập 1.4 (Day 4) — Dựng Bố cục Giao diện (Jetpack Compose & SwiftUI)
- **Mục tiêu**: Nắm vững cú pháp Declarative UI: Bố cục cột ([`Column`](https://developer.android.com/develop/ui/compose/layouts/basics) / [`VStack`](https://developer.apple.com/documentation/swiftui/vstack)), ô nhập văn bản, nút bấm và danh sách cuộn tối ưu ([`LazyColumn`](https://developer.android.com/develop/ui/compose/lists) / [`List`](https://developer.apple.com/documentation/swiftui/list)).
- **Đề bài**: Dựng màn hình hiển thị tiêu đề, ô nhập tên thiết bị, nút bấm "Thêm vào danh sách" và danh sách hiển thị các phần tử đã thêm.

:::: details 💡 Gợi ý & Mã nguồn mẫu (Click để mở)

::: code-group
```kotlin [Kotlin (Android - Jetpack Compose Layout)]
@Composable
fun MainDeviceScreen() {
    var inputText by remember { mutableStateOf("") }
    val deviceList = remember { mutableStateListOf("Sensor_A (-65 dBm)", "OBD_01 (-72 dBm)") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Native Device Monitor",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Nhập tên thiết bị...") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (inputText.isNotBlank()) {
                    deviceList.add(inputText)
                    inputText = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Thêm vào danh sách")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Danh sách thiết bị:", style = MaterialTheme.typography.titleMedium)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(deviceList) { deviceName ->
                Text(
                    text = "• $deviceName",
                    modifier = Modifier.padding(vertical = 4.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
```

```swift [Swift (iOS - SwiftUI Layout)]
struct MainDeviceView: View {
    @State private var inputText = ""
    @State private var deviceList = ["Sensor_A (-65 dBm)", "OBD_01 (-72 dBm)"]

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Native Device Monitor")
                .font(.title2)
                .bold()

            TextField("Nhập tên thiết bị...", text: $inputText)
                .textFieldStyle(.roundedBorder)

            Button("Thêm vào danh sách") {
                if !inputText.trimmingCharacters(in: .whitespaces).isEmpty {
                    deviceList.append(inputText)
                    inputText = ""
                }
            }
            .buttonStyle(.borderedProminent)

            Text("Danh sách thiết bị:")
                .font(.headline)
                .padding(.top, 8)

            List(deviceList, id: \.self) { device in
                Text("• \(device)")
            }
            .listStyle(.plain)
        }
        .padding()
    }
}
```
:::

::::

<CheckTask id="w1-d4" tag="iOS">Hoàn thành bài tập 1.4: Dựng giao diện khai báo Declarative UI trên cả 2 nền tảng.</CheckTask>

---

### Bài tập 1.5 (Day 5) — Cập nhật Async & Tự động Recompose
- **Mục tiêu**: Tuân thủ quy tắc bất đồng bộ: Thực thi tác vụ nặng ở luồng nền (Background IO), chuyển kết quả về Main Thread để cập nhật State (`mutableStateOf` / `@State`), kích hoạt cơ chế tự động Recompose / Render lại giao diện mà không gây giật lag.
- **Đề bài**: Bấm nút "Bắt đầu tác vụ" ➔ Hiện Spinner (`CircularProgressIndicator` / `ProgressView`) ➔ Delay 1.5 giây ở Background ➔ Ẩn Spinner ➔ Cập nhật Text: "Hoàn tất xử lý tác vụ!".

:::: details 💡 Gợi ý & Mã nguồn mẫu (Click để mở)

::: code-group
```kotlin [Kotlin (Android - Jetpack Compose Async)]
@Composable
fun AsyncProcessScreen() {
    var statusText by remember { mutableStateOf("Sẵn sàng") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = statusText, style = MaterialTheme.typography.titleMedium)

        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            enabled = !isLoading,
            onClick = {
                scope.launch {
                    isLoading = true
                    statusText = "Đang xử lý ở luồng nền..."

                    // 1. Chạy tác vụ nặng ở Background IO
                    withContext(Dispatchers.IO) {
                        Thread.sleep(1500)
                    }

                    // 2. Cập nhật State -> Jetpack Compose tự động Recompose
                    isLoading = false
                    statusText = "Hoàn tất xử lý tác vụ!"
                }
            }
        ) {
            Text("Bắt đầu tác vụ")
        }
    }
}
```

```swift [Swift (iOS - SwiftUI Async)]
struct AsyncProcessView: View {
    @State private var statusText = "Sẵn sàng"
    @State private var isLoading = false

    var body: some View {
        VStack(spacing: 16) {
            Text(statusText)
                .font(.headline)

            if isLoading {
                ProgressView()
            }

            Button("Bắt đầu tác vụ") {
                Task {
                    isLoading = true
                    statusText = "Đang xử lý ở luồng nền..."

                    // 1. Chạy ngầm trong 1.5 giây
                    try? await Task.sleep(nanoseconds: 1_500_000_000)

                    // 2. Cập nhật State trên MainActor -> SwiftUI tự render lại
                    await MainActor.run {
                        isLoading = false
                        statusText = "Hoàn tất xử lý tác vụ!"
                    }
                }
            }
            .disabled(isLoading)
            .buttonStyle(.borderedProminent)
        }
        .padding()
    }
}
```
:::

- **Hành vi kiểm chứng (Expected UI Behavior)**:
  - Khi bấm nút, nút tự đổi trạng thái disable để tránh bấm nhiều lần, vòng tròn quay `CircularProgressIndicator` / `ProgressView` xuất hiện ngay lập tức.
  - Sau 1.5 giây, trạng thái chuyển về hoàn thành, giao diện mượt mà 60/120fps không hề giật khung hình.

::::

<CheckTask id="w1-d5">Hoàn thành bài tập 1.5: Thao tác UI từ async callback chạy mượt mà không crash.</CheckTask>

---

## Tuần 2: Native APIs & Luồng BLE Hoàn Chỉnh

### Bài tập 2.1 (Day 1) — Xin Quyền Bluetooth & Camera
- **Mục tiêu**: Xử lý 2 nhánh kết quả: Người dùng Đồng ý (Granted) và Bị từ chối (Denied). Bắt lỗi khi chưa khai báo trong `AndroidManifest.xml` hoặc `Info.plist`.
- **Đề bài**: Viết hàm yêu cầu cấp quyền Camera. Nếu bị từ chối, hiển thị hộp thoại giải thích và nút mở nhanh trang Cài đặt ứng dụng (App Settings).

:::: details 💡 Gợi ý & Mã nguồn mẫu (Click để mở)

::: code-group
```kotlin [Kotlin (Android - ActivityResultLauncher)]
private val requestCameraLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
) { isGranted ->
    if (isGranted) {
        Toast.makeText(this, "Quyền Camera đã được cấp!", Toast.LENGTH_SHORT).show()
    } else {
        showOpenSettingsDialog("Ứng dụng cần quyền Camera để chụp ảnh thiết bị.")
    }
}

fun requestCamera() {
    requestCameraLauncher.launch(Manifest.permission.CAMERA)
}

private fun showOpenSettingsDialog(msg: String) {
    AlertDialog.Builder(this)
        .setTitle("Cần cấp quyền")
        .setMessage(msg)
        .setPositiveButton("Mở Cài đặt") { _, _ ->
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        }
        .setNegativeButton("Hủy", null)
        .show()
}
```

```swift [Swift (iOS - AVCaptureDevice)]
import AVFoundation

func requestCameraPermission() {
    switch AVCaptureDevice.authorizationStatus(for: .video) {
    case .authorized:
        print("Đã được cấp quyền Camera")
    case .notDetermined:
        AVCaptureDevice.requestAccess(for: .video) { granted in
            DispatchQueue.main.async {
                if granted {
                    print("Người dùng vừa đồng ý cấp quyền")
                } else {
                    self.showSettingsAlert()
                }
            }
        }
    case .denied, .restricted:
        showSettingsAlert()
    @unknown default:
        break
    }
}

private fun showSettingsAlert() {
    let alert = UIAlertController(title: "Cần cấp quyền", message: "Vui lòng mở Cài đặt để cho phép truy cập Camera.", preferredStyle: .alert)
    alert.addAction(UIAlertAction(title: "Mở Cài đặt", style: .default) { _ in
        if let url = URL(string: UIApplication.openSettingsURLString) {
            UIApplication.shared.open(url)
        }
    })
    alert.addAction(UIAlertAction(title: "Hủy", style: .cancel))
    present(alert, animated: true)
}
```
:::

::::

<CheckTask id="w2-d1">Hoàn thành bài tập 2.1: Xử lý xin cấp quyền trên Android & iOS.</CheckTask>

---

### Bài tập 2.2 (Day 2) — Quét & Kết nối Thiết bị BLE
- **Mục tiêu**: Sử dụng [`BluetoothLeScanner`](https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner) / [`CBCentralManager`](https://developer.apple.com/documentation/corebluetooth/cbcentralmanager) tìm thiết bị phát sóng (hoặc app nRF Connect / ESP32); cập nhật danh sách hiển thị tên, địa chỉ MAC/UUID và RSSI.
- **Đề bài**: Bấm "Quét BLE" ➔ Bắt kết quả scan ➔ Hiển thị lên danh sách ➔ Nhấn chọn 1 thiết bị để kết nối.

:::: details 💡 Gợi ý & Mã nguồn mẫu (Click để mở)

::: code-group
```kotlin [Kotlin (Android - ScanCallback)]
private val scanCallback = object : ScanCallback() {
    override fun onScanResult(callbackType: Int, result: ScanResult) {
        val device = result.device
        val rssi = result.rssi
        val name = device.name ?: "Unknown"
        Log.d("BLE", "Tìm thấy: $name (${device.address}) | RSSI: $rssi dBm")
    }

    override fun onScanFailed(errorCode: Int) {
        Log.e("BLE", "Quét BLE thất bại với mã lỗi: $errorCode")
    }
}

// Bắt đầu quét
bluetoothAdapter.bluetoothLeScanner?.startScan(scanCallback)
```

```swift [Swift (iOS - CBCentralManagerDelegate)]
// MARK: - CBCentralManagerDelegate
func centralManagerDidUpdateState(_ central: CBCentralManager) {
    if central.state == .poweredOn {
        // Bắt đầu quét các thiết bị xung quanh
        central.scanForPeripherals(withServices: nil, options: nil)
    }
}

func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
    let name = peripheral.name ?? "Unknown"
    print("Tìm thấy: \(name) (\(peripheral.identifier.uuidString)) | RSSI: \(RSSI) dBm")
}
```
:::

- **Kết quả mẫu trên Console**:
```text
Tìm thấy: CarMD_BT_Sensor (E4:65:B8:21:40:9A) | RSSI: -65 dBm
Tìm thấy: SmartWatch (F1:34:CC:89:12:00) | RSSI: -82 dBm
```

::::

<CheckTask id="w2-d2">Hoàn thành bài tập 2.2: Quét, kết nối và cập nhật trạng thái Connected/Disconnected.</CheckTask>

---

### Bài tập 2.3 (Day 3) — Khám phá GATT & Đọc Dữ liệu Thô
- **Mục tiêu**: Sau khi kết nối thành công, kích hoạt khám phá danh sách Services và Characteristics; gửi lệnh đọc giá trị Characteristic và in ra màn hình dạng Hex string + Text UTF-8.
- **Đề bài**: Tìm Service Battery (`0x180F`) hoặc Custom Sensor Service, đọc Characteristic dữ liệu và hiển thị giá trị.

:::: details 💡 Gợi ý & Mã nguồn mẫu (Click để mở)

::: code-group
```kotlin [Kotlin (Android - BluetoothGattCallback)]
override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
    if (status == BluetoothGatt.GATT_SUCCESS) {
        val targetService = gatt.getService(UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb"))
        val targetChar = targetService?.getCharacteristic(UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb"))
        if (targetChar != null) {
            gatt.readCharacteristic(targetChar)
        }
    }
}

override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
    val bytes = characteristic.value ?: byteArrayOf()
    val hexString = bytes.joinToString("") { "%02X ".format(it) }
    Log.d("BLE_READ", "Dữ liệu Hex: $hexString | Số nguyên: ${bytes.firstOrNull()?.toInt() ?: 0}")
}
```

```swift [Swift (iOS - CBPeripheralDelegate)]
func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
    guard let services = peripheral.services else { return }
    for service in services {
        peripheral.discoverCharacteristics(nil, for: service)
    }
}

func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
    guard let data = characteristic.value else { return }
    let hexString = data.map { String(format: "%02X ", $0) }.joined()
    let intVal = data.first ?? 0
    print("Dữ liệu Hex: \(hexString) | Số nguyên: \(intVal)")
}
```
:::

- **Kết quả mẫu**:
```text
Dữ liệu Hex: 5B | Số nguyên: 91 (Phần trăm pin: 91%)
```

::::

<CheckTask id="w2-d3">Hoàn thành bài tập 2.3: Tìm Characteristic mục tiêu và đọc dữ liệu thành công.</CheckTask>

---

### Bài tập 2.4 (Day 4) — Nhận Stream Notify & Bắt Lỗi Rớt Kết Nối
- **Mục tiêu**: Đăng ký nhận thông báo liên tục (Notification); chủ động tắt thiết bị ngoại vi giữa chừng để kiểm chứng ứng dụng bắt sự kiện ngắt kết nối an toàn, không bị treo giao diện.
- **Đề bài**: Bật Notify qua CCCD Descriptor; ghi nhận khi ngắt kết nối đột ngột và hiển thị nút "Kết nối lại".

:::: details 💡 Gợi ý & Mã nguồn mẫu (Click để mở)

::: code-group
```kotlin [Kotlin (Android - Handle Disconnect)]
override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
    if (newState == BluetoothProfile.STATE_DISCONNECTED) {
        Log.w("BLE", "Cảnh báo: Thiết bị đã ngắt kết nối (status: $status)")
        gatt.close() // Giải phóng GATT client
        
        runOnUiThread {
            tvConnectionStatus.text = "Đã ngắt kết nối! Vui lòng thử lại."
            btnReconnect.visibility = View.VISIBLE
        }
    }
}
```

```swift [Swift (iOS - Handle Disconnect)]
func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
    print("Cảnh báo: Thiết bị đã ngắt kết nối. Lỗi: \(error?.localizedDescription ?? "Không có lỗi")")
    
    DispatchQueue.main.async {
        self.statusLabel.text = "Đã ngắt kết nối! Vui lòng thử lại."
        self.reconnectButton.isHidden = false
    }
}
```
:::

::::

<CheckTask id="w2-d4">Hoàn thành bài tập 2.4: Nhận stream mượt mà và xử lý rớt kết nối an toàn.</CheckTask>

---

### Bài tập 2.5 (Day 5) — Chụp/Chọn Ảnh & Gọi REST API
- **Mục tiêu**: Hoàn tất 2 module phụ trợ: Chụp 1 ảnh thumbnail từ máy ảnh/thư viện và gửi 1 request HTTP GET lấy JSON từ internet có bắt lỗi timeout.
- **Đề bài**: Dùng Photo Picker chọn ảnh ➔ hiển thị lên ImageView. Gọi API `https://jsonplaceholder.typicode.com/posts/1` ➔ hiển thị tiêu đề bài viết.

:::: details 💡 Gợi ý & Mã nguồn mẫu (Click để mở)

::: code-group
```kotlin [Kotlin (Android - API GET)]
val client = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .build()

val request = Request.Builder()
    .url("https://jsonplaceholder.typicode.com/posts/1")
    .build()

lifecycleScope.launch(Dispatchers.IO) {
    try {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            withContext(Dispatchers.Main) {
                tvApiResult.text = "HTTP ${response.code}: $body"
            }
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            tvApiResult.text = "Lỗi kết nối: ${e.message}"
        }
    }
}
```

```swift [Swift (iOS - URLSession GET)]
guard let url = URL(string: "https://jsonplaceholder.typicode.com/posts/1") else { return }

Task {
    do {
        var req = URLRequest(url: url)
        req.timeoutInterval = 10.0
        let (data, response) = try await URLSession.shared.data(for: req)
        let bodyString = String(data: data, encoding: .utf8)
        let status = (response as? HTTPURLResponse)?.statusCode ?? 0
        
        await MainActor.run {
            self.apiResultLabel.text = "HTTP \(status): \(bodyString ?? "")"
        }
    } catch {
        await MainActor.run {
            self.apiResultLabel.text = "Lỗi kết nối: \(error.localizedDescription)"
        }
    }
}
```
:::

::::

<CheckTask id="w2-d5">Hoàn thành bài tập 2.5: Chụp ảnh, chọn ảnh và gọi HTTP API thành công.</CheckTask>

---

## Tuần 3: NativeCore & Background Monitor

### Bài tập 3.1 (Day 1) — Tách Mã nguồn vào 4 Services
- **Mục tiêu**: Di dời toàn bộ logic phần cứng ra khỏi Activity/ViewController vào 4 services độc lập (`PermissionManager`, `BluetoothManager`, `MediaManager`, `ApiClient`) tuân theo Interface/Protocol đã định nghĩa trong Kiến trúc.
- **Đề bài**: Refactor màn hình chính sao cho Activity/ViewController chỉ gọi hàm qua Service Interface, không còn bất kỳ import trực tiếp nào liên quan tới BluetoothGatt hay OkHttp.

:::: details 💡 Gợi ý & Mã nguồn mẫu (Click để mở)

::: code-group
```kotlin [Kotlin (Android - Gọi qua Interface)]
class MainActivity : AppCompatActivity() {
    // Inject hoặc khởi tạo Service độc lập
    private lateinit var bleManager: BluetoothManager
    private lateinit var apiClient: ApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bleManager = NativeBluetoothManager(applicationContext)
        apiClient = NativeApiClient()
        
        btnScan.setOnClickListener {
            bleManager.startScan(
                onDeviceFound = { dev -> updateList(dev) },
                onError = { err -> showError(err) }
            )
        }
    }
}
```

```swift [Swift (iOS - Gọi qua Protocol)]
class MainViewController: UIViewController {
    var bleManager: BluetoothManagerProtocol = NativeBluetoothManager()
    var apiClient: ApiClientProtocol = NativeApiClient()

    override func viewDidLoad() {
        super.viewDidLoad()
        
        scanButton.addAction(UIAction { [weak self] _ in
            self?.bleManager.startScan(
                onDeviceFound = { dev in self?.updateList(dev) },
                onError = { err in self?.showError(err) }
            )
        }, for: .touchUpInside)
    }
}
```
:::

::::

<CheckTask id="w3-d1">Hoàn thành bài tập 3.1: Không còn dòng code phần cứng nào trong UI.</CheckTask>

---

### Bài tập 3.2 (Day 2) — Xây dựng các Device Provider
- **Mục tiêu**: Xây dựng 3 lớp Provider độc lập: `BatteryProvider` (đọc % pin), `NetworkProvider` (kiểm tra Wi-Fi / 4G), `BluetoothProvider` (trạng thái Adapter).
- **Đề bài**: Thu thập đồng thời dữ liệu từ 3 Provider và đóng gói thành đối tượng `DeviceSnapshot`.

:::: details 💡 Gợi ý & Mã nguồn mẫu (Click để mở)

::: code-group
```kotlin [Kotlin (Android - BatteryProvider)]
class BatteryProvider(private val context: Context) {
    fun getBatteryLevel(): Int {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val status = context.registerReceiver(null, filter)
        val level = status?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = status?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()).toInt() else 0
    }
}
```

```swift [Swift (iOS - BatteryProvider)]
class BatteryProvider {
    func getBatteryLevel() -> Int {
        UIDevice.current.isBatteryMonitoringEnabled = true
        let raw = UIDevice.current.batteryLevel
        return raw >= 0 ? Int(raw * 100) : 100
    }
}
```
:::

::::

<CheckTask id="w3-d2">Hoàn thành bài tập 3.2: Các Provider trả về đúng dữ liệu phần cứng.</CheckTask>

---

### Bài tập 3.3 (Day 3) — Ghi File Nhật ký JSONL
- **Mục tiêu**: Cứ mỗi 5 giây, thu thập 1 snapshot trạng thái thiết bị và ghi nối dòng (append) vào file `monitor_logs.jsonl` trong bộ nhớ nội bộ. Cung cấp hàm đọc toàn bộ log và hàm xóa file.
- **Đề bài**: Kiểm tra xem file có tự động tăng kích thước đều đặn và mỗi dòng có đúng chuẩn JSON hay không.

:::: details 💡 Gợi ý & Mã nguồn mẫu (Click để mở)

::: code-group
```kotlin [Kotlin (Android - FileLogger.kt)]
class FileLogger(private val context: Context, private val fileName: String = "monitor_logs.jsonl") {
    private val file get() = File(context.filesDir, fileName)

    fun append(line: String) {
        file.appendText("$line\n", Charsets.UTF_8)
    }

    fun readAll(): List<String> = if (file.exists()) file.readLines(Charsets.UTF_8) else emptyList()

    fun clear() {
        if (file.exists()) file.delete()
    }
}
```

```swift [Swift (iOS - FileLogger.swift)]
final class FileLogger {
    private let fileURL: URL

    init(fileName: String = "monitor_logs.jsonl") {
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        self.fileURL = docs.appendingPathComponent(fileName)
    }

    func append(_ line: String) {
        guard let data = "\(line)\n".data(using: .utf8) else { return }
        if FileManager.default.fileExists(atPath: fileURL.path) {
            if let handle = try? FileHandle(forWritingTo: fileURL) {
                handle.seekToEndOfFile()
                handle.write(data)
                handle.closeFile()
            }
        } else {
            try? data.write(to: fileURL, options: .atomic)
        }
    }

    func readAll() -> [String] {
        guard let text = try? String(contentsOf: fileURL, encoding: .utf8) else { return [] }
        return text.components(separatedBy: .newlines).filter { !$0.isEmpty }
    }

    func clear() {
        try? FileManager.default.removeItem(at: fileURL)
    }
}
```
:::

- **Mẫu file log tạo ra (`monitor_logs.jsonl`)**:
```json
{"timestamp":"2026-09-15T14:00:00Z","battery":85,"wifi":true,"cellular":false,"ble":true}
{"timestamp":"2026-09-15T14:00:05Z","battery":85,"wifi":true,"cellular":false,"ble":true}
```

::::

<CheckTask id="w3-d3">Hoàn thành bài tập 3.3: Ghi file JSONL đều đặn, đọc và xóa file trơn tru.</CheckTask>

---

### Bài tập 3.4 (Day 4) — Thực nghiệm Ma trận 5 Trạng thái OS
- **Mục tiêu**: Bật `BackgroundMonitor` và quan sát chu kỳ ghi log qua 5 trạng thái ứng dụng:
  1. **Foreground**: Đang mở trên màn hình.
  2. **Background**: Bấm nút Home / Thu nhỏ app.
  3. **Return**: Mở lại app sau 2 phút.
  4. **OS Kill**: Hệ thống giải phóng do thiếu RAM.
  5. **Force-kill**: Người dùng vuốt tắt app từ trình đa nhiệm.
- **Đề bài**: Lập bảng báo cáo ghi nhận số lượng bản ghi và độ lệch thời gian giữa Android (có và không có Foreground Service) so với iOS.

:::: details 💡 Gợi ý & Mẫu bảng ghi nhận (Click để mở)

| Trạng thái | Android (No Service) | Android (Foreground Service) | iOS (Standard App) |
| :--- | :--- | :--- | :--- |
| **Foreground** | Log đều đặn mỗi 5s | Log đều đặn mỗi 5s | Log đều đặn mỗi 5s |
| **Background** | Bị giãn cách khi vào Doze | Chạy liên tục nhờ Notification | ⚠️ Dừng sau ~30 giây |
| **Return** | Tiếp tục bình thường | Tiếp tục bình thường | Khôi phục Timer ngay |
| **OS Kill** | Dừng khi thiếu RAM | Khó bị kill hơn, auto-restart | Khởi động lại từ đầu |
| **Force-kill** | Bị tiêu diệt hoàn toàn | Bị tiêu diệt hoàn toàn | Bị tiêu diệt hoàn toàn |

::::

<CheckTask id="w3-d4">Hoàn thành bài tập 3.4: Lập bảng tài liệu so sánh hành vi nền Android vs iOS.</CheckTask>

---

### Bài tập 3.5 (Day 5) — Nối Toàn diện vào Màn hình Điều khiển
- **Mục tiêu**: Tích hợp toàn bộ các Service vào một màn hình duy nhất (**Native Device Monitor Demo Dashboard**).
- **Đề bài**: Giao diện gồm 5 khối chức năng:
  1. Thẻ Permission: Trạng thái các quyền & nút xin cấp quyền.
  2. Thẻ BLE: Trạng thái kết nối, nút quét, RSSI live stream.
  3. Thẻ Media: Nút chụp ảnh, chọn ảnh & khung xem trước.
  4. Thẻ API: Nút GET / POST & khung hiển thị JSON response.
  5. Thẻ Monitor: Nút Bật/Tắt giám sát, thông số Pin/Mạng tức thời, số lượng dòng log trong file JSONL.

:::: details 💡 Gợi ý bố cục giao diện Dashboard (Click để mở)

```text
+---------------------------------------------------+
|            NATIVE DEVICE MONITOR DEMO             |
+---------------------------------------------------+
| [!] Permissions: Camera [✓] | Bluetooth [✓]       |
|     [ Xin cấp quyền bổ sung ]                     |
+---------------------------------------------------+
| [BLE] Status: Connected (Sensor_A)  RSSI: -65dBm  |
|     [ Quét thiết bị ]    [ Ngắt kết nối ]         |
+---------------------------------------------------+
| [Media] [ Chụp ảnh ]  [ Chọn từ thư viện ]        |
|     +-----------------------+                     |
|     |     [Ảnh hiển thị]    |                     |
|     +-----------------------+                     |
+---------------------------------------------------+
| [API] [ Gửi GET ]  [ Gửi POST ]                   |
|     Status: 200 OK | Body: {"id": 1, ...}         |
+---------------------------------------------------+
| [Monitor] Pin: 85% | Wi-Fi: Bật | Log: 42 dòng    |
|     [ Bắt đầu giám sát ]    [ Xem & Xóa Log ]     |
+---------------------------------------------------+
```

::::

<CheckTask id="w3-d5">Hoàn thành bài tập 3.5: Vận hành toàn bộ hệ thống từ một giao diện duy nhất.</CheckTask>

---

## Tuần 4: Kiểm thử End-to-End & Tối ưu Hoàn thiện

### Bài tập 4.1 (Day 1) — Chạy Toàn bộ Bộ kịch bản Kiểm thử E2E (17 Hạng mục)
- **Mục tiêu**: Chạy kiểm thử toàn diện từ đầu đến cuối trên thiết bị thật theo danh mục **17 tiêu chí E2E** ([week-4](/schedule/week-4#thu-hai-day-1-kiem-thu-toan-dien-end-to-end-4-gio)).
- **Đề bài**: Tạo bảng ghi nhận lỗi (Bug Log) gồm các cột: Hạng mục kiểm thử, Trạng thái (Pass/Fail), Nền tảng (Android/iOS), Mô tả lỗi & Cách khắc phục.

:::: details 💡 Mẫu bảng ghi nhận lỗi (Bug Log Template) (Click để mở)

| STT | Luồng kiểm thử | Android | iOS | Ghi chú & Lỗi phát hiện |
| :--- | :--- | :---: | :---: | :--- |
| 1 | Xin quyền: Lần đầu hiện dialog | ✅ | ✅ | Pass |
| 2 | Xin quyền: Deny không crash | ✅ | ✅ | Pass |
| 3 | BLE: Quét & Kết nối thiết bị thật | ✅ | ✅ | Pass |
| 4 | BLE: Rút nguồn peripheral đột ngột | ✅ | ✅ | Tự bắt lỗi disconnect sau 3s |
| 5 | Background: Ghi log JSONL đều đặn | ✅ | ⚠️ | iOS dừng sau ~30s (Đúng thiết kế OS) |

::::

<CheckTask id="w4-m1">Hoàn thành bài tập 4.1: Vượt qua bộ kiểm thử End-to-End toàn diện.</CheckTask>

---

### Bài tập 4.2 (Day 2) — Kiểm định Mã nguồn theo Refactor Checklist
- **Mục tiêu**: Rà soát toàn bộ dự án theo **8 tiêu chuẩn chất lượng mã nguồn** ([week-4](/schedule/week-4#thu-ba-day-2-ra-soat-ma-nguon-toi-uu-hoan-thien-4-gio)).
- **Đề bài**:
  1. Đảm bảo UI không chứa code phần cứng.
  2. Kiểm tra các hàm callback GATT, Coroutine Job, Task đã được hủy (`cancel()` / `close()`) trong `onDestroy` hoặc `viewWillDisappear`.
  3. Gỡ bỏ mọi thư viện phụ thuộc thừa thãi khỏi file build (`build.gradle.kts` / `Podfile` / `Package.swift`).

:::: details 💡 Gợi ý rà soát & Tối ưu mã nguồn (Click để mở)

1. **Giao diện (UI Layer)**: Tìm kiếm các từ khóa `BluetoothGatt`, `OkHttpClient`, `URLSession` trong code Activity / ViewController. Nếu còn xuất hiện ➔ Di chuyển ngay sang Service tương ứng.
2. **Quản lý Vòng đời & Bộ nhớ**:
   - Android: `bluetoothGatt?.close()`, `job.cancel()`.
   - iOS: `centralManager.stopScan()`, `task?.cancel()`, sử dụng `[weak self]` trong các escaping closure.
3. **Dependencies Audit**: Xóa bỏ các thư viện test không dùng, thư viện bên thứ ba không cần thiết.

::::

<CheckTask id="w4-m2">Hoàn thành bài tập 4.2: Toàn bộ 8 tiêu chí Refactor Checklist đạt trạng thái xanh.</CheckTask>

---

## 🌟 Bài tập Mở rộng (Bonus Challenges - Tùy chọn)

Dành cho các lập trình viên hoàn thành sớm lộ trình hoặc muốn nâng cấp kỹ năng chuyên sâu phục vụ giai đoạn đóng gói thư viện (Capacitor/Ionic Plugin):

### Bài tập 4.3 (Bonus) — Đóng gói Release APK & Xcode Archive
- **Mục tiêu**: Tạo bản build phát hành chính thức thay vì bản Debug.
- **Thực hành**:
  - **Android**: Cấu hình file Keystore ký số (`keystore.jks`), kích hoạt ProGuard / R8 minification để nén code và làm rối mã nguồn; xuất file Release APK.
  - **iOS**: Cấu hình Signing & Capabilities trong Xcode với chứng chỉ Apple Developer, tạo bản lưu trữ Xcode Archive và xuất file IPA (Ad-Hoc hoặc Development distribution).

:::: details 💡 Gợi ý cấu hình phát hành (Release Build) (Click để mở)

::: code-group
```kotlin [Android (build.gradle.kts)]
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
        signingConfig = signingConfigs.getByName("release")
    }
}
```

```text [iOS (Xcode Archive)]
1. Xcode Menu: Product ➔ Archive
2. Organizer Window: Chọn bản build vừa tạo ➔ Bấm "Distribute App"
3. Chọn phương thức: Development hoặc Ad-Hoc
4. Xuất file .ipa và tệp tin manifest
```
:::

::::

### Bài tập 4.4 (Bonus) — Viết Unit Test & Mock Service
- **Mục tiêu**: Kiểm thử tự động logic của Service mà không cần bật thiết bị phần cứng thật.
- **Thực hành**:
  - Viết `MockApiClient` trả về dữ liệu JSON giả lập sau 200ms để kiểm thử ViewModel.
  - Sử dụng JUnit / Mockk (trên Android) và XCTest (trên iOS) để kiểm tra tính đúng đắn của logic tính toán mức pin trung bình và giải mã chuỗi hex từ BLE Characteristic.

:::: details 💡 Gợi ý mã nguồn Mock Service & Unit Test (Click để mở)

::: code-group
```kotlin [Kotlin (Android - MockApiClient.kt)]
class MockApiClient(private val shouldSucceed: Boolean = true) : ApiClient {
    override suspend fun get(url: String, headers: Map<String, String>?): ApiResponse {
        delay(200) // Giả lập độ trễ mạng
        return if (shouldSucceed) {
            ApiResponse(200, """{"title":"Mock Post"}""", true)
        } else {
            ApiResponse(500, null, false, "Internal Server Error")
        }
    }
    override suspend fun post(url: String, jsonBody: String, headers: Map<String, String>?): ApiResponse {
        delay(200)
        return ApiResponse(201, jsonBody, true)
    }
}
```

```swift [Swift (iOS - MockApiClient.swift)]
final class MockApiClient: ApiClientProtocol {
    var shouldSucceed: Bool = true
    
    func get(url: String, headers: [String : String]?) async -> ApiResponse {
        try? await Task.sleep(nanoseconds: 200_000_000)
        if shouldSucceed {
            return ApiResponse(statusCode: 200, body: "{\"title\":\"Mock Post\"}", isSuccessful: true, errorMessage: nil)
        } else {
            return ApiResponse(statusCode: 500, body: nil, isSuccessful: false, errorMessage: "Internal Server Error")
        }
    }
    
    func post(url: String, jsonBody: String, headers: [String : String]?) async -> ApiResponse {
        try? await Task.sleep(nanoseconds: 200_000_000)
        return ApiResponse(statusCode: 201, body: jsonBody, isSuccessful: true, errorMessage: nil)
    }
}
```
:::

::::
