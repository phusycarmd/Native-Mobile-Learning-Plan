# Tuần 3: NativeCore Refactoring & Background Monitor

Cột mốc mục tiêu tuần 3: **Tái cấu trúc mã nguồn thành kiến trúc phân tầng chuẩn NativeCore**, triển khai dịch vụ giám sát thiết bị **BackgroundMonitor** ghi nhật ký định dạng JSONL, và thực hiện kiểm thử ma trận hành vi 5 trạng thái nền tảng.

---

## 1. Mục tiêu bàn giao (Milestone Deliverables)

<ProgressTracker prefix="w3" title="Tiến độ Tuần 3" :total="11" />

<CheckTask id="w3-m1">1. Tách rời hoàn toàn logic phần cứng khỏi UI vào 4 Service: Permission, Bluetooth, Media, ApiClient.</CheckTask>
<CheckTask id="w3-m2">2. Các Provider (Battery, Network, Bluetooth) trả về dữ liệu chuẩn xác mức pin, mạng Wi-Fi/4G, trạng thái BLE.</CheckTask>
<CheckTask id="w3-m3">3. FileLogger hỗ trợ append, read và clear file JSONL an toàn.</CheckTask>
<CheckTask id="w3-m4">4. BackgroundMonitor thực thi chu trình collect → JSON record → save định kỳ theo interval.</CheckTask>
<CheckTask id="w3-m5">5. Hoàn thành bảng ma trận hành vi 5 trạng thái (Foreground, Background, Return, OS Kill, Force-kill) trên Android và iOS.</CheckTask>
<CheckTask id="w3-m6">6. Màn hình chính tích hợp đầy đủ nút điều khiển tất cả các Service.</CheckTask>

---

## 2. Kế hoạch chi tiết từng ngày (Daily Plan)

### Thứ Hai (Day 1) — Tái cấu trúc NativeCore (~3 giờ)
- **Nội dung thực hiện**: Tách mã nguồn từ Tuần 1 và 2 thành 4 module độc lập: `PermissionManager`, `BluetoothManager`, `MediaManager`, `ApiClient`.
- **Bài tập thực hành**:
  <CheckTask id="w3-d1">Chuyển toàn bộ device logic ra khỏi Activity/ViewController vào 4 services có interface rõ ràng.</CheckTask>
- **Tiêu chuẩn đạt được**: Giao diện không còn bất kỳ dòng code điều khiển thiết bị nào; mỗi Service có interface công khai rõ ràng.

### Thứ Ba (Day 2) — Xây dựng Device Providers & Bắt đầu FileLogger (~2 giờ)
- **Nội dung thực hiện**: `BatteryProvider`, `NetworkProvider`, `BluetoothProvider`; hàm `FileLogger.append()`.
- **Bài tập thực hành**:
  <CheckTask id="w3-d2">Lấy dữ liệu từ 3 provider, đóng gói JSON và gọi FileLogger.append() ghi dòng log đầu tiên vào file nội bộ.</CheckTask>
- **Tiêu chuẩn đạt được**: File `monitor_logs.jsonl` được tạo thành công trong bộ nhớ thiết bị với dòng log hợp lệ đầu tiên.

### Thứ Tư (Day 3) — Hoàn thiện FileLogger & Khởi chạy BackgroundMonitor (~2 giờ)
- **Nội dung thực hiện**: `getLogs()`, `clearLogs()`; xây dựng vòng lặp đếm giờ (`interval`) trong `BackgroundMonitor`.
- **Bài tập thực hành**:
  <CheckTask id="w3-d3">Bật monitor interval 5s ở foreground, kiểm tra log ghi đều đặn; gọi clearLogs() làm sạch file.</CheckTask>
- **Tiêu chuẩn đạt được**: Monitor hoạt động ổn định ở chế độ Foreground; đọc và xóa nhật ký trơn tru.

### Thứ Năm (Day 4) — Kiểm thử ma trận 5 trạng thái nền tảng (~5 giờ)
- **Nội dung thực hiện**: Thử nghiệm hành vi thực tế của hệ điều hành với tiến trình nền:
  - **Android**: Thử nghiệm với Service thông thường vs [ForegroundService](https://developer.android.com/develop/background-work/services/foreground-services) có notification treo.
  - **iOS**: Quan sát hành vi bị đình trệ (freeze) sau ~30 giây khi bấm Home/thu nhỏ ứng dụng; tìm hiểu cơ chế [`BGTaskScheduler`](https://developer.apple.com/documentation/backgroundtasks/bgtaskscheduler).
- **Bài tập thực hành**:
  <CheckTask id="w3-d4">Thực nghiệm và lập bảng ghi nhận ma trận 5 trạng thái (Foreground, Background, Return, OS Kill, Force-kill) trên cả Android và iOS.</CheckTask>
- **Tiêu chuẩn đạt được**: Hoàn thành tài liệu ma trận hành vi so sánh giữa Android và iOS; ghi nhận rõ lý do iOS không hỗ trợ chu kỳ nền cố định.

### Thứ Sáu (Day 5) — Tích hợp toàn diện giao diện & Dự phòng (~2 giờ)
- **Nội dung thực hiện**: Nối toàn bộ các chức năng đã đóng gói vào giao diện người dùng.
- **Bài tập thực hành**:
  <CheckTask id="w3-d5">Một màn hình duy nhất vận hành: Quét/kết nối BLE, chụp/chọn ảnh, gọi GET/POST, bật/tắt Background Monitor và xem log.</CheckTask>
- **Tiêu chuẩn đạt được**: Mọi tính năng đều vận hành trơn tru thông qua giao diện người dùng.

---

## 3. Tài liệu tra cứu tham chiếu (Ref Docs)

### <span class="badge-android">Android Ref</span>
- [Foreground Services Official Guide](https://developer.android.com/develop/background-work/services/foreground-services): Hướng dẫn tạo thông báo liên tục cho tiến trình chạy nền.
- [Android Internal Storage & filesDir](https://developer.android.com/training/data-storage/app-specific): Hướng dẫn lưu file an toàn trong thư mục nội bộ của ứng dụng.

### <span class="badge-ios">iOS Ref</span>
- [Apple BackgroundTasks Framework](https://developer.apple.com/documentation/backgroundtasks): Cơ chế lập lịch tác vụ nền chuẩn mực của Apple.
- [FileManager Official Documentation](https://developer.apple.com/documentation/foundation/filemanager): Thao tác đọc/ghi file trong thư mục [`Documents`](https://developer.apple.com/documentation/foundation/filemanager/searchpathdirectory/documentdirectory) của iOS sandbox.
