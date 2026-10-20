//
//  DeviceSnapshot.swift
//  NativeDeviceMonitorDemo
//
//  Tuần 3 - Bài tập 3.2: Bản ghi trạng thái phần cứng thiết bị
//

import Foundation

public struct DeviceSnapshot: Codable, Equatable, Identifiable {
    public var id: String { timestamp }
    public let timestamp: String
    public let battery: Int
    public let wifiConnected: Bool
    public let cellularAvailable: Bool
    public let bluetoothEnabled: Bool
    public let bluetoothConnected: Bool

    public init(
        timestamp: String = ISO8601DateFormatter().string(from: Date()),
        battery: Int,
        wifiConnected: Bool,
        cellularAvailable: Bool,
        bluetoothEnabled: Bool,
        bluetoothConnected: Bool
    ) {
        self.timestamp = timestamp
        self.battery = battery
        self.wifiConnected = wifiConnected
        self.cellularAvailable = cellularAvailable
        self.bluetoothEnabled = bluetoothEnabled
        self.bluetoothConnected = bluetoothConnected
    }

    /// Chuyển đối tượng thành chuỗi JSON một dòng (chuẩn JSONL)
    public func toJsonString() -> String? {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.sortedKeys]
        guard let data = try? encoder.encode(self) else { return nil }
        return String(data: data, encoding: .utf8)
    }

    /// Phân tích một dòng JSON thành [DeviceSnapshot]
    public static func fromJsonString(_ json: String) -> DeviceSnapshot? {
        guard let data = json.trimmingCharacters(in: .whitespacesAndNewlines).data(using: .utf8) else {
            return nil
        }
        return try? JSONDecoder().decode(DeviceSnapshot.self, from: data)
    }
}
