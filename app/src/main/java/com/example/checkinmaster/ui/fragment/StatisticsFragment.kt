package com.alosir.task.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.alosir.task.R
import com.alosir.task.data.entity.CheckinType
import com.alosir.task.databinding.FragmentStatisticsBinding
import com.alosir.task.ui.adapter.StreakRankAdapter
import com.alosir.task.ui.view.PieChartView
import com.alosir.task.ui.viewmodel.StatisticsViewModel
import com.alosir.task.util.CrashLogger
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by viewModels()
    private val streakAdapter = StreakRankAdapter()

    private var dateDetailDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return try {
            _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
            binding.root
        } catch (e: Exception) {
            CrashLogger.saveCrashLog(requireContext(), e)
            e.printStackTrace()
            createErrorView(e)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (_binding == null) return

        try {
            setupRecyclerView()
            setupCalendar()
            setupMonthNavigation()
            setupSwipeRefresh()
            observeViewModel()
        } catch (e: Exception) {
            CrashLogger.saveCrashLog(requireContext(), e)
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            viewModel.loadStatistics()
        } catch (e: Exception) {
            CrashLogger.saveCrashLog(requireContext(), e)
            e.printStackTrace()
        }
    }

    private fun createErrorView(e: Exception): View {
        val context = requireContext()
        val scrollView = ScrollView(context)
        val textView = TextView(context).apply {
            setPadding(32, 32, 32, 32)
            textSize = 14f
            setTextIsSelectable(true)
            text = buildString {
                appendLine("统计页加载失败：")
                appendLine(e.message ?: "未知错误")
                appendLine()
                appendLine("崩溃日志已保存至：")
                appendLine(CrashLogger.getLatestCrashLogFile(context)?.absolutePath ?: "未知")
                appendLine()
                appendLine("请通过 adb 执行以下命令导出日志：")
                appendLine("adb pull ${CrashLogger.getLatestCrashLogFile(context)?.absolutePath ?: ""}")
                appendLine()
                appendLine("详细堆栈：")
                append(e.stackTraceToString())
            }
        }
        scrollView.addView(textView)
        return scrollView
    }

    private fun setupRecyclerView() {
        binding.streakRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.streakRecyclerView.adapter = streakAdapter
    }

    private fun setupCalendar() {
        binding.calendarView.onDateClickListener = { dateStr ->
            try {
                viewModel.selectDate(dateStr)
            } catch (e: Exception) {
                CrashLogger.saveCrashLog(requireContext(), e)
                e.printStackTrace()
            }
        }
        setupCalendarSwipe()
    }

    private fun setupCalendarSwipe() {
        val gestureDetector = android.view.GestureDetector(requireContext(),
            object : android.view.GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: android.view.MotionEvent?,
                    e2: android.view.MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    val startX = e1?.x ?: return false
                    val startY = e1?.y ?: return false
                    val dx = e2.x - startX
                    val dy = e2.y - startY
                    if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > 100) {
                        if (dx > 0) {
                            viewModel.goToPreviousMonth()
                        } else {
                            viewModel.goToNextMonth()
                        }
                        return true
                    }
                    return false
                }
            })

        binding.calendarView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun setupMonthNavigation() {
        binding.btnPreviousMonth.setOnClickListener {
            try {
                viewModel.goToPreviousMonth()
            } catch (e: Exception) {
                CrashLogger.saveCrashLog(requireContext(), e)
                e.printStackTrace()
            }
        }
        binding.btnNextMonth.setOnClickListener {
            try {
                viewModel.goToNextMonth()
            } catch (e: Exception) {
                CrashLogger.saveCrashLog(requireContext(), e)
                e.printStackTrace()
            }
        }
        binding.btnToday.setOnClickListener {
            try {
                viewModel.resetToCurrentMonth()
            } catch (e: Exception) {
                CrashLogger.saveCrashLog(requireContext(), e)
                e.printStackTrace()
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            try {
                viewModel.loadStatistics()
            } catch (e: Exception) {
                CrashLogger.saveCrashLog(requireContext(), e)
                e.printStackTrace()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            try {
                binding.swipeRefreshLayout.isRefreshing = false
                binding.monthDisplayText.text = state.monthDisplay

                updateOverview(state)
                updateCalendar(state)
                updateTypeDistribution(state)
                updateStreakRanking(state)

                if (state.selectedDate != null && state.selectedDateRecords.isNotEmpty()) {
                    showDateDetailDialog(state.selectedDate, state.selectedDateRecords)
                    viewModel.clearSelectedDate()
                }
            } catch (e: Exception) {
                CrashLogger.saveCrashLog(requireContext(), e)
                e.printStackTrace()
            }
        }
    }

    private fun updateOverview(state: StatisticsViewModel.StatisticsUiState) {
        binding.weeklyRateText.text = getString(R.string.statistics_rate_format, state.weeklyStats.rate)
        binding.monthlyRateText.text = getString(R.string.statistics_rate_format, state.monthlyStats.rate)
        binding.totalCheckinsText.text = state.totalCheckins.toString()
    }

    private fun updateCalendar(state: StatisticsViewModel.StatisticsUiState) {
        binding.calendarView.days = state.calendarDays
        binding.calendarView.selectedDate = state.selectedDate
        updateMonthNavigationButtons(state)
    }

    private fun updateMonthNavigationButtons(state: StatisticsViewModel.StatisticsUiState) {
        val canGoPrev = state.year > state.earliestYear ||
                (state.year == state.earliestYear && state.month > state.earliestMonth)
        val canGoNext = state.year < state.currentYear ||
                (state.year == state.currentYear && state.month < state.currentMonth)
        val isCurrent = state.year == state.currentYear && state.month == state.currentMonth

        binding.btnPreviousMonth.isEnabled = canGoPrev
        binding.btnPreviousMonth.alpha = if (canGoPrev) 1f else 0.3f

        binding.btnNextMonth.isEnabled = canGoNext
        binding.btnNextMonth.alpha = if (canGoNext) 1f else 0.3f

        binding.btnToday.isEnabled = !isCurrent
        binding.btnToday.alpha = if (isCurrent) 0.3f else 1f
    }

    private fun updateTypeDistribution(state: StatisticsViewModel.StatisticsUiState) {
        val distribution = state.typeDistribution
        val total = distribution.totalCount

        val slices = mutableListOf<PieChartView.Slice>()
        if (distribution.appCount > 0) {
            slices.add(PieChartView.Slice(distribution.appCount.toFloat(), requireContext().getColor(R.color.checkin_success)))
        }
        if (distribution.websiteCount > 0) {
            slices.add(PieChartView.Slice(distribution.websiteCount.toFloat(), requireContext().getColor(R.color.md_primary)))
        }
        if (distribution.otherCount > 0) {
            slices.add(PieChartView.Slice(distribution.otherCount.toFloat(), requireContext().getColor(R.color.md_tertiary)))
        }
        binding.pieChart.setSlices(slices)

        binding.legendAppLabel.text = getString(R.string.tab_app)
        binding.legendAppColor.backgroundTintList = android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.checkin_success))
        binding.legendAppValue.text = getDistributionText(distribution.appCount, total)

        binding.legendWebsiteLabel.text = getString(R.string.tab_website)
        binding.legendWebsiteColor.backgroundTintList = android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.md_primary))
        binding.legendWebsiteValue.text = getDistributionText(distribution.websiteCount, total)

        binding.legendOtherLabel.text = getString(R.string.tab_other)
        binding.legendOtherColor.backgroundTintList = android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.md_tertiary))
        binding.legendOtherValue.text = getDistributionText(distribution.otherCount, total)
    }

    private fun getDistributionText(count: Int, total: Int): String {
        return if (total == 0) {
            "0 (0%)"
        } else {
            "$count (${count * 100 / total}%)"
        }
    }

    private fun updateStreakRanking(state: StatisticsViewModel.StatisticsUiState) {
        if (state.streakRanking.isEmpty()) {
            binding.streakRecyclerView.visibility = View.GONE
            binding.emptyStreakText.visibility = View.VISIBLE
        } else {
            binding.streakRecyclerView.visibility = View.VISIBLE
            binding.emptyStreakText.visibility = View.GONE
            streakAdapter.submitList(state.streakRanking)
        }
    }

    private fun showDateDetailDialog(
        dateStr: String,
        records: List<StatisticsViewModel.RecordWithItem>
    ) {
        dateDetailDialog?.dismiss()

        val items = records.map { recordWithItem ->
            val record = recordWithItem.record
            val item = recordWithItem.item
            val typeLabel = when (item.type) {
                CheckinType.APP -> getString(R.string.tab_app)
                CheckinType.WEBSITE -> getString(R.string.tab_website)
                else -> getString(R.string.tab_other)
            }
            val autoLabel = if (record.isAuto) " · ${getString(R.string.statistics_auto_record)}" else ""
            "${item.name} ($typeLabel)$autoLabel\n${getString(R.string.statistics_record_time, record.checkinTime)}"
        }.toTypedArray()

        dateDetailDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.statistics_day_records_title, dateStr))
            .setItems(items, null)
            .setPositiveButton(R.string.button_confirm) { dialog, _ ->
                dialog.dismiss()
            }
            .setOnDismissListener {
                dateDetailDialog = null
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dateDetailDialog?.dismiss()
        dateDetailDialog = null
        _binding = null
    }
}
