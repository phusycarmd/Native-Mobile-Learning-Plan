package com.phusy2001.nativedevicemonitordemo.nativecore.permission

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.util.UUID

class PermissionManagerImpl(
    private val context: Context
) : PermissionManager {
    override fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun requestPermission(
        activity: ComponentActivity,
        permission: String,
        onResult: (Boolean) -> Unit
    ) {
        if (checkPermission(permission)) {
            onResult(true)
            return
        }
        val key = "request_perm_${UUID.randomUUID()}"
        var launcher: ActivityResultLauncher<String>? = null

        launcher = activity.activityResultRegistry.register(
            key,
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            onResult(isGranted)
            launcher?.unregister()
        }
        launcher.launch(permission)
    }

    override fun checkMultiplePermissions(permissions: List<String>): Map<String, Boolean> {
        return permissions.associateWith { permission ->
            checkPermission(permission)
        }
    }

    override fun requestMultiplePermissions(
        activity: ComponentActivity,
        permissions: List<String>,
        onResult: (Map<String, Boolean>) -> Unit
    ) {
        val currentStatuses = checkMultiplePermissions(permissions)
        if (currentStatuses.values.all { it }) {
            onResult(currentStatuses)
            return
        }
        val key = "request_multi_perm_${UUID.randomUUID()}"
        var launcher: ActivityResultLauncher<Array<String>>? = null
        launcher = activity.activityResultRegistry.register(
            key,
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            onResult(result)
            launcher?.unregister()
        }
        launcher.launch(permissions.toTypedArray())
    }

    override fun isPermanentlyDenied(activity: ComponentActivity, permission: String): Boolean {
        return !checkPermission(permission) &&
                !activity.shouldShowRequestPermissionRationale(permission)
    }

    override fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).apply {
            // context là applicationContext nên cần NEW_TASK để mở Activity
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}