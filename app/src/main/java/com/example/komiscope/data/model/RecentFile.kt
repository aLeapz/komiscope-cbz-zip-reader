package com.example.komiscope.data.model

/**
 * Represents a recently opened comic file
 * @param fileName Name of the comic file
 * @param uri URI string of the file
 * @param lastPage Last page the user was reading (0-indexed)
 * @param totalPages Total number of pages in the comic
 * @param lastOpened Timestamp when the file was last opened
 */
data class RecentFile(
    val fileName: String,
    val uri: String,
    val lastPage: Int = 0,
    val totalPages: Int = 0,
    val lastOpened: Long = System.currentTimeMillis()
) {
    /**
     * Calculate reading progress percentage
     */
    val progressPercentage: Int
        get() = if (totalPages > 0) {
            ((lastPage + 1) * 100 / totalPages).coerceIn(0, 100)
        } else 0

    /**
     * Check if the comic has been started
     */
    val isStarted: Boolean
        get() = lastPage > 0
}