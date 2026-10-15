//
//  FileLogger.swift
//  NativeDeviceMonitorDemo
//
//  Tuần 3 - Bài tập 3.3: Ghi nối tiếp (Append) file nhật ký JSONL vào thư mục Documents
//

import Foundation

public protocol FileLoggerProtocol {
    func append(_ snapshot: DeviceSnapshot)
    func appendRaw(_ line: String)
    func readAll() -> [DeviceSnapshot]
    func readRawLines() -> [String]
    func clear()
    func getFileSizeBytes() -> Int64
    func getLogCount() -> Int
    var fileURL: URL { get }
}

public final class FileLogger: FileLoggerProtocol {
    public static let shared = FileLogger()

    public let fileURL: URL
    private let lock = NSLock()

    public init(fileName: String = "monitor_logs.jsonl") {
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        self.fileURL = docs.appendingPathComponent(fileName)
    }

    public func append(_ snapshot: DeviceSnapshot) {
        guard let jsonString = snapshot.toJsonString() else { return }
        appendRaw(jsonString)
    }

    public func appendRaw(_ line: String) {
        lock.lock()
        defer { lock.unlock() }

        guard let data = "\(line)\n".data(using: .utf8) else { return }

        if FileManager.default.fileExists(atPath: fileURL.path) {
            if let handle = try? FileHandle(forWritingTo: fileURL) {
                defer { try? handle.close() }
                handle.seekToEndOfFile()
                handle.write(data)
            }
        } else {
            try? data.write(to: fileURL, options: .atomic)
        }
    }

    public func readAll() -> [DeviceSnapshot] {
        let lines = readRawLines()
        return lines.compactMap { DeviceSnapshot.fromJsonString($0) }
    }

    public func readRawLines() -> [String] {
        lock.lock()
        defer { lock.unlock() }

        guard FileManager.default.fileExists(atPath: fileURL.path),
              let content = try? String(contentsOf: fileURL, encoding: .utf8) else {
            return []
        }
        return content.components(separatedBy: .newlines).filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty }
    }

    public func clear() {
        lock.lock()
        defer { lock.unlock() }

        try? FileManager.default.removeItem(at: fileURL)
    }

    public func getFileSizeBytes() -> Int64 {
        lock.lock()
        defer { lock.unlock() }

        guard let attrs = try? FileManager.default.attributesOfItem(atPath: fileURL.path),
              let size = attrs[.size] as? Int64 else {
            return 0
        }
        return size
    }

    public func getLogCount() -> Int {
        return readRawLines().count
    }
}
