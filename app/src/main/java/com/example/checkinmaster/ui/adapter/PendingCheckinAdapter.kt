package com.alosir.task.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.content.Context
import com.alosir.task.R
import com.alosir.task.data.entity.CheckinItem
import com.alosir.task.data.entity.CheckinType
import com.alosir.task.databinding.ItemCheckinListBinding
import com.alosir.task.ui.viewmodel.CheckinListViewModel
import com.alosir.task.util.CycleCalculator
import com.alosir.task.util.IconManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PendingCheckinAdapter(
    private val onItemClick: (CheckinListViewModel.PendingItemUiModel) -> Unit,
    private val onCompleteClick: (CheckinListViewModel.PendingItemUiModel) -> Unit,
    private val onOpenAppClick: (CheckinListViewModel.PendingItemUiModel) -> Unit,
    private val onOpenWebsiteClick: (CheckinListViewModel.PendingItemUiModel) -> Unit
) : ListAdapter<CheckinListViewModel.PendingItemUiModel, PendingCheckinAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCheckinListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getItemAt(position: Int): CheckinListViewModel.PendingItemUiModel = getItem(position)

    inner class ViewHolder(
        private val binding: ItemCheckinListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(pos))
                }
            }
        }

        fun bind(model: CheckinListViewModel.PendingItemUiModel) {
            val item = model.item
            val context = binding.root.context

            binding.nameText.text = item.name
            if (item.description.isNullOrEmpty()) {
                binding.descText.visibility = View.GONE
            } else {
                binding.descText.text = item.description
                binding.descText.visibility = View.VISIBLE
            }

            binding.cycleText.text = CycleCalculator.getCycleShortDescription(item)
            binding.nextDateText.text = CycleCalculator.formatRelativeDate(model.nextDate)

            if (item.reminderTime.isNullOrEmpty()) {
                binding.reminderTimeText.visibility = View.GONE
            } else {
                binding.reminderTimeText.text = item.reminderTime
                binding.reminderTimeText.visibility = View.VISIBLE
            }

            loadTypeIcon(context, item)

            val isToday = dateFormat.format(model.nextDate) == dateFormat.format(Date())
            if (isToday) {
                binding.actionContainer.visibility = View.VISIBLE
                binding.actionCompleteButton.visibility = View.VISIBLE
                binding.actionCompleteButton.setOnClickListener { handleCompleteClick(model) }
            } else {
                binding.actionContainer.visibility = View.GONE
            }
        }

        private fun handleCompleteClick(model: CheckinListViewModel.PendingItemUiModel) {
            val item = model.item
            when {
                item.type == CheckinType.APP && !item.packageName.isNullOrEmpty() -> {
                    onOpenAppClick(model)
                    onCompleteClick(model)
                }
                item.type == CheckinType.WEBSITE && !item.url.isNullOrEmpty() -> {
                    onOpenWebsiteClick(model)
                    onCompleteClick(model)
                }
                else -> onCompleteClick(model)
            }
        }

        private fun loadTypeIcon(context: Context, item: CheckinItem) {
            when (item.type) {
                CheckinType.APP -> {
                    val packageName = item.packageName
                    if (!packageName.isNullOrEmpty()) {
                        val icon = IconManager.loadAppIcon(context, packageName)
                        if (icon != null) {
                            binding.iconImage.setImageDrawable(icon)
                        } else {
                            binding.iconImage.setImageResource(R.drawable.ic_default_app)
                        }
                    } else {
                        binding.iconImage.setImageResource(R.drawable.ic_default_app)
                    }
                }
                CheckinType.WEBSITE -> {
                    binding.iconImage.setImageResource(R.drawable.ic_default_website)
                }
                CheckinType.OTHER -> {
                    binding.iconImage.setImageResource(R.drawable.ic_default_other)
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CheckinListViewModel.PendingItemUiModel>() {
        override fun areItemsTheSame(
            oldItem: CheckinListViewModel.PendingItemUiModel,
            newItem: CheckinListViewModel.PendingItemUiModel
        ): Boolean {
            return oldItem.item.id == newItem.item.id
        }

        override fun areContentsTheSame(
            oldItem: CheckinListViewModel.PendingItemUiModel,
            newItem: CheckinListViewModel.PendingItemUiModel
        ): Boolean {
            return oldItem == newItem
        }
    }
}
