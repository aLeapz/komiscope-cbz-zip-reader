package com.example.komiscope.data.model

import java.io.File

/**
 * Represents a single page in the manga/comic
 * @param file The image file for this page
 * @param pageNumber The sequential page number (0-indexed)
 */
data class MangaPage(
    val file: File,
    val pageNumber: Int
) {
    val fileName: String
        get() = file.name

    val filePath: String
        get() = file.absolutePath
}