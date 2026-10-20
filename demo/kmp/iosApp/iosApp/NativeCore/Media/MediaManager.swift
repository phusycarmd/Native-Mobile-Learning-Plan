//
//  MediaManager.swift
//  NativeDeviceMonitorDemo
//

import Foundation
import UIKit
import PhotosUI
import SwiftUI

// MARK: - Data Models

public struct MediaInfo {
    public let image: UIImage
    public let width: CGFloat
    public let height: CGFloat
    public let sizeBytes: Int

    public init(image: UIImage, width: CGFloat, height: CGFloat, sizeBytes: Int) {
        self.image = image
        self.width = width
        self.height = height
        self.sizeBytes = sizeBytes
    }
}

// MARK: - Protocol

public protocol MediaManagerProtocol {
    /// Mở camera hệ thống để chụp ảnh.
    /// Phải gọi từ một UIViewController.
    func launchCamera(
        from viewController: UIViewController,
        onResult: @escaping (UIImage?) -> Void
    )

    /// Mở PHPickerViewController để chọn ảnh từ thư viện Photos.
    func launchPhotoPicker(
        from viewController: UIViewController,
        onResult: @escaping (UIImage?) -> Void
    )

    /// Trích xuất thông tin ảnh (width, height, sizeBytes)
    func getMediaInfo(image: UIImage) -> MediaInfo

    // Chuẩn docs/guide/architecture.md & docs/modules/media.md
    func requestCameraPermission(completion: @escaping (Bool) -> Void)
    func capturePhoto(from viewController: UIViewController, completion: @escaping (Result<UIImage, Error>) -> Void)
    func pickMedia(from viewController: UIViewController, completion: @escaping (Result<UIImage, Error>) -> Void)
}

// MARK: - Implementation

public final class MediaManager: NSObject, MediaManagerProtocol {

    // Giữ callback trong khi delegate đang chờ
    private var cameraCallback: ((UIImage?) -> Void)?
    private var pickerCallback: ((UIImage?) -> Void)?

    public override init() {
        super.init()
    }

    // MARK: - Camera

    public func launchCamera(
        from viewController: UIViewController,
        onResult: @escaping (UIImage?) -> Void
    ) {
        guard UIImagePickerController.isSourceTypeAvailable(.camera) else {
            print("[MediaManager] Camera không khả dụng trên thiết bị này (Simulator?).")
            onResult(nil)
            return
        }

        cameraCallback = onResult

        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.allowsEditing = false
        picker.delegate = self
        viewController.present(picker, animated: true)
    }

    // MARK: - Photo Picker

    public func launchPhotoPicker(
        from viewController: UIViewController,
        onResult: @escaping (UIImage?) -> Void
    ) {
        pickerCallback = onResult

        var config = PHPickerConfiguration(photoLibrary: .shared())
        config.filter = .images          // chỉ hiển thị ảnh tĩnh
        config.selectionLimit = 1        // chọn 1 ảnh

        let picker = PHPickerViewController(configuration: config)
        picker.delegate = self
        viewController.present(picker, animated: true)
    }

    // MARK: - Media Info

    public func getMediaInfo(image: UIImage) -> MediaInfo {
        let sizeBytes = image.jpegData(compressionQuality: 0.9)?.count ?? 0
        return MediaInfo(
            image: image,
            width: image.size.width * image.scale,
            height: image.size.height * image.scale,
            sizeBytes: sizeBytes
        )
    }
}

// MARK: - UIImagePickerControllerDelegate (Camera)

extension MediaManager: UIImagePickerControllerDelegate, UINavigationControllerDelegate {

    public func imagePickerController(
        _ picker: UIImagePickerController,
        didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
    ) {
        let image = info[.originalImage] as? UIImage
        picker.dismiss(animated: true)
        cameraCallback?(image)
        cameraCallback = nil
    }

    public func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        picker.dismiss(animated: true)
        cameraCallback?(nil)
        cameraCallback = nil
    }
}

// MARK: - PHPickerViewControllerDelegate (Photo Picker)

extension MediaManager: PHPickerViewControllerDelegate {

    public func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
        picker.dismiss(animated: true)

        guard let result = results.first else {
            pickerCallback?(nil)
            pickerCallback = nil
            return
        }

        // Chỉ load khi item provider hỗ trợ UIImage
        guard result.itemProvider.canLoadObject(ofClass: UIImage.self) else {
            print("[MediaManager] Item không thể load thành UIImage.")
            pickerCallback?(nil)
            pickerCallback = nil
            return
        }

        result.itemProvider.loadObject(ofClass: UIImage.self) { [weak self] object, error in
            DispatchQueue.main.async {
                if let error {
                    print("[MediaManager] Lỗi load ảnh: \(error.localizedDescription)")
                    self?.pickerCallback?(nil)
                } else {
                    self?.pickerCallback?(object as? UIImage)
                }
                self?.pickerCallback = nil
            }
        }
    }

    // MARK: - Architecture Protocol Methods

    public func requestCameraPermission(completion: @escaping (Bool) -> Void) {
        AVCaptureDevice.requestAccess(for: .video) { granted in
            DispatchQueue.main.async {
                completion(granted)
            }
        }
    }

    public func capturePhoto(from viewController: UIViewController, completion: @escaping (Result<UIImage, Error>) -> Void) {
        launchCamera(from: viewController) { image in
            if let image = image {
                completion(.success(image))
            } else {
                let err = NSError(domain: "MediaManager", code: -1, userInfo: [NSLocalizedDescriptionKey: "User cancelled camera capture"])
                completion(.failure(err))
            }
        }
    }

    public func pickMedia(from viewController: UIViewController, completion: @escaping (Result<UIImage, Error>) -> Void) {
        launchPhotoPicker(from: viewController) { image in
            if let image = image {
                completion(.success(image))
            } else {
                let err = NSError(domain: "MediaManager", code: -1, userInfo: [NSLocalizedDescriptionKey: "User cancelled photo picking"])
                completion(.failure(err))
            }
        }
    }
}
