package com.alosir.task.ui.bottomsheet

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alosir.task.R
import com.alosir.task.data.CheckinDatabase
import com.alosir.task.data.entity.CheckinCycleType
import com.alosir.task.data.entity.CheckinItem
import com.alosir.task.data.entity.CheckinType
import com.alosir.task.data.repository.CheckinItemRepository
import com.alosir.task.databinding.BottomSheetAddCheckinAppBinding
import com.alosir.task.databinding.BottomSheetAddCheckinAppDetailBinding
import com.alosir.task.databinding.BottomSheetAddCheckinBinding
import com.alosir.task.databinding.BottomSheetAddCheckinOtherBinding
import com.alosir.task.databinding.BottomSheetAddCheckinWebsiteBinding
import com.alosir.task.ui.adapter.AppListAdapter
import com.alosir.task.ui.view.CyclePickerView
import com.alosir.task.ui.view.TimePickerView
import com.alosir.task.ui.viewmodel.CheckinListViewModel
import com.alosir.task.util.AppLauncher
import com.alosir.task.util.IconManager
import com.alosir.task.util.NotificationPermissionHelper
import com.alosir.task.util.ReminderScheduler
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddCheckinBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_TYPE = "type"

        fun newInstance(type: Int): AddCheckinBottomSheet {
            return AddCheckinBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TYPE, type)
                }
            }
        }
    }

    private var _binding: BottomSheetAddCheckinBinding? = null
    private val binding get() = _binding!!

    private var checkinType: Int = CheckinType.APP
    private lateinit var viewModel: CheckinListViewModel
    private lateinit var repository: CheckinItemRepository

    // APP selection state
    private var selectedPackageName: String? = null
    private var selectedAppName: String? = null
    private var selectedAppIcon: android.graphics.drawable.Drawable? = null

    // Content bindings
    private var appBinding: BottomSheetAddCheckinAppBinding? = null
    private var appDetailBinding: BottomSheetAddCheckinAppDetailBinding? = null
    private var websiteBinding: BottomSheetAddCheckinWebsiteBinding? = null
    private var otherBinding: BottomSheetAddCheckinOtherBinding? = null

    private var appListAdapter: AppListAdapter? = null
    private var notificationPermissionHelper: NotificationPermissionHelper? = null
    private var pendingReminderPermissionCheck = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            refreshAppSelection()
        } else {
            showPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkinType = arguments?.getInt(ARG_TYPE) ?: CheckinType.APP
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddCheckinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        val database = CheckinDatabase.getDatabase(context)
        repository = CheckinItemRepository(database.checkinItemDao(), database.checkinRecordDao())
        viewModel = ViewModelProvider(requireActivity())[CheckinListViewModel::class.java]

        setupTitle()
        setupContent()
        setupButtons()
        expandBottomSheet()
        notificationPermissionHelper = NotificationPermissionHelper(this)
    }

    override fun onResume() {
        super.onResume()
        if (pendingReminderPermissionCheck) {
            pendingReminderPermissionCheck = false
            notificationPermissionHelper?.requestNotificationPermission { granted, needsResumeCheck ->
                if (granted || !needsResumeCheck) {
                    dismissAllowingStateLoss()
                } else {
                    pendingReminderPermissionCheck = true
                }
            }
        }
    }

    private fun expandBottomSheet() {
        val bottomSheetDialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
            behavior.isDraggable = false
        }
        setupDragHandle()
    }

    private fun setupDragHandle() {
        var startY = 0f
        var isDragging = false
        binding.dragHandle.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dy = event.rawY - startY
                    if (dy > 24) isDragging = true
                    if (isDragging && dy > 120) {
                        dismiss()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        val dy = event.rawY - startY
                        if (dy > 120) dismiss()
                    }
                    isDragging = false
                    true
                }
                else -> false
            }
        }
    }

    private fun setupTitle() {
        val titleRes = when (checkinType) {
            CheckinType.WEBSITE -> R.string.add_website
            CheckinType.OTHER -> R.string.add_other
            else -> R.string.add_app
        }
        binding.titleText.setText(titleRes)
    }

    private fun setupContent() {
        val inflater = LayoutInflater.from(requireContext())
        binding.contentContainer.removeAllViews()

        when (checkinType) {
            CheckinType.APP -> checkAndShowAppSelection(inflater)
            CheckinType.WEBSITE -> showWebsiteForm(inflater)
            CheckinType.OTHER -> showOtherForm(inflater)
        }
    }

    private fun checkAndShowAppSelection(inflater: LayoutInflater) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || hasQueryAllPackagesPermission()) {
            showAppSelection(inflater)
        } else {
            showPermissionRequest()
        }
    }

    private fun hasQueryAllPackagesPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.QUERY_ALL_PACKAGES
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun showPermissionRequest() {
        binding.contentContainer.removeAllViews()
        val permissionView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 48)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }

        val message = android.widget.TextView(requireContext()).apply {
            text = getString(R.string.permission_query_apps_required)
            textSize = 14f
            setTextColor(requireContext().getColor(R.color.md_on_surface))
            gravity = android.view.Gravity.CENTER
        }

        val button = com.google.android.material.button.MaterialButton(requireContext()).apply {
            text = getString(R.string.permission_request)
            setOnClickListener {
                requestPermissionLauncher.launch(Manifest.permission.QUERY_ALL_PACKAGES)
            }
        }

        permissionView.addView(message)
        permissionView.addView(button)
        binding.contentContainer.addView(permissionView)
    }

    private fun refreshAppSelection() {
        val inflater = LayoutInflater.from(requireContext())
        binding.contentContainer.removeAllViews()
        showAppSelection(inflater)
    }

    private fun showPermissionDenied() {
        binding.contentContainer.removeAllViews()
        val message = android.widget.TextView(requireContext()).apply {
            text = getString(R.string.permission_query_apps_denied)
            textSize = 14f
            setTextColor(requireContext().getColor(R.color.md_on_surface))
            gravity = android.view.Gravity.CENTER
            setPadding(32, 48, 32, 48)
        }
        binding.contentContainer.addView(message)
    }

    private fun showAppSelection(inflater: LayoutInflater) {
        appBinding = BottomSheetAddCheckinAppBinding.inflate(inflater, binding.contentContainer, true)

        appListAdapter = AppListAdapter { appInfo, appName ->
            selectedPackageName = appInfo.packageName
            selectedAppName = appName
            selectedAppIcon = appInfo.loadIcon(requireContext().packageManager)
            showAppDetail()
        }

        appBinding?.apply {
            appsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            appsRecyclerView.adapter = appListAdapter

            progressBar.visibility = View.VISIBLE
            appsRecyclerView.visibility = View.GONE

            searchEditText.doAfterTextChanged { text ->
                appListAdapter?.filter?.filter(text)
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val entries = AppLauncher.getInstalledApps(requireContext())
                val installedApps = entries.map { it.applicationInfo }
                val appNames = entries.map { it.label }

                withContext(Dispatchers.Main) {
                    appListAdapter?.submitData(installedApps, appNames)
                    setupLetterIndex()
                    progressBar.visibility = View.GONE
                    appsRecyclerView.visibility = View.VISIBLE
                }
            }

            appListAdapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onChanged() {
                    setupLetterIndex()
                }
            })
        }
    }

    private fun setupLetterIndex() {
        appBinding?.letterIndexContainer?.apply {
            removeAllViews()
            val letters = appListAdapter?.getAvailableInitials() ?: return

            letters.forEach { letter ->
                val tv = android.widget.TextView(requireContext()).apply {
                    text = letter
                    textSize = 10f
                    setTextColor(requireContext().getColor(R.color.md_primary))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                    )
                    gravity = android.view.Gravity.CENTER
                    setOnClickListener {
                        val position = appListAdapter?.findPositionByLetter(letter) ?: -1
                        if (position >= 0) {
                            appBinding?.appsRecyclerView?.scrollToPosition(position)
                        }
                    }
                }
                addView(tv)
            }
        }
    }

    private fun showAppDetail() {
        appBinding?.root?.visibility = View.GONE
        appBinding = null

        val inflater = LayoutInflater.from(requireContext())
        appDetailBinding = BottomSheetAddCheckinAppDetailBinding.inflate(inflater, binding.contentContainer, true)

        appDetailBinding?.apply {
            appNameText.text = selectedAppName
            appIcon.setImageDrawable(selectedAppIcon)
            setupReminderCheckbox(checkEnableReminder, timePicker)

            btnChangeApp.setOnClickListener {
                appDetailBinding = null
                binding.contentContainer.removeAllViews()
                showAppSelection(inflater)
            }
        }
    }

    private fun setupReminderCheckbox(checkBox: CheckBox, picker: TimePickerView) {
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            picker.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun showWebsiteForm(inflater: LayoutInflater) {
        websiteBinding = BottomSheetAddCheckinWebsiteBinding.inflate(inflater, binding.contentContainer, true)

        websiteBinding?.apply {
            setupReminderCheckbox(checkEnableReminder, timePicker)
            // Try to auto-fill from clipboard
            fillUrlFromClipboard()

            // Paste button
            editUrl.setOnTouchListener { _, event ->
                if (event.action == android.view.MotionEvent.ACTION_UP) {
                    val drawableRight = 2
                    if (event.rawX >= (editUrl.right - (editUrl.compoundDrawables[drawableRight]?.bounds?.width() ?: 0))) {
                        fillUrlFromClipboard()
                        return@setOnTouchListener true
                    }
                }
                false
            }

            editUrl.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    autoCompleteUrl()
                }
            }
        }
    }

    private fun showOtherForm(inflater: LayoutInflater) {
        otherBinding = BottomSheetAddCheckinOtherBinding.inflate(inflater, binding.contentContainer, true)
        otherBinding?.apply {
            setupReminderCheckbox(checkEnableReminder, timePicker)
        }
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            saveCheckinItem()
        }
    }

    private fun saveCheckinItem() {
        when (checkinType) {
            CheckinType.APP -> saveAppCheckin()
            CheckinType.WEBSITE -> saveWebsiteCheckin()
            CheckinType.OTHER -> saveOtherCheckin()
        }
    }

    private fun saveAppCheckin() {
        val appName = selectedAppName
        val packageName = selectedPackageName
        val desc = appDetailBinding?.editDesc?.text?.toString()?.trim()
        val cycle = appDetailBinding?.cyclePicker?.getCycleRules()
        val endRules = appDetailBinding?.endPicker?.getEndRules()
        val reminderTime = if (appDetailBinding?.checkEnableReminder?.isChecked == true) {
            appDetailBinding?.timePicker?.getTime()
        } else null

        if (appName.isNullOrEmpty() || packageName.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "请选择应用", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val item = CheckinItem(
                    name = appName,
                    type = CheckinType.APP,
                    packageName = packageName,
                    iconPath = "",
                    description = desc?.ifEmpty { null },
                    cycleType = cycle?.type ?: CheckinCycleType.DAY,
                    cycleValue = cycle?.value ?: 1,
                    cycleWeekDays = cycle?.weekDaysJson,
                    cycleMonthDays = cycle?.monthDaysJson,
                    skipHolidays = cycle?.skipHolidays ?: false,
                    skipWeekends = cycle?.skipWeekends ?: false,
                    reminderTime = reminderTime?.ifEmpty { null },
                    endType = endRules?.endType ?: com.alosir.task.data.entity.CheckinEndType.NEVER,
                    endCount = endRules?.endCount ?: 0,
                    endDate = endRules?.endDate
                )
                val id = repository.insertItem(item).toInt()
                ReminderScheduler.schedule(requireContext(), item.copy(id = id))
                viewModel.loadCheckinStatus()
                Toast.makeText(requireContext(), "已添加: $appName", Toast.LENGTH_SHORT).show()
                handleReminderPermission(reminderTime?.ifEmpty { null })
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "添加失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveWebsiteCheckin() {
        websiteBinding?.apply {
            val name = editName.text.toString().trim()
            var url = editUrl.text.toString().trim()
            val desc = editDesc.text.toString().trim()
            val cycle = cyclePicker.getCycleRules()
            val endRules = endPicker.getEndRules()
            val reminderTime = if (checkEnableReminder.isChecked) timePicker.getTime() else null

            if (name.isEmpty()) {
                editName.error = "请输入网站名称"
                return
            }
            if (url.isEmpty()) {
                editUrl.error = "请输入网址"
                return
            }

            url = normalizeUrl(url)

            lifecycleScope.launch {
                try {
                    val domain = extractDomain(url)
                    val iconPath = IconManager.saveDefaultIcon(requireContext(), "web_${domain.replace(".", "_")}.png")
                    val item = CheckinItem(
                        name = name,
                        type = CheckinType.WEBSITE,
                        url = url,
                        iconPath = iconPath,
                        description = desc.ifEmpty { null },
                        cycleType = cycle.type,
                        cycleValue = cycle.value,
                        cycleWeekDays = cycle.weekDaysJson,
                        cycleMonthDays = cycle.monthDaysJson,
                        skipHolidays = cycle.skipHolidays,
                        skipWeekends = cycle.skipWeekends,
                        reminderTime = reminderTime?.ifEmpty { null },
                        endType = endRules.endType,
                        endCount = endRules.endCount,
                        endDate = endRules.endDate
                    )
                    val id = repository.insertItem(item).toInt()
                    ReminderScheduler.schedule(requireContext(), item.copy(id = id))
                    viewModel.loadCheckinStatus()
                    Toast.makeText(requireContext(), "已添加: $name", Toast.LENGTH_SHORT).show()
                    handleReminderPermission(reminderTime?.ifEmpty { null })
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "添加失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveOtherCheckin() {
        otherBinding?.apply {
            val name = editName.text.toString().trim()
            val desc = editDesc.text.toString().trim()
            val cycle = cyclePicker.getCycleRules()
            val endRules = endPicker.getEndRules()
            val reminderTime = if (checkEnableReminder.isChecked) timePicker.getTime() else null

            if (name.isEmpty()) {
                editName.error = "请输入任务名称"
                return
            }

            lifecycleScope.launch {
                try {
                    val iconPath = IconManager.saveDefaultIcon(requireContext(), "other_${System.currentTimeMillis()}.png")
                    val item = CheckinItem(
                        name = name,
                        type = CheckinType.OTHER,
                        iconPath = iconPath,
                        description = desc.ifEmpty { null },
                        cycleType = cycle.type,
                        cycleValue = cycle.value,
                        cycleWeekDays = cycle.weekDaysJson,
                        cycleMonthDays = cycle.monthDaysJson,
                        skipHolidays = cycle.skipHolidays,
                        skipWeekends = cycle.skipWeekends,
                        reminderTime = reminderTime?.ifEmpty { null },
                        endType = endRules.endType,
                        endCount = endRules.endCount,
                        endDate = endRules.endDate
                    )
                    val id = repository.insertItem(item).toInt()
                    ReminderScheduler.schedule(requireContext(), item.copy(id = id))
                    viewModel.loadCheckinStatus()
                    Toast.makeText(requireContext(), "已添加: $name", Toast.LENGTH_SHORT).show()
                    handleReminderPermission(reminderTime?.ifEmpty { null })
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "添加失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleReminderPermission(reminderTime: String?) {
        if (reminderTime.isNullOrEmpty()) {
            dismiss()
            return
        }

        notificationPermissionHelper?.requestNotificationPermission { granted, needsResumeCheck ->
            if (granted) {
                dismiss()
            } else if (needsResumeCheck) {
                pendingReminderPermissionCheck = true
            } else {
                dismissAllowingStateLoss()
            }
        }
    }

    private fun fillUrlFromClipboard() {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = clipboard?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: return
            if (isValidUrl(text)) {
                websiteBinding?.editUrl?.setText(text)
                autoCompleteUrl()
                // Try to extract domain as name
                if (websiteBinding?.editName?.text?.isEmpty() == true) {
                    websiteBinding?.editName?.setText(extractDomainName(text))
                }
            }
        }
    }

    private fun autoCompleteUrl() {
        websiteBinding?.editUrl?.let { edit ->
            val url = edit.text.toString().trim()
            if (url.isNotEmpty() && !url.startsWith("http://") && !url.startsWith("https://")) {
                edit.setText("https://$url")
            }
        }
    }

    private fun normalizeUrl(url: String): String {
        return if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            "https://$url"
        }
    }

    private fun extractDomain(url: String): String {
        return url.removePrefix("http://").removePrefix("https://").removePrefix("www.").split("/").firstOrNull() ?: "unknown"
    }

    private fun isValidUrl(text: String): Boolean {
        return text.startsWith("http://") || text.startsWith("https://") ||
                text.contains(".") && !text.contains(" ")
    }

    private fun extractDomainName(url: String): String {
        return try {
            val host = java.net.URL(normalizeUrl(url)).host ?: return ""
            host.removePrefix("www.").substringBefore(".")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } catch (e: Exception) {
            ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        appBinding = null
        appDetailBinding = null
        websiteBinding = null
        otherBinding = null
    }
}
