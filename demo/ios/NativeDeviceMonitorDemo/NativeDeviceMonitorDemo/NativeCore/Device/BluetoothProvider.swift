//
//  BluetoothProvider.swift
//  NativeDeviceMonitorDemo
//
//  Tuần 3 - Bài tập 3.2: Kiểm tra trạng thái Bluetooth
//

import Foundation
import CoreBluetooth

public struct BluetoothInfo {
    public let isEnabled: Bool
    public let isConnected: Bool
}

public protocol BluetoothProvider {
    func isBluetoothEnabled() -> Bool
    func isBluetoothConnected() -> Bool
    func getBluetoothInfo() -> BluetoothInfo
}

public final class BluetoothProviderImpl: BluetoothProvider {
    public static let shared = BluetoothProviderImpl()

    private let bluetoothManager: BluetoothManagerProtocol

    public init(bluetoothManager: BluetoothManagerProtocol = BluetoothManager.shared) {
        self.bluetoothManager = bluetoothManager
    }

    public func isBluetoothEnabled() -> Bool {
        return bluetoothManager.isEnabled()
    }

    public func isBluetoothConnected() -> Bool {
        return bluetoothManager.getConnectionState() == .connected
    }

    public func getBluetoothInfo() -> BluetoothInfo {
        return BluetoothInfo(
            isEnabled: isBluetoothEnabled(),
            isConnected: isBluetoothConnected()
        )
    }
}
