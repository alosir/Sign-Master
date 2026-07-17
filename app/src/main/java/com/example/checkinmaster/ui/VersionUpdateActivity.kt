package com.alosir.task.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.alosir.task.BuildConfig
import com.alosir.task.R
import com.alosir.task.databinding.ActivityVersionUpdateBinding
import com.alosir.task.util.UpdateChecker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.File

/**
 * 版本更新页面：展示面向用户的更新日志，并支持从 GitHub 检查/下载新版 APK。
 */
class VersionUpdateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVersionUpdateBinding
    private var downloadReceiver: BroadcastReceiver? = null
    private var pendingDownloadId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVersionUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        renderChangelog()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_version_update, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_check_update -> {
                checkUpdate()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun renderChangelog() {
        val container = binding.changelogContainer
        container.removeAllViews()

        val inflater = LayoutInflater.from(this)

        ChangelogData.versions.forEach { version ->
            val cardView = inflater.inflate(R.layout.item_changelog_version, container, false)

            cardView.findViewById<TextView>(R.id.versionNameText).text = version.name
            cardView.findViewById<TextView>(R.id.versionDateText).text = version.date

            val bulletContainer = cardView.findViewById<LinearLayout>(R.id.bulletContainer)
            bulletContainer.removeAllViews()

            version.items.forEach { item ->
                val bullet = inflater.inflate(R.layout.item_changelog_bullet, bulletContainer, false)
                bullet.findViewById<TextView>(R.id.bulletText).text = item
                bulletContainer.addView(bullet)
            }

            container.addView(cardView)
        }
    }

    private fun checkUpdate() {
        Toast.makeText(this, R.string.checking_update, Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val release = UpdateChecker.checkLatestRelease()
                val apkAsset = release?.assets?.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }

                // 没有 Release 或 Release 中没有 APK 时，视为已是最新
                if (release == null || apkAsset == null) {
                    Toast.makeText(this@VersionUpdateActivity, R.string.already_latest, Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val latestVersion = release.tagName.trimStart('v', 'V')
                val currentVersion = BuildConfig.VERSION_NAME

                if (UpdateChecker.isNewerVersion(currentVersion, latestVersion)) {
                    showUpdateDialog(release)
                } else {
                    Toast.makeText(this@VersionUpdateActivity, R.string.already_latest, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@VersionUpdateActivity, R.string.check_update_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showUpdateDialog(release: UpdateChecker.ReleaseInfo) {
        val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
        if (apkAsset == null) {
            Toast.makeText(this, R.string.check_update_failed, Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.update_available, release.tagName))
            .setMessage(release.body.takeIf { it.isNotBlank() } ?: getString(R.string.update_available, release.tagName))
            .setPositiveButton("下载更新") { _, _ ->
                startApkDownload(apkAsset.downloadUrl, apkAsset.name)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun startApkDownload(url: String, fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                Toast.makeText(this, "请允许安装未知来源应用", Toast.LENGTH_LONG).show()
                return
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_WRITE_STORAGE
                )
                return
            }
        }

        val request = android.app.DownloadManager.Request(Uri.parse(url))
            .setTitle("签到大师更新")
            .setDescription("正在下载 $fileName")
            .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        pendingDownloadId = dm.enqueue(request)

        Toast.makeText(this, R.string.download_started, Toast.LENGTH_SHORT).show()

        registerDownloadReceiver()
    }

    private fun registerDownloadReceiver() {
        if (downloadReceiver != null) return

        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id != pendingDownloadId) return

                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                val query = android.app.DownloadManager.Query().setFilterById(id)
                val cursor = dm.query(query)

                if (cursor != null && cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_STATUS)
                    val status = cursor.getInt(statusIndex)

                    if (status == android.app.DownloadManager.STATUS_SUCCESSFUL) {
                        val uriIndex = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_LOCAL_URI)
                        val localUri = cursor.getString(uriIndex)
                        openApkInstall(localUri)
                    } else {
                        Toast.makeText(context, R.string.download_failed, Toast.LENGTH_SHORT).show()
                    }
                }
                cursor?.close()

                unregisterDownloadReceiver()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver, IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(downloadReceiver, IntentFilter(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun unregisterDownloadReceiver() {
        try {
            downloadReceiver?.let { unregisterReceiver(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        downloadReceiver = null
    }

    private fun openApkInstall(localUri: String?) {
        if (localUri.isNullOrBlank()) return

        val file = File(Uri.parse(localUri).path ?: return)
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "无法打开安装界面", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_STORAGE && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "请重新点击检查更新", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterDownloadReceiver()
    }

    companion object {
        private const val REQUEST_WRITE_STORAGE = 1001
    }
}
