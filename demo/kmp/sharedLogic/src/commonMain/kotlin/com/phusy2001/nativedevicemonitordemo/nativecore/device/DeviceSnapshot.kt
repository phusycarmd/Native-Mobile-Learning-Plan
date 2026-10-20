package com.phusy2001.nativedevicemonitordemo.nativecore.device

/**
 * Bản ghi trạng thái phần cứng của thiết bị tại một thời điểm (Snapshot).
 * Được dùng để ghi log định dạng JSONL theo chu kỳ đa nền tảng.
 */
data class DeviceSnapshot(
    val timestamp: String,
    val battery: Int,
    val wifiConnected: Boolean,
    val cellularAvailable: Boolean,
    val bluetoothEnabled: Boolean,
    val bluetoothConnected: Boolean
) {
    /**
     * Chuyển đổi thành chuỗi JSON một dòng (chuẩn JSONL) mà không phụ thuộc thư viện riêng của OS.
     */
    fun toJson(): String {
        return buildString {
            append("{")
            append("\"timestamp\":\"").append(escapeJson(timestamp)).append("\",")
            append("\"battery\":").append(battery).append(",")
            append("\"wifiConnected\":").append(wifiConnected).append(",")
            append("\"cellularAvailable\":").append(cellularAvailable).append(",")
            append("\"bluetoothEnabled\":").append(bluetoothEnabled).append(",")
            append("\"bluetoothConnected\":").append(bluetoothConnected)
            append("}")
        }
    }

    companion object {
        private fun escapeJson(str: String): String {
            return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
        }

        /**
         * Phân tích một dòng JSON thành đối tượng [DeviceSnapshot].
         */
        fun fromJson(json: String): DeviceSnapshot? {
            return try {
                val trimmed = json.trim()
                if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null

                fun extractString(key: String): String? {
                    val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
                    return pattern.find(trimmed)?.groupValues?.get(1)
                }

                fun extractInt(key: String): Int? {
                    val pattern = "\"$key\"\\s*:\\s*(-?\\d+)".toRegex()
                    return pattern.find(trimmed)?.groupValues?.get(1)?.toIntOrNull()
                }

                fun extractBoolean(key: String): Boolean? {
                    val pattern = "\"$key\"\\s*:\\s*(true|false)".toRegex()
                    return pattern.find(trimmed)?.groupValues?.get(1)?.toBooleanStrictOrNull()
                }

                val timestamp = extractString("timestamp") ?: ""
                val battery = extractInt("battery") ?: 0
                val wifiConnected = extractBoolean("wifiConnected") ?: extractBoolean("wifi") ?: false
                val cellularAvailable = extractBoolean("cellularAvailable") ?: extractBoolean("cellular") ?: false
                val bluetoothEnabled = extractBoolean("bluetoothEnabled") ?: extractBoolean("ble") ?: false
                val bluetoothConnected = extractBoolean("bluetoothConnected") ?: false

                DeviceSnapshot(
                    timestamp = timestamp,
                    battery = battery,
                    wifiConnected = wifiConnected,
                    cellularAvailable = cellularAvailable,
                    bluetoothEnabled = bluetoothEnabled,
                    bluetoothConnected = bluetoothConnected
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
