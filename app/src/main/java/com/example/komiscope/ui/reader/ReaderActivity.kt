package com.example.komiscope.ui.reader

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import androidx.activity.OnBackPressedCallback
import com.example.komiscope.data.datastore.PreferencesManager
import com.example.komiscope.data.model.MangaPage
import com.example.komiscope.data.zip.ZipExtractor
import com.example.komiscope.databinding.ActivityReaderBinding
import com.example.komiscope.utils.showToast
import kotlinx.coroutines.launch

/**
 * Main reader activity for displaying comic pages
 * Uses ViewPager2 for page swiping and saves progress
 */
class ReaderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReaderBinding
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var zipExtractor: ZipExtractor
    private lateinit var pagerAdapter: PageAdapter

    private var fileUri: Uri? = null
    private var fileName: String = ""
    private var totalPages: Int = 0
    private var currentPage: Int = 0

    companion object {
        const val EXTRA_FILE_URI = "file_uri"
        const val EXTRA_FILE_NAME = "file_name"
        const val EXTRA_START_PAGE = "start_page"
        const val EXTRA_TOTAL_PAGES = "total_pages"

        // Static variable to pass pages between activities
        // Alternative: use Parcelable or serialize
        var currentPages: List<MangaPage>? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Your custom logic here
                saveProgress()
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })

        supportActionBar?.hide()

        preferencesManager = PreferencesManager(this)
        zipExtractor = ZipExtractor(this)

        // Get intent extras
        fileUri = intent.getStringExtra(EXTRA_FILE_URI)?.let { Uri.parse(it) }
        fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: ""
        currentPage = intent.getIntExtra(EXTRA_START_PAGE, 0)
        totalPages = intent.getIntExtra(EXTRA_TOTAL_PAGES, 0)

        if (fileUri == null || currentPages == null) {
            showToast("Error loading comic")
            finish()
            return
        }

        setupViewPager(currentPages!!)
        setupUI()
    }

    /**
     * Setup ViewPager2 with pages
     */
    private fun setupViewPager(pages: List<MangaPage>) {
        pagerAdapter = PageAdapter(pages)

        binding.viewPager.apply {
            adapter = pagerAdapter

            // Set initial page
            setCurrentItem(currentPage, false)

            // Disable user input initially (will be controlled by PhotoView)
            isUserInputEnabled = true

            // Listen to page changes
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    currentPage = position
                    updatePageInfo()
                    saveProgress()
                }
            })
        }

        updatePageInfo()
    }

    /**
     * Setup UI controls
     */
    private fun setupUI() {
        binding.apply {
            // Toggle UI visibility on tap
            viewPager.setOnClickListener {
                toggleUIVisibility()
            }

            // Navigation buttons
            btnPrevious.setOnClickListener {
                if (currentPage > 0) {
                    viewPager.currentItem = currentPage - 1
                }
            }

            btnNext.setOnClickListener {
                if (currentPage < totalPages - 1) {
                    viewPager.currentItem = currentPage + 1
                }
            }

            btnClose.setOnClickListener {
                finish()
            }

            tvFileName.text = fileName
        }
    }

    /**
     * Update page counter display
     */
    private fun updatePageInfo() {
        binding.tvPageNumber.text = "${currentPage + 1} / $totalPages"

        // Update button states
        binding.btnPrevious.isEnabled = currentPage > 0
        binding.btnNext.isEnabled = currentPage < totalPages - 1
    }

    /**
     * Toggle UI controls visibility
     */
    private fun toggleUIVisibility() {
        val newVisibility = if (binding.topBar.visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }

        binding.topBar.visibility = newVisibility
        binding.bottomBar.visibility = newVisibility
    }

    /**
     * Save reading progress to DataStore
     */
    private fun saveProgress() {
        lifecycleScope.launch {
            try {
                preferencesManager.saveReadingProgress(
                    fileName = fileName,
                    uri = fileUri.toString(),
                    currentPage = currentPage,
                    totalPages = totalPages
                )
            } catch (e: Exception) {
                // Silent fail - don't interrupt reading experience
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Save progress when user leaves
        saveProgress()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear static pages reference
        currentPages = null

        // Clear temporary extracted files
        zipExtractor.clearTempDirectory()
    }
}