package com.example.komiscope.ui.loader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.komiscope.data.model.MangaPage
import com.example.komiscope.data.zip.ZipExtractor
import com.example.komiscope.databinding.ActivityReaderLoaderBinding
import com.example.komiscope.ui.main.MainActivity
import com.example.komiscope.ui.reader.ReaderActivity
import com.example.komiscope.utils.showToast
import kotlinx.coroutines.launch

/**
 * Loading screen that extracts CBZ file and prepares for reading
 * Shows progress and navigates to ReaderActivity when done
 * Displays visual error state with image if extraction fails
 */
class ReaderLoaderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReaderLoaderBinding
    private lateinit var zipExtractor: ZipExtractor

    private var fileUri: Uri? = null
    private var fileName: String? = null
    private var startPage: Int = 0

    companion object {
        const val EXTRA_FILE_URI = "file_uri"
        const val EXTRA_FILE_NAME = "file_name"
        const val EXTRA_START_PAGE = "start_page"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReaderLoaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        zipExtractor = ZipExtractor(this)

        // Get extras from intent
        val fileUriString = intent.getStringExtra(EXTRA_FILE_URI)
        fileName = intent.getStringExtra(EXTRA_FILE_NAME)
        startPage = intent.getIntExtra(EXTRA_START_PAGE, 0)

        if (fileUriString == null || fileName == null) {
            showErrorState(
                message = "File Not Found",
                detail = "Could not load the selected file"
            )
            return
        }

        fileUri = Uri.parse(fileUriString)

        binding.tvLoadingMessage.text = "Loading $fileName..."

        // Setup back to home button
        binding.btnBackHome.setOnClickListener {
            backToHome()
        }

        // Start extraction
        extractAndLoad(fileUri!!, fileName!!, startPage)
    }

    /**
     * Extract CBZ file and navigate to reader
     */
    private fun extractAndLoad(uri: Uri, fileName: String, startPage: Int) {
        // Show loading state
        showLoadingState()

        lifecycleScope.launch {
            try {
                binding.tvLoadingMessage.text = "Extracting images..."

                val result = zipExtractor.extractPages(uri)

                result.onSuccess { pages ->
                    if (pages.isEmpty()) {
                        return@launch
                    }

                    binding.tvLoadingMessage.text = "Found ${pages.size} pages"

                    // Navigate to reader
                    openReader(uri, fileName, pages, startPage)

                }.onFailure { error ->
                    showErrorState(
                        message = "No Images Found",
                        detail = "This file doesn't contain any readable images"
                    )
                }

            } catch (e: Exception) {
                showErrorState(
                    message = "Something Went Wrong",
                    detail = "Unable to read this file"
                )
            }
        }
    }

    /**
     * Navigate back to MainActivity
     */
    private fun backToHome() {
        val intent = Intent(this, MainActivity::class.java).apply {
            // Clear back stack so user can't go back to error state
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    /**
     * Show loading state (progress bar + message)
     */
    private fun showLoadingState() {
        binding.apply {
            progressBar.visibility = View.VISIBLE
            tvLoadingMessage.visibility = View.VISIBLE
            errorContainer.visibility = View.GONE
        }
    }

    /**
     * Show error state with image (hide loading, show error image)
     */
    private fun showErrorState(message: String, detail: String) {
        binding.apply {
            // Hide loading
            progressBar.visibility = View.GONE
            tvLoadingMessage.visibility = View.GONE

            // Show error container with image
            errorContainer.visibility = View.VISIBLE
            tvErrorMessage.text = message
            tvErrorDetail.text = detail

            // Show back to home button
            btnBackHome.visibility = View.VISIBLE
        }

        // Also show toast for quick feedback
        showToast(message)
    }

    /**
     * Get user-friendly error message
     */
    private fun getErrorMessage(error: Throwable): String {
        return when {
            error.message?.contains("Permission", ignoreCase = true) == true ->
                "Permission denied. Please try another file."

            error.message?.contains("not found", ignoreCase = true) == true ->
                "File not found or has been moved."

            else ->
                "Unable to open this file. Please try another one."
        }
    }

    /**
     * Open ReaderActivity with extracted pages
     */
    private fun openReader(
        uri: Uri,
        fileName: String,
        pages: List<MangaPage>,
        startPage: Int
    ) {
        // Store page paths in static variable for passing to ReaderActivity
        ReaderActivity.currentPages = pages

        val intent = Intent(this, ReaderActivity::class.java).apply {
            putExtra(ReaderActivity.EXTRA_FILE_URI, uri.toString())
            putExtra(ReaderActivity.EXTRA_FILE_NAME, fileName)
            putExtra(ReaderActivity.EXTRA_START_PAGE, startPage)
            putExtra(ReaderActivity.EXTRA_TOTAL_PAGES, pages.size)
        }

        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't clear temp files here as ReaderActivity still needs them
    }
}