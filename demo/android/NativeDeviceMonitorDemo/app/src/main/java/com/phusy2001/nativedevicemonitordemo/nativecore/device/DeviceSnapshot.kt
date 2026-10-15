package com.phusy2001.nativedevicemonitordemo.nativecore.device

import org.json.JSONObject
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Bản ghi trạng thái phần cứng của thiết bị tại một thời điểm (Snapshot).
 * Được dùng để ghi log định dạng JSONL theo chu kỳ (Tuần 3 - Bài tập 3.2 & 3.3).
 */
data class DeviceSnapshot(
    val timestamp: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
    val battery: Int,
    val wifiConnected: Boolean,
    val cellularAvailable: Boolean,
    val bluetoothEnabled: Boolean,
    val bluetoothConnected: Boolean
) {
    /**
     * Chuyển đổi thành chuỗi JSON một dòng (chuẩn JSONL).
     */
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("timestamp", timestamp)
        obj.put("battery", battery)
        obj.put("wifiConnected", wifiConnected)
        obj.put("cellularAvailable", cellularAvailable)
        obj.put("bluetoothEnabled", bluetoothEnabled)
        obj.put("bluetoothConnected", bluetoothConnected)
        return obj.toString()
    }

    companion object {
        /**
         * Phân tích một dòng JSON thành đối tượng [DeviceSnapshot].
         * Hỗ trợ tương thích ngược với các tên key rút gọn ("wifi", "cellular", "ble").
         */
        fun fromJson(json: String): DeviceSnapshot? {
            return try {
                val obj = JSONObject(json.trim())
                DeviceSnapshot(
                    timestamp = obj.optString("timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now())),
                    battery = obj.optInt("battery", 0),
                    wifiConnected = obj.optBoolean("wifiConnected", obj.optBoolean("wifi", false)),
                    cellularAvailable = obj.optBoolean("cellularAvailable", obj.optBoolean("cellular", false)),
                    bluetoothEnabled = obj.optBoolean("bluetoothEnabled", obj.optBoolean("ble", false)),
                    bluetoothConnected = obj.optBoolean("bluetoothConnected", false)
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
