package com.alosir.task.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.OutputStream

object DownloadFileHelper {

    data class SaveResult(
        val success: Boolean,
        val path: String?,
        val error: String?
    )

    fun saveTextToDownloads(context: Context, fileName: String, content: String): SaveResult {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, fileName, content)
        } else {
            saveViaLegacyDirectory(context, fileName, content)
        }
    }

    private fun saveViaMediaStore(context: Context, fileName: String, content: String): SaveResult {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        var uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: return SaveResult(false, null, "无法创建 MediaStore 条目")

        return try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray(Charsets.UTF_8))
            }
            SaveResult(true, uri.toString(), null)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                resolver.delete(uri, null, null)
            } catch (_: Exception) {}
            SaveResult(false, null, e.message)
        }
    }

    private fun saveViaLegacyDirectory(@Suppress("UNUSED_PARAMETER") context: Context, fileName: String, content: String): SaveResult {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val file = File(downloadsDir, fileName)
            file.writeText(content, Charsets.UTF_8)
            SaveResult(true, file.absolutePath, null)
        } catch (e: Exception) {
            e.printStackTrace()
            SaveResult(false, null, e.message)
        }
    }
}
