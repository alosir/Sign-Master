package com.alosir.task.util

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 检查 GitHub Releases 最新版本。
 */
object UpdateChecker {

    private const val GITHUB_RELEASES_API =
        "https://api.github.com/repos/alosir/Sign-Master/releases/latest"

    data class ReleaseInfo(
        @SerializedName("tag_name") val tagName: String,
        @SerializedName("body") val body: String,
        @SerializedName("assets") val assets: List<AssetInfo>
    )

    data class AssetInfo(
        @SerializedName("name") val name: String,
        @SerializedName("browser_download_url") val downloadUrl: String
    )

    suspend fun checkLatestRelease(): ReleaseInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = URL(GITHUB_RELEASES_API).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "SignMaster-Android")

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext null
            }

            val json = connection.inputStream.bufferedReader().use { it.readText() }
            Gson().fromJson(json, ReleaseInfo::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * 比较版本号。
     *
     * @param current 当前版本，如 "1.0.2"
     * @param latest 最新版本，如 "1.1.0"
     * @return true 表示 latest 比 current 新
     */
    fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = current.trimStart('v', 'V').split(".")
        val latestParts = latest.trimStart('v', 'V').split(".")

        val maxLength = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxLength) {
            val currentPart = currentParts.getOrNull(i)?.toIntOrNull() ?: 0
            val latestPart = latestParts.getOrNull(i)?.toIntOrNull() ?: 0

            when {
                latestPart > currentPart -> return true
                latestPart < currentPart -> return false
            }
        }
        return false
    }
}
