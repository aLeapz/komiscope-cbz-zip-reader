package com.example.komiscope.ui.reader

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.komiscope.R
import com.example.komiscope.data.model.MangaPage
import com.example.komiscope.databinding.ItemPageBinding
import com.github.chrisbanes.photoview.PhotoView

/**
 * Adapter for ViewPager2 to display comic pages
 * Uses Glide for efficient image loading
 * Uses PhotoView for pinch zoom functionality
 */
class PageAdapter(
    private val pages: List<MangaPage>
) : RecyclerView.Adapter<PageAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    override fun getItemCount(): Int = pages.size

    inner class PageViewHolder(
        private val binding: ItemPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(page: MangaPage) {
            val photoView = binding.imageView as PhotoView

            // Configure PhotoView
            photoView.apply {
                // Set scale type
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER

                // Set zoom limits
                minimumScale = 1f
                mediumScale = 2.5f
                maximumScale = 5f

                // Enable zoom
                isZoomable = true
            }

            // Load image using Glide
            Glide.with(binding.root.context)
                .load(page.file)
                .placeholder(R.drawable.ic_loading) // Optional: add loading placeholder
                .error(R.drawable.ic_error) // Optional: add error placeholder
                .diskCacheStrategy(DiskCacheStrategy.NONE) // Don't cache temp files
                .into(photoView)

            // Optional: show page number for debugging
            // binding.tvPageDebug.text = "Page ${page.pageNumber + 1}"
        }
    }
}