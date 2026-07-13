package com.alosir.task.util

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CrashLogger {

    private const val CRASH_LOG_DIR = "crash_logs"
    private const val LATEST_LOG_NAME = "latest_crash.log"

    fun init(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                saveCrashLog(context, throwable)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun saveCrashLog(context: Context, throwable: Throwable) {
        val dir = getLogDir(context)
        val latestFile = File(dir, LATEST_LOG_NAME)

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val log = buildString {
            appendLine("Crash Time: $timestamp")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("App Version: ${context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode}")
            appendLine("Thread: ${Thread.currentThread().name}")
            appendLine("Exception: ${throwable.javaClass.name}: ${throwable.message}")
            appendLine("Stack Trace:")
            append(throwable.stackTraceToString())
        }

        latestFile.writeText(log)
    }

    fun getLatestCrashLogFile(context: Context): File? {
        val file = File(getLogDir(context), LATEST_LOG_NAME)
        return if (file.exists()) file else null
    }

    fun getLatestCrashLogContent(context: Context): String {
        return getLatestCrashLogFile(context)?.readText() ?: "无崩溃日志"
    }

    fun clearCrashLog(context: Context) {
        File(getLogDir(context), LATEST_LOG_NAME).delete()
    }

    private fun getLogDir(context: Context): File {
        val externalDir = context.getExternalFilesDir(null)
        val baseDir = externalDir ?: context.filesDir
        return File(baseDir, CRASH_LOG_DIR).apply { mkdirs() }
    }
}
