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
import com.alosir.task.databinding.FragmentCheckinListBinding
import com.alosir.task.ui.adapter.PendingCheckinAdapter
import com.alosir.task.ui.bottomsheet.EditCheckinBottomSheet
import com.alosir.task.ui.viewmodel.CheckinListViewModel
import com.alosir.task.util.AppLauncher
import com.alosir.task.util.PendingSwipeCallback
import com.google.android.material.snackbar.Snackbar

class PendingCheckinFragment : Fragment() {

    private var _binding: FragmentCheckinListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CheckinListViewModel
    private lateinit var adapter: PendingCheckinAdapter

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
        adapter = PendingCheckinAdapter(
            onItemClick = { model -> showTaskDetail(model.item) },
            onCompleteClick = { model -> markComplete(model.item.id) },
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

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@PendingCheckinFragment.adapter
        }

        val swipeCallback = PendingSwipeCallback(
            adapter = adapter,
            onComplete = { position ->
                val model = adapter.getItemAt(position)
                markComplete(model.item.id)
            },
            onSkip = { position ->
                val model = adapter.getItemAt(position)
                markComplete(model.item.id)
                showSnackbar(getString(R.string.skip_success))
            },
            onEdit = { position ->
                val model = adapter.getItemAt(position)
                openEditDialog(model.item.id)
                adapter.notifyItemChanged(position)
            },
            onDelete = { position ->
                val model = adapter.getItemAt(position)
                showDeleteConfirmation(model.item, position)
            }
        )
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerView)
    }

    private fun observeViewModel() {
        viewModel.allPendingItems.observe(viewLifecycleOwner) { items ->
            binding.swipeRefreshLayout.isRefreshing = false
            adapter.submitList(items) {
                binding.recyclerView.requestLayout()
            }
            if (items.isEmpty()) {
                binding.emptyView.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.emptyView.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun markComplete(itemId: Int) {
        viewModel.markCheckined(itemId)
        showSnackbar(getString(R.string.checkin_success))
    }

    private fun showTaskDetail(item: com.alosir.task.data.entity.CheckinItem) {
        val cycle = com.alosir.task.util.CycleCalculator.getCycleShortDescription(item)
        val nextDate = com.alosir.task.util.CycleCalculator.getNextCheckinDate(item)
            ?.let { com.alosir.task.util.CycleCalculator.formatRelativeDate(it) }
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

    private fun showDeleteConfirmation(item: com.alosir.task.data.entity.CheckinItem, position: Int) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_item)
            .setMessage(R.string.delete_recurring_confirmation)
            .setPositiveButton(R.string.button_confirm) { _, _ ->
                viewModel.deleteItem(item)
            }
            .setNegativeButton(R.string.button_cancel) { _, _ ->
                adapter.notifyItemChanged(position)
            }
            .setOnCancelListener {
                adapter.notifyItemChanged(position)
            }
            .show()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
