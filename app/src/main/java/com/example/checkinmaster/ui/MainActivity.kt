package com.alosir.task.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.alosir.task.R
import com.alosir.task.data.entity.CheckinType
import com.alosir.task.databinding.ActivityMainBinding
import com.alosir.task.ui.bottomsheet.AddCheckinBottomSheet
import com.alosir.task.util.DataExportImport
import com.alosir.task.util.DownloadFileHelper
import com.alosir.task.util.ReminderScheduler
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var isSpeedDialOpen = false

    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            importFromFile(uri)
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        rescheduleReminders()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupSpeedDial()
        checkPermissions()
        handleNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("navigate_to_today", false) == true) {
            binding.bottomNav.selectedItemId = R.id.navigation_today
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)) {
                android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                    checkAlarmPermission()
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            checkAlarmPermission()
        }
    }

    private fun checkAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
        rescheduleReminders()
    }

    private fun rescheduleReminders() {
        lifecycleScope.launch {
            try {
                val database = com.alosir.task.data.CheckinDatabase.getDatabase(this@MainActivity)
                val items = database.checkinItemDao().getAll()
                ReminderScheduler.rescheduleAll(this@MainActivity, items)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_today, R.id.navigation_tasks -> {
                    binding.fabContainer.visibility = View.VISIBLE
                }
                else -> {
                    closeSpeedDial()
                    binding.fabContainer.visibility = View.GONE
                }
            }
        }
    }

    private fun setupSpeedDial() {
        binding.fabMain.setOnClickListener {
            toggleSpeedDial()
        }

        binding.scrimView.setOnClickListener {
            closeSpeedDial()
        }

        binding.btnAddApp.setOnClickListener {
            openAddBottomSheet(CheckinType.APP)
            closeSpeedDial()
        }

        binding.btnAddWebsite.setOnClickListener {
            openAddBottomSheet(CheckinType.WEBSITE)
            closeSpeedDial()
        }

        binding.btnAddOther.setOnClickListener {
            openAddBottomSheet(CheckinType.OTHER)
            closeSpeedDial()
        }
    }

    private fun toggleSpeedDial() {
        if (isSpeedDialOpen) {
            closeSpeedDial()
        } else {
            openSpeedDial()
        }
    }

    private fun openSpeedDial() {
        isSpeedDialOpen = true
        binding.fabMain.animate().rotation(45f).setDuration(200).start()

        binding.scrimView.apply {
            alpha = 0f
            visibility = View.VISIBLE
            animate().alpha(1f).setDuration(200).start()
        }

        binding.actionButtonsContainer.apply {
            visibility = View.VISIBLE
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            translationY = 40f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(200)
                .start()
        }

        animateChildButtonsIn()
    }

    private fun closeSpeedDial() {
        if (!isSpeedDialOpen) return
        isSpeedDialOpen = false
        binding.fabMain.animate().rotation(0f).setDuration(200).start()

        binding.scrimView.animate().alpha(0f).setDuration(200).withEndAction {
            binding.scrimView.visibility = View.GONE
        }.start()

        binding.actionButtonsContainer.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .translationY(40f)
            .setDuration(200)
            .withEndAction {
                binding.actionButtonsContainer.visibility = View.INVISIBLE
            }
            .start()
    }

    private fun animateChildButtonsIn() {
        val buttons = listOf(binding.btnAddApp, binding.btnAddWebsite, binding.btnAddOther)
        buttons.forEachIndexed { index, button ->
            button.alpha = 0f
            button.translationX = 40f
            button.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(180)
                .setStartDelay((index * 40).toLong())
                .start()
        }
    }

    private fun openAddBottomSheet(type: Int) {
        AddCheckinBottomSheet.newInstance(type)
            .show(supportFragmentManager, "AddCheckinBottomSheet_$type")
    }

    fun exportData() {
        lifecycleScope.launch {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                val fileName = "alosir_task_export_${dateFormat.format(Date())}.json"
                val json = DataExportImport.exportToJsonString(this@MainActivity)
                val result = DownloadFileHelper.saveTextToDownloads(this@MainActivity, fileName, json)

                if (result.success) {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.export_success, result.path ?: fileName),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.export_failed_format, result.error),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, R.string.export_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun importData() {
        importFileLauncher.launch("*/*")
    }

    private fun importFromFile(uri: Uri) {
        lifecycleScope.launch {
            try {
                val tempFile = File(cacheDir, "import_${System.currentTimeMillis()}.json")
                contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val result = DataExportImport.importFromJson(this@MainActivity, tempFile)
                val message = when {
                    !result.success -> result.errorMessage ?: getString(R.string.import_failed)
                    result.totalImported == 0 && result.totalSkipped == 0 -> getString(R.string.import_nothing)
                    result.totalImported == 0 -> getString(R.string.import_zero_imported, result.totalSkipped)
                    else -> getString(
                        R.string.import_success_detail,
                        result.importedItems,
                        result.importedRecords,
                        result.totalSkipped
                    )
                }

                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()

                if (result.success && result.totalImported > 0) {
                    rescheduleReminders()
                }

                tempFile.delete()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.import_failed_detail, e.message),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
