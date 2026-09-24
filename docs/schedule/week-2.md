# Tuần 2: Native APIs & Luồng BLE Hoàn Chỉnh

Cột mốc mục tiêu tuần 2: **Vận hành trọn vẹn các tính năng phần cứng** — hệ thống quyền hạn, chu trình Bluetooth BLE 9 bước hoàn chỉnh (với thiết bị ngoại vi kiểm thử như nRF Connect / ESP32), chụp và chọn ảnh Camera/Media, và gọi HTTP API GET/POST.

---

## 1. Mục tiêu bàn giao (Milestone Deliverables)

<ProgressTracker prefix="w2" title="Tiến độ Tuần 2" :total="11" />

<CheckTask id="w2-m1">1. Hệ thống xin quyền (Permission) hoạt động chuẩn trên cả 2 nền tảng (Granted và Denied).</CheckTask>
<CheckTask id="w2-m2">2. Luồng BLE hoàn chỉnh chạy thông suốt với thiết bị ngoại vi: Scan → Connect → Discover → Notify → Read → Disconnect.</CheckTask>
<CheckTask id="w2-m3">3. Giao diện người dùng phản ánh đúng từng trạng thái: Scanning, Connecting, Connected, Live Stream, Disconnected.</CheckTask>
<CheckTask id="w2-m4">4. Xử lý thành công tình huống ngắt kết nối đột ngột (rút nguồn peripheral) và ứng dụng khôi phục an toàn.</CheckTask>
<CheckTask id="w2-m5">5. Chụp ảnh từ camera và chọn ảnh từ thư viện hiển thị lên màn hình.</CheckTask>
<CheckTask id="w2-m6">6. Thực hiện thành công yêu cầu HTTP GET và POST có gửi/nhận JSON, bắt được lỗi Timeout khi ngắt mạng.</CheckTask>

---

## 2. Kế hoạch chi tiết từng ngày (Daily Plan)

### Thứ Hai (Day 1) — Quản lý Quyền & Khái niệm BLE (~3 giờ)
- **Nội dung học**: Vòng lặp xin quyền (check, request, callback), cấu hình Manifest và [`Info.plist`](https://developer.apple.com/documentation/bundleresources/information_property_list); các thuật ngữ BLE (Central, Peripheral, Service, Characteristic, UUID, Read, Write, Notify, CCCD).
- **Bài tập thực hành**:
  <CheckTask id="w2-d1">Viết code xin quyền BLE và Camera trên cả Android & iOS; vẽ sơ đồ mô hình đối tượng GATT.</CheckTask>
- **Tiêu chuẩn đạt được**: Nắm vững luồng xin quyền đa nền tảng và giải thích rành mạch mô hình GATT.

### Thứ Ba (Day 2) — BLE Scan & Kết nối — Bước 2 & 3 (~4 giờ)
- **Nội dung học**: [`startScan`](https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner#startScan(android.bluetooth.le.ScanCallback)) / [`stopScan`](https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner#stopScan(android.bluetooth.le.ScanCallback)), lọc thiết bị theo tên hoặc UUID, bắt sự kiện [`onScanResult`](https://developer.android.com/reference/android/bluetooth/le/ScanCallback#onScanResult(int,android.bluetooth.le.ScanResult)) / [`didDiscover`](https://developer.apple.com/documentation/corebluetooth/cbcentralmanagerdelegate/1518937-centralmanager), khởi tạo kết nối qua [`connectGatt`](https://developer.android.com/reference/android/bluetooth/BluetoothDevice#connectGatt(android.content.Context,boolean,android.bluetooth.BluetoothGattCallback)) / [`connect(peripheral)`](https://developer.apple.com/documentation/corebluetooth/cbcentralmanager/1518765-connect).
- **Bài tập thực hành**:
  <CheckTask id="w2-d2">Quét và hiển thị danh sách thiết bị kèm RSSI; bấm chọn thiết bị và duy trì kết nối Connected/Disconnected.</CheckTask>
- **Tiêu chuẩn đạt được**: Quét, chọn thiết bị và duy trì kết nối ổn định trên cả 2 hệ điều hành.

### Thứ Tư (Day 3) — Khám phá GATT & Đọc dữ liệu — Bước 4 đến 6 (~4 giờ)
- **Nội dung học**: Khám phá danh sách Services và Characteristics; phân loại thuộc tính (readable, notify-capable); thực hiện lệnh đọc (Read) và chuyển đổi mảng byte sang kiểu dữ liệu có nghĩa (String, Int, Float).
- **Bài tập thực hành**:
  <CheckTask id="w2-d3">In cây Service/Characteristic ra log và giao diện; đọc một Characteristic và in giá trị byte hex + đã giải mã.</CheckTask>
- **Tiêu chuẩn đạt được**: Tìm đúng Characteristic mục tiêu và đọc dữ liệu thành công.

### Thứ Năm (Day 4) — Đăng ký Notification, Ngắt kết nối & Bắt lỗi — Bước 7 đến 9 (~3 giờ)
- **Nội dung học**: Đăng ký nhận thông báo (Subscribe Notification qua CCCD trên Android hoặc [`setNotifyValue`](https://developer.apple.com/documentation/corebluetooth/cbperipheral/1518949-setnotifyvalue) trên iOS); truyền mảng byte nhận được lên UI; ngắt kết nối chủ động và phát hiện ngắt kết nối đột ngột.
- **Bài tập thực hành**:
  <CheckTask id="w2-d4">Đăng ký stream dữ liệu cảm biến liên tục lên UI; tắt nguồn thiết bị ngoại vi và xử lý mất kết nối an toàn.</CheckTask>
- **Tiêu chuẩn đạt được**: Luồng BLE 9 bước hoàn chỉnh chạy thông suốt và ổn định 100%.

### Thứ Sáu (Day 5) — Camera, Media & HTTP Network Client (~3 giờ)
- **Nội dung học**: Khởi chạy camera hệ thống chụp ảnh; mở Photo Picker chọn ảnh; gửi request GET/POST bằng [`OkHttp`](https://square.github.io/okhttp/) / [`URLSession`](https://developer.apple.com/documentation/foundation/urlsession), gắn timeout và xử lý bất đồng bộ.
- **Bài tập thực hành**:
  <CheckTask id="w2-d5">Chụp 1 ảnh + chọn 1 ảnh hiển thị lên UI; tạo màn hình GET JSON và POST JSON kèm xử lý lỗi timeout.</CheckTask>
- **Tiêu chuẩn đạt được**: Bàn giao đầy đủ các tính năng phần cứng và mạng trước khi bước vào tuần tối ưu kiến trúc.

---

## 3. Tài liệu tra cứu tham chiếu (Ref Docs)

### <span class="badge-android">Android Ref</span>
- [Android BLE Connectivity Guide](https://developer.android.com/develop/connectivity/bluetooth/ble): Hướng dẫn chính thức về BLE trên Android.
- [BluetoothGatt Class Reference](https://developer.android.com/reference/android/bluetooth/BluetoothGatt): Tài liệu chi tiết về GATT client và các hàm đọc/ghi/notify.
- [Android Photo Picker Guide](https://developer.android.com/training/data-storage/shared/photopicker).
- [OkHttp Official Documentation](https://square.github.io/okhttp/).

### <span class="badge-ios">iOS Ref</span>
- [Apple CoreBluetooth Framework](https://developer.apple.com/documentation/corebluetooth).
- [UIImagePickerController (Camera)](https://developer.apple.com/documentation/uikit/uiimagepickercontroller).
- [PHPickerViewController (Photo Picker)](https://developer.apple.com/documentation/photokit/phpickerviewcontroller).
- [Apple URLSession Official Guide](https://developer.apple.com/documentation/foundation/urlsession).
