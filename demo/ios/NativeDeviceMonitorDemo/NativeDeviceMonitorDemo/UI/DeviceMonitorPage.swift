//
//  DeviceMonitorPage.swift
//  NativeDeviceMonitorDemo
//
//  Tuần 3 - Bài tập 3.5: Màn hình giám sát phần cứng và ghi nhật ký JSONL
//

import SwiftUI

struct DeviceMonitorPage: View {
    let onMenuTap: () -> Void

    @StateObject private var monitor = BackgroundMonitor.shared
    @State private var selectedInterval: TimeInterval = 5.0
    @State private var logEntries: [DeviceSnapshot] = []
    @State private var fileSizeBytes: Int64 = 0
    @State private var showClearAlert: Bool = false
    @State private var selectedJsonDetail: String? = nil

    private let primaryBlue = Color(red: 37/255, green: 99/255, blue: 235/255)
    private let dangerRed = Color(red: 220/255, green: 38/255, blue: 38/255)
    private let successGreen = Color(red: 22/255, green: 163/255, blue: 74/255)

    var body: some View {
        VStack(spacing: 0) {
            topBar

            ScrollView {
                VStack(spacing: 16) {
                    liveHardwareCard
                    monitorControlCard
                    jsonlStorageCard

                    if !logEntries.isEmpty {
                        VStack(spacing: 10) {
                            ForEach(Array(logEntries.enumerated()), id: \.element.id) { index, entry in
                                logItemRow(index: logEntries.count - index, entry: entry)
                                    .onTapGesture {
                                        selectedJsonDetail = entry.toJsonString()
                                    }
                            }
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
            }
            .background(Color(UIColor.systemGroupedBackground))
        }
        .onAppear {
            refreshLogs()
        }
        .onChange(of: monitor.logCount) { _ in
            refreshLogs()
        }
        .alert("Xác nhận xóa nhật ký", isPresented: $showClearAlert) {
            Button("Hủy", role: .cancel) {}
            Button("Xóa file", role: .destructive) {
                monitor.clearLogs()
                refreshLogs()
            }
        } message: {
            Text("Bạn có chắc chắn muốn xóa toàn bộ file monitor_logs.jsonl không?")
        }
        .alert("Chi tiết bản ghi JSON", isPresented: Binding(
            get: { selectedJsonDetail != nil },
            set: { if !$0 { selectedJsonDetail = nil } }
        )) {
            Button("Đóng", role: .cancel) {}
        } message: {
            Text(selectedJsonDetail ?? "")
        }
    }

    // MARK: - Top Bar

    private var topBar: some View {
        HStack(spacing: 12) {
            Button(action: onMenuTap) {
                Image(systemName: "line.3.horizontal")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.primary)
                    .frame(width: 40, height: 40)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text("Device Monitor")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.primary)
                Text("Hardware metrics & JSONL Logger")
                    .font(.system(size: 12))
                    .foregroundColor(.secondary)
            }

            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(Color(UIColor.systemBackground))
        .shadow(color: Color.black.opacity(0.05), radius: 2, y: 2)
    }

    // MARK: - Live Hardware Card

    private var liveHardwareCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("TRẠNG THÁI PHẦN CỨNG (LIVE)")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(.secondary)

                Spacer()

                Text(monitor.latestSnapshot != nil ? "Cập nhật realtime" : "Chờ dữ liệu")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(primaryBlue)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(primaryBlue.opacity(0.1))
                    .cornerRadius(12)
            }

            let snap = monitor.latestSnapshot

            HStack(spacing: 10) {
                metricBox(
                    label: "Pin (Battery)",
                    value: snap != nil ? "\(snap!.battery)%" : "--",
                    subtext: "Mức pin hiện tại",
                    badgeColor: (snap?.battery ?? 0) > 20 ? successGreen : dangerRed
                )

                metricBox(
                    label: "Wi-Fi",
                    value: snap != nil ? (snap!.wifiConnected ? "KẾT NỐI" : "NGẮT") : "--",
                    subtext: snap?.cellularAvailable == true ? "+ 4G/Cellular" : "Chỉ Wi-Fi",
                    badgeColor: snap?.wifiConnected == true ? successGreen : .gray
                )

                metricBox(
                    label: "Bluetooth",
                    value: snap != nil ? (snap!.bluetoothEnabled ? "BẬT" : "TẮT") : "--",
                    subtext: snap?.bluetoothConnected == true ? "Đã kết nối" : "Chưa nối",
                    badgeColor: snap?.bluetoothEnabled == true ? primaryBlue : dangerRed
                )
            }

            if let timestamp = snap?.timestamp {
                Text("Thời gian ghi nhận: \(timestamp)")
                    .font(.system(size: 11, design: .monospaced))
                    .foregroundColor(.secondary)
            }
        }
        .padding(16)
        .background(Color(UIColor.secondarySystemGroupedBackground))
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color(UIColor.separator), lineWidth: 0.5)
        )
    }

    // MARK: - Monitor Control Card

    private var monitorControlCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("DỊCH VỤ CHẠY NỀN (BACKGROUND MONITOR)")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(.secondary)

                Spacer()

                HStack(spacing: 6) {
                    Circle()
                        .fill(monitor.isMonitoring ? successGreen : dangerRed)
                        .frame(width: 8, height: 8)
                    Text(monitor.isMonitoring ? "RUNNING" : "STOPPED")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(monitor.isMonitoring ? successGreen : dangerRed)
                }
            }

            Text("Chu kỳ lấy mẫu (Interval):")
                .font(.system(size: 13, weight: .medium))

            HStack(spacing: 8) {
                ForEach([2.0, 5.0, 10.0], id: \.self) { interval in
                    Button(action: {
                        if !monitor.isMonitoring {
                            selectedInterval = interval
                        }
                    }) {
                        Text("\(Int(interval)) giây")
                            .font(.system(size: 12, weight: selectedInterval == interval ? .semibold : .regular))
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(selectedInterval == interval ? primaryBlue.opacity(0.15) : Color(UIColor.tertiarySystemGroupedBackground))
                            .foregroundColor(selectedInterval == interval ? primaryBlue : .primary)
                            .cornerRadius(16)
                    }
                    .disabled(monitor.isMonitoring)
                }
            }

            HStack(spacing: 10) {
                Button(action: toggleMonitoring) {
                    Text(monitor.isMonitoring ? "Dừng Giám Sát" : "Bắt Đầu Giám Sát")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(monitor.isMonitoring ? dangerRed : primaryBlue)
                        .cornerRadius(8)
                }

                Button(action: manualSnapshot) {
                    Text("Ghi 1 Log Ngay")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(primaryBlue)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(Color(UIColor.tertiarySystemGroupedBackground))
                        .overlay(
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(primaryBlue.opacity(0.5), lineWidth: 1)
                        )
                        .cornerRadius(8)
                }
            }

            // Educational Callout
            VStack(alignment: .leading, spacing: 4) {
                Text("📌 Hành vi hệ điều hành iOS (Ma trận 5 trạng thái):")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(.primary)
                Text("Khi thu nhỏ app (Background), iOS cho phép chạy tối đa ~30 giây qua UIBackgroundTaskIdentifier trước khi đưa ứng dụng vào trạng thái Suspended (đình trệ). Khi mở lại app, timer lập tức khôi phục bình thường.")
                    .font(.system(size: 11))
                    .foregroundColor(.secondary)
                    .lineSpacing(2)
            }
            .padding(10)
            .background(Color(UIColor.tertiarySystemGroupedBackground))
            .cornerRadius(8)
        }
        .padding(16)
        .background(Color(UIColor.secondarySystemGroupedBackground))
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color(UIColor.separator), lineWidth: 0.5)
        )
    }

    // MARK: - JSONL Storage Card

    private var jsonlStorageCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("NHẬT KÝ FILE JSONL")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(.secondary)
                    Text("monitor_logs.jsonl (\(logEntries.count) bản ghi · \(String(format: "%.1f KB", Double(fileSizeBytes) / 1024.0)))")
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                }

                Spacer()

                Button("Làm mới", action: refreshLogs)
                    .font(.system(size: 12))
                    .foregroundColor(primaryBlue)

                Button("Xóa file") {
                    showClearAlert = true
                }
                .font(.system(size: 12))
                .foregroundColor(dangerRed)
            }

            if logEntries.isEmpty {
                Text("Chưa có bản ghi nào. Bấm 'Bắt Đầu Giám Sát' hoặc 'Ghi 1 Log Ngay' để tạo dữ liệu.")
                    .font(.system(size: 12))
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.vertical, 20)
            }
        }
        .padding(16)
        .background(Color(UIColor.secondarySystemGroupedBackground))
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color(UIColor.separator), lineWidth: 0.5)
        )
    }

    // MARK: - Components

    private func metricBox(label: String, value: String, subtext: String, badgeColor: Color) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.system(size: 11, weight: .medium))
                .foregroundColor(.secondary)
            HStack(spacing: 5) {
                Circle()
                    .fill(badgeColor)
                    .frame(width: 6, height: 6)
                Text(value)
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(.primary)
            }
            Text(subtext)
                .font(.system(size: 10))
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(10)
        .background(Color(UIColor.tertiarySystemGroupedBackground))
        .cornerRadius(8)
    }

    private func logItemRow(index: Int, entry: DeviceSnapshot) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("#\(index) · \(entry.timestamp)")
                    .font(.system(size: 12, weight: .medium, design: .monospaced))
                    .foregroundColor(.primary)
                Spacer()
                Text("JSONL")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(primaryBlue)
            }

            HStack(spacing: 6) {
                statusTag(text: "Pin: \(entry.battery)%", color: entry.battery > 20 ? successGreen : dangerRed)
                statusTag(text: entry.wifiConnected ? "Wi-Fi: ON" : "Wi-Fi: OFF", color: entry.wifiConnected ? primaryBlue : .secondary)
                statusTag(text: entry.cellularAvailable ? "4G: YES" : "4G: NO", color: entry.cellularAvailable ? primaryBlue : .secondary)
                statusTag(text: entry.bluetoothEnabled ? "BLE: ON" : "BLE: OFF", color: entry.bluetoothEnabled ? primaryBlue : dangerRed)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(UIColor.secondarySystemGroupedBackground))
        .cornerRadius(8)
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(Color(UIColor.separator), lineWidth: 0.5)
        )
    }

    private func statusTag(text: String, color: Color) -> some View {
        Text(text)
            .font(.system(size: 10, weight: .medium))
            .foregroundColor(color)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(color.opacity(0.12))
            .cornerRadius(4)
    }

    // MARK: - Actions

    private func toggleMonitoring() {
        if monitor.isMonitoring {
            monitor.stopMonitoring()
        } else {
            monitor.startMonitoring(intervalSeconds: selectedInterval)
        }
    }

    private func manualSnapshot() {
        monitor.triggerManualSnapshot()
        refreshLogs()
    }

    private func refreshLogs() {
        logEntries = monitor.readLogEntries().reversed()
        fileSizeBytes = monitor.fileLogger.getFileSizeBytes()
    }
}
