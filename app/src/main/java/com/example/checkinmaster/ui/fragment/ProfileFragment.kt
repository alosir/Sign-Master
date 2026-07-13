package com.alosir.task.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.alosir.task.R
import com.alosir.task.databinding.FragmentProfileBinding
import com.alosir.task.ui.MainActivity
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

        binding.btnExport.setOnClickListener {
            (requireActivity() as? MainActivity)?.exportData()
        }

        binding.btnImport.setOnClickListener {
            (requireActivity() as? MainActivity)?.importData()
        }

        binding.btnNotificationPermission.setOnClickListener {
            handleNotificationPermissionClick()
        }
    }

    override fun onResume() {
        super.onResume()
        updateNotificationStatus()
    }

    private fun updateNotificationStatus() {
        val granted = notificationPermissionHelper.checkNotificationPermission()
        binding.notificationStatusText.text = getString(
            if (granted) R.string.notification_status_enabled else R.string.notification_status_disabled
        )
    }

    private fun handleNotificationPermissionClick() {
        notificationPermissionHelper.openNotificationSettings()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
