//
//  DeviceMonitorTests.swift
//  NativeDeviceMonitorDemoTests
//
//  Unit tests cho DeviceSnapshot và JSONL FileLogger trên iOS
//

import XCTest
@testable import NativeDeviceMonitorDemo

final class DeviceMonitorTests: XCTestCase {

    func testDeviceSnapshotSerialization() {
        let snapshot = DeviceSnapshot(
            timestamp: "2026-10-15T10:30:00Z",
            battery: 88,
            wifiConnected: true,
            cellularAvailable: false,
            bluetoothEnabled: true,
            bluetoothConnected: true
        )

        let json = snapshot.toJsonString()
        XCTAssertNotNil(json)
        XCTAssertTrue(json!.contains("\"battery\":88"))
        XCTAssertTrue(json!.contains("\"wifiConnected\":true"))

        let parsed = DeviceSnapshot.fromJsonString(json!)
        XCTAssertNotNil(parsed)
        XCTAssertEqual(parsed?.battery, 88)
        XCTAssertEqual(parsed?.wifiConnected, true)
        XCTAssertEqual(parsed?.cellularAvailable, false)
        XCTAssertEqual(parsed?.bluetoothEnabled, true)
        XCTAssertEqual(parsed?.bluetoothConnected, true)
        XCTAssertEqual(parsed?.timestamp, "2026-10-15T10:30:00Z")
    }

    func testFileLoggerAppendAndRead() {
        let testFileName = "test_device_monitor_\(UUID().uuidString).jsonl"
        let logger = FileLogger(fileName: testFileName)

        let s1 = DeviceSnapshot(timestamp: "2026-10-15T10:00:00Z", battery: 80, wifiConnected: true, cellularAvailable: true, bluetoothEnabled: true, bluetoothConnected: false)
        let s2 = DeviceSnapshot(timestamp: "2026-10-15T10:00:05Z", battery: 79, wifiConnected: true, cellularAvailable: true, bluetoothEnabled: true, bluetoothConnected: false)

        logger.append(s1)
        logger.append(s2)

        let entries = logger.readAll()
        XCTAssertEqual(entries.count, 2)
        XCTAssertEqual(entries[0].battery, 80)
        XCTAssertEqual(entries[1].battery, 79)

        XCTAssertTrue(logger.getFileSizeBytes() > 0)
        XCTAssertEqual(logger.getLogCount(), 2)

        logger.clear()
        XCTAssertEqual(logger.getLogCount(), 0)
    }
}
