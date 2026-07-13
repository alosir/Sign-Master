package com.alosir.task.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class NotificationPermissionHelper(private val fragment: Fragment) {

    private var onPermissionResult: ((granted: Boolean, needsResumeCheck: Boolean) -> Unit)? = null

    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                onPermissionResult?.invoke(true, false)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (!fragment.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                        showSettingsDialog()
                    } else {
                        onPermissionResult?.invoke(false, false)
                    }
                } else {
                    onPermissionResult?.invoke(false, false)
                }
            }
        }

    fun requestNotificationPermission(onResult: (granted: Boolean, needsResumeCheck: Boolean) -> Unit) {
        onPermissionResult = onResult

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onResult(true, false)
            return
        }

        val context = fragment.requireContext()
        when (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)) {
            PackageManager.PERMISSION_GRANTED -> onResult(true, false)
            else -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(fragment.requireContext())
            .setTitle("需要通知权限")
            .setMessage("为了准时提醒您签到，请前往系统设置开启通知权限。")
            .setPositiveButton("去设置") { _, _ ->
                openNotificationSettings()
                onPermissionResult?.invoke(false, true)
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
                onPermissionResult?.invoke(false, false)
            }
            .setCancelable(false)
            .show()
    }

    fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, fragment.requireContext().packageName)
        }
        fragment.startActivity(intent)
    }

    fun checkNotificationPermission(): Boolean {
        val context = fragment.requireContext()
        val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val runtimeGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return notificationsEnabled && runtimeGranted
    }
}
