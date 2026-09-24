package com.phusy2001.nativedevicemonitordemo.nativecore.permission

import androidx.activity.ComponentActivity

interface PermissionManager {
    fun checkPermission(permission: String): Boolean
    fun requestPermission(activity: ComponentActivity, permission: String, onResult: (Boolean) -> Unit)
    fun checkMultiplePermissions(permissions: List<String>): Map<String, Boolean>
    fun requestMultiplePermissions(
        activity: ComponentActivity,
        permissions: List<String>,
        onResult: (Map<String, Boolean>) -> Unit
    )

    /**
     * Quyền đã bị khoá ("Don't ask again"): hệ thống sẽ không hiện hộp thoại xin quyền nữa.
     * Chỉ chính xác khi gọi SAU khi đã request, vì trước lần hỏi đầu tiên hàm rationale cũng trả về false.
     */
    fun isPermanentlyDenied(activity: ComponentActivity, permission: String): Boolean

    fun openAppSettings()
}