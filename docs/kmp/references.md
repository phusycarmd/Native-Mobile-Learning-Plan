# Tra cứu & Tài liệu tham khảo KMP

Tập hợp các tài liệu chính thức từ JetBrains, Apple, Google và các thư viện chuẩn trong hệ sinh thái Kotlin Multiplatform.

---

## 1. Tài liệu chính thức từ JetBrains & Google

| Chủ đề | Tài liệu tham khảo | Mục đích tra cứu |
| :--- | :--- | :--- |
| **KMP Overview** | [Kotlin Multiplatform Docs](https://kotlinlang.org/docs/multiplatform.html) | Cấu trúc project, Gradle DSL, Source sets |
| **Jetpack Compose (Android)** | [Android Jetpack Compose Docs](https://developer.android.com/develop/ui/compose) | Declarative UI cho Android, Material 3, ViewModel |
| **SwiftUI (iOS)** | [Apple SwiftUI Docs](https://developer.apple.com/documentation/swiftui) | Declarative UI cho iOS, State & Data Flow, Navigation |
| **Kotlin/Native Interop** | [Kotlin/Native as Apple Framework](https://kotlinlang.org/docs/apple-framework.html) | Cơ chế ánh xạ kiểu dữ liệu Kotlin <-> Objective-C/Swift |
| **Kotlin Coroutines** | [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) | Flow, StateFlow, Coroutine Dispatchers trên iOS |
| **Kotlin Serialization** | [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) | JSON Parsing đa nền tảng |

---

## 2. Các thư viện chuẩn trong hệ sinh thái KMP

| Thư viện | Tác giả / Tổ chức | Mục đích sử dụng | Khuyến nghị cho dự án |
| :--- | :--- | :--- | :---: |
| **Ktor Client** | JetBrains | HTTP Client cho iOS (Darwin) & Android (OkHttp) | ⭐ Khuyên dùng (P1) |
| **Okio** | Square | Thao tác File System, Path, ByteString | ⭐ Khuyên dùng (P1) |
| **Kable** | Juul Labs | Thư viện Bluetooth Low Energy (BLE) chuẩn KMP | Tham khảo thêm (P0) |
| **Koin Multiplatform** | Kotzilla | Dependency Injection nhẹ nhàng, không cần code-gen | Tham khảo thêm (P2) |
| **Moko Permissions** | IceRock | Quản lý xin quyền hệ thống đa nền tảng | Tham khảo thêm (P1) |

---

## 3. Bảng ánh xạ kiểu dữ liệu Kotlin/Native và Apple Swift/Obj-C

Khi làm việc trong `demo/kmp/sharedLogic/src/iosMain`, bạn có thể gọi trực tiếp các API của Apple. Bảng dưới đây thể hiện cách Kotlin/Native chuyển đổi kiểu dữ liệu:

| Kotlin Type | Objective-C Type | Swift Type | Lưu ý |
| :--- | :--- | :--- | :--- |
| `Byte`, `Short`, `Int`, `Long` | `int8_t`, `int16_t`, `int32_t`, `int64_t` | `Int8`, `Int16`, `Int32`, `Int64` | Số nguyên có dấu |
| `Float`, `Double` | `float`, `double` | `Float`, `Double` | Số thực |
| `Boolean` | `BOOL` | `Bool` | Đúng / Sai |
| `String` | `NSString *` | `String` | Tự động chuyển đổi UTF-8 |
| `List<T>` | `NSArray *` | `Array<T>` | Ánh xạ sang mảng |
| `Map<K, V>` | `NSDictionary *` | `Dictionary<K, V>` | Ánh xạ sang dictionary |
| `ByteArray` | `NSData *` | `Data` | Sử dụng để truyền nhận mảng byte BLE |

---

## 4. Những lưu ý sống còn về Memory & Threading trong Kotlin/Native

1. **Kotlin/Native Memory Manager mới (từ Kotlin 1.7.20+):**
   - Không còn khái niệm `freeze()` phức tạp như trước.
   - Các đối tượng có thể tự do chia sẻ giữa các threads mà không gây ngoại lệ `InvalidMutabilityException`.
   - Trình thu gom rác (Garbage Collector) chạy song song trong nền giống như JVM GC.
2. **Main Thread trên iOS:**
   - Các tương tác liên quan đến UI hoặc cập nhật `StateFlow` gắn với Composable phải được điều phối về `Dispatchers.Main`.
   - Các tác vụ tốn CPU hoặc xử lý mảng byte nên đẩy sang `Dispatchers.Default`.
