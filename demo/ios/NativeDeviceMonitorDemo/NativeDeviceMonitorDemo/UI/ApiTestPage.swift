//
//  ApiTestPage.swift
//  NativeDeviceMonitorDemo
//

import SwiftUI

private enum HttpMethod: String, CaseIterable {
    case get = "GET"
    case post = "POST"
}

/// URL mẫu để test nhanh đủ các trường hợp: thành công, lỗi HTTP và timeout (client timeout là 10s).
/// Timeout: cổng 81 bị firewall bỏ qua nên kết nối treo đến khi hết giờ.
/// Không dùng httpbin.org/delay vì httpbin giới hạn delay tối đa 10s, trùng với timeout của client
private let quickUrls: [(label: String, url: String)] = [
    ("GET 200", "https://httpbin.org/get"),
    ("POST 200", "https://httpbin.org/post"),
    ("404", "https://httpbin.org/status/404"),
    ("500", "https://httpbin.org/status/500"),
    ("Timeout", "https://example.com:81")
]

struct ApiTestPage: View {
    // Design colors matching ApiTestPage.kt
    private let primaryBlue = Color(red: 0.145, green: 0.388, blue: 0.922)      // #2563EB
    private let lightBlueBg = Color(red: 0.937, green: 0.957, blue: 1.0)        // #EFF4FF
    private let lightBlueBorder = Color(red: 0.859, green: 0.894, blue: 1.0)    // #DBE4FF
    private let textPrimary = Color(red: 0.122, green: 0.161, blue: 0.216)      // #1F2937
    private let textSecondary = Color(red: 0.294, green: 0.333, blue: 0.388)    // #4B5563
    private let textLabel = Color(red: 0.216, green: 0.255, blue: 0.318)        // #374151
    private let borderColor = Color(red: 0.898, green: 0.906, blue: 0.922)      // #E5E7EB
    private let placeholderColor = Color(red: 0.612, green: 0.639, blue: 0.686) // #9CA3AF
    private let terminalBg = Color(red: 0.059, green: 0.090, blue: 0.165)       // #0F172A
    private let terminalText = Color(red: 0.290, green: 0.871, blue: 0.502)     // #4ADE80
    private let terminalLabel = Color(red: 0.580, green: 0.639, blue: 0.722)    // #94A3B8
    private let terminalError = Color(red: 0.973, green: 0.443, blue: 0.443)    // #F87171
    private let statusGreen = Color(red: 0.063, green: 0.725, blue: 0.506)      // #10B981
    private let statusAmber = Color(red: 0.961, green: 0.620, blue: 0.043)      // #F59E0B
    private let stopRed = Color(red: 0.863, green: 0.149, blue: 0.149)          // #DC2626

    private let apiClient: ApiClientProtocol = ApiClient.shared

    let onMenuTap: () -> Void

    @State private var url = quickUrls[0].url
    @State private var selectedMethod: HttpMethod = .get
    @State private var isLoading = false
    @State private var response: ApiResponse?

    @FocusState private var isUrlFocused: Bool

    // Khai báo init tường minh: memberwise init sẽ là private vì có thuộc tính @State private
    init(onMenuTap: @escaping () -> Void = {}) {
        self.onMenuTap = onMenuTap
    }

    var body: some View {
        VStack(spacing: 0) {
            AppHeader(title: "API Test", onMenuTap: onMenuTap)

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    // --- Section: API URL ---
                    VStack(alignment: .leading, spacing: 8) {
                        Text("API URL")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(textLabel)

                        TextField(
                            "",
                            text: $url,
                            prompt: Text("https://...")
                                .font(.system(size: 14))
                                .foregroundColor(placeholderColor)
                        )
                        .font(.system(size: 14, design: .monospaced))
                        .foregroundColor(textPrimary)
                        .tint(primaryBlue)
                        .focused($isUrlFocused)
                        .keyboardType(.URL)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                        .submitLabel(.go)
                        .onSubmit { send(selectedMethod) }
                        .padding(.horizontal, 16)
                        .frame(height: 56)
                        .background(Color.white)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(
                                    isUrlFocused ? primaryBlue : borderColor,
                                    lineWidth: isUrlFocused ? 2 : 1
                                )
                        )

                        // URL mẫu
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(quickUrls, id: \.label) { preset in
                                    let isSelected = url == preset.url
                                    Button {
                                        url = preset.url
                                    } label: {
                                        Text(preset.label)
                                            .font(.system(size: 13, weight: .medium))
                                            .foregroundColor(isSelected ? primaryBlue : textSecondary)
                                            .padding(.horizontal, 12)
                                            .padding(.vertical, 6)
                                            .background(isSelected ? lightBlueBg : Color.white)
                                            .clipShape(Capsule())
                                            .overlay(Capsule().stroke(isSelected ? primaryBlue : borderColor, lineWidth: 1))
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                            .padding(1)
                        }
                    }

                    // --- Buttons: GET / POST ---
                    // Nút của method vừa chọn được tô đậm, giống mockup
                    HStack(spacing: 12) {
                        ForEach(HttpMethod.allCases, id: \.self) { method in
                            methodButton(method)
                        }
                    }

                    // --- Section: Response ---
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Response")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(textLabel)

                        // Cho phép bôi đen / copy nội dung response
                        Text(responseText)
                            .font(.system(size: 13, design: .monospaced))
                            .foregroundColor(responseColor)
                            .lineSpacing(3)
                            .textSelection(.enabled)
                            .frame(maxWidth: .infinity, minHeight: 112, alignment: .topLeading)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 14)
                            .background(terminalBg)
                            .clipShape(RoundedRectangle(cornerRadius: 10))

                        // --- Status: ● 200 OK · 128 ms ---
                        if let response, !isLoading {
                            HStack(spacing: 8) {
                                Circle()
                                    .fill(statusColor(for: response))
                                    .frame(width: 10, height: 10)
                                Text(statusLine(for: response))
                                    .font(.system(size: 14))
                                    .foregroundColor(textSecondary)
                            }
                        }
                    }

                    Spacer().frame(height: 16)
                }
                .padding(20)
            }
            .scrollDismissesKeyboard(.interactively)
        }
        .background(Color.white.ignoresSafeArea())
    }

    // MARK: - Subviews

    private func methodButton(_ method: HttpMethod) -> some View {
        let isPrimary = method == selectedMethod
        return Button {
            send(method)
        } label: {
            ZStack {
                if isLoading && isPrimary {
                    ProgressView()
                        .tint(.white)
                } else {
                    Text(method.rawValue)
                        .font(.system(size: 16, weight: .bold))
                }
            }
            .foregroundColor(isPrimary ? .white : primaryBlue)
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(isPrimary ? primaryBlue : lightBlueBg)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isPrimary ? Color.clear : lightBlueBorder, lineWidth: 1)
            )
        }
        .disabled(isLoading)
        .opacity(isLoading && !isPrimary ? 0.6 : 1)
    }

    // MARK: - Actions

    private func send(_ method: HttpMethod) {
        guard !isLoading else { return }
        isUrlFocused = false
        selectedMethod = method
        isLoading = true
        let target = url.trimmingCharacters(in: .whitespacesAndNewlines)

        Task {
            switch method {
            case .get:
                response = await apiClient.get(url: target, headers: nil)
            case .post:
                response = await apiClient.post(url: target, jsonBody: Self.samplePostBody(), headers: nil)
            }
            isLoading = false
        }
    }

    // MARK: - Display helpers

    private var responseText: String {
        if isLoading { return "Đang gửi \(selectedMethod.rawValue) request..." }
        guard let response else { return "Bấm GET hoặc POST để gửi request." }
        if let body = response.body, !body.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return Self.prettyJson(body)
        }
        return response.errorMessage ?? "(Không có nội dung)"
    }

    private var responseColor: Color {
        guard let response, !isLoading else { return terminalLabel }
        return response.statusCode == 0 ? terminalError : terminalText
    }

    private func statusColor(for response: ApiResponse) -> Color {
        if response.isSuccessful { return statusGreen }
        if (300...499).contains(response.statusCode) { return statusAmber }
        return stopRed
    }

    private func statusLine(for response: ApiResponse) -> String {
        let label: String
        if response.statusCode > 0 {
            label = "\(response.statusCode) \(Self.httpStatusText(response.statusCode))"
                .trimmingCharacters(in: .whitespaces)
        } else if response.isTimeout {
            label = "Timeout"
        } else {
            label = "Network error"
        }
        return "\(label) · \(response.durationMs) ms"
    }

    /// Tự map mã -> chữ: HTTPURLResponse.localizedString(forStatusCode: 200) trả về "no error"
    private static func httpStatusText(_ code: Int) -> String {
        switch code {
        case 200: return "OK"
        case 201: return "Created"
        case 202: return "Accepted"
        case 204: return "No Content"
        case 301: return "Moved Permanently"
        case 302: return "Found"
        case 304: return "Not Modified"
        case 400: return "Bad Request"
        case 401: return "Unauthorized"
        case 403: return "Forbidden"
        case 404: return "Not Found"
        case 405: return "Method Not Allowed"
        case 408: return "Request Timeout"
        case 429: return "Too Many Requests"
        case 500: return "Internal Server Error"
        case 502: return "Bad Gateway"
        case 503: return "Service Unavailable"
        case 504: return "Gateway Timeout"
        default: return ""
        }
    }

    /// Format JSON thụt lề cho dễ đọc; không phải JSON thì giữ nguyên
    private static func prettyJson(_ raw: String) -> String {
        guard let data = raw.data(using: .utf8),
              let object = try? JSONSerialization.jsonObject(with: data),
              let pretty = try? JSONSerialization.data(
                withJSONObject: object,
                options: [.prettyPrinted, .sortedKeys, .withoutEscapingSlashes]
              ),
              let text = String(data: pretty, encoding: .utf8)
        else { return raw }
        return text
    }

    /// Body mẫu theo mockup. Battery đang là giá trị giả, Tuần 3 sẽ lấy từ BatteryProvider
    private static func samplePostBody() -> String {
        let payload: [String: Any] = [
            "status": "ok",
            "battery": 82,
            "ts": ISO8601DateFormatter().string(from: Date())
        ]
        guard let data = try? JSONSerialization.data(withJSONObject: payload, options: [.sortedKeys]),
              let text = String(data: data, encoding: .utf8)
        else { return "{}" }
        return text
    }
}

#Preview {
    ApiTestPage()
}
