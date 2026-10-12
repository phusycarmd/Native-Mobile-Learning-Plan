package com.phusy2001.nativedevicemonitordemo.nativecore.media

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import java.util.UUID

// MARK: - Data Models

data class MediaInfo(
    val uri: Uri,
    val width: Int,
    val height: Int,
    val sizeBytes: Long
)

// MARK: - Interface

interface MediaManager {
    /**
     * Mở camera hệ thống để chụp ảnh.
     * [onResult] trả về Uri của ảnh đã chụp, hoặc null nếu người dùng huỷ.
     */
    fun launchCamera(
        activity: ComponentActivity,
        onResult: (Uri?) -> Unit
    )

    /**
     * Mở Photo Picker để chọn ảnh từ thư viện.
     * [onResult] trả về Uri của ảnh đã chọn, hoặc null nếu người dùng huỷ.
     */
    fun launchPhotoPicker(
        activity: ComponentActivity,
        onResult: (Uri?) -> Unit
    )

    /**
     * Trích xuất thông tin ảnh (width, height, sizeBytes).
     */
    fun getMediaInfo(uri: Uri): MediaInfo?
}

// MARK: - Implementation

class MediaManagerImpl(
    private val context: Context
) : MediaManager {

    // Lưu Uri của ảnh camera tạm thời để truyền vào TakePicture contract
    private var pendingCameraUri: Uri? = null
    private var cameraLauncher: ActivityResultLauncher<Uri>? = null
    private var pickerLauncher: ActivityResultLauncher<String>? = null

    override fun launchCamera(activity: ComponentActivity, onResult: (Uri?) -> Unit) {
        val photoUri = createCameraImageUri() ?: run {
            onResult(null)
            return
        }
        pendingCameraUri = photoUri

        val key = "camera_${UUID.randomUUID()}"
        var launcher: ActivityResultLauncher<Uri>? = null

        launcher = activity.activityResultRegistry.register(
            key,
            ActivityResultContracts.TakePicture()
        ) { success ->
            launcher?.unregister()
            onResult(if (success) pendingCameraUri else null)
            pendingCameraUri = null
        }
        cameraLauncher = launcher
        launcher.launch(photoUri)
    }

    override fun launchPhotoPicker(activity: ComponentActivity, onResult: (Uri?) -> Unit) {
        val key = "picker_${UUID.randomUUID()}"
        var launcher: ActivityResultLauncher<String>? = null

        launcher = activity.activityResultRegistry.register(
            key,
            ActivityResultContracts.GetContent()
        ) { uri ->
            launcher?.unregister()
            onResult(uri)
        }
        pickerLauncher = launcher
        // Chỉ cho phép chọn ảnh (image/*)
        launcher.launch("image/*")
    }

    override fun getMediaInfo(uri: Uri): MediaInfo? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            var sizeBytes = 0L
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }
            MediaInfo(
                uri = uri,
                width = options.outWidth,
                height = options.outHeight,
                sizeBytes = sizeBytes
            )
        } catch (e: Exception) {
            null
        }
    }

    // MARK: - Private

    /**
     * Tạo Uri MediaStore để camera lưu ảnh vào thư viện hệ thống.
     * Cách này không cần quyền WRITE_EXTERNAL_STORAGE trên Android 10+.
     */
    private fun createCameraImageUri(): Uri? {
        val fileName = "PHOTO_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        return context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
    }
}
