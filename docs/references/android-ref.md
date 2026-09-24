# Tra cứu Tài liệu Android (Android Reference)

Tổng hợp các liên kết tài liệu chính thức từ Google, mã cấu hình mẫu và các đoạn mã tra cứu nhanh dành cho nền tảng Android.

---

## 1. Cổng tài liệu chính thức (Official Links)

| Chuyên đề | Liên kết tài liệu chính thức của Google |
| :--- | :--- |
| **Android Developers Portal** | [developer.android.com](https://developer.android.com) |
| **Kotlin Documentation** | [kotlinlang.org/docs](https://kotlinlang.org/docs/home.html) |
| **Bluetooth Low Energy (BLE)** | [developer.android.com/develop/connectivity/bluetooth/ble](https://developer.android.com/develop/connectivity/bluetooth/ble) |
| **BluetoothGatt Reference** | [developer.android.com/reference/android/bluetooth/BluetoothGatt](https://developer.android.com/reference/android/bluetooth/BluetoothGatt) |
| **App Permissions Guide** | [developer.android.com/training/permissions/requesting](https://developer.android.com/training/permissions/requesting) |
| **Android Photo Picker** | [developer.android.com/training/data-storage/shared/photopicker](https://developer.android.com/training/data-storage/shared/photopicker) |
| **Kotlin Coroutines Guide** | [developer.android.com/kotlin/coroutines](https://developer.android.com/kotlin/coroutines) |
| **Foreground Services Guide** | [developer.android.com/develop/background-work/services/foreground-services](https://developer.android.com/develop/background-work/services/foreground-services) |
| **Battery Monitoring Guide** | [developer.android.com/topic/performance/power/battery-monitoring](https://developer.android.com/topic/performance/power/battery-monitoring) |

---

## 2. File cấu hình AndroidManifest.xml hoàn chỉnh cho dự án

Dưới đây là mẫu khai báo đầy đủ tất cả các quyền cần thiết cho toàn bộ 6 module của ứng dụng:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- 1. Quyền Mạng & Internet -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!-- 2. Quyền Camera -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" android:required="false" />

    <!-- 3. Quyền Bluetooth trên Android 12 (API 31) trở lên -->
    <uses-permission 
        android:name="android.permission.BLUETOOTH_SCAN"
        android:usesPermissionFlags="neverForLocation" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

    <!-- 4. Quyền Bluetooth cho Android 11 (API 30) trở xuống (Backward Compatibility) -->
    <uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:maxSdkVersion="30" />

    <!-- 5. Quyền Chạy nền Foreground Service (nếu dùng) -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="Device Monitor"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.DeviceMonitor"
        android:usesCleartextTraffic="true">
        
        <activity
            android:name=".ui.MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>
</manifest>
```

---

## 3. Khung mã nguồn BluetoothGattCallback chuẩn

```kotlin
private val gattCallback = object : BluetoothGattCallback() {
    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        when (newState) {
            BluetoothProfile.STATE_CONNECTED -> {
                Log.d("BLE", "Connected to GATT server.")
                gatt.discoverServices()
            }
            BluetoothProfile.STATE_DISCONNECTED -> {
                Log.d("BLE", "Disconnected from GATT server.")
                gatt.close()
            }
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            for (service in gatt.services) {
                Log.d("BLE", "Discovered Service: ${service.uuid}")
            }
        }
    }

    // Android 13 (API 33) trở lên khuyên dùng overload có sẵn ByteArray value
    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        Log.d("BLE", "Notify data received: ${value.joinToString { "%02X".format(it) }}")
    }

    // Tương thích ngược phiên bản cũ:
    @Deprecated("Deprecated in Java")
    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        val value = characteristic.value
        Log.d("BLE", "Legacy Notify: ${value?.joinToString { "%02X".format(it) }}")
    }
}
```

---

## 4. Lệnh gỡ lỗi ADB hữu ích

```bash
# Xem log lọc theo tag Bluetooth
adb logcat -s BLE:D DeviceMonitor:D

# Giả lập thay đổi mức pin (kiểm thử BatteryProvider)
adb shell dumpsys battery set level 50
adb shell dumpsys battery reset

# Giả lập ngắt kết nối mạng
adb shell svc wifi disable
adb shell svc data disable
```
