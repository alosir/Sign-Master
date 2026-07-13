package com.alosir.task.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alosir.task.R
import com.alosir.task.data.entity.CheckinItem
import com.alosir.task.databinding.FragmentCheckinListBinding
import com.alosir.task.ui.adapter.CompletedCheckinAdapter
import com.alosir.task.ui.bottomsheet.EditCheckinBottomSheet
import com.alosir.task.ui.viewmodel.CheckinListViewModel
import com.alosir.task.util.CompletedSwipeCallback
import com.alosir.task.util.CycleCalculator
import com.google.android.material.snackbar.Snackbar

class CompletedCheckinFragment : Fragment() {

    private var _binding: FragmentCheckinListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CheckinListViewModel
    private lateinit var adapter: CompletedCheckinAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckinListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[CheckinListViewModel::class.java]

        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
        viewModel.loadCheckinStatus()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadCheckinStatus()
        }
    }

    private fun setupRecyclerView() {
        adapter = CompletedCheckinAdapter(
            onItemClick = { model -> showTaskDetail(model.item) }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@CompletedCheckinFragment.adapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleCount = layoutManager.childCount
                    val totalCount = layoutManager.itemCount
                    val firstVisible = layoutManager.findFirstVisibleItemPosition()
                    if (dy > 0 && visibleCount + firstVisible >= totalCount - 3) {
                        viewModel.loadMoreCompletedRecords()
                    }
                }
            })
        }

        val callback = CompletedSwipeCallback(
            adapter = adapter,
            onRestore = { position ->
                val model = adapter.getItemAt(position)
                viewModel.restoreTodayRecord(model.record)
            },
            onDelete = { position ->
                val model = adapter.getItemAt(position)
                viewModel.deleteRecord(model.record)
            }
        )
        ItemTouchHelper(callback).attachToRecyclerView(binding.recyclerView)
    }

    private fun observeViewModel() {
        viewModel.completedRecords.observe(viewLifecycleOwner) { records ->
            binding.swipeRefreshLayout.isRefreshing = false
            adapter.submitList(records) {
                binding.recyclerView.requestLayout()
            }
            if (records.isEmpty()) {
                binding.emptyView.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.emptyView.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun showTaskDetail(item: CheckinItem) {
        val cycle = CycleCalculator.getCycleShortDescription(item)
        val nextDate = CycleCalculator.getNextCheckinDate(item)
            ?.let { CycleCalculator.formatRelativeDate(it) }
            ?: "未知"
        val reminder = if (item.reminderTime.isNullOrEmpty()) "未设置" else item.reminderTime

        val message = buildString {
            appendLine("周期：$cycle")
            appendLine("下次签到：$nextDate")
            appendLine("提醒时间：$reminder")
            if (!item.description.isNullOrEmpty()) {
                appendLine("描述：${item.description}")
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(item.name)
            .setMessage(message)
            .setPositiveButton(R.string.edit_item) { _, _ -> openEditDialog(item.id) }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
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

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
