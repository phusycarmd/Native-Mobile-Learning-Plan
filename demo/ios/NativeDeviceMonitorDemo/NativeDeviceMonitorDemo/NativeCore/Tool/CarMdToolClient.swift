//
//  CarMdToolClient.swift
//  NativeDeviceMonitorDemo
//

import Foundation
import CoreBluetooth

public struct ToolInfo {
    public let bootloaderVersion: String?
    public let firmwareVersion: String?
    public let guid: String?
    public let usbProductId: Int?
}

/// Talks to a CarMD dongle using its proprietary command protocol
/// (reference: carmd-connect-app/libs/ble-tool and the Bluetooth_CommandSet spec).
///
/// Request:  AD | cmdId | len | data... | CS
/// Response: DA (positive) or DD (status) | cmdId + 1 | lenLo | lenHi | data... | CS
/// CS = sum of all preceding bytes mod 256.
///
/// Commands are written to the Write characteristic; responses come back as Notify packets
/// (possibly split into several 20-byte packets).
public final class CarMdToolClient {

    private static let requestHeader: UInt8 = 0xAD
    private static let responsePositive: UInt8 = 0xDA
    private static let responseStatus: UInt8 = 0xDD

    private static let cmdGetToolSetting: UInt8 = 0xE1
    private static let cmdGetDeviceGuid: UInt8 = 0xE5
    private static let cmdGetUsbProductId: UInt8 = 0xE7

    private static let commandTimeout: TimeInterval = 5
    // Wait for the CCCD write before the first command (the original app waits 400 ms after connecting)
    private static let notifySetupDelay: TimeInterval = 0.5

    // (Write, Notify) pairs per BLE module inside the dongle
    private static let characteristicPairs: [(write: CBUUID, notify: CBUUID)] = [
        (CBUUID(string: "FFF2"), CBUUID(string: "FFF1")), // FSC-BT646 / 826E / 825 (CarMD)
        (CBUUID(string: "FFE9"), CBUUID(string: "FFE4"))  // RF-BMS02A / ESP32
    ]

    private struct Response {
        let frame: [UInt8]
        var isPositive: Bool { frame[0] == CarMdToolClient.responsePositive }
        var commandId: UInt8 { frame[1] &- 1 }
        var body: [UInt8] { Array(frame[4..<(frame.count - 1)]) }
    }

    private struct PendingCommand {
        let id = UUID()
        let commandId: UInt8
        let onResult: (Response?, String?) -> Void
    }

    private let bluetoothManager: BluetoothManagerProtocol
    private let writeUuid: CBUUID
    public let notifyUuid: CBUUID
    private let onRawNotification: ((Data) -> Void)?

    // Everything runs on the main queue (BluetoothManager delivers callbacks on main)
    private var receiveBuffer: [UInt8] = []
    private var pendingCommand: PendingCommand?
    private var timeoutWorkItem: DispatchWorkItem?
    private var isNotifying = false

    private init(bluetoothManager: BluetoothManagerProtocol, writeUuid: CBUUID, notifyUuid: CBUUID, onRawNotification: ((Data) -> Void)?) {
        self.bluetoothManager = bluetoothManager
        self.writeUuid = writeUuid
        self.notifyUuid = notifyUuid
        self.onRawNotification = onRawNotification
    }

    /// Returns nil when the peripheral does not expose the CarMD dongle characteristics.
    public static func create(
        bluetoothManager: BluetoothManagerProtocol,
        services: [BleService],
        onRawNotification: ((Data) -> Void)? = nil
    ) -> CarMdToolClient? {
        let available = Set(services.flatMap { $0.characteristics.map(\.id) })
        guard let pair = characteristicPairs.first(where: { available.contains($0.write) && available.contains($0.notify) }) else {
            return nil
        }
        return CarMdToolClient(bluetoothManager: bluetoothManager, writeUuid: pair.write, notifyUuid: pair.notify, onRawNotification: onRawNotification)
    }

    // MARK: - Public API

    /// Reads E1 (versions), E5 (GUID) and E7 (USB product id) one after another.
    public func getToolInfo(completion: @escaping (ToolInfo?, String?) -> Void) {
        ensureNotifying { [weak self] in
            guard let self else { return }
            self.sendCommand(Self.cmdGetToolSetting, data: [0x00]) { settingBody, settingError in
                self.sendCommand(Self.cmdGetDeviceGuid) { guidBody, _ in
                    self.sendCommand(Self.cmdGetUsbProductId) { productIdBody, _ in
                        let versions = settingBody.flatMap(Self.parseToolSetting)
                        let info = ToolInfo(
                            bootloaderVersion: versions?.bootloader,
                            firmwareVersion: versions?.firmware,
                            guid: guidBody.flatMap(Self.parseGuid),
                            usbProductId: productIdBody.flatMap(Self.parseUsbProductId)
                        )
                        if versions == nil && info.guid == nil && info.usbProductId == nil {
                            completion(nil, settingError ?? "Không đọc được thông tin tool.")
                        } else {
                            completion(info, nil)
                        }
                    }
                }
            }
        }
    }

    // MARK: - Protocol

    static func buildRequest(commandId: UInt8, data: [UInt8] = []) -> [UInt8] {
        var frame: [UInt8] = [requestHeader, commandId, UInt8(data.count)] + data
        frame.append(checksum(frame))
        return frame
    }

    private static func checksum<S: Sequence>(_ bytes: S) -> UInt8 where S.Element == UInt8 {
        UInt8(bytes.reduce(0) { $0 + Int($1) } % 256)
    }

    /// E1: bootloader = body[0..14], firmware = body[16..30] (ASCII, NUL stripped)
    static func parseToolSetting(_ body: [UInt8]) -> (bootloader: String, firmware: String)? {
        guard body.count >= 32 else { return nil }
        return (asciiString(body[0..<15]), asciiString(body[16..<31]))
    }

    /// E5: 16 bytes in .NET GUID order -> reverse bytes 0-3, 4-5, 6-7, then format 8-4-4-4-12
    static func parseGuid(_ body: [UInt8]) -> String? {
        guard body.count == 16, !body.allSatisfy({ $0 == 0x00 }), !body.allSatisfy({ $0 == 0xFF }) else { return nil }
        let bytes = Array(body[0..<4].reversed()) + Array(body[4..<6].reversed()) + Array(body[6..<8].reversed()) + Array(body[8...])
        let hex = bytes.map { String(format: "%02x", $0) }.joined()
        let parts = [0..<8, 8..<12, 12..<16, 16..<20, 20..<32].map { range in
            String(hex[hex.index(hex.startIndex, offsetBy: range.lowerBound)..<hex.index(hex.startIndex, offsetBy: range.upperBound)])
        }
        return parts.joined(separator: "-")
    }

    /// E7: 2 bytes little-endian
    static func parseUsbProductId(_ body: [UInt8]) -> Int? {
        guard body.count == 2 else { return nil }
        return Int(body[0]) | (Int(body[1]) << 8)
    }

    private static func asciiString(_ bytes: ArraySlice<UInt8>) -> String {
        String(decoding: bytes.filter { (1...127).contains($0) }, as: UTF8.self)
    }

    // MARK: - Transport

    private func ensureNotifying(_ action: @escaping () -> Void) {
        if isNotifying {
            action()
            return
        }
        bluetoothManager.subscribe(characteristicUuid: notifyUuid) { [weak self] data in
            self?.handleNotification(data)
        }
        isNotifying = true
        DispatchQueue.main.asyncAfter(deadline: .now() + Self.notifySetupDelay, execute: action)
    }

    /// onResult receives the body of a DA response, or nil plus an error message
    private func sendCommand(_ commandId: UInt8, data: [UInt8] = [], onResult: @escaping ([UInt8]?, String?) -> Void) {
        guard pendingCommand == nil else {
            onResult(nil, "Tool đang xử lý lệnh khác.")
            return
        }

        let command = PendingCommand(commandId: commandId) { response, error in
            guard let response else {
                onResult(nil, error)
                return
            }
            if response.isPositive {
                onResult(response.body, nil)
            } else {
                // DD frame: body[0] = C1 (ok) / C2 (error), body[1] = error code
                let errorCode = response.body.count > 1 ? String(format: "0x%02X", response.body[1]) : "?"
                onResult(nil, String(format: "Tool từ chối lệnh 0x%02X", commandId) + " (mã lỗi \(errorCode)).")
            }
        }
        pendingCommand = command

        let timeout = DispatchWorkItem { [weak self] in
            print(String(format: "[CarMdToolClient] Command 0x%02X timed out", commandId))
            self?.receiveBuffer.removeAll()
            self?.finishPendingCommand(nil, "Hết thời gian chờ phản hồi từ tool.")
        }
        timeoutWorkItem = timeout
        DispatchQueue.main.asyncAfter(deadline: .now() + Self.commandTimeout, execute: timeout)

        let frame = Self.buildRequest(commandId: commandId, data: data)
        print("[CarMdToolClient] TX \(frame.hexString)")
        bluetoothManager.write(characteristicUuid: writeUuid, data: Data(frame)) { [weak self] error in
            guard let self, let error, self.pendingCommand?.id == command.id else { return }
            self.finishPendingCommand(nil, "Gửi lệnh thất bại: \(error.localizedDescription)")
        }
    }

    private func handleNotification(_ data: Data) {
        onRawNotification?(data)
        receiveBuffer.append(contentsOf: data)

        // Reassemble notification packets into complete frames using the length field
        while true {
            guard let start = receiveBuffer.firstIndex(where: { $0 == Self.responsePositive || $0 == Self.responseStatus }) else {
                receiveBuffer.removeAll()
                return
            }
            if start > 0 { receiveBuffer.removeFirst(start) }
            guard receiveBuffer.count >= 4 else { return }

            let frameLength = (Int(receiveBuffer[2]) | (Int(receiveBuffer[3]) << 8)) + 5
            guard receiveBuffer.count >= frameLength else { return }

            let frame = Array(receiveBuffer[0..<frameLength])
            receiveBuffer.removeFirst(frameLength)
            handleFrame(frame)
        }
    }

    private func handleFrame(_ frame: [UInt8]) {
        print("[CarMdToolClient] RX \(frame.hexString)")
        if Self.checksum(frame.dropLast()) != frame.last {
            // The original app does not verify response checksums, so only warn
            print("[CarMdToolClient] Response checksum mismatch")
        }

        let response = Response(frame: frame)
        if response.commandId == pendingCommand?.commandId {
            finishPendingCommand(response, nil)
        } else {
            // Unsolicited frame (e.g. DA C6 ... low battery) or a late response to a timed-out command
            print(String(format: "[CarMdToolClient] Ignoring frame for command 0x%02X", response.commandId))
        }
    }

    private func finishPendingCommand(_ response: Response?, _ error: String?) {
        timeoutWorkItem?.cancel()
        timeoutWorkItem = nil
        guard let command = pendingCommand else { return }
        pendingCommand = nil
        command.onResult(response, error)
    }
}

private extension Array where Element == UInt8 {
    var hexString: String {
        map { String(format: "%02X", $0) }.joined(separator: " ")
    }
}
