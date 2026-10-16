//
//  NetworkProvider.swift
//  NativeDeviceMonitorDemo
//
//  Tuần 3 - Bài tập 3.2: Theo dõi mạng Wi-Fi và Cellular qua NWPathMonitor
//

import Foundation
import Network

public struct NetworkInfo {
    public let isConnected: Bool
    public let isWifiConnected: Bool
    public let isCellularAvailable: Bool
}

public protocol NetworkProvider {
    func isConnected() -> Bool
    func isWifiConnected() -> Bool
    func isCellularAvailable() -> Bool
    func getNetworkInfo() -> NetworkInfo
}

public final class NetworkProviderImpl: NetworkProvider {
    public static let shared = NetworkProviderImpl()

    private let monitor: NWPathMonitor
    private let queue = DispatchQueue(label: "com.phusy2001.networkmonitor", qos: .utility)
    private var currentPath: NWPath?
    private let lock = NSLock()

    public init() {
        self.monitor = NWPathMonitor()
        self.monitor.pathUpdateHandler = { [weak self] path in
            guard let self = self else { return }
            self.lock.lock()
            self.currentPath = path
            self.lock.unlock()
        }
        self.monitor.start(queue: queue)
    }

    deinit {
        monitor.cancel()
    }

    public func isConnected() -> Bool {
        lock.lock()
        defer { lock.unlock() }
        guard let path = currentPath else { return true }
        return path.status == .satisfied
    }

    public func isWifiConnected() -> Bool {
        lock.lock()
        defer { lock.unlock() }
        guard let path = currentPath else { return false }
        return path.status == .satisfied && path.usesInterfaceType(.wifi)
    }

    public func isCellularAvailable() -> Bool {
        lock.lock()
        defer { lock.unlock() }
        guard let path = currentPath else { return false }
        return path.usesInterfaceType(.cellular)
    }

    public func getNetworkInfo() -> NetworkInfo {
        return NetworkInfo(
            isConnected: isConnected(),
            isWifiConnected: isWifiConnected(),
            isCellularAvailable: isCellularAvailable()
        )
    }
}
