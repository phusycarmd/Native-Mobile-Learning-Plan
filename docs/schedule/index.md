# Tổng quan Lộ trình thực hành (Schedule)

Chương trình đào tạo thực chiến kéo dài khoảng **3.5 tuần** với tổng thời lượng dự kiến **~56 giờ** làm việc tập trung (tương đương 2.5 – 3 giờ mỗi ngày làm việc từ Thứ Hai đến Thứ Sáu).

<GlobalProgress />

---

## 1. Phân bổ thời lượng theo tuần

| Tuần | Tên giai đoạn | Thời lượng | Cột mốc bàn giao (Demo Milestone) |
| :--- | :--- | :---: | :--- |
| [**Tuần 1**](/schedule/week-1) | **Fundamentals & Native UI** | ~17 giờ | Vỏ ứng dụng (App shell) chạy trên cả Android và iOS: ô nhập text, nút bấm, danh sách, ảnh, cập nhật trạng thái async. |
| [**Tuần 2**](/schedule/week-2) | **Native APIs & BLE Flow** | ~17 giờ | Hoàn chỉnh tính năng phần cứng: Xin quyền, trọn vẹn luồng BLE 9 bước, chụp/chọn ảnh Camera, gọi HTTP GET/POST. |
| [**Tuần 3**](/schedule/week-3) | **NativeCore & Background Monitor** | ~14 giờ | Tách toàn bộ kiến trúc NativeCore thành các Service độc lập; ghi log JSONL định kỳ trạng thái Pin/Mạng/BLE; kiểm thử ma trận 5 trạng thái OS. |
| [**Tuần 4**](/schedule/week-4) | **Testing & Refactoring (Nửa tuần)** | ~8 giờ | Hoàn tất bộ checklist kiểm thử End-to-End; dọn dẹp mã nguồn theo bảng kiểm định kiến trúc (Refactor Checklist); bàn giao bản demo hoàn thiện. |
| **Tổng cộng** | **Toàn khóa học** | **~56 giờ** | **Native Device Monitor Demo** hoàn chỉnh chạy độc lập trên Android và iOS. |

---

## 2. Bảng phân cấp mức độ ưu tiên (Priority Order)

<ProgressTracker prefix="p0" title="Tiến độ 11 mục tiêu cốt lõi (P0)" :total="11" />

Nếu gặp áp lực về mặt thời gian, lập trình viên phải bám sát thứ tự ưu tiên sau để đảm bảo những tính năng quan trọng nhất được hoàn thành trước:

### <span class="badge-p0">P0 — Bắt buộc phải hoàn thành (Must Complete)</span>
Các thành phần sống còn quyết định sự thành công của dự án:

<CheckTask id="p0-1" tag="P0">1. Nền tảng ngôn ngữ Kotlin (Kotlin fundamentals)</CheckTask>
<CheckTask id="p0-2" tag="P0">2. Nền tảng ngôn ngữ Swift (Swift fundamentals)</CheckTask>
<CheckTask id="p0-3" tag="P0">3. Giao diện người dùng Native cơ bản (Native UI basics)</CheckTask>
<CheckTask id="p0-4" tag="P0">4. Xử lý quyền hệ thống (Permission handling)</CheckTask>
<CheckTask id="p0-5" tag="P0">5. Quét thiết bị Bluetooth (BLE Scan)</CheckTask>
<CheckTask id="p0-6" tag="P0">6. Kết nối Bluetooth (BLE Connect)</CheckTask>
<CheckTask id="p0-7" tag="P0">7. Khám phá Service & Characteristic (BLE Discovery)</CheckTask>
<CheckTask id="p0-8" tag="P0">8. Đăng ký nhận thông báo dữ liệu liên tục (BLE Notification)</CheckTask>
<CheckTask id="p0-9" tag="P0">9. Đọc dữ liệu từ Characteristic (BLE Read / Receive)</CheckTask>
<CheckTask id="p0-10" tag="P0">10. Ngắt kết nối và phục hồi trạng thái (BLE Disconnect & Error recovery)</CheckTask>
<CheckTask id="p0-11" tag="P0">11. Dịch vụ giám sát thiết bị (Background Device Monitor)</CheckTask>

---

### <span class="badge-p1">P1 — Nên hoàn thành (Should Complete)</span>
Các tính năng mở rộng cần thiết cho bức tranh tổng thể:

<CheckTask id="p1-12" tag="P1">12. Chụp ảnh từ Camera native (Camera capture)</CheckTask>
<CheckTask id="p1-13" tag="P1">13. Chọn ảnh từ Thư viện (Media Picker)</CheckTask>
<CheckTask id="p1-14" tag="P1">14. Gọi HTTP API GET (Fetch JSON data)</CheckTask>
<CheckTask id="p1-15" tag="P1">15. Gọi HTTP API POST kèm JSON body</CheckTask>
<CheckTask id="p1-16" tag="P1">16. Ghi và đọc log file cục bộ (File Logging JSONL)</CheckTask>

---

### <span class="badge-p2">P2 — Có thể tinh giản nếu thiếu thời gian (Can Simplify)</span>
Các tính năng có thể giản lược hoặc làm ở mức tối thiểu:

<CheckTask id="p2-17" tag="P2">17. Xử lý nâng cao các mã lỗi HTTP API</CheckTask>
<CheckTask id="p2-18" tag="P2">18. Xử lý siêu dữ liệu đa phương tiện nâng cao</CheckTask>
<CheckTask id="p2-19" tag="P2">19. Giao diện nâng cao và bố cục phức tạp</CheckTask>
<CheckTask id="p2-20" tag="P2">20. Các thử nghiệm bổ sung về vòng đời ứng dụng</CheckTask>
