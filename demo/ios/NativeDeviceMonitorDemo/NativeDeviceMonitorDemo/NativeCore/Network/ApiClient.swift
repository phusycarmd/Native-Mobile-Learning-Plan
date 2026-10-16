//
//  ApiClient.swift
//  NativeDeviceMonitorDemo
//

import Foundation
import Alamofire

// MARK: - Response Model (theo đặc tả docs)

public struct ApiResponse {
    public let statusCode: Int
    public let body: String?
    public let isSuccessful: Bool       // statusCode in 200...299
    public let errorMessage: String?
    /// Thời gian từ lúc gửi request đến lúc nhận xong response (hoặc gặp lỗi)
    public let durationMs: Int
    /// true khi request bị huỷ do vượt quá timeout
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

// MARK: - Implementation (Alamofire)

public final class ApiClient: ApiClientProtocol {

    /// Dùng chung một Session: Session bị giải phóng sẽ huỷ luôn các request đang chạy
    public static let shared = ApiClient()

    private let session: Session

    public init(timeoutInterval: TimeInterval = 10) {
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = timeoutInterval
        configuration.timeoutIntervalForResource = timeoutInterval
        self.session = Session(configuration: configuration)
    }

    // MARK: - GET

    public func get(url: String, headers: [String: String]? = nil) async -> ApiResponse {
        guard URL(string: url) != nil else {
            return ApiResponse(statusCode: 0, body: nil, isSuccessful: false, errorMessage: "URL không hợp lệ: \(url)")
        }

        var httpHeaders = HTTPHeaders()
        httpHeaders.add(name: "Accept", value: "application/json")
        headers?.forEach { httpHeaders.add(name: $0.key, value: $0.value) }

        print("[ApiClient-Alamofire] GET \(url)")

        let startedAt = Date()
        let dataTask = session.request(
            url,
            method: .get,
            headers: httpHeaders
        ).serializingString()

        let response = await dataTask.response
        return mapResponse(response, durationMs: elapsedMs(since: startedAt))
    }

    // MARK: - POST

    public func post(url: String, jsonBody: String, headers: [String: String]? = nil) async -> ApiResponse {
        guard URL(string: url) != nil else {
            return ApiResponse(statusCode: 0, body: nil, isSuccessful: false, errorMessage: "URL không hợp lệ: \(url)")
        }

        var httpHeaders = HTTPHeaders()
        httpHeaders.add(name: "Content-Type", value: "application/json")
        httpHeaders.add(name: "Accept", value: "application/json")
        headers?.forEach { httpHeaders.add(name: $0.key, value: $0.value) }

        print("[ApiClient-Alamofire] POST \(url) | body: \(jsonBody)")

        let data = jsonBody.data(using: .utf8) ?? Data()

        let startedAt = Date()
        let dataTask = session.upload(
            data,
            to: url,
            method: .post,
            headers: httpHeaders
        ).serializingString()

        let response = await dataTask.response
        return mapResponse(response, durationMs: elapsedMs(since: startedAt))
    }

    // MARK: - Private

    private func mapResponse(_ response: DataResponse<String, AFError>, durationMs: Int) -> ApiResponse {
        let statusCode = response.response?.statusCode ?? 0
        let isSuccess = (200...299).contains(statusCode)

        switch response.result {
        case .success(let body):
            print("[ApiClient-Alamofire] Status \(statusCode) -> Success")
            return ApiResponse(
                statusCode: statusCode,
                body: body,
                isSuccessful: isSuccess,
                errorMessage: isSuccess ? nil : "HTTP \(statusCode)",
                durationMs: durationMs
            )
        case .failure(let error):
            print("[ApiClient-Alamofire] Error: \(error.localizedDescription)")
            let body = response.data.flatMap { String(data: $0, encoding: .utf8) }
            // Alamofire bọc lỗi gốc của URLSession bên trong AFError
            let isTimeout = (error.underlyingError as? URLError)?.code == .timedOut
            return ApiResponse(
                statusCode: statusCode,
                body: body,
                isSuccessful: false,
                errorMessage: isTimeout ? "Request timeout: \(error.localizedDescription)" : error.localizedDescription,
                durationMs: durationMs,
                isTimeout: isTimeout
            )
        }
    }

    private func elapsedMs(since start: Date) -> Int {
        Int(Date().timeIntervalSince(start) * 1000)
    }
}
