package com.alosir.task.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alosir.task.databinding.ItemStreakRankBinding
import com.alosir.task.util.StatisticsCalculator

class StreakRankAdapter : RecyclerView.Adapter<StreakRankAdapter.ViewHolder>() {

    private val items = mutableListOf<StatisticsCalculator.StreakInfo>()

    fun submitList(newItems: List<StatisticsCalculator.StreakInfo>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStreakRankBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position + 1)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(
        private val binding: ItemStreakRankBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(info: StatisticsCalculator.StreakInfo, rank: Int) {
            binding.rankNumber.text = rank.toString()
            binding.itemName.text = info.item.name
            binding.streakDays.text = "${info.streak} 天"
        }
    }
}
