package com.phusy2001.nativedevicemonitordemo.nativecore.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DeviceMonitorTests {

    @Test
    fun testDeviceSnapshotJsonSerialization() {
        val snapshot = DeviceSnapshot(
            timestamp = "2026-10-15T10:30:00Z",
            battery = 85,
            wifiConnected = true,
            cellularAvailable = false,
            bluetoothEnabled = true,
            bluetoothConnected = true
        )

        val json = snapshot.toJson()
        assertTrue(json.contains("\"battery\":85"))
        assertTrue(json.contains("\"wifiConnected\":true"))

        val parsed = DeviceSnapshot.fromJson(json)
        assertNotNull(parsed)
        assertEquals(85, parsed?.battery)
        assertEquals(true, parsed?.wifiConnected)
        assertEquals(false, parsed?.cellularAvailable)
        assertEquals(true, parsed?.bluetoothEnabled)
        assertEquals(true, parsed?.bluetoothConnected)
        assertEquals("2026-10-15T10:30:00Z", parsed?.timestamp)
    }

    @Test
    fun testDeviceSnapshotJsonBackwardCompatibility() {
        // Hỗ trợ định dạng rút gọn từ các ngày trước: wifi, cellular, ble
        val oldJson = """{"timestamp":"2026-10-15T10:30:00Z","battery":90,"wifi":true,"cellular":false,"ble":true}"""
        val parsed = DeviceSnapshot.fromJson(oldJson)
        assertNotNull(parsed)
        assertEquals(90, parsed?.battery)
        assertEquals(true, parsed?.wifiConnected)
        assertEquals(false, parsed?.cellularAvailable)
        assertEquals(true, parsed?.bluetoothEnabled)
    }

    @Test
    fun testFileLoggingJsonlAppendAndRead() {
        val tempDir = createTempDir(prefix = "monitor_test")
        val logFile = File(tempDir, "test_monitor_logs.jsonl")

        val s1 = DeviceSnapshot(timestamp = "2026-10-15T10:00:00Z", battery = 80, wifiConnected = true, cellularAvailable = true, bluetoothEnabled = true, bluetoothConnected = false)
        val s2 = DeviceSnapshot(timestamp = "2026-10-15T10:00:05Z", battery = 79, wifiConnected = true, cellularAvailable = true, bluetoothEnabled = true, bluetoothConnected = false)

        logFile.appendText("${s1.toJson()}\n", Charsets.UTF_8)
        logFile.appendText("${s2.toJson()}\n", Charsets.UTF_8)

        val lines = logFile.readLines(Charsets.UTF_8)
        assertEquals(2, lines.size)

        val parsed1 = DeviceSnapshot.fromJson(lines[0])
        val parsed2 = DeviceSnapshot.fromJson(lines[1])

        assertEquals(80, parsed1?.battery)
        assertEquals(79, parsed2?.battery)

        tempDir.deleteRecursively()
    }
}
