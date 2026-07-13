package com.alosir.task.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.alosir.task.databinding.ItemAppListBinding

class AppListAdapter(
    private val onAppClick: (android.content.pm.ApplicationInfo, String) -> Unit
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>(), Filterable {

    private var allApps: List<android.content.pm.ApplicationInfo> = emptyList()
    private var allAppNames: List<String> = emptyList()
    private var filteredApps: List<android.content.pm.ApplicationInfo> = emptyList()
    private var filteredAppNames: List<String> = emptyList()

    fun submitData(apps: List<android.content.pm.ApplicationInfo>, appNames: List<String>) {
        // 按应用名排序
        val sortedPairs = apps.zip(appNames).sortedBy { it.second.lowercase() }
        allApps = sortedPairs.map { it.first }
        allAppNames = sortedPairs.map { it.second }
        filteredApps = allApps
        filteredAppNames = allAppNames
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(filteredApps[position], filteredAppNames[position])
    }

    override fun getItemCount(): Int = filteredApps.size

    fun getAvailableInitials(): List<String> {
        return filteredAppNames
            .map { name ->
                val first = name.firstOrNull()?.uppercaseChar()
                when {
                    first == null -> "#"
                    first in 'A'..'Z' -> first.toString()
                    else -> "#"
                }
            }
            .distinct()
            .sortedWith(compareBy({ it == "#" }, { it }))
    }

    fun findPositionByLetter(letter: String): Int {
        return filteredAppNames.indexOfFirst { name ->
            val first = name.firstOrNull()?.uppercaseChar()
            val initial = when {
                first == null -> "#"
                first in 'A'..'Z' -> first.toString()
                else -> "#"
            }
            initial == letter
        }
    }

    inner class ViewHolder(
        private val binding: ItemAppListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(appInfo: android.content.pm.ApplicationInfo, appName: String) {
            binding.appName.text = appName

            try {
                val icon = binding.root.context.packageManager.getApplicationIcon(appInfo)
                binding.appIcon.setImageDrawable(icon)
            } catch (e: Exception) {
                binding.appIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            binding.root.setOnClickListener {
                onAppClick(appInfo, appName)
            }
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.lowercase() ?: ""

                val filteredList = if (query.isEmpty()) {
                    allApps
                } else {
                    allApps.filterIndexed { index, _ ->
                        allAppNames[index].lowercase().contains(query)
                    }
                }

                val filteredNameList = if (query.isEmpty()) {
                    allAppNames
                } else {
                    allAppNames.filter { it.lowercase().contains(query) }
                }

                return FilterResults().apply {
                    values = Pair(filteredList, filteredNameList)
                }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                val (apps, names) = results?.values as? Pair<List<android.content.pm.ApplicationInfo>, List<String>>
                    ?: Pair(emptyList(), emptyList())

                filteredApps = apps
                filteredAppNames = names
                notifyDataSetChanged()
            }
        }
    }
}
