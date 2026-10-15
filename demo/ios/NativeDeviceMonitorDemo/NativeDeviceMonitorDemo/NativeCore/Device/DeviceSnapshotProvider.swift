//
//  DeviceSnapshotProvider.swift
//  NativeDeviceMonitorDemo
//
//  Tuần 3 - Bài tập 3.2: Tổng hợp dữ liệu từ 3 Provider thành DeviceSnapshot
//

import Foundation

public protocol DeviceSnapshotProvider {
    func takeSnapshot() -> DeviceSnapshot
}

public final class DeviceSnapshotProviderImpl: DeviceSnapshotProvider {
    private let batteryProvider: BatteryProvider
    private let networkProvider: NetworkProvider
    private let bluetoothProvider: BluetoothProvider

    public init(
        batteryProvider: BatteryProvider = BatteryProviderImpl.shared,
        networkProvider: NetworkProvider = NetworkProviderImpl.shared,
        bluetoothProvider: BluetoothProvider = BluetoothProviderImpl.shared
    ) {
        self.batteryProvider = batteryProvider
        self.networkProvider = networkProvider
        self.bluetoothProvider = bluetoothProvider
    }

    public func takeSnapshot() -> DeviceSnapshot {
        return DeviceSnapshot(
            timestamp: ISO8601DateFormatter().string(from: Date()),
            battery: batteryProvider.getBatteryLevel(),
            wifiConnected: networkProvider.isWifiConnected(),
            cellularAvailable: networkProvider.isCellularAvailable(),
            bluetoothEnabled: bluetoothProvider.isBluetoothEnabled(),
            bluetoothConnected: bluetoothProvider.isBluetoothConnected()
        )
    }
}
