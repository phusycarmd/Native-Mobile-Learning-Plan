# Tra cứu Tài liệu iOS (iOS Reference)

Tổng hợp các liên kết tài liệu chính thức từ Apple, file cấu hình [`Info.plist`](https://developer.apple.com/documentation/bundleresources/information_property_list) mẫu và các đoạn mã tra cứu nhanh dành cho nền tảng iOS.

---

## 1. Cổng tài liệu chính thức của Apple (Official Links)

| Chuyên đề | Liên kết tài liệu chính thức của Apple |
| :--- | :--- |
| **Apple Developer Documentation** | [developer.apple.com/documentation](https://developer.apple.com/documentation) |
| **Swift Programming Language** | [docs.swift.org](https://docs.swift.org/swift-book/) |
| **CoreBluetooth Framework** | [developer.apple.com/documentation/corebluetooth](https://developer.apple.com/documentation/corebluetooth) |
| **CBCentralManager Reference** | [developer.apple.com/documentation/corebluetooth/cbcentralmanager](https://developer.apple.com/documentation/corebluetooth/cbcentralmanager) |
| **CBPeripheral Reference** | [developer.apple.com/documentation/corebluetooth/cbperipheral](https://developer.apple.com/documentation/corebluetooth/cbperipheral) |
| **User Privacy & Permissions** | [developer.apple.com/documentation/uikit/protecting_the_user_s_privacy](https://developer.apple.com/documentation/uikit/protecting_the_user_s_privacy) |
| **PhotosUI & PHPicker** | [developer.apple.com/documentation/photokit/phpickerviewcontroller](https://developer.apple.com/documentation/photokit/phpickerviewcontroller) |
| **URLSession Networking** | [developer.apple.com/documentation/foundation/urlsession](https://developer.apple.com/documentation/foundation/urlsession) |
| **BackgroundTasks Framework** | [developer.apple.com/documentation/backgroundtasks](https://developer.apple.com/documentation/backgroundtasks) |
| **Network.framework (NWPathMonitor)** | [developer.apple.com/documentation/network/nwpathmonitor](https://developer.apple.com/documentation/network/nwpathmonitor) |

---

## 2. File cấu hình Info.plist mẫu cho dự án

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleName</key>
    <string>DeviceMonitor</string>
    <key>CFBundleIdentifier</key>
    <string>com.carmdconnect.devicemonitor</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0</string>
    <key>CFBundleVersion</key>
    <string>1</string>

    <!-- 1. Quyền Bluetooth -->
    <key>NSBluetoothAlwaysUsageDescription</key>
    <string>Ứng dụng cần sử dụng Bluetooth để quét và kết nối với cảm biến BLE ngoại vi.</string>
    <key>NSBluetoothPeripheralUsageDescription</key>
    <string>Ứng dụng cần truy cập Bluetooth để giao tiếp với thiết bị đo.</string>

    <!-- 2. Quyền Camera -->
    <key>NSCameraUsageDescription</key>
    <string>Ứng dụng cần mở máy ảnh để chụp hình phục vụ kiểm thử MediaManager.</string>

    <!-- 3. Quyền Thư viện ảnh -->
    <key>NSPhotoLibraryUsageDescription</key>
    <string>Ứng dụng cần truy cập thư viện ảnh để chọn hình ảnh.</string>

    <!-- 4. Cho phép kiểm thử HTTP nội bộ nếu cần (App Transport Security) -->
    <key>NSAppTransportSecurity</key>
    <dict>
        <key>NSAllowsArbitraryLoads</key>
        <true/>
    </dict>
</dict>
</plist>
```

---

## 3. Khung mã nguồn CoreBluetooth chuẩn (Swift)

```swift
import Foundation
import CoreBluetooth

class BleServiceManager: NSObject, CBCentralManagerDelegate, CBPeripheralDelegate {
    private var centralManager: CBCentralManager!
    private var connectedPeripheral: CBPeripheral?

    override init() {
        super.init()
        centralManager = CBCentralManager(delegate: self, queue: nil)
    }

    // 1. Lắng nghe trạng thái Bluetooth của điện thoại
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        switch central.state {
        case .poweredOn:
            print("Bluetooth is ON. Ready to scan.")
        case .poweredOff:
            print("Bluetooth is turned OFF.")
        case .unauthorized:
            print("Bluetooth permission denied.")
        default:
            break
        }
    }

    // 2. Tìm thấy thiết bị khi quét
    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        print("Discovered: \(peripheral.name ?? "Unknown") [RSSI: \(RSSI)]")
    }

    // 3. Kết nối thành công
    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        print("Connected to \(peripheral.name ?? "device"). Discovering services...")
        connectedPeripheral = peripheral
        peripheral.delegate = self
        peripheral.discoverServices(nil)
    }

    // 4. Mất kết nối
    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        print("Disconnected: \(error?.localizedDescription ?? "Clean disconnect")")
        connectedPeripheral = nil
    }

    // 5. Khám phá Services
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        guard let services = peripheral.services else { return }
        for service in services {
            print("Service UUID: \(service.uuid)")
            peripheral.discoverCharacteristics(nil, for: service)
        }
    }

    // 6. Nhận dữ liệu Notification hoặc kết quả lệnh Read
    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        if let data = characteristic.value {
            let hex = data.map { String(format: "%02hhX", $0) }.joined(separator: " ")
            print("Received bytes: \(hex)")
        }
    }
}
```

---

## 4. Lưu ý khi kiểm thử trên Simulator vs Thiết bị thật

::: warning LƯU Ý PHẦN CỨNG (CRITICAL)
- **Bluetooth BLE**: **Xcode Simulator KHÔNG hỗ trợ quét hoặc kết nối Bluetooth BLE thực tế**. Bạn **bắt buộc phải có một thiết bị iPhone/iPad thật** để kiểm thử luồng BLE.
- **Camera**: Simulator không có camera thực (sẽ hiển thị khung hình giả lập hoặc báo lỗi không khả dụng). Cần máy thật để test [`UIImagePickerController`](https://developer.apple.com/documentation/uikit/uiimagepickercontroller) với Camera.
- **Photo Picker (PHPicker)**: Chạy tốt trên Simulator (dùng thư viện ảnh mặc định của simulator với [`PHPickerViewController`](https://developer.apple.com/documentation/photokit/phpickerviewcontroller)).
- **Network (URLSession)**: Hoạt động bình thường trên Simulator thông qua mạng internet của máy Mac qua [`URLSession`](https://developer.apple.com/documentation/foundation/urlsession).
:::
