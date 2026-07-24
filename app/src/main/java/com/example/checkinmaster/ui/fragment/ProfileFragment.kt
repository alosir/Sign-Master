package com.alosir.task.ui.fragment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.alosir.task.BuildConfig
import com.alosir.task.R
import com.alosir.task.databinding.FragmentProfileBinding
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

        binding.btnBatteryOptimization.setOnClickListener {
            requestIgnoreBatteryOptimizations()
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
                Toast.makeText(requireContext(), "无法打开电池优化设置，请手动前往系统设置", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
