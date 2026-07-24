package com.alosir.task.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.alosir.task.R
import com.alosir.task.data.CheckinDatabase
import com.alosir.task.data.entity.CheckinEndType
import com.alosir.task.data.entity.CheckinItem
import com.alosir.task.data.entity.CheckinType
import com.alosir.task.databinding.DialogTaskDetailBinding
import com.alosir.task.ui.bottomsheet.EditCheckinBottomSheet
import com.alosir.task.ui.viewmodel.CheckinListViewModel
import com.alosir.task.util.AppLauncher
import com.alosir.task.util.CycleCalculator
import com.alosir.task.util.IconManager
import com.alosir.task.util.ReminderScheduler
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TaskDetailDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_ITEM_ID = "item_id"

        fun newInstance(itemId: Int): TaskDetailDialogFragment {
            return TaskDetailDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ITEM_ID, itemId)
                }
            }
        }
    }

    private var _binding: DialogTaskDetailBinding? = null
    private val binding get() = _binding!!

    private var itemId: Int = 0
    private var item: CheckinItem? = null
    private lateinit var viewModel: CheckinListViewModel
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemId = arguments?.getInt(ARG_ITEM_ID) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTaskDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[CheckinListViewModel::class.java]
        loadItemData()
    }

    private fun loadItemData() {
        lifecycleScope.launch {
            try {
                val database = CheckinDatabase.getDatabase(requireContext())
                val loadedItem = withContext(Dispatchers.IO) {
                    database.checkinItemDao().getById(itemId)
                }
                item = loadedItem

                if (loadedItem == null) {
                    Toast.makeText(requireContext(), "任务不存在", Toast.LENGTH_SHORT).show()
                    dismiss()
                    return@launch
                }

                renderTask(loadedItem)
                setupButtons(loadedItem)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }

    private fun renderTask(item: CheckinItem) {
        binding.taskNameText.text = item.name
        binding.taskDescText.text = item.description ?: ""
        binding.taskDescText.visibility = if (item.description.isNullOrEmpty()) View.GONE else View.VISIBLE

        // 图标
        loadTaskIcon(item)

        // 状态
        val todayStr = dateFormat.format(Date())
        val statusText = when {
            item.terminated == 1 -> getString(R.string.status_terminated)
            item.lastCheckinDate == todayStr -> getString(R.string.status_completed_today)
            CycleCalculator.isCheckinAvailable(item) -> {
                val nextDate = CycleCalculator.getNextCheckinDate(item)
                if (nextDate != null && dateFormat.format(nextDate) == todayStr) {
                    getString(R.string.status_pending_today)
                } else {
                    getString(R.string.status_pending_future, CycleCalculator.formatRelativeDate(nextDate ?: Date()))
                }
            }
            else -> getString(R.string.status_completed_history)
        }
        binding.statusText.text = statusText

        // 周期
        binding.cycleText.text = CycleCalculator.getCycleShortDescription(item)

        // 下次签到
        if (item.terminated == 1) {
            binding.nextDateText.text = item.terminatedDate ?: "-"
        } else {
            val nextDate = CycleCalculator.getNextCheckinDate(item)
            binding.nextDateText.text = nextDate?.let { CycleCalculator.formatRelativeDate(it) } ?: "-"
        }

        // 提醒时间
        binding.reminderText.text = item.reminderTime ?: getString(R.string.not_set)

        // 结束规则
        binding.endRuleText.text = when (item.endType) {
            CheckinEndType.BY_COUNT -> getString(R.string.end_desc_count, item.endCount)
            CheckinEndType.BY_DATE -> {
                val endDate = item.endDate?.let {
                    try {
                        val parsed = dateFormat.parse(it)
                        parsed?.let { d ->
                            SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(d)
                        } ?: it
                    } catch (e: Exception) {
                        it
                    }
                } ?: getString(R.string.end_type_date)
                getString(R.string.end_desc_date, endDate)
            }
            else -> getString(R.string.end_desc_never)
        }
    }

    private fun loadTaskIcon(item: CheckinItem) {
        when (item.type) {
            CheckinType.APP -> {
                val icon = IconManager.loadAppIcon(requireContext(), item.packageName ?: "")
                if (icon != null) {
                    binding.taskIcon.setImageDrawable(icon)
                } else {
                    binding.taskIcon.setImageResource(R.drawable.ic_default_app)
                }
            }
            CheckinType.WEBSITE -> {
                binding.taskIcon.setImageResource(R.drawable.ic_default_website)
            }
            else -> {
                binding.taskIcon.setImageResource(R.drawable.ic_default_other)
            }
        }
    }

    private fun setupButtons(item: CheckinItem) {
        val todayStr = dateFormat.format(Date())
        val isTodayPending = item.terminated == 0 &&
                item.lastCheckinDate != todayStr &&
                CycleCalculator.isCheckinAvailable(item) &&
                CycleCalculator.getNextCheckinDate(item)?.let { dateFormat.format(it) == todayStr } == true

        val isTodayCompleted = item.terminated == 0 && item.lastCheckinDate == todayStr
        val canTerminate = item.terminated == 0

        binding.btnComplete.visibility = if (isTodayPending) View.VISIBLE else View.GONE
        binding.btnSkip.visibility = if (isTodayPending) View.VISIBLE else View.GONE
        binding.btnRestore.visibility = if (isTodayCompleted) View.VISIBLE else View.GONE
        binding.btnEdit.visibility = if (item.terminated == 0) View.VISIBLE else View.GONE
        binding.btnTerminate.visibility = if (canTerminate) View.VISIBLE else View.GONE

        binding.btnComplete.setOnClickListener {
            viewModel.markCheckined(item.id)
            dismiss()
        }

        binding.btnSkip.setOnClickListener {
            viewModel.skipCheckin(item.id)
            dismiss()
        }

        binding.btnRestore.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val database = CheckinDatabase.getDatabase(requireContext())
                    val record = withContext(Dispatchers.IO) {
                        database.checkinRecordDao().getByDate(item.id, todayStr)
                    }
                    record?.let { viewModel.restoreTodayRecord(it) }
                    dismiss()
                } catch (e: Exception) {
                    e.printStackTrace()
                    dismiss()
                }
            }
        }

        binding.btnEdit.setOnClickListener {
            EditCheckinBottomSheet
                .newInstance(item.id)
                .apply {
                    setOnEditSuccessListener {
                        viewModel.loadCheckinStatus()
                    }
                }
                .show(parentFragmentManager, "edit_sheet_${item.id}")
            dismiss()
        }

        binding.btnTerminate.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.terminate_item)
                .setMessage(R.string.terminate_confirm)
                .setPositiveButton(R.string.button_confirm) { _, _ ->
                    viewModel.terminateItem(item.id)
                    dismiss()
                }
                .setNegativeButton(R.string.button_cancel, null)
                .show()
        }

        binding.btnDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_item)
                .setMessage(R.string.delete_item_confirm)
                .setPositiveButton(R.string.button_confirm) { _, _ ->
                    viewModel.deleteItem(item)
                    dismiss()
                }
                .setNegativeButton(R.string.button_cancel, null)
                .show()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
