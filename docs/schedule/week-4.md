# Tuần 4: Kiểm thử End-to-End & Tối ưu Hoàn thiện

Giai đoạn nửa tuần cuối cùng (~8 giờ) tập trung toàn bộ nguồn lực vào việc: **chạy kiểm thử toàn diện từ đầu đến cuối (End-to-End Testing)** trên cả hai thiết bị thật Android & iOS, rà soát mã nguồn theo bộ tiêu chuẩn **Refactoring Checklist** và đóng gói bàn giao demo.

---

## 1. Mục tiêu bàn giao (Milestone Deliverables)

<CheckTask id="w4-m1">1. Toàn bộ các luồng tính năng vượt qua bộ tiêu chí kiểm thử End-to-End (E2E Checklist).</CheckTask>
<CheckTask id="w4-m2">2. Tất cả 8 tiêu chí trong Refactor Checklist đều đạt trạng thái xanh (Green).</CheckTask>
<CheckTask id="w4-m3">3. Ứng dụng Native Device Monitor Demo chạy mượt mà, không giật lag, không rò rỉ bộ nhớ trên cả Android và iOS.</CheckTask>

---

## 2. Kế hoạch chi tiết từng ngày (Daily Plan)

### Thứ Hai (Day 1) — Kiểm thử toàn diện End-to-End (~4 giờ)

<ProgressTracker prefix="w4-e2e" title="Tiến độ kiểm thử End-to-End" :total="17" />

#### Quyền hạn (Permission)
<CheckTask id="w4-e2e-1">Lần đầu mở tính năng: Hiện hộp thoại xin quyền hệ thống.</CheckTask>
<CheckTask id="w4-e2e-2">Bấm từ chối (Deny): Ứng dụng không crash, hiển thị thông báo giải thích.</CheckTask>
<CheckTask id="w4-e2e-3">Bấm thử lại hoặc mở Settings: Cấp quyền thành công -> tính năng mở được ngay.</CheckTask>

#### Bluetooth BLE
<CheckTask id="w4-e2e-4">Quét thiết bị (Scan) → Hiện đúng tên và RSSI.</CheckTask>
<CheckTask id="w4-e2e-5">Kết nối (Connect) → Khám phá Services/Characteristics → Đăng ký nhận Notify.</CheckTask>
<CheckTask id="w4-e2e-6">Nhận dữ liệu stream đều đặn → Đọc thử Characteristic.</CheckTask>
<CheckTask id="w4-e2e-7">Ngắt kết nối thủ công (Disconnect) → Trạng thái về idle, giải phóng tài nguyên.</CheckTask>
<CheckTask id="w4-e2e-8">Ngắt đột ngột (Tắt peripheral giữa chừng) → Bắt được lỗi và cập nhật UI.</CheckTask>

#### Camera & Thư viện ảnh
<CheckTask id="w4-e2e-9">Chụp ảnh từ camera → Hiển thị lên màn hình.</CheckTask>
<CheckTask id="w4-e2e-10">Chọn ảnh từ thư viện → Hiển thị lên màn hình.</CheckTask>

#### HTTP API Client
<CheckTask id="w4-e2e-11">GET thành công → Hiển thị JSON và status 200.</CheckTask>
<CheckTask id="w4-e2e-12">POST thành công → Gửi đúng body và nhận status 201/200.</CheckTask>
<CheckTask id="w4-e2e-13">Tắt mạng hoặc nhập sai URL → Báo lỗi mạng rõ ràng, không treo app.</CheckTask>

#### Background Monitor
<CheckTask id="w4-e2e-14">Bật monitor → Đọc đúng pin, wifi, cellular, bluetooth.</CheckTask>
<CheckTask id="w4-e2e-15">Ghi nối dòng vào file JSONL → Đọc lại và xem log trên UI.</CheckTask>
<CheckTask id="w4-e2e-16">Thu nhỏ app, mở lại app → Tiếp tục hoạt động bình thường.</CheckTask>
<CheckTask id="w4-e2e-17">Bấm dừng monitor → Tiến trình dừng hẳn, không còn chạy ngầm.</CheckTask>

---

### Thứ Ba (Day 2) — Rà soát mã nguồn & Tối ưu hoàn thiện (~4 giờ)

<ProgressTracker prefix="w4-rf" title="Tiến độ Refactor Checklist" :total="8" />

<CheckTask id="w4-rf-1">1. Giao diện (UI) tuyệt đối không chứa logic điều khiển thiết bị trực tiếp.</CheckTask>
<CheckTask id="w4-rf-2">2. Các Service (Bluetooth, Media, Api,...) có trách nhiệm độc lập, ranh giới rõ ràng.</CheckTask>
<CheckTask id="w4-rf-3">3. Mọi tác vụ bất đồng bộ (Async/Coroutines/Task) đều xử lý lỗi đầy đủ (try-catch, Result).</CheckTask>
<CheckTask id="w4-rf-4">4. Giải phóng tài nguyên triệt để (Hủy đăng ký GATT callback, hủy Coroutine Job/Task khi màn hình đóng).</CheckTask>
<CheckTask id="w4-rf-5">5. Trạng thái kết nối Bluetooth luôn nhất quán và không bị race-condition.</CheckTask>
<CheckTask id="w4-rf-6">6. Background Monitor có thể bật và tắt một cách sạch sẽ, không để lại tiến trình ma (zombie process).</CheckTask>
<CheckTask id="w4-rf-7">7. Không có thư viện phụ thuộc thừa thãi (No dead dependencies).</CheckTask>
<CheckTask id="w4-rf-8">8. Không có đoạn code lặp lại logic phần cứng ở nhiều nơi.</CheckTask>

---

## 3. Tổng kết nghiệm thu chương trình

Sau khi hoàn tất Tuần 4:
1. Lập trình viên tự tin làm việc trên cả hai hệ sinh thái **Android (Kotlin)** và **iOS (Swift)**.
2. Hiểu rõ cách tương tác với phần cứng thiết bị di động ở mức native.
3. Sẵn sàng cho các giai đoạn tiếp theo (như đóng gói thư viện Native thành plugin Capacitor/Ionic hoặc phát triển ứng dụng di động độc lập).
