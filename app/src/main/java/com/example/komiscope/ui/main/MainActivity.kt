package com.example.komiscope.ui.main

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.komiscope.R
import com.example.komiscope.data.datastore.PreferencesManager
import com.example.komiscope.data.model.RecentFile
import com.example.komiscope.databinding.ActivityMainBinding
import com.example.komiscope.ui.loader.ReaderLoaderActivity
import com.example.komiscope.utils.FileUtils
import com.example.komiscope.utils.showToast
import kotlinx.coroutines.launch

/**
 * Main screen of the app
 * Shows recent files and allows opening new CBZ files
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var recentFilesAdapter: RecentFilesAdapter

    /**
     * File picker launcher using Storage Access Framework
     */
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedFile(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(this)

        setupUI()
        setupRecyclerView()
        loadRecentFiles()
    }

    /**
     * Setup UI components and click listeners
     */
    private fun setupUI() {
        binding.btnOpenFile.setOnClickListener {
            openFilePicker()
        }

        // Show app info
        binding.tvAppVersion.text = "Version 1.0"
    }

    /**
     * Setup RecyclerView for recent files
     */
    private fun setupRecyclerView() {
        recentFilesAdapter = RecentFilesAdapter(
            onItemClick = { recentFile ->
                openRecentFile(recentFile)
            },
            onDeleteClick = { recentFile ->
                confirmDeleteRecentFile(recentFile)
            }
        )

        binding.rvRecentFiles.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = recentFilesAdapter
        }
    }

    /**
     * Load recent files from DataStore
     */
    private fun loadRecentFiles() {
        lifecycleScope.launch {
            preferencesManager.getAllRecentFiles().collect { recentFiles ->
                if (recentFiles.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.rvRecentFiles.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.rvRecentFiles.visibility = View.VISIBLE
                    recentFilesAdapter.submitList(recentFiles)
                }
            }
        }
    }

    /**
     * Open file picker to select CBZ file
     */
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/zip",
                "application/x-cbz"
            ))
        }

        try {
            filePickerLauncher.launch(intent)
        } catch (e: Exception) {
            showToast("Error opening file picker: ${e.message}")
        }
    }

    /**
     * Handle file selected from picker
     */
    private fun handleSelectedFile(uri: Uri) {
        val fileName = FileUtils.getFileName(this, uri)

        if (fileName == null) {
            showToast("Could not get file name")
            return
        }

        if (!FileUtils.isCbzFile(fileName)) {
            showToast("Please select a valid CBZ or ZIP file")
            return
        }

        // Take persistable permission for the URI
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            // Permission may not be available for all URIs
        }

        openLoaderActivity(uri, fileName)
    }

    /**
     * Open a recent file
     */
    private fun openRecentFile(recentFile: RecentFile) {
        val uri = Uri.parse(recentFile.uri)
        openLoaderActivity(uri, recentFile.fileName, recentFile.lastPage)
    }

    /**
     * Navigate to ReaderLoaderActivity
     */
    private fun openLoaderActivity(uri: Uri, fileName: String, startPage: Int = 0) {
        val intent = Intent(this, ReaderLoaderActivity::class.java).apply {
            putExtra(ReaderLoaderActivity.EXTRA_FILE_URI, uri.toString())
            putExtra(ReaderLoaderActivity.EXTRA_FILE_NAME, fileName)
            putExtra(ReaderLoaderActivity.EXTRA_START_PAGE, startPage)
        }
        startActivity(intent)
    }

    /**
     * Confirm before deleting recent file
     */
    private fun confirmDeleteRecentFile(recentFile: RecentFile) {
        AlertDialog.Builder(this)
            .setTitle("Remove from Recent")
            .setMessage("Remove \"${recentFile.fileName}\" from recent files?")
            .setPositiveButton("Remove") { _, _ ->
                deleteRecentFile(recentFile)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Delete recent file from DataStore
     */
    private fun deleteRecentFile(recentFile: RecentFile) {
        lifecycleScope.launch {
            try {
                preferencesManager.deleteReadingProgress(recentFile.fileName)
                showToast("Removed from recent files")
            } catch (e: Exception) {
                showToast("Error removing file: ${e.message}")
            }
        }
    }
}