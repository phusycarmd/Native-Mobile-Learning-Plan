//
//  AppRoot.swift
//  NativeDeviceMonitorDemo
//

import SwiftUI
import UIKit

/// Các màn hình có trong Side Menu. Thêm màn mới: thêm 1 case ở đây và 1 nhánh trong AppRoot
enum AppScreen: CaseIterable, Identifiable {
    case bluetooth
    case apiClient

    var id: Self { self }

    var title: String {
        switch self {
        case .bluetooth: return "Bluetooth BLE"
        case .apiClient: return "API Client"
        }
    }

    var subtitle: String {
        switch self {
        case .bluetooth: return "Scan, connect, notify"
        case .apiClient: return "HTTP GET / POST"
        }
    }
}

/// SwiftUI trên iPhone không có sẵn navigation drawer như Android,
/// nên tự dựng: màn hình hiện tại + lớp phủ mờ + menu trượt từ cạnh trái.
struct AppRoot: View {
    private let menuWidth: CGFloat = 300

    @State private var currentScreen: AppScreen = .bluetooth
    @State private var isMenuOpen = false
    // Độ lệch khi vuốt menu sang trái để đóng
    @GestureState private var menuDragOffset: CGFloat = 0

    var body: some View {
        ZStack(alignment: .leading) {
            // Màn Bluetooth luôn nằm trong view hierarchy, chỉ bị ẩn khi sang màn khác.
            // Nếu bị gỡ ra, onDisappear của HomePage sẽ ngắt BLE và mất toàn bộ @State (thiết bị, dữ liệu notify)
            let isBluetoothVisible = currentScreen == .bluetooth
            HomePage(onMenuTap: openMenu)
                .opacity(isBluetoothVisible ? 1 : 0)
                .allowsHitTesting(isBluetoothVisible)
                .accessibilityHidden(!isBluetoothVisible)

            if currentScreen == .apiClient {
                ApiTestPage(onMenuTap: openMenu)
            }

            if isMenuOpen {
                // Chạm ra ngoài menu để đóng
                Color.black.opacity(0.35)
                    .ignoresSafeArea()
                    .onTapGesture(perform: closeMenu)
                    .transition(.opacity)

                SideMenu(currentScreen: currentScreen, onScreenSelected: select)
                    .frame(width: menuWidth)
                    .offset(x: min(menuDragOffset, 0))
                    .gesture(
                        DragGesture()
                            .updating($menuDragOffset) { value, state, _ in
                                state = value.translation.width
                            }
                            .onEnded { value in
                                if value.translation.width < -menuWidth / 3 {
                                    closeMenu()
                                }
                            }
                    )
                    .transition(.move(edge: .leading))
                    .zIndex(1)
            }
        }
        .animation(.easeOut(duration: 0.25), value: isMenuOpen)
    }

    private func openMenu() {
        // Ẩn bàn phím trước khi mở menu
        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
        isMenuOpen = true
    }

    private func closeMenu() {
        isMenuOpen = false
    }

    private func select(_ screen: AppScreen) {
        currentScreen = screen
        closeMenu()
    }
}

// MARK: - Side Menu

private struct SideMenu: View {
    // Design colors matching AppNavigation.kt
    private let primaryBlue = Color(red: 0.145, green: 0.388, blue: 0.922)      // #2563EB
    private let lightBlueBg = Color(red: 0.937, green: 0.957, blue: 1.0)        // #EFF4FF
    private let textPrimary = Color(red: 0.122, green: 0.161, blue: 0.216)      // #1F2937
    private let textSecondary = Color(red: 0.420, green: 0.447, blue: 0.502)    // #6B7280

    let currentScreen: AppScreen
    let onScreenSelected: (AppScreen) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            VStack(alignment: .leading, spacing: 4) {
                Text("Native Device Monitor")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.white)
                Text("iOS · NativeCore Demo")
                    .font(.system(size: 13))
                    .foregroundColor(.white.opacity(0.8))
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 20)
            .padding(.vertical, 24)
            .background(primaryBlue.ignoresSafeArea(edges: .top))

            VStack(spacing: 4) {
                ForEach(AppScreen.allCases) { screen in
                    let isSelected = screen == currentScreen
                    Button {
                        onScreenSelected(screen)
                    } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(screen.title)
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(isSelected ? primaryBlue : textPrimary)
                            Text(screen.subtitle)
                                .font(.system(size: 12))
                                .foregroundColor(textSecondary)
                        }
                        .frame(maxWidth: .infinity, minHeight: 56, alignment: .leading)
                        .padding(.horizontal, 16)
                        .background(isSelected ? lightBlueBg : Color.clear)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(12)

            Spacer(minLength: 0)
        }
        .frame(maxHeight: .infinity)
        .background(Color.white.ignoresSafeArea())
        .shadow(color: .black.opacity(0.15), radius: 12, x: 4, y: 0)
    }
}

// MARK: - Header

/// Thanh tiêu đề dùng chung cho mọi màn hình: nút mở menu + tiêu đề
struct AppHeader: View {
    private let primaryBlue = Color(red: 0.145, green: 0.388, blue: 0.922)      // #2563EB

    let title: String
    let onMenuTap: () -> Void

    var body: some View {
        HStack(spacing: 4) {
            Button(action: onMenuTap) {
                Image(systemName: "line.3.horizontal")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
            .accessibilityLabel("Mở menu")

            Text(title)
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(.white)
            Spacer()
        }
        .padding(.leading, 8)
        .padding(.trailing, 20)
        .padding(.vertical, 6)
        .background(primaryBlue.ignoresSafeArea(edges: .top))
    }
}

#Preview {
    AppRoot()
}
