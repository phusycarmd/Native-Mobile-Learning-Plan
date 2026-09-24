# Tuần 1: Fundamentals & Native UI

Cột mốc mục tiêu tuần 1: **Xây dựng vỏ ứng dụng (App Shell) chạy hoàn chỉnh trên cả Android và iOS Simulator** gồm ô nhập liệu, nút bấm, danh sách, hình ảnh, chỉ báo chờ và cập nhật trạng thái bất đồng bộ.

---

## 1. Mục tiêu bàn giao (Milestone Deliverables)

<ProgressTracker prefix="w1" title="Tiến độ Tuần 1" :total="10" />

<CheckTask id="w1-m1">1. Ứng dụng biên dịch thành công và chạy mượt mà trên Android và iOS Simulator.</CheckTask>
<CheckTask id="w1-m2">2. Màn hình có đủ: TextField, Button, RecyclerView/List, ImageView, ProgressBar/Spinner, Status TextView.</CheckTask>
<CheckTask id="w1-m3">3. Nhập text và bấm nút sẽ thêm phần tử mới vào danh sách.</CheckTask>
<CheckTask id="w1-m4">4. Bấm nút kích hoạt tác vụ bất đồng bộ giả lập (chờ ~1s với spinner), cập nhật text trạng thái không làm đơ giao diện.</CheckTask>
<CheckTask id="w1-m5">5. Ghi lại log toàn bộ các hàm vòng đời (Lifecycle) từ khi khởi tạo tới khi đóng app.</CheckTask>

---

## 2. Kế hoạch chi tiết từng ngày (Daily Plan)

### Thứ Hai (Day 1) — Kotlin Fundamentals (~3 giờ)
- **Nội dung học**: Biến ([`val`](https://kotlinlang.org/docs/basic-syntax.html#variables) / [`var`](https://kotlinlang.org/docs/basic-syntax.html#variables)), hàm, [`class`](https://kotlinlang.org/docs/classes.html) & [`interface`](https://kotlinlang.org/docs/interfaces.html), cơ chế an toàn null ([`?`](https://kotlinlang.org/docs/null-safety.html#nullable-types-and-non-nullable-types), [`?:`](https://kotlinlang.org/docs/null-safety.html#elvis-operator), [`!!`](https://kotlinlang.org/docs/null-safety.html#not-null-assertion-operator)), [`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/) / [`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/), biểu thức [`lambda`](https://kotlinlang.org/docs/lambdas.html), khái niệm cơ bản về [Coroutines](https://kotlinlang.org/docs/coroutines-guide.html).
- **Bài tập thực hành**:
  <CheckTask id="w1-d1" tag="Android">Viết chương trình Kotlin: data class Device(name, rssi), lọc List bằng lambda, in null-safe với ?.let.</CheckTask>
- **Tiêu chuẩn đạt được**: Tự tin đọc hiểu cú pháp Kotlin, giải thích được Null Safety và tự viết được lambda không cần trợ giúp.

### Thứ Ba (Day 2) — Swift Fundamentals (~3 giờ)
- **Nội dung học**: Biến ([`let`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/thebasics/#Constants-and-Variables) / [`var`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/thebasics/#Constants-and-Variables)), hàm, phân biệt [`class`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/classesandstructures/) (reference type) vs [`struct`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/classesandstructures/) (value type), [`protocol`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/protocols/), [`optional`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/thebasics/#Optionals) ([`?`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/thebasics/#Optionals), [`!`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/thebasics/#Implicitly-Unwrapped-Optionals), [`if let`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/thebasics/#Optional-Binding), [`guard let`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/controlflow/#Early-Exit)), [`closure`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/closures/), khái niệm [`async/await`](https://docs.swift.org/swift-book/documentation/the-swift-programming-language/concurrency/).
- **Bài tập thực hành**:
  <CheckTask id="w1-d2" tag="iOS">Viết Swift Playground: struct Device, lọc mảng với closure và compactMap, mở gói optional với guard let.</CheckTask>
- **Tiêu chuẩn đạt được**: Hiểu sâu cơ chế Optional, phân biệt rõ Struct vs Class, viết thành thạo closure trong Swift.

### Thứ Tư (Day 3) — Android Fundamentals & ComponentActivity (~3 giờ)
- **Nội dung học**:
  - Cấu trúc project Android Studio, vai trò của [`ComponentActivity`](https://developer.android.com/reference/androidx/activity/ComponentActivity), [`Context`](https://developer.android.com/reference/android/content/Context), [`Intent`](https://developer.android.com/reference/android/content/Intent), [`AndroidManifest.xml`](https://developer.android.com/guide/topics/manifest/manifest-intro), vòng đời Activity ([`onCreate`](https://developer.android.com/reference/android/app/Activity#onCreate(android.os.Bundle)), [`onStart`](https://developer.android.com/reference/android/app/Activity#onStart()), [`onResume`](https://developer.android.com/reference/android/app/Activity#onResume()), [`onPause`](https://developer.android.com/reference/android/app/Activity#onPause()), [`onStop`](https://developer.android.com/reference/android/app/Activity#onStop()), [`onDestroy`](https://developer.android.com/reference/android/app/Activity#onDestroy())), hệ thống build Gradle.
  - Chuẩn Jetpack Compose: Khởi tạo giao diện bằng `setContent { ... }` thay thế hoàn toàn XML và `setContentView`.
  - Kỹ năng đọc mã nguồn Java: Nhận diện [`class`](https://docs.oracle.com/javase/tutorial/java/javaOO/classes.html), [`interface`](https://docs.oracle.com/javase/tutorial/java/IandI/createinterface.html), [`@Override`](https://docs.oracle.com/javase/tutorial/java/annotations/predefined.html), getter/setter, anonymous callback.
- **Bài tập thực hành**:
  <CheckTask id="w1-d3" tag="Android">Tạo app Android (ComponentActivity + Compose setContent) in log vòng đời onCreate->onDestroy; đọc và ghi chú một file Java mẫu.</CheckTask>
- **Tiêu chuẩn đạt được**: App chạy và hiển thị đầy đủ log lifecycle trong Logcat; đọc hiểu luồng code Java mà không cần phải viết Java.

### Thứ Năm (Day 4) — Declarative UI: Jetpack Compose & SwiftUI (~4 giờ)
- **Nội dung học**:
  - Tư duy Declarative UI: Khai báo giao diện theo trạng thái (State-driven UI).
  - Android Jetpack Compose: [`Text`](https://developer.android.com/develop/ui/compose/text), [`OutlinedTextField`](https://developer.android.com/develop/ui/compose/text/user-input), [`Button`](https://developer.android.com/develop/ui/compose/components/button), [`LazyColumn`](https://developer.android.com/develop/ui/compose/lists), [`Column`](https://developer.android.com/develop/ui/compose/layouts/basics), [`Row`](https://developer.android.com/develop/ui/compose/layouts/basics).
  - iOS SwiftUI: [`Text`](https://developer.apple.com/documentation/swiftui/text), [`TextField`](https://developer.apple.com/documentation/swiftui/textfield), [`Button`](https://developer.apple.com/documentation/swiftui/button), [`List`](https://developer.apple.com/documentation/swiftui/list), [`VStack`](https://developer.apple.com/documentation/swiftui/vstack), [`HStack`](https://developer.apple.com/documentation/swiftui/hstack).
- **Bài tập thực hành**:
  <CheckTask id="w1-d4" tag="iOS">Dựng bố cục màn hình bằng Jetpack Compose (Android) và SwiftUI (iOS) gồm tiêu đề, ô nhập, nút bấm và danh sách cuộn.</CheckTask>
- **Tiêu chuẩn đạt được**: Cả hai app Android & iOS đều khởi chạy thành công và bố trí các điều khiển mượt mà bằng Declarative UI.

### Thứ Sáu (Day 5) — Quản lý State & Cập nhật Async Recompose (~4 giờ)
- **Nội dung học**: Quản lý State (`mutableStateOf` / `@State`); kích hoạt tác vụ nền qua Coroutines / Task; cập nhật State trên Main Thread kích hoạt Recomposition tự động; hiển thị chỉ báo chờ (`CircularProgressIndicator` / `ProgressView`).
- **Bài tập thực hành**:
  <CheckTask id="w1-d5">Bấm nút thêm phần tử vào danh sách State; nút async giả lập tải 1.5s hiện spinner và cập nhật nhãn trạng thái tự động recompose.</CheckTask>
- **Tiêu chuẩn đạt được**: Bàn giao phiên bản Tuần 1 chạy hoàn hảo trên cả 2 nền tảng với kiến trúc Declarative UI.

---

## 3. Tài liệu tra cứu tham chiếu (Ref Docs)

### <span class="badge-android">Android Ref</span>
- [Kotlin Official Documentation](https://kotlinlang.org/docs/home.html): Cẩm nang ngôn ngữ Kotlin.
- [Android Activity Lifecycle Guide](https://developer.android.com/guide/components/activities/activity-lifecycle): Sơ đồ chi tiết vòng đời Activity từ Google.
- [Create dynamic lists with RecyclerView](https://developer.android.com/develop/ui/views/layout/recyclerview).

### <span class="badge-ios">iOS Ref</span>
- [The Swift Programming Language Book](https://docs.swift.org/swift-book/): Tài liệu chính thức về Swift từ Apple.
- [UIViewController Lifecycle Documentation](https://developer.apple.com/documentation/uikit/uiviewcontroller).
- [Apple UITableView Programming Guide](https://developer.apple.com/documentation/uikit/uitableview).
