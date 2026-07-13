package com.alosir.task.ui.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.alosir.task.R
import com.alosir.task.data.CheckinDatabase
import com.alosir.task.data.entity.CheckinItem
import com.alosir.task.data.entity.CheckinType
import com.alosir.task.data.repository.CheckinItemRepository
import com.alosir.task.databinding.BottomSheetAddCheckinBinding
import com.alosir.task.databinding.BottomSheetEditCheckinBinding
import com.alosir.task.ui.viewmodel.CheckinListViewModel
import com.alosir.task.util.ReminderScheduler
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditCheckinBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_ITEM_ID = "item_id"

        fun newInstance(itemId: Int): EditCheckinBottomSheet {
            return EditCheckinBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ITEM_ID, itemId)
                }
            }
        }
    }

    private var _binding: BottomSheetAddCheckinBinding? = null
    private val binding get() = _binding!!

    private var contentBinding: BottomSheetEditCheckinBinding? = null

    private var itemId: Int = 0
    private var originalItem: CheckinItem? = null
    private lateinit var viewModel: CheckinListViewModel
    private lateinit var repository: CheckinItemRepository

    private var onEditSuccessListener: (() -> Unit)? = null

    fun setOnEditSuccessListener(listener: () -> Unit) {
        onEditSuccessListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemId = arguments?.getInt(ARG_ITEM_ID) ?: 0
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

        binding.titleText.setText(R.string.edit_item)

        val inflater = LayoutInflater.from(context)
        contentBinding = BottomSheetEditCheckinBinding.inflate(inflater, binding.contentContainer, true)

        contentBinding?.apply {
            checkEnableReminder.setOnCheckedChangeListener { _, isChecked ->
                timePicker.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
        }

        setupButtons()
        loadItemData()
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            saveChanges()
        }
    }

    private fun loadItemData() {
        lifecycleScope.launch {
            try {
                val item = withContext(Dispatchers.IO) {
                    repository.getItemById(itemId)
                }
                originalItem = item

                item?.let {
                    contentBinding?.apply {
                        editName.setText(it.name)
                        editDesc.setText(it.description ?: "")
                        val hasReminder = !it.reminderTime.isNullOrEmpty()
                        checkEnableReminder.isChecked = hasReminder
                        timePicker.visibility = if (hasReminder) View.VISIBLE else View.GONE
                        timePicker.setTime(it.reminderTime)
                        cyclePicker.setCycleRules(
                            it.cycleType,
                            it.cycleValue,
                            it.cycleWeekDays,
                            it.cycleMonthDays,
                            it.skipHolidays,
                            it.skipWeekends
                        )

                        when (it.type) {
                            CheckinType.WEBSITE -> {
                                urlLayout.visibility = View.VISIBLE
                                editUrl.setText(it.url ?: "")
                            }
                            CheckinType.APP -> {
                                packageLayout.visibility = View.VISIBLE
                                editPackage.setText(it.packageName ?: "")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }

    private fun saveChanges() {
        val item = originalItem ?: return

        contentBinding?.apply {
            val newName = editName.text.toString().trim()
            if (newName.isEmpty()) {
                editName.error = "名称不能为空"
                return
            }

            val newDesc = editDesc.text.toString().trim()
            val newUrl = editUrl.text.toString().trim()
            val newReminderTime = if (checkEnableReminder.isChecked) timePicker.getTime() else null
            val cycle = cyclePicker.getCycleRules()

            lifecycleScope.launch {
                try {
                    val updatedItem = item.copy(
                        name = newName,
                        description = newDesc.ifEmpty { null },
                        url = if (item.type == CheckinType.WEBSITE) newUrl.ifEmpty { null } else item.url,
                        cycleType = cycle.type,
                        cycleValue = cycle.value,
                        cycleWeekDays = cycle.weekDaysJson,
                        cycleMonthDays = cycle.monthDaysJson,
                        skipHolidays = cycle.skipHolidays,
                        skipWeekends = cycle.skipWeekends,
                        reminderTime = newReminderTime
                    )

                    withContext(Dispatchers.IO) {
                        repository.updateItem(updatedItem)

                        if (item.cycleType != cycle.type ||
                            item.cycleValue != cycle.value ||
                            item.cycleWeekDays != cycle.weekDaysJson ||
                            item.cycleMonthDays != cycle.monthDaysJson
                        ) {
                            repository.clearHistoryByItemId(itemId)
                        }
                    }

                    if (updatedItem.reminderTime.isNullOrEmpty()) {
                        ReminderScheduler.cancel(requireContext(), updatedItem.id)
                    } else {
                        ReminderScheduler.schedule(requireContext(), updatedItem)
                    }

                    Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show()
                    viewModel.loadCheckinStatus()
                    onEditSuccessListener?.invoke()
                    dismiss()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        contentBinding = null
    }
}
