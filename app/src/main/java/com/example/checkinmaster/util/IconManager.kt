package com.alosir.task.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

object IconManager {
    
    private const val ICONS_DIR = "icons"
    
    /**
     * 动态加载应用图标（从 PackageManager，无需文件存储）
     * 返回 null 表示应用未安装或无法获取图标
     */
    fun loadAppIcon(context: Context, packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }
    
    @Deprecated("APP 类型图标已改为动态加载，不再保存为文件", ReplaceWith(""))
    fun saveAppIcon(context: Context, packageName: String, drawable: Drawable): String {
        val iconsDir = File(context.filesDir, ICONS_DIR)
        if (!iconsDir.exists()) {
            iconsDir.mkdirs()
        }
        
        val fileName = "app_${packageName.replace(".", "_")}.png"
        val file = File(iconsDir, fileName)
        
        val bitmap = drawable.toBitmap()
        file.outputStream().use { os ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
        }
        
        return file.absolutePath
    }
    
    suspend fun saveWebsiteIcon(context: Context, url: String): String {
        return withContext(Dispatchers.IO) {
            val domain = extractDomain(url)
            val fileName = "web_${domain.replace(".", "_")}.png"
            val file = File(context.filesDir, ICONS_DIR)
            if (!file.exists()) {
                file.mkdirs()
            }
            val iconFile = File(file, fileName)
            
            try {
                val faviconUrls = listOf(
                    "https://$domain/favicon.ico",
                    "https://www.$domain/favicon.ico",
                    "http://$domain/favicon.ico",
                    "http://www.$domain/favicon.ico"
                )
                
                var bitmap: Bitmap? = null
                for (faviconUrl in faviconUrls) {
                    bitmap = downloadBitmap(faviconUrl)
                    if (bitmap != null && bitmap.width > 0 && bitmap.height > 0) {
                        break
                    }
                }
                
                if (bitmap != null && bitmap.width > 0 && bitmap.height > 0) {
                    iconFile.outputStream().use { os ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                    }
                    iconFile.absolutePath
                } else {
                    saveDefaultIcon(context, fileName)
                }
            } catch (e: Exception) {
                saveDefaultIcon(context, fileName)
            }
        }
    }
    
    private suspend fun downloadBitmap(urlString: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val connection = url.openConnection()
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.connect()
                val inputStream = connection.getInputStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                bitmap
            } catch (e: Exception) {
                null
            }
        }
    }
    
    private fun extractDomain(url: String): String {
        val cleanUrl = url.removePrefix("http://").removePrefix("https://").removePrefix("www.")
        return cleanUrl.split("/").firstOrNull() ?: "unknown"
    }
    
    fun saveBitmapToFile(context: Context, bitmap: Bitmap, fileName: String): String {
        val iconsDir = File(context.filesDir, ICONS_DIR)
        if (!iconsDir.exists()) {
            iconsDir.mkdirs()
        }
        
        val file = File(iconsDir, fileName)
        file.outputStream().use { os ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
        }
        
        return file.absolutePath
    }
    
    fun saveDefaultIcon(context: Context, fileName: String): String {
        return saveBitmapToFile(context, createDefaultIcon(), fileName)
    }
    
    private fun createDefaultIcon(): Bitmap {
        val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = android.graphics.Color.parseColor("#5dade2")
            isAntiAlias = true
        }
        canvas.drawRoundRect(android.graphics.RectF(0f, 0f, 96f, 96f), 16f, 16f, paint)
        return bitmap
    }
}