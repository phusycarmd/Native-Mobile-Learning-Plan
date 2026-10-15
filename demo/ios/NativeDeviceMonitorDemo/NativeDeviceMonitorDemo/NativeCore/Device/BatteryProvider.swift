//
//  BatteryProvider.swift
//  NativeDeviceMonitorDemo
//
//  Tuần 3 - Bài tập 3.2: Đọc thông số Pin từ UIDevice
//

import UIKit

public struct BatteryInfo {
    public let level: Int
    public let isCharging: Bool
}

public protocol BatteryProvider {
    func getBatteryLevel() -> Int
    func isCharging() -> Bool
    func getBatteryInfo() -> BatteryInfo
}

public final class BatteryProviderImpl: BatteryProvider {
    public static let shared = BatteryProviderImpl()

    public init() {
        UIDevice.current.isBatteryMonitoringEnabled = true
    }

    public func getBatteryLevel() -> Int {
        UIDevice.current.isBatteryMonitoringEnabled = true
        let raw = UIDevice.current.batteryLevel // -1.0 nếu trên simulator hoặc chưa đo
        if raw >= 0 {
            return Int(raw * 100)
        }
        return 100 // Fallback mặc định trên Simulator
    }

    public func isCharging() -> Bool {
        UIDevice.current.isBatteryMonitoringEnabled = true
        let state = UIDevice.current.batteryState
        return state == .charging || state == .full
    }

    public func getBatteryInfo() -> BatteryInfo {
        return BatteryInfo(
            level: getBatteryLevel(),
            isCharging: isCharging()
        )
    }
}
