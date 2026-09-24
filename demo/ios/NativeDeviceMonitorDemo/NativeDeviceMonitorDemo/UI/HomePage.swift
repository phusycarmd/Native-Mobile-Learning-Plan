//
//  HomePage.swift
//  NativeDeviceMonitorDemo
//
//  Created by synguyen on 16/9/26.
//

import SwiftUI

struct DiscoveredDevice: Identifiable, Equatable {
    let id: String
    let name: String
    let rssi: Int

    var rssiString: String {
        "\(rssi) dBm"
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
    private let buttonGrayBg = Color(red: 0.953, green: 0.957, blue: 0.969)     // #F3F4F6
    private let terminalBg = Color(red: 0.059, green: 0.090, blue: 0.165)       // #0F172A
    private let terminalText = Color(red: 0.290, green: 0.871, blue: 0.502)     // #4ADE80
    private let statusGreen = Color(red: 0.063, green: 0.725, blue: 0.506)      // #10B981

    // Managers
    private let permissionManager: PermissionManagerProtocol = PermissionManager.shared
    private let bluetoothManager: BluetoothManagerProtocol = BluetoothManager.shared

    // State matching HomePage.kt
    @State private var searchText = ""
    @State private var selectedDeviceId = "device_a"
    @State private var isConnected = true
    @State private var isScanning = false
    @State private var receivedData = "01 02 03 04"

    // Alerts
    @State private var showPermissionAlert = false
    @State private var showBluetoothOffAlert = false
    @State private var alertMessage = ""
    @State private var hideUnnamed = true

    @State private var discoveredDevices: [DiscoveredDevice] = [
        DiscoveredDevice(id: "device_a", name: "Device A", rssi: -58),
        DiscoveredDevice(id: "device_b", name: "Device B", rssi: -63),
        DiscoveredDevice(id: "device_c", name: "Device C", rssi: -77)
    ]

    private var filteredDevices: [DiscoveredDevice] {
        let query = searchText.trimmingCharacters(in: .whitespacesAndNewlines)
        var list = discoveredDevices
        if hideUnnamed {
            list = list.filter { $0.name != "Unknown Device" && !$0.name.isEmpty }
        }
        if !query.isEmpty {
            list = list.filter {
                $0.name.localizedCaseInsensitiveContains(query) ||
                $0.id.localizedCaseInsensitiveContains(query)
            }
        }
        return list.sorted { $0.rssi > $1.rssi }
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
            .padding(.bottom, 16)
            .padding(.top, 8)
            .background(primaryBlue.ignoresSafeArea(edges: .top))

            // Body Content
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    // --- Section: Device Name ---
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Device Name")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(textLabel)

                        HStack {
                            TextField("Search device...", text: $searchText)
                                .font(.system(size: 15))
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 12)
                        .background(Color.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(borderColor, lineWidth: 1)
                        )
                    }

                    // --- Button: Scan Bluetooth ---
                    Button(action: {
                        handleScanButtonTapped()
                    }) {
                        Text(isScanning ? "Stop Scanning" : "Scan Bluetooth")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 50)
                            .background(primaryBlue)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }

                    // --- Section: Discovered Devices ---
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text("Discovered Devices")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(textLabel)

                            Spacer()

                            Text("\(filteredDevices.count) found")
                                .font(.system(size: 13))
                                .foregroundColor(textRssi)
                        }

                        Toggle(isOn: $hideUnnamed) {
                            Text("Filter unnamed devices")
                                .font(.system(size: 13))
                                .foregroundColor(textSecondary)
                        }
                        .toggleStyle(SwitchToggleStyle(tint: primaryBlue))
                        .padding(.vertical, 2)

                        let itemHeight: CGFloat = 52
                        let maxVisibleItems: CGFloat = 5
                        let listHeight: CGFloat = filteredDevices.isEmpty
                            ? itemHeight
                            : min(CGFloat(filteredDevices.count) * itemHeight, itemHeight * maxVisibleItems)

                        ScrollView(.vertical, showsIndicators: true) {
                            VStack(spacing: 0) {
                                if filteredDevices.isEmpty {
                                    HStack {
                                        Spacer()
                                        Text(isScanning ? "Scanning for nearby devices..." : "No devices found")
                                            .font(.system(size: 14))
                                            .foregroundColor(textRssi)
                                        Spacer()
                                    }
                                    .frame(height: itemHeight)
                                } else {
                                    ForEach(Array(filteredDevices.enumerated()), id: \.element.id) { index, device in
                                        let isSelected = device.id == selectedDeviceId

                                        Button(action: {
                                            selectedDeviceId = device.id
                                        }) {
                                            HStack {
                                                Text(device.name)
                                                    .font(.system(size: 16, weight: isSelected ? .bold : .medium))
                                                    .foregroundColor(isSelected ? primaryBlue : textPrimary)
                                                    .lineLimit(1)

                                                Spacer()

                                                Text(device.rssiString)
                                                    .font(.system(size: 15))
                                                    .foregroundColor(textRssi)
                                            }
                                            .padding(.horizontal, 16)
                                            .frame(height: itemHeight)
                                            .background(isSelected ? lightBlueBg : Color.white)
                                        }
                                        .buttonStyle(.plain)

                                        if index < filteredDevices.count - 1 {
                                            Divider()
                                                .background(dividerColor)
                                        }
                                    }
                                }
                            }
                        }
                        .frame(height: listHeight)
                        .background(Color.white)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                        .overlay(
                            RoundedRectangle(cornerRadius: 14)
                                .stroke(borderColor, lineWidth: 1)
                        )
                    }

                    // --- Action Buttons: Connect & Disconnect ---
                    HStack(spacing: 14) {
                        Button(action: {
                            handleConnectTapped()
                        }) {
                            Text("Connect")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(primaryBlue)
                                .frame(maxWidth: .infinity)
                                .frame(height: 48)
                                .background(lightBlueBg)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        }

                        Button(action: {
                            handleDisconnectTapped()
                        }) {
                            Text("Disconnect")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(textSecondary)
                                .frame(maxWidth: .infinity)
                                .frame(height: 48)
                                .background(buttonGrayBg)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                    }

                    // --- Section: Status Indicator ---
                    HStack(spacing: 8) {
                        Circle()
                            .fill(isConnected ? statusGreen : Color.gray)
                            .frame(width: 10, height: 10)

                        Text(isConnected ? "Status: Connected" : "Status: Disconnected")
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
                            Text(receivedData)
                                .font(.system(size: 15, weight: .medium, design: .monospaced))
                                .foregroundColor(terminalText)
                            Spacer()
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 14)
                        .background(terminalBg)
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 20)
                .padding(.bottom, 24)
            }
        }
        .background(Color.white.ignoresSafeArea())
        .onDisappear {
            if isScanning {
                bluetoothManager.stopScan()
                isScanning = false
            }
        }
        .alert("Bluetooth Permission Required", isPresented: $showPermissionAlert) {
            Button("Open Settings") {
                permissionManager.openAppSettings()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text(alertMessage)
        }
        .alert("Bluetooth Unavailable", isPresented: $showBluetoothOffAlert) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(alertMessage)
        }
    }

    // MARK: - Actions

    private func handleScanButtonTapped() {
        if isScanning {
            bluetoothManager.stopScan()
            isScanning = false
            return
        }

        let status = permissionManager.checkPermission(type: .bluetooth)
        if status == .denied || status == .restricted {
            alertMessage = "Bluetooth permission is denied. Please enable Bluetooth in Settings to scan for nearby devices."
            showPermissionAlert = true
            return
        }

        Task {
            let granted = await permissionManager.requestPermission(type: .bluetooth)
            guard granted else {
                alertMessage = "Bluetooth permission is required to scan for BLE devices. Please allow it in Settings."
                showPermissionAlert = true
                return
            }

            guard bluetoothManager.isEnabled() else {
                alertMessage = "Bluetooth is currently turned off. Please turn on Bluetooth in iOS Settings or Control Center."
                showBluetoothOffAlert = true
                return
            }

            // Clear previous devices to start a fresh scan
            discoveredDevices.removeAll()
            selectedDeviceId = ""

            isScanning = true
            bluetoothManager.startScan(
                onDeviceFound: { bleDevice in
                    let newDevice = DiscoveredDevice(
                        id: bleDevice.id.uuidString,
                        name: bleDevice.name,
                        rssi: bleDevice.rssi
                    )

                    if let index = discoveredDevices.firstIndex(where: { $0.id == newDevice.id }) {
                        discoveredDevices[index] = newDevice
                    } else {
                        // Prevent hoarding endless anonymous rotating beacons
                        if newDevice.name == "Unknown Device" && discoveredDevices.count >= 60 {
                            return
                        }
                        discoveredDevices.append(newDevice)
                    }
                },
                onError: { error in
                    isScanning = false
                    alertMessage = error
                    showBluetoothOffAlert = true
                }
            )
        }
    }

    private func handleConnectTapped() {
        guard !selectedDeviceId.isEmpty else { return }
        guard let uuid = UUID(uuidString: selectedDeviceId) else {
            // For mock demo devices
            isConnected = true
            return
        }
        
        bluetoothManager.connect(peripheralId: uuid) { state in
            switch state {
            case .connected:
                isConnected = true
            case .disconnected:
                isConnected = false
            case .connecting, .disconnecting:
                break
            }
        }
    }

    private func handleDisconnectTapped() {
        bluetoothManager.disconnect()
        isConnected = false
    }
}

#Preview {
    HomePage()
}
