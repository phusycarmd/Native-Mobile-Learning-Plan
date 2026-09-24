//
//  HomePage.swift
//  NativeDeviceMonitorDemo
//
//  Created by synguyen on 16/9/26.
//

import SwiftUI
import CoreBluetooth

struct DiscoveredDevice: Identifiable, Equatable {
    let id: String
    let name: String
    let rssi: Int
    let isConnectable: Bool
}

/// Một dòng trong ô Received Data: giá trị mới nhất của một characteristic
struct CharacteristicRow: Identifiable {
    let id: CBUUID
    let canRead: Bool
    let canNotify: Bool
    var value = "..."
    var notificationCount = 0

    var label: String {
        let modes = [canRead ? "Read" : nil, canNotify ? "Notify" : nil]
            .compactMap { $0 }
            .joined(separator: "/")
        let count = notificationCount > 0 ? "  #\(notificationCount)" : ""
        // CBUUID tự rút gọn UUID chuẩn Bluetooth SIG thành 4 ký tự (vd: 2A37)
        return "\(id.uuidString)  [\(modes)]\(count)"
    }
}

struct HomePage: View {
    // Design colors matching HomePage.kt
    private let primaryBlue = Color(red: 0.145, green: 0.388, blue: 0.922)      // #2563EB
    private let lightBlueBg = Color(red: 0.937, green: 0.957, blue: 1.0)        // #EFF4FF
    private let textPrimary = Color(red: 0.122, green: 0.161, blue: 0.216)      // #1F2937
    private let textSecondary = Color(red: 0.294, green: 0.333, blue: 0.388)    // #4B5563
    private let textLabel = Color(red: 0.216, green: 0.255, blue: 0.318)        // #374151
    private let textRssi = Color(red: 0.420, green: 0.447, blue: 0.502)         // #6B7280
    private let borderColor = Color(red: 0.898, green: 0.906, blue: 0.922)      // #E5E7EB
    private let dividerColor = Color(red: 0.953, green: 0.957, blue: 0.965)     // #F3F4F6
    private let buttonGrayBg = Color(red: 0.953, green: 0.957, blue: 0.965)     // #F3F4F6
    private let terminalBg = Color(red: 0.059, green: 0.090, blue: 0.165)       // #0F172A
    private let terminalText = Color(red: 0.290, green: 0.871, blue: 0.502)     // #4ADE80
    private let terminalLabel = Color(red: 0.580, green: 0.639, blue: 0.722)    // #94A3B8
    private let statusGreen = Color(red: 0.063, green: 0.725, blue: 0.506)      // #10B981
    private let statusAmber = Color(red: 0.961, green: 0.620, blue: 0.043)      // #F59E0B
    private let statusGray = Color(red: 0.533, green: 0.533, blue: 0.533)       // Compose Color.Gray #888888
    private let placeholderColor = Color(red: 0.612, green: 0.639, blue: 0.686) // #9CA3AF
    private let stopRed = Color(red: 0.863, green: 0.149, blue: 0.149)          // #DC2626

    private let deviceRowHeight: CGFloat = 54
    private let maxVisibleDevices = 5
    // Tín hiệu yếu hơn ngưỡng này coi là quá xa, kết nối không ổn định
    private let minRssi = -90

    // Managers
    private let permissionManager: PermissionManagerProtocol = PermissionManager.shared
    private let bluetoothManager: BluetoothManagerProtocol = BluetoothManager.shared

    // Interactive UI state (matching HomePage.kt)
    @State private var searchQuery = ""
    @State private var selectedDeviceId = ""
    @State private var connectionState: ConnectionState = .disconnected
    @State private var isScanning = false
    @State private var receivedData = "--"
    @State private var characteristicRows: [CharacteristicRow] = []
    // Only set when the connected peripheral is a CarMD dongle
    @State private var toolClient: CarMdToolClient?
    @State private var toolInfoText: String?
    @State private var isReadingToolInfo = false

    @State private var discoveredDevices: [DiscoveredDevice] = []

    // Toast (equivalent of Android Toast)
    @State private var toastMessage: String?
    @State private var toastWorkItem: DispatchWorkItem?

    // iOS chỉ hỏi quyền một lần; sau khi bị từ chối phải hướng người dùng vào Settings
    @State private var showPermissionAlert = false

    @FocusState private var isSearchFocused: Bool

    /// Lọc theo từ khoá, sau đó sắp xếp tín hiệu mạnh nhất (dBm lớn nhất) lên đầu
    private var filteredDevices: [DiscoveredDevice] {
        // Ẩn thiết bị nhiễu: không kết nối được hoặc tín hiệu quá yếu
        let cleanDevices = discoveredDevices.filter {
            $0.isConnectable && $0.rssi >= minRssi
        }
        let query = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        let list = query.isEmpty ? cleanDevices : cleanDevices.filter {
            $0.name.localizedCaseInsensitiveContains(query) ||
            $0.id.localizedCaseInsensitiveContains(query)
        }
        return list.sorted { $0.rssi > $1.rssi }
    }

    /// Hiển thị tối đa 5 thiết bị, nhiều hơn thì cuộn trong khung
    private var deviceListHeight: CGFloat {
        let visibleCount = min(max(filteredDevices.count, 1), maxVisibleDevices)
        return CGFloat(visibleCount) * deviceRowHeight + CGFloat(visibleCount - 1)
    }

    var body: some View {
        VStack(spacing: 0) {
            // Top Bar
            HStack {
                Text("Native Device Monitor")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(.white)
                Spacer()
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
            .background(primaryBlue.ignoresSafeArea(edges: .top))

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    // --- Section: Device Name ---
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Device Name")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(textLabel)

                        TextField(
                            "",
                            text: $searchQuery,
                            prompt: Text("Search device...")
                                .font(.system(size: 15))
                                .foregroundColor(placeholderColor)
                        )
                        .font(.system(size: 16))
                        .foregroundColor(textPrimary)
                        .tint(primaryBlue)
                        .focused($isSearchFocused)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                        .padding(.horizontal, 16)
                        .frame(height: 56)
                        .background(Color.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(
                                    isSearchFocused ? primaryBlue : borderColor,
                                    lineWidth: isSearchFocused ? 2 : 1
                                )
                        )
                    }

                    // --- Button: Scan Bluetooth ---
                    Button(action: handleScanButtonTapped) {
                        Text(isScanning ? "Stop Scan" : "Scan Bluetooth")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 50)
                            .background(isScanning ? stopRed : primaryBlue)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }

                    // --- Section: Discovered Devices ---
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Discovered Devices")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(textLabel)

                        ScrollView(.vertical, showsIndicators: true) {
                            LazyVStack(spacing: 0) {
                                if filteredDevices.isEmpty {
                                    Text(isScanning ? "Đang quét thiết bị xung quanh..." : "Chưa có thiết bị nào")
                                        .font(.system(size: 14))
                                        .foregroundColor(textRssi)
                                        .frame(maxWidth: .infinity)
                                        .frame(height: deviceRowHeight)
                                }
                                ForEach(Array(filteredDevices.enumerated()), id: \.element.id) { index, device in
                                    let isSelected = device.id == selectedDeviceId

                                    Button(action: { selectedDeviceId = device.id }) {
                                        HStack {
                                            Text(device.name)
                                                .font(.system(size: 16, weight: isSelected ? .bold : .medium))
                                                .foregroundColor(isSelected ? primaryBlue : textPrimary)
                                                .lineLimit(1)

                                            Spacer()

                                            Text("\(device.rssi) dBm")
                                                .font(.system(size: 15))
                                                .foregroundColor(textRssi)
                                        }
                                        .padding(.horizontal, 16)
                                        .frame(maxWidth: .infinity)
                                        .frame(height: deviceRowHeight)
                                        .background(isSelected ? lightBlueBg : Color.white)
                                        .contentShape(Rectangle())
                                    }
                                    .buttonStyle(.plain)

                                    if index < filteredDevices.count - 1 {
                                        Rectangle()
                                            .fill(dividerColor)
                                            .frame(height: 1)
                                    }
                                }
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: deviceListHeight)
                        .background(Color.white)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                        .overlay(
                            RoundedRectangle(cornerRadius: 14)
                                .stroke(borderColor, lineWidth: 1)
                        )
                    }

                    // --- Action Buttons: Connect & Disconnect ---
                    HStack(spacing: 14) {
                        Button(action: handleConnectTapped) {
                            Text("Connect")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(primaryBlue)
                                .frame(maxWidth: .infinity)
                                .frame(height: 48)
                                .background(lightBlueBg)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        }

                        Button(action: handleDisconnectTapped) {
                            Text("Disconnect")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(textSecondary)
                                .frame(maxWidth: .infinity)
                                .frame(height: 48)
                                .background(buttonGrayBg)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                    }

                    // --- Button: Get Tool Info (CarMD dongle) ---
                    Button(action: handleGetToolInfoTapped) {
                        Text("Get Tool Info")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(primaryBlue)
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                            .background(lightBlueBg)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .disabled(connectionState != .connected || isReadingToolInfo)
                    .opacity(connectionState != .connected || isReadingToolInfo ? 0.5 : 1)

                    // --- Section: Status Indicator ---
                    HStack(spacing: 8) {
                        Circle()
                            .fill(statusColor)
                            .frame(width: 10, height: 10)

                        Text(statusText)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(textLabel)
                    }
                    .padding(.vertical, 2)

                    // --- Section: Received Data ---
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Received Data")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(textLabel)

                        HStack {
                            if characteristicRows.isEmpty {
                                Text(receivedData)
                                    .font(.system(size: 15, weight: .medium, design: .monospaced))
                                    .foregroundColor(terminalText)
                            } else {
                                VStack(alignment: .leading, spacing: 10) {
                                    ForEach(characteristicRows) { row in
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(row.label)
                                                .font(.system(size: 12, design: .monospaced))
                                                .foregroundColor(terminalLabel)
                                            Text(row.value)
                                                .font(.system(size: 14, weight: .medium, design: .monospaced))
                                                .foregroundColor(terminalText)
                                        }
                                    }
                                }
                            }
                            Spacer(minLength: 0)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 14)
                        .background(terminalBg)
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                    }

                    // --- Section: Tool Info ---
                    if let toolInfoText {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Tool Info")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(textLabel)

                            HStack {
                                Text(toolInfoText)
                                    .font(.system(size: 14, weight: .medium, design: .monospaced))
                                    .foregroundColor(terminalText)
                                Spacer(minLength: 0)
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 14)
                            .background(terminalBg)
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                        }
                    }

                    Spacer().frame(height: 16)
                }
                .padding(20)
            }
        }
        .background(Color.white.ignoresSafeArea())
        .overlay(alignment: .bottom) {
            if let toastMessage {
                Text(toastMessage)
                    .font(.system(size: 14))
                    .foregroundColor(.white)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color(white: 0.2).opacity(0.92))
                    .clipShape(Capsule())
                    .padding(.horizontal, 32)
                    .padding(.bottom, 48)
                    .transition(.opacity)
                    .allowsHitTesting(false)
            }
        }
        .alert("Cần quyền Bluetooth", isPresented: $showPermissionAlert) {
            Button("Mở Cài đặt") {
                permissionManager.openAppSettings()
            }
            Button("Huỷ", role: .cancel) {}
        } message: {
            Text("Quyền Bluetooth đã bị từ chối. Vui lòng bật lại trong Cài đặt để quét thiết bị xung quanh.")
        }
        .onDisappear {
            bluetoothManager.stopScan()
            bluetoothManager.disconnect()
        }
    }

    // MARK: - Actions

    private func handleScanButtonTapped() {
        if isScanning {
            bluetoothManager.stopScan()
            isScanning = false
            showToast("Đã dừng quét Bluetooth.")
            return
        }

        Task {
            let status = permissionManager.checkPermission(type: .bluetooth)
            var granted = false
            if status != .denied && status != .restricted {
                granted = await permissionManager.requestPermission(type: .bluetooth)
            }
            guard granted else {
                showPermissionAlert = true
                return
            }

            guard bluetoothManager.isEnabled() else {
                showToast("Bluetooth đang tắt! Vui lòng bật Bluetooth trên thiết bị.", long: true)
                return
            }

            // Quét lại: xoá danh sách cũ
            discoveredDevices.removeAll()
            selectedDeviceId = ""
            isScanning = true
            showToast("Đang quét thiết bị BLE xung quanh...")

            bluetoothManager.startScan(
                onDeviceFound: { bleDevice in
                    // Thiết bị không tên là nhiễu, bỏ qua
                    guard bleDevice.name != "Unknown Device" else { return }
                    let id = bleDevice.id.uuidString
                    let newItem = DiscoveredDevice(
                        id: id,
                        name: bleDevice.name,
                        rssi: bleDevice.rssi,
                        isConnectable: bleDevice.isConnectable
                    )

                    // Cập nhật danh sách: nếu đã tồn tại thì cập nhật thông tin mới nhất, nếu chưa có thì thêm vào
                    if let index = discoveredDevices.firstIndex(where: { $0.id == id }) {
                        discoveredDevices[index] = newItem
                    } else {
                        discoveredDevices.append(newItem)
                    }
                },
                onError: { error in
                    isScanning = false
                    showToast(error, long: true)
                }
            )
        }
    }

    private func handleConnectTapped() {
        guard let uuid = UUID(uuidString: selectedDeviceId) else {
            showToast("Hãy chọn một thiết bị trong danh sách.")
            return
        }

        bluetoothManager.stopScan()
        isScanning = false
        characteristicRows.removeAll()
        toolClient = nil
        toolInfoText = nil
        receivedData = "Đang kết nối..."

        bluetoothManager.connect(peripheralId: uuid) { state in
            let wasConnected = connectionState == .connected
            connectionState = state
            switch state {
            case .connected:
                discoverReadAndSubscribeAll()
            case .disconnected:
                characteristicRows.removeAll()
                toolClient = nil
                isReadingToolInfo = false
                // Không đi qua .disconnecting nghĩa là không phải người dùng bấm Disconnect
                if wasConnected {
                    receivedData = "Mất kết nối đột ngột!"
                    showToast("Mất kết nối với thiết bị.", long: true)
                }
            case .connecting, .disconnecting:
                break
            }
        }
    }

    /// Khám phá services, sau đó đọc mọi characteristic Read và subscribe mọi characteristic Notify/Indicate.
    /// CoreBluetooth tự xếp hàng các lệnh nên có thể gửi liên tiếp.
    private func discoverReadAndSubscribeAll() {
        bluetoothManager.discoverServices(
            onSuccess: { services in
                // The tool client takes over the response characteristic's notifications,
                // so forward them to keep that row updating
                if toolClient == nil {
                    toolClient = CarMdToolClient.create(bluetoothManager: bluetoothManager, services: services) { data in
                        guard let uuid = toolClient?.notifyUuid else { return }
                        updateRow(uuid) { row in
                            row.value = data.displayString
                            row.notificationCount += 1
                        }
                    }
                }
                // iOS gọi callback này một lần cho mỗi service (danh sách tăng dần),
                // nên chỉ xử lý characteristic chưa có trong danh sách
                for characteristic in services.flatMap({ $0.characteristics }) {
                    let uuid = characteristic.id
                    let canRead = characteristic.properties.contains(.read)
                    let canNotify = characteristic.properties.contains(.notify) || characteristic.properties.contains(.indicate)
                    // Bỏ qua characteristic chỉ Write, và UUID trùng ở nhiều service
                    guard canRead || canNotify, !characteristicRows.contains(where: { $0.id == uuid }) else { continue }

                    characteristicRows.append(CharacteristicRow(id: uuid, canRead: canRead, canNotify: canNotify))
                    if canRead {
                        bluetoothManager.read(characteristicUuid: uuid) { data, error in
                            updateRow(uuid) { row in
                                // Notification có thể tới trước kết quả read, khi đó giữ giá trị mới hơn
                                guard row.notificationCount == 0 else { return }
                                row.value = error?.localizedDescription ?? data?.displayString ?? "--"
                            }
                        }
                    }
                    if canNotify {
                        bluetoothManager.subscribe(characteristicUuid: uuid) { data in
                            updateRow(uuid) { row in
                                row.value = data.displayString
                                row.notificationCount += 1
                            }
                        }
                    }
                }
                if characteristicRows.isEmpty {
                    receivedData = "Không có characteristic Read/Notify"
                }
            },
            onError: { error in
                receivedData = error
            }
        )
    }

    private func updateRow(_ uuid: CBUUID, _ transform: (inout CharacteristicRow) -> Void) {
        guard let index = characteristicRows.firstIndex(where: { $0.id == uuid }) else { return }
        transform(&characteristicRows[index])
    }

    private func handleGetToolInfoTapped() {
        guard let toolClient else {
            showToast("Thiết bị đang kết nối không phải CarMD tool.")
            return
        }
        isReadingToolInfo = true
        toolInfoText = "Đang đọc thông tin tool..."
        toolClient.getToolInfo { info, error in
            isReadingToolInfo = false
            toolInfoText = error ?? info.map(Self.toolInfoDisplayText)
        }
    }

    private static func toolInfoDisplayText(_ info: ToolInfo) -> String {
        [
            "Bootloader: \(info.bootloaderVersion ?? "--")",
            "Firmware:   \(info.firmwareVersion ?? "--")",
            "GUID:       \(info.guid ?? "--")",
            "USB PID:    \(info.usbProductId.map { String(format: "0x%04X (%d)", $0, $0) } ?? "--")"
        ].joined(separator: "\n")
    }

    private func handleDisconnectTapped() {
        // disconnect() tự huỷ mọi subscription và lệnh read đang chờ
        characteristicRows.removeAll()
        toolClient = nil
        toolInfoText = nil
        bluetoothManager.disconnect()
        receivedData = "Đã ngắt kết nối"
    }

    private func showToast(_ message: String, long: Bool = false) {
        toastWorkItem?.cancel()
        withAnimation(.easeInOut(duration: 0.2)) { toastMessage = message }
        let workItem = DispatchWorkItem {
            withAnimation(.easeInOut(duration: 0.2)) { toastMessage = nil }
        }
        toastWorkItem = workItem
        // Tương đương Toast.LENGTH_SHORT (2s) / Toast.LENGTH_LONG (3.5s)
        DispatchQueue.main.asyncAfter(deadline: .now() + (long ? 3.5 : 2.0), execute: workItem)
    }

    private var statusColor: Color {
        switch connectionState {
        case .connected: return statusGreen
        case .connecting, .disconnecting: return statusAmber
        case .disconnected: return statusGray
        }
    }

    private var statusText: String {
        switch connectionState {
        case .connected: return "Status: Connected"
        case .connecting: return "Status: Connecting..."
        case .disconnecting: return "Status: Disconnecting..."
        case .disconnected: return "Status: Disconnected"
        }
    }
}

private extension Data {
    var hexString: String {
        map { String(format: "%02X", $0) }.joined(separator: " ")
    }

    /// Hex, kèm chuỗi ASCII nếu mọi byte đều in được (vd: tên nhà sản xuất, số serial)
    var displayString: String {
        guard !isEmpty else { return "(rỗng)" }
        let isPrintable = allSatisfy { (0x20...0x7E).contains($0) }
        guard isPrintable, let text = String(data: self, encoding: .ascii) else { return hexString }
        return "\(hexString)  \"\(text)\""
    }
}

#Preview {
    HomePage()
}
