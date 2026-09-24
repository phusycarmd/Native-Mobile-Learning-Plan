//
//  NativeDeviceMonitorDemoTests.swift
//  NativeDeviceMonitorDemoTests
//
//  Created by synguyen on 15/9/26.
//

import Testing
@testable import NativeDeviceMonitorDemo

struct NativeDeviceMonitorDemoTests {

    @Test func permissionStatusIsGranted() {
        #expect(PermissionStatus.authorized.isGranted == true)
        #expect(PermissionStatus.limited.isGranted == true)
        #expect(PermissionStatus.denied.isGranted == false)
        #expect(PermissionStatus.restricted.isGranted == false)
        #expect(PermissionStatus.notDetermined.isGranted == false)
    }

    @Test func checkAllPermissionTypes() {
        let manager = PermissionManager.shared
        for type in PermissionType.allCases {
            let status = manager.checkPermission(type: type)
            // Ensure status returns one of the defined enum cases
            #expect([PermissionStatus.authorized, .denied, .restricted, .notDetermined, .limited].contains(status))
        }
    }

    @Test func checkMultiplePermissionsReturnsAllTypes() {
        let manager = PermissionManager.shared
        let types: [PermissionType] = [.bluetooth, .camera, .photoLibrary]
        let results = manager.checkMultiplePermissions(types: types)
        
        #expect(results.count == 3)
        #expect(results[.bluetooth] != nil)
        #expect(results[.camera] != nil)
        #expect(results[.photoLibrary] != nil)
    }

    @Test func customMockPermissionManagerProtocol() async {
        class MockPermissionManager: PermissionManagerProtocol {
            var permissionResults: [PermissionType: PermissionStatus] = [
                .bluetooth: .authorized,
                .camera: .denied,
                .photoLibrary: .limited
            ]
            var openSettingsCalled = false

            func checkPermission(type: PermissionType) -> PermissionStatus {
                return permissionResults[type] ?? .notDetermined
            }

            func requestPermission(type: PermissionType, completion: @escaping (Bool) -> Void) {
                let status = checkPermission(type: type)
                completion(status.isGranted)
            }

            func openAppSettings() {
                openSettingsCalled = true
            }
        }

        let mock = MockPermissionManager()
        #expect(mock.checkPermission(type: .bluetooth) == .authorized)
        #expect(mock.checkPermission(type: .camera) == .denied)
        #expect(mock.checkPermission(type: .photoLibrary) == .limited)

        let bluetoothGranted = await mock.requestPermission(type: .bluetooth)
        #expect(bluetoothGranted == true)

        let cameraGranted = await mock.requestPermission(type: .camera)
        #expect(cameraGranted == false)

        let multiResults = await mock.requestMultiplePermissions(types: [.bluetooth, .camera])
        #expect(multiResults[.bluetooth] == true)
        #expect(multiResults[.camera] == false)

        mock.openAppSettings()
        #expect(mock.openSettingsCalled == true)
    }
}
