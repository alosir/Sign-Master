package com.alosir.task.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alosir.task.R
import com.alosir.task.data.entity.CheckinItem
import com.alosir.task.databinding.FragmentTodayBinding
import com.alosir.task.ui.adapter.CompletedCheckinAdapter
import com.alosir.task.ui.adapter.PendingCheckinAdapter
import com.alosir.task.ui.bottomsheet.EditCheckinBottomSheet
import com.alosir.task.ui.viewmodel.CheckinListViewModel
import com.alosir.task.util.AppLauncher
import com.alosir.task.util.CompletedSwipeCallback
import com.alosir.task.util.CycleCalculator
import com.alosir.task.util.PendingSwipeCallback
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TodayFragment : Fragment() {

    private var _binding: FragmentTodayBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CheckinListViewModel
    private lateinit var pendingAdapter: PendingCheckinAdapter
    private lateinit var completedAdapter: CompletedCheckinAdapter

    private var isCompletedExpanded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[CheckinListViewModel::class.java]

        setupToolbar()
        setupRecyclerViews()
        setupCompletedSection()
        setupSwipeRefresh()
        observeViewModel()
        updateGreetingAndDate()

        viewModel.loadCheckinStatus()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadCheckinStatus()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                else -> false
            }
        }
    }

    private fun setupRecyclerViews() {
        pendingAdapter = PendingCheckinAdapter(
            onItemClick = { model -> showTaskDetail(model.item) },
            onCompleteClick = { model -> viewModel.markCheckined(model.item.id) },
            onOpenAppClick = { model ->
                model.item.packageName?.let { pkg ->
                    AppLauncher.launchApp(requireContext(), pkg)
                }
            },
            onOpenWebsiteClick = { model ->
                model.item.url?.let { url ->
                    AppLauncher.openWebsite(requireContext(), url)
                }
            }
        )

        completedAdapter = CompletedCheckinAdapter(
            onItemClick = { model -> showTaskDetail(model.item) }
        )

        binding.pendingRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = pendingAdapter
            isNestedScrollingEnabled = false
        }

        binding.completedRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = completedAdapter
            isNestedScrollingEnabled = false
        }

        attachPendingSwipeHelper(binding.pendingRecyclerView)
        attachCompletedSwipeHelper(binding.completedRecyclerView)
    }

    private fun attachPendingSwipeHelper(recyclerView: RecyclerView) {
        val callback = PendingSwipeCallback(
            adapter = pendingAdapter,
            onComplete = { position ->
                val model = pendingAdapter.getItemAt(position)
                viewModel.markCheckined(model.item.id)
            },
            onSkip = { position ->
                val model = pendingAdapter.getItemAt(position)
                viewModel.markCheckined(model.item.id)
                showSnackbar(getString(R.string.skip_success))
            },
            onEdit = { position ->
                val model = pendingAdapter.getItemAt(position)
                openEditDialog(model.item.id)
                pendingAdapter.notifyItemChanged(position)
            },
            onDelete = { position ->
                val model = pendingAdapter.getItemAt(position)
                showDeleteConfirmation(model.item, position)
            }
        )
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
    }

    private fun attachCompletedSwipeHelper(recyclerView: RecyclerView) {
        val callback = CompletedSwipeCallback(
            adapter = completedAdapter,
            onRestore = { position ->
                val model = completedAdapter.getItemAt(position)
                viewModel.restoreTodayRecord(model.record)
            },
            onDelete = { position ->
                val model = completedAdapter.getItemAt(position)
                viewModel.deleteRecord(model.record)
            },
            onDetail = { position ->
                val model = completedAdapter.getItemAt(position)
                showTaskDetail(model.item)
                completedAdapter.notifyItemChanged(position)
            }
        )
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
    }

    private fun setupCompletedSection() {
        binding.completedHeader.setOnClickListener {
            isCompletedExpanded = !isCompletedExpanded
            updateCompletedSectionVisibility()
        }
        updateCompletedSectionVisibility()
    }

    private fun updateCompletedSectionVisibility() {
        binding.completedRecyclerView.visibility = if (isCompletedExpanded) View.VISIBLE else View.GONE
        binding.completedExpandIcon.setImageResource(
            if (isCompletedExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
        )
    }

    private fun observeViewModel() {
        viewModel.pendingItems.observe(viewLifecycleOwner) { items ->
            binding.swipeRefreshLayout.isRefreshing = false
            pendingAdapter.submitList(items) {
                binding.pendingRecyclerView.requestLayout()
            }
            updatePendingVisibility(items)
            updateStats()
        }

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        viewModel.completedRecords.observe(viewLifecycleOwner) { records ->
            binding.swipeRefreshLayout.isRefreshing = false
            val todayRecords = records.filter { it.record.checkinDate == todayStr }
            completedAdapter.submitList(todayRecords) {
                binding.completedRecyclerView.requestLayout()
            }
            updateCompletedVisibility(todayRecords)
            updateStats()
        }
    }

    private fun updateStats() {
        val pendingCount = viewModel.pendingItems.value?.size ?: 0
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val completedCount = viewModel.completedRecords.value?.count { it.record.checkinDate == todayStr } ?: 0

        binding.statsCard.pendingCountText.text = pendingCount.toString()
        binding.statsCard.completedCountText.text = completedCount.toString()
        binding.pendingCountBadge.text = pendingCount.toString()
        binding.completedCountBadge.text = completedCount.toString()

        lifecycleScope.launch {
            val streak = calculateCurrentStreak()
            binding.statsCard.streakCountText.text = streak.toString()
        }
    }

    private fun updatePendingVisibility(items: List<CheckinListViewModel.PendingItemUiModel>) {
        if (items.isEmpty()) {
            binding.pendingRecyclerView.visibility = View.GONE
            binding.pendingEmptyView.root.visibility = View.VISIBLE
            binding.pendingEmptyView.emptyTitle.text = getString(R.string.today_empty_pending_title)
            binding.pendingEmptyView.emptySubtitle.text = getString(R.string.today_empty_pending_subtitle)
        } else {
            binding.pendingRecyclerView.visibility = View.VISIBLE
            binding.pendingEmptyView.root.visibility = View.GONE
        }
    }

    private fun updateCompletedVisibility(items: List<CheckinListViewModel.CompletedRecordUiModel>) {
        if (items.isEmpty()) {
            binding.completedRecyclerView.visibility = View.GONE
            binding.completedEmptyView.root.visibility = View.VISIBLE
            binding.completedEmptyView.emptyTitle.text = getString(R.string.today_empty_completed_title)
            binding.completedEmptyView.emptySubtitle.text = getString(R.string.today_empty_completed_subtitle)
        } else {
            binding.completedEmptyView.root.visibility = View.GONE
            if (isCompletedExpanded) {
                binding.completedRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun updateGreetingAndDate() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 5..11 -> getString(R.string.greeting_morning)
            in 12..17 -> getString(R.string.greeting_afternoon)
            in 18..22 -> getString(R.string.greeting_evening)
            else -> getString(R.string.greeting_night)
        }
        binding.greetingText.text = getString(R.string.greeting_format, greeting)

        val dateFormat = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.getDefault())
        binding.dateText.text = dateFormat.format(Date())
    }

    private fun showTaskDetail(item: CheckinItem) {
        com.alosir.task.ui.dialog.TaskDetailDialogFragment
            .newInstance(item.id)
            .show(parentFragmentManager, "task_detail_${item.id}")
    }

    private fun openEditDialog(itemId: Int) {
        EditCheckinBottomSheet
            .newInstance(itemId)
            .apply {
                setOnEditSuccessListener {
                    viewModel.loadCheckinStatus()
                }
            }
            .show(parentFragmentManager, "edit_sheet_$itemId")
    }

    private fun showDeleteConfirmation(item: CheckinItem, position: Int) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_item)
            .setMessage(R.string.delete_recurring_confirmation)
            .setPositiveButton(R.string.button_confirm) { _, _ ->
                viewModel.deleteItem(item)
            }
            .setNegativeButton(R.string.button_cancel) { _, _ ->
                pendingAdapter.notifyItemChanged(position)
            }
            .setOnCancelListener {
                pendingAdapter.notifyItemChanged(position)
            }
            .show()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private suspend fun calculateCurrentStreak(): Int = withContext(Dispatchers.IO) {
        try {
            val recordDao = com.alosir.task.data.CheckinDatabase.getDatabase(requireContext()).checkinRecordDao()
            val dates = recordDao.getAllDates().toSet()
            if (dates.isEmpty()) return@withContext 0

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            var streak = 0

            while (true) {
                val dateStr = dateFormat.format(calendar.time)
                if (dateStr in dates) {
                    streak++
                    calendar.add(Calendar.DAY_OF_MONTH, -1)
                } else if (streak == 0 && dateStr == dateFormat.format(Date())) {
                    calendar.add(Calendar.DAY_OF_MONTH, -1)
                } else {
                    break
                }
            }
            streak
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
