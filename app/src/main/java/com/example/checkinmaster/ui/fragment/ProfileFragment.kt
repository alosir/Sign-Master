package com.alosir.task.ui.fragment

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.alosir.task.BuildConfig
import com.alosir.task.R
import com.alosir.task.databinding.FragmentProfileBinding
import com.alosir.task.service.ForegroundKeepAliveService
import com.alosir.task.ui.MainActivity
import com.alosir.task.ui.VersionUpdateActivity
import com.alosir.task.util.NotificationPermissionHelper

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var notificationPermissionHelper: NotificationPermissionHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        notificationPermissionHelper = NotificationPermissionHelper(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.versionText.text = BuildConfig.VERSION_NAME

        binding.btnVersion.setOnClickListener {
            startActivity(Intent(requireContext(), VersionUpdateActivity::class.java))
        }

        binding.btnExport.setOnClickListener {
            (requireActivity() as? MainActivity)?.exportData()
        }

        binding.btnImport.setOnClickListener {
            (requireActivity() as? MainActivity)?.importData()
        }

        binding.btnNotificationPermission.setOnClickListener {
            handleNotificationPermissionClick()
        }

        binding.btnAutoStart.setOnClickListener {
            openAutoStartSettings()
        }

        binding.btnBatteryOptimization.setOnClickListener {
            requestIgnoreBatteryOptimizations()
        }

        binding.switchKeepAlive.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startKeepAliveService()
            } else {
                stopKeepAliveService()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateNotificationStatus()
        updateBatteryOptimizationStatus()
    }

    private fun updateNotificationStatus() {
        val granted = notificationPermissionHelper.checkNotificationPermission()
        binding.notificationStatusText.text = getString(
            if (granted) R.string.notification_status_enabled else R.string.notification_status_disabled
        )
    }

    private fun updateBatteryOptimizationStatus() {
        val pm = requireContext().getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isIgnoring = pm?.isIgnoringBatteryOptimizations(requireContext().packageName) == true
        binding.btnBatteryOptimization.text = getString(
            if (isIgnoring) R.string.background_battery_hint else R.string.background_battery
        )
    }

    private fun handleNotificationPermissionClick() {
        notificationPermissionHelper.openNotificationSettings()
    }

    private fun openAutoStartSettings() {
        val packageName = requireContext().packageName
        val intents = listOf(
            // 通用应用详情页
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            },
            // 小米
            Intent("miui.intent.action.APP_AUTO_START").apply {
                addCategory(Intent.CATEGORY_DEFAULT)
            },
            // 华为
            Intent().apply {
                component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
            },
            // OPPO
            Intent().apply {
                component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")
            },
            // vivo
            Intent().apply {
                component = ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
            }
        )

        for (intent in intents) {
            try {
                startActivity(intent)
                return
            } catch (e: Exception) {
                // 尝试下一个
            }
        }

        Toast.makeText(requireContext(), R.string.background_autostart_hint, Toast.LENGTH_SHORT).show()
    }

    private fun requestIgnoreBatteryOptimizations() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${requireContext().packageName}")
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(intent)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    private fun startKeepAliveService() {
        val intent = Intent(requireContext(), ForegroundKeepAliveService::class.java).apply {
            action = ForegroundKeepAliveService.ACTION_START
        }
        ContextCompat.startForegroundService(requireContext(), intent)
    }

    private fun stopKeepAliveService() {
        val intent = Intent(requireContext(), ForegroundKeepAliveService::class.java).apply {
            action = ForegroundKeepAliveService.ACTION_STOP
        }
        requireContext().startService(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
