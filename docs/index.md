---
layout: home

hero:
  name: "Native Mobile Learning"
  text: "Lộ trình làm chủ Kotlin & Swift Native"
  tagline: "Xây dựng ứng dụng Native Device Monitor Demo với kiến trúc dịch vụ tái sử dụng, Bluetooth BLE, và xử lý nền chuẩn quy chuẩn OS."
  actions:
    - theme: brand
      text: Bắt đầu học ngay
      link: /guide/overview
    - theme: alt
      text: Lộ trình 4 tuần (Schedule)
      link: /schedule/
    - theme: alt
      text: Tra cứu Android & iOS Ref
      link: /references/comparison

features:
  - icon: 📱
    title: Hands-on Native (Không dùng AI sinh code)
    details: "Viết từng dòng code bằng tay để thấu hiểu tường tận vòng đời ứng dụng (Lifecycle), Context, UI threading và luồng xử lý bất đồng bộ."
  - icon: ⚡
    title: Phân tầng NativeCore (UI != Device Logic)
    details: "Tách biệt hoàn toàn giao diện người dùng khỏi phần cứng thông qua các Service: PermissionManager, BluetoothManager, MediaManager, ApiClient, BackgroundMonitor."
  - icon: 📡
    title: Bluetooth BLE 9 Bước Hoàn Chỉnh
    details: "Làm chủ trọn vẹn luồng BLE thực tế: Scan → Connect → Discover Services → Find Characteristic → Subscribe Notify → Read Data → Disconnect & Error Handling."
  - icon: 🔋
    title: Background Monitor & JSONL Logging
    details: "Giám sát định kỳ Pin, Wi-Fi, Cellular, Bluetooth; lưu trữ JSONL cục bộ; kiểm thử thực tế đa trạng thái: Foreground, Background, Return, OS Termination, Force-kill."
  - icon: 📚
    title: Đính kèm Tài liệu Tra cứu (Ref Docs)
    details: "Tích hợp đầy đủ các liên kết tra cứu chính thức từ Google Android Developers và Apple Developer Documentation cho từng API và Permission."
  - icon: 🎯
    title: Phân cấp Ưu tiên P0 / P1 / P2
    details: "Lộ trình ~56 giờ học (~3.5 tuần, 2.5–3h/ngày) với các mốc kiểm thử rõ ràng, đảm bảo mục tiêu tối quan trọng (P0) luôn hoàn thành đúng hạn."
---

<div class="vp-doc" style="max-width: 900px; margin: 3rem auto 0 auto;">

::: tip QUY TẮC CỐT LÕI CỦA DỰ ÁN
**Không dùng AI để sinh code demo**. Mọi thành phần phải được viết bằng tay. Mục tiêu của kế hoạch này là hiểu sâu sắc về lập trình Native; việc dùng AI tạo code sẽ triệt tiêu mục tiêu học tập. AI và tài liệu chỉ được sử dụng cho mục đích tra cứu khái niệm và APIs.
:::

<GlobalProgress />

## Cấu trúc tài liệu

Tài liệu được biên soạn dựa trên chương trình đào tạo của Team **CarMDConnect**, chia thành 3 phần chính:

| Phần | Nội dung chính | Đối tượng & Mục tiêu |
| :--- | :--- | :--- |
| [**1. Hướng dẫn & Kiến trúc**](/guide/overview) | Mục tiêu, Scope, Kiến trúc NativeCore, Public Interfaces | Nắm vững nguyên lý tách biệt UI và Native Logic |
| [**2. Part I — Modules kiến thức**](/modules/ui) | 6 Module chuyên sâu: UI, Permission, BLE, Media, API, Background Monitor | Kèm tài liệu tham khảo chính thức của **Android** & **iOS** |
| [**3. Part II — Lộ trình tuần**](/schedule/) | Kế hoạch chi tiết từ Tuần 1 đến Tuần 4 (~56 giờ) | Bài tập từng ngày, tiêu chí bàn giao (Milestones) & Thứ tự ưu tiên |
| [**4. Trung tâm tra cứu**](/references/comparison) | Bảng đối chiếu song song Android vs iOS, Manifest & Info.plist keys | Tra cứu nhanh hàm, lớp và cấu hình quyền |

</div>
