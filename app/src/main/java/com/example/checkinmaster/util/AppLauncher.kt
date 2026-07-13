package com.alosir.task.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat.startActivity

object AppLauncher {
    
    fun launchApp(context: Context, packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    fun openWebsite(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // 处理错误
        }
    }
    
    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun getInstalledApps(context: Context): List<AppEntry> {
        return try {
            val pm = context.packageManager
            val myPackage = context.packageName

            val entries = mutableListOf<AppEntry>()
            for (app in pm.getInstalledApplications(0)) {
                if (app.packageName == myPackage) continue
                if (pm.getLaunchIntentForPackage(app.packageName) == null) continue
                entries.add(AppEntry(app, app.loadLabel(pm).toString()))
            }
            entries.sortBy { it.label }
            entries
        } catch (e: Exception) {
            emptyList()
        }
    }

    data class AppEntry(
        val applicationInfo: android.content.pm.ApplicationInfo,
        val label: String
    )
}
