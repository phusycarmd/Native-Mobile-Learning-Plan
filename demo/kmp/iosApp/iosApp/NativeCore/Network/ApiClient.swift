//
//  ApiClient.swift
//  NativeDeviceMonitorDemo
//

import Foundation

// MARK: - Response Model

public struct ApiResponse {
    public let statusCode: Int
    public let body: String?
    public let isSuccessful: Bool
    public let errorMessage: String?
    public let durationMs: Int
    public let isTimeout: Bool

    public init(
        statusCode: Int,
        body: String?,
        isSuccessful: Bool,
        errorMessage: String? = nil,
        durationMs: Int = 0,
        isTimeout: Bool = false
    ) {
        self.statusCode = statusCode
        self.body = body
        self.isSuccessful = isSuccessful
        self.errorMessage = errorMessage
        self.durationMs = durationMs
        self.isTimeout = isTimeout
    }
}

// MARK: - Protocol

public protocol ApiClientProtocol {
    func get(url: String, headers: [String: String]?) async -> ApiResponse
    func post(url: String, jsonBody: String, headers: [String: String]?) async -> ApiResponse
}

// MARK: - Implementation (Foundation URLSession)

public final class ApiClient: ApiClientProtocol {
    public static let shared = ApiClient()

    private let session: URLSession

    public init(timeoutInterval: TimeInterval = 10) {
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = timeoutInterval
        configuration.timeoutIntervalForResource = timeoutInterval
        self.session = URLSession(configuration: configuration)
    }

    public func get(url: String, headers: [String: String]? = nil) async -> ApiResponse {
        guard let requestUrl = URL(string: url) else {
            return ApiResponse(statusCode: 0, body: nil, isSuccessful: false, errorMessage: "URL không hợp lệ: \(url)")
        }
        var request = URLRequest(url: requestUrl)
        request.httpMethod = "GET"
        headers?.forEach { request.setValue($0.value, forHTTPHeaderField: $0.key) }

        let startTime = DispatchTime.now()
        do {
            let (data, response) = try await session.data(for: request)
            let endTime = DispatchTime.now()
            let durationMs = Int((endTime.uptimeNanoseconds - startTime.uptimeNanoseconds) / 1_000_000)
            let httpResponse = response as? HTTPURLResponse
            let statusCode = httpResponse?.statusCode ?? 200
            let body = String(data: data, encoding: .utf8)
            return ApiResponse(
                statusCode: statusCode,
                body: body,
                isSuccessful: (200...299).contains(statusCode),
                durationMs: durationMs
            )
        } catch {
            let endTime = DispatchTime.now()
            let durationMs = Int((endTime.uptimeNanoseconds - startTime.uptimeNanoseconds) / 1_000_000)
            let nsError = error as NSError
            let isTimeout = nsError.code == NSURLErrorTimedOut
            return ApiResponse(
                statusCode: 0,
                body: nil,
                isSuccessful: false,
                errorMessage: error.localizedDescription,
                durationMs: durationMs,
                isTimeout: isTimeout
            )
        }
    }

    public func post(url: String, jsonBody: String, headers: [String: String]? = nil) async -> ApiResponse {
        guard let requestUrl = URL(string: url) else {
            return ApiResponse(statusCode: 0, body: nil, isSuccessful: false, errorMessage: "URL không hợp lệ: \(url)")
        }
        var request = URLRequest(url: requestUrl)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = jsonBody.data(using: .utf8)
        headers?.forEach { request.setValue($0.value, forHTTPHeaderField: $0.key) }

        let startTime = DispatchTime.now()
        do {
            let (data, response) = try await session.data(for: request)
            let endTime = DispatchTime.now()
            let durationMs = Int((endTime.uptimeNanoseconds - startTime.uptimeNanoseconds) / 1_000_000)
            let httpResponse = response as? HTTPURLResponse
            let statusCode = httpResponse?.statusCode ?? 200
            let body = String(data: data, encoding: .utf8)
            return ApiResponse(
                statusCode: statusCode,
                body: body,
                isSuccessful: (200...299).contains(statusCode),
                durationMs: durationMs
            )
        } catch {
            let endTime = DispatchTime.now()
            let durationMs = Int((endTime.uptimeNanoseconds - startTime.uptimeNanoseconds) / 1_000_000)
            let nsError = error as NSError
            let isTimeout = nsError.code == NSURLErrorTimedOut
            return ApiResponse(
                statusCode: 0,
                body: nil,
                isSuccessful: false,
                errorMessage: error.localizedDescription,
                durationMs: durationMs,
                isTimeout: isTimeout
            )
        }
    }
}
