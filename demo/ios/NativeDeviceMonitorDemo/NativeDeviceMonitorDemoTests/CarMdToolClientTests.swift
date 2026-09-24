//
//  CarMdToolClientTests.swift
//  NativeDeviceMonitorDemoTests
//

import Foundation
import Testing
@testable import NativeDeviceMonitorDemo

// Sample data from the Bluetooth_CommandSet spec and carmd-connect-app fakers
struct CarMdToolClientTests {

    private func bytes(_ hex: String) -> [UInt8] {
        let clean = hex.replacingOccurrences(of: " ", with: "")
        return stride(from: 0, to: clean.count, by: 2).map { offset in
            let start = clean.index(clean.startIndex, offsetBy: offset)
            return UInt8(clean[start..<clean.index(start, offsetBy: 2)], radix: 16)!
        }
    }

    /// Body = frame without the 4 header bytes (DA, id, lenLo, lenHi) and the trailing checksum
    private func body(_ frame: String) -> [UInt8] {
        let all = bytes(frame)
        return Array(all[4..<(all.count - 1)])
    }

    @Test func buildRequestMatchesSpecBytes() {
        #expect(CarMdToolClient.buildRequest(commandId: 0xE1, data: [0x00]) == bytes("AD E1 01 00 8F"))
        #expect(CarMdToolClient.buildRequest(commandId: 0xE5) == bytes("AD E5 00 92"))
        #expect(CarMdToolClient.buildRequest(commandId: 0xE7) == bytes("AD E7 00 94"))
        #expect(CarMdToolClient.buildRequest(commandId: 0xF3, data: [0x02]) == bytes("AD F3 01 02 A3"))
    }

    @Test func parseToolSettingFakerResponse() {
        let frame = "dae220005630312e30312e3035000000000000005630312e30342e3033000000000000008f"
        let versions = CarMdToolClient.parseToolSetting(body(frame))
        #expect(versions?.bootloader == "V01.01.05")
        #expect(versions?.firmware == "V01.04.03")
    }

    @Test func parseGuidSwapsDotNetByteOrder() {
        #expect(CarMdToolClient.parseGuid(bytes("44cf4f308522c14bb18f7c51dae1428e")) == "304fcf44-2285-4bc1-b18f-7c51dae1428e")
        #expect(CarMdToolClient.parseGuid([UInt8](repeating: 0, count: 16)) == nil)
    }

    @Test func parseUsbProductIdLittleEndianUnsigned() {
        #expect(CarMdToolClient.parseUsbProductId(body("dae802001803df")) == 0x0318)
        #expect(CarMdToolClient.parseUsbProductId(body("DAE80200ADDA4B")) == 0xDAAD)
    }
}
