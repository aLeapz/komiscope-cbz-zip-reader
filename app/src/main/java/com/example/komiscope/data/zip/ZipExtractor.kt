package com.example.komiscope.data.zip

import android.content.Context
import android.net.Uri
import com.example.komiscope.data.model.MangaPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * Handles extraction of CBZ (ZIP) files and manages temporary image files
 */
class ZipExtractor(private val context: Context) {

    companion object {
        private const val TEMP_DIR = "temp_manga"
        private val SUPPORTED_EXTENSIONS = listOf("jpg", "jpeg", "png", "gif", "webp")
    }

    /**
     * Extract images from a CBZ file and return sorted list of pages
     * @param uri URI of the CBZ file
     * @return List of MangaPage objects sorted by filename
     */
    suspend fun extractPages(uri: Uri): Result<List<MangaPage>> = withContext(Dispatchers.IO) {
        try {
            // Clear previous temporary files
            clearTempDirectory()

            // Create temp directory
            val tempDir = getTempDirectory()

            // Extract ZIP contents
            val extractedFiles = mutableListOf<File>()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipStream ->
                    var entry = zipStream.nextEntry

                    while (entry != null) {
                        val fileName = entry.name

                        // Skip directories and non-image files
                        if (!entry.isDirectory && isImageFile(fileName)) {
                            val file = File(tempDir, getFileNameFromPath(fileName))

                            FileOutputStream(file).use { outputStream ->
                                zipStream.copyTo(outputStream)
                            }

                            extractedFiles.add(file)
                        }

                        zipStream.closeEntry()
                        entry = zipStream.nextEntry
                    }
                }
            }

            // Sort files naturally (handle names like page1, page2, page10 correctly)
            val sortedFiles = extractedFiles.sortedWith(naturalOrderComparator())

            // Create MangaPage objects
            val pages = sortedFiles.mapIndexed { index, file ->
                MangaPage(file, index)
            }

            if (pages.isEmpty()) {
                Result.failure(Exception("No valid image files found in the CBZ archive"))
            } else {
                Result.success(pages)
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clear all temporary files
     */
    fun clearTempDirectory() {
        val tempDir = getTempDirectory()
        if (tempDir.exists()) {
            tempDir.listFiles()?.forEach { it.delete() }
        }
    }

    /**
     * Get or create temp directory
     */
    private fun getTempDirectory(): File {
        val dir = File(context.cacheDir, TEMP_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Check if file is a supported image format
     */
    private fun isImageFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in SUPPORTED_EXTENSIONS
    }

    /**
     * Extract filename from path (handles paths with slashes)
     */
    private fun getFileNameFromPath(path: String): String {
        return path.substringAfterLast('/')
    }

    /**
     * Natural order comparator for sorting filenames
     * Handles numeric sequences properly (e.g., page2 comes before page10)
     */
    private fun naturalOrderComparator(): Comparator<File> {
        return Comparator { f1, f2 ->
            val name1 = f1.nameWithoutExtension
            val name2 = f2.nameWithoutExtension

            // Extract numeric parts for comparison
            val num1 = name1.filter { it.isDigit() }.toIntOrNull() ?: 0
            val num2 = name2.filter { it.isDigit() }.toIntOrNull() ?: 0

            if (num1 != num2) {
                num1.compareTo(num2)
            } else {
                name1.compareTo(name2, ignoreCase = true)
            }
        }
    }
}