//
//  BackgroundMonitor.swift
//  NativeDeviceMonitorDemo
//
//  Tuần 3 - Bài tập 3.4 & docs/modules/background.md
//  Dịch vụ giám sát trạng thái thiết bị theo chu kỳ trên iOS
//

import Foundation
import UIKit
import Combine

public protocol BackgroundMonitorProtocol: ObservableObject {
    var isMonitoring: Bool { get }
    var latestSnapshot: DeviceSnapshot? { get }
    var logCount: Int { get }

    func startMonitoring(intervalSeconds: TimeInterval)
    func stopMonitoring()
    func getLatestSnapshot() -> DeviceSnapshot?
    func readLogEntries() -> [DeviceSnapshot]
    func triggerManualSnapshot() -> DeviceSnapshot
    func clearLogs()
}

public final class BackgroundMonitor: ObservableObject, BackgroundMonitorProtocol {
    public static let shared = BackgroundMonitor()

    @Published public private(set) var isMonitoring: Bool = false
    @Published public private(set) var latestSnapshot: DeviceSnapshot?
    @Published public private(set) var logCount: Int = 0

    private var timer: Timer?
    private var currentInterval: TimeInterval = 5.0
    private var backgroundTaskId: UIBackgroundTaskIdentifier = .invalid
    private var cancellables = Set<AnyCancellable>()

    public let fileLogger: FileLoggerProtocol
    public let snapshotProvider: DeviceSnapshotProvider

    public init(
        fileLogger: FileLoggerProtocol = FileLogger.shared,
        snapshotProvider: DeviceSnapshotProvider = DeviceSnapshotProviderImpl()
    ) {
        self.fileLogger = fileLogger
        self.snapshotProvider = snapshotProvider

        // Đồng bộ dữ liệu log có sẵn
        self.logCount = fileLogger.getLogCount()
        self.latestSnapshot = fileLogger.readAll().last

        setupLifecycleObservers()
    }

    deinit {
        stopMonitoring()
    }

    public func startMonitoring(intervalSeconds: TimeInterval = 5.0) {
        guard !isMonitoring else { return }
        self.currentInterval = intervalSeconds
        self.isMonitoring = true

        // Thu thập ngay 1 snapshot khởi đầu
        recordSnapshot()

        // Bật Timer chu kỳ
        startTimer()
    }

    public func stopMonitoring() {
        guard isMonitoring else { return }
        isMonitoring = false
        stopTimer()
        endBackgroundTaskIfNeeded()
    }

    public func getLatestSnapshot() -> DeviceSnapshot? {
        return latestSnapshot ?? fileLogger.readAll().last
    }

    public func readLogEntries() -> [DeviceSnapshot] {
        return fileLogger.readAll()
    }

    @discardableResult
    public func triggerManualSnapshot() -> DeviceSnapshot {
        return recordSnapshot()
    }

    public func clearLogs() {
        fileLogger.clear()
        logCount = 0
    }

    // MARK: - Internal Timer & Collection

    private func startTimer() {
        stopTimer()
        timer = Timer.scheduledTimer(withTimeInterval: currentInterval, repeats: true) { [weak self] _ in
            self?.recordSnapshot()
        }
        RunLoop.main.add(timer!, forMode: .common)
    }

    private func stopTimer() {
        timer?.invalidate()
        timer = nil
    }

    @discardableResult
    private func recordSnapshot() -> DeviceSnapshot {
        let snapshot = snapshotProvider.takeSnapshot()
        fileLogger.append(snapshot)

        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            self.latestSnapshot = snapshot
            self.logCount = self.fileLogger.getLogCount()
        }
        return snapshot
    }

    // MARK: - iOS 5-State Behavior Matrix Handling

    private func setupLifecycleObservers() {
        NotificationCenter.default.publisher(for: UIApplication.didEnterBackgroundNotification)
            .sink { [weak self] _ in
                self?.handleDidEnterBackground()
            }
            .store(in: &cancellables)

        NotificationCenter.default.publisher(for: UIApplication.willEnterForegroundNotification)
            .sink { [weak self] _ in
                self?.handleWillEnterForeground()
            }
            .store(in: &cancellables)
    }

    private func handleDidEnterBackground() {
        guard isMonitoring else { return }

        // Trên iOS, ứng dụng bình thường bị suspend sau ~30 giây khi bấm Home.
        // Để timer tiếp tục chạy trong khoảng 30s được cấp phép, đăng ký beginBackgroundTask:
        backgroundTaskId = UIApplication.shared.beginBackgroundTask(withName: "DeviceMonitorBackground") { [weak self] in
            // Expiration Handler: iOS chuẩn bị đình trệ (freeze) tiến trình
            self?.endBackgroundTaskIfNeeded()
        }
    }

    private func handleWillEnterForeground() {
        endBackgroundTaskIfNeeded()
        if isMonitoring {
            // Khôi phục timer khi người dùng mở lại app
            startTimer()
            recordSnapshot()
        }
    }

    private func endBackgroundTaskIfNeeded() {
        if backgroundTaskId != .invalid {
            UIApplication.shared.endBackgroundTask(backgroundTaskId)
            backgroundTaskId = .invalid
        }
    }
}
