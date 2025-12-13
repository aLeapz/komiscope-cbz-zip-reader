package com.example.komiscope.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.komiscope.data.model.RecentFile
import com.example.komiscope.databinding.ItemRecentFileBinding
import com.example.komiscope.utils.toRelativeTime

/**
 * Adapter for displaying recent files in RecyclerView
 */
class RecentFilesAdapter(
    private val onItemClick: (RecentFile) -> Unit,
    private val onDeleteClick: (RecentFile) -> Unit
) : ListAdapter<RecentFile, RecentFilesAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentFileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemRecentFileBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(recentFile: RecentFile) {
            binding.apply {
                tvFileName.text = recentFile.fileName
                tvPageInfo.text = "Page ${recentFile.lastPage + 1} of ${recentFile.totalPages}"
                tvLastOpened.text = recentFile.lastOpened.toRelativeTime()

                // Set progress bar
                progressBar.max = 100
                progressBar.progress = recentFile.progressPercentage
                tvProgress.text = "${recentFile.progressPercentage}%"

                // Click listeners
                root.setOnClickListener {
                    onItemClick(recentFile)
                }

                btnDelete.setOnClickListener {
                    onDeleteClick(recentFile)
                }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<RecentFile>() {
        override fun areItemsTheSame(oldItem: RecentFile, newItem: RecentFile): Boolean {
            return oldItem.fileName == newItem.fileName
        }

        override fun areContentsTheSame(oldItem: RecentFile, newItem: RecentFile): Boolean {
            return oldItem == newItem
        }
    }
}