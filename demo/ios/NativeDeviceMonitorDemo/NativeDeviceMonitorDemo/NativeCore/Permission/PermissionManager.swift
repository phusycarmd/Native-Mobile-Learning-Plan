//
//  PermissionManager.swift
//  NativeDeviceMonitorDemo
//
//  Created by synguyen on 17/9/26.
//

import Foundation
#if canImport(UIKit)
import UIKit
#endif
#if canImport(AppKit)
import AppKit
#endif
import AVFoundation
import Photos
import CoreBluetooth

public enum PermissionType: CaseIterable {
    case bluetooth
    case camera
    case photoLibrary
}

public enum PermissionStatus: Equatable {
    case authorized
    case denied
    case notDetermined
    case restricted
    case limited
    
    public var isGranted: Bool {
        return self == .authorized || self == .limited
    }
}

public protocol PermissionManagerProtocol {
    func checkPermission(type: PermissionType) -> PermissionStatus
    func requestPermission(type: PermissionType, completion: @escaping (Bool) -> Void)
    func openAppSettings()
}

public extension PermissionManagerProtocol {
    func requestPermission(type: PermissionType) async -> Bool {
        await withCheckedContinuation { continuation in
            requestPermission(type: type) { granted in
                continuation.resume(returning: granted)
            }
        }
    }
    
    func checkMultiplePermissions(types: [PermissionType]) -> [PermissionType: PermissionStatus] {
        var results: [PermissionType: PermissionStatus] = [:]
        for type in types {
            results[type] = checkPermission(type: type)
        }
        return results
    }
    
    func requestMultiplePermissions(
        types: [PermissionType],
        completion: @escaping ([PermissionType: Bool]) -> Void
    ) {
        guard !types.isEmpty else {
            DispatchQueue.main.async {
                completion([:])
            }
            return
        }
        
        var results: [PermissionType: Bool] = [:]
        let group = DispatchGroup()
        let lock = NSLock()
        
        for type in types {
            group.enter()
            requestPermission(type: type) { granted in
                lock.lock()
                results[type] = granted
                lock.unlock()
                group.leave()
            }
        }
        
        group.notify(queue: .main) {
            completion(results)
        }
    }
    
    func requestMultiplePermissions(types: [PermissionType]) async -> [PermissionType: Bool] {
        await withCheckedContinuation { continuation in
            requestMultiplePermissions(types: types) { results in
                continuation.resume(returning: results)
            }
        }
    }
}

public class PermissionManager: NSObject, PermissionManagerProtocol, CBCentralManagerDelegate {
    
    public static let shared = PermissionManager()
    
    private var centralManager: CBCentralManager?
    private var bluetoothCallbacks: [(Bool) -> Void] = []
    
    public override init() {
        super.init()
    }
    
    // MARK: - Check Permission
    
    public func checkPermission(type: PermissionType) -> PermissionStatus {
        switch type {
        case .bluetooth:
            switch CBManager.authorization {
            case .allowedAlways:
                return .authorized
            case .denied:
                return .denied
            case .restricted:
                return .restricted
            case .notDetermined:
                return .notDetermined
            @unknown default:
                return .notDetermined
            }
            
        case .camera:
            switch AVCaptureDevice.authorizationStatus(for: .video) {
            case .authorized:
                return .authorized
            case .denied:
                return .denied
            case .restricted:
                return .restricted
            case .notDetermined:
                return .notDetermined
            @unknown default:
                return .notDetermined
            }
            
        case .photoLibrary:
            switch PHPhotoLibrary.authorizationStatus(for: .readWrite) {
            case .authorized:
                return .authorized
            case .limited:
                return .limited
            case .denied:
                return .denied
            case .restricted:
                return .restricted
            case .notDetermined:
                return .notDetermined
            @unknown default:
                return .notDetermined
            }
        }
    }
    
    // MARK: - Request Permission
    
    public func requestPermission(type: PermissionType, completion: @escaping (Bool) -> Void) {
        switch type {
        case .bluetooth:
            requestBluetoothPermission(completion: completion)
            
        case .camera:
            requestCameraPermission(completion: completion)
            
        case .photoLibrary:
            requestPhotoLibraryPermission(completion: completion)
        }
    }
    
    // MARK: - Open App Settings
    
    public func openAppSettings() {
        #if os(iOS) || os(tvOS) || os(visionOS)
        DispatchQueue.main.async {
            guard let settingsURL = URL(string: UIApplication.openSettingsURLString),
                  UIApplication.shared.canOpenURL(settingsURL) else {
                return
            }
            UIApplication.shared.open(settingsURL, options: [:], completionHandler: nil)
        }
        #elseif os(macOS)
        DispatchQueue.main.async {
            if let url = URL(string: "x-apple.systempreferences:com.apple.preference.security?Privacy") {
                NSWorkspace.shared.open(url)
            }
        }
        #endif
    }
    
    // MARK: - Private Helpers
    
    private func requestCameraPermission(completion: @escaping (Bool) -> Void) {
        let status = AVCaptureDevice.authorizationStatus(for: .video)
        switch status {
        case .authorized:
            DispatchQueue.main.async {
                completion(true)
            }
        case .denied, .restricted:
            DispatchQueue.main.async {
                completion(false)
            }
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { granted in
                DispatchQueue.main.async {
                    completion(granted)
                }
            }
        @unknown default:
            DispatchQueue.main.async {
                completion(false)
            }
        }
    }
    
    private func requestPhotoLibraryPermission(completion: @escaping (Bool) -> Void) {
        let status = PHPhotoLibrary.authorizationStatus(for: .readWrite)
        switch status {
        case .authorized, .limited:
            DispatchQueue.main.async {
                completion(true)
            }
        case .denied, .restricted:
            DispatchQueue.main.async {
                completion(false)
            }
        case .notDetermined:
            PHPhotoLibrary.requestAuthorization(for: .readWrite) { newStatus in
                DispatchQueue.main.async {
                    completion(newStatus == .authorized || newStatus == .limited)
                }
            }
        @unknown default:
            DispatchQueue.main.async {
                completion(false)
            }
        }
    }
    
    private func requestBluetoothPermission(completion: @escaping (Bool) -> Void) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            
            let status = self.checkPermission(type: .bluetooth)
            switch status {
            case .authorized, .limited:
                completion(true)
            case .denied, .restricted:
                completion(false)
            case .notDetermined:
                self.bluetoothCallbacks.append(completion)
                if self.centralManager == nil {
                    self.centralManager = CBCentralManager(
                        delegate: self,
                        queue: .main,
                        options: [CBCentralManagerOptionShowPowerAlertKey: false]
                    )
                }
            }
        }
    }
    
    // MARK: - CBCentralManagerDelegate
    
    public func centralManagerDidUpdateState(_ central: CBCentralManager) {
        let auth = CBManager.authorization
        
        // Wait for system prompt resolution if state is still initializing
        if auth == .notDetermined && central.state == .unknown {
            return
        }
        
        let isGranted: Bool
        if auth == .allowedAlways || central.state == .poweredOn || central.state == .poweredOff {
            isGranted = true
        } else if auth == .denied || auth == .restricted || central.state == .unauthorized || central.state == .unsupported {
            isGranted = false
        } else {
            return
        }
        
        let pendingCallbacks = bluetoothCallbacks
        bluetoothCallbacks.removeAll()
        
        centralManager?.delegate = nil
        centralManager = nil
        
        for callback in pendingCallbacks {
            callback(isGranted)
        }
    }
}
