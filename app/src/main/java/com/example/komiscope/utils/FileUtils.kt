package com.example.komiscope.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

/**
 * Utility functions for file operations
 */
object FileUtils {

    /**
     * Get filename from URI using ContentResolver
     */
    fun getFileName(context: Context, uri: Uri): String? {
        var fileName: String? = null

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }

        // Fallback to last path segment if query fails
        return fileName ?: uri.lastPathSegment
    }

    /**
     * Check if filename has CBZ or ZIP extension
     */
    fun isCbzFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension == "cbz" || extension == "zip"
    }

    /**
     * Format file size in human-readable format
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    /**
     * Get file size from URI
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        var size = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }

        return size
    }
}