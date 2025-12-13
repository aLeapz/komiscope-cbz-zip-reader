package com.example.komiscope.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.komiscope.data.model.RecentFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Manages app preferences using DataStore
 * Stores reading progress and recent files
 */
class PreferencesManager(private val context: Context) {

    companion object {
        private const val DATASTORE_NAME = "komiscope_preferences"
        private const val MAX_RECENT_FILES = 10

        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = DATASTORE_NAME
        )
    }

    /**
     * Save reading progress for a file
     */
    suspend fun saveReadingProgress(
        fileName: String,
        uri: String,
        currentPage: Int,
        totalPages: Int
    ) {
        val fileKey = sanitizeKey(fileName)

        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey("${fileKey}_uri")] = uri
            prefs[intPreferencesKey("${fileKey}_page")] = currentPage
            prefs[intPreferencesKey("${fileKey}_total")] = totalPages
            prefs[longPreferencesKey("${fileKey}_time")] = System.currentTimeMillis()
        }
    }

    /**
     * Get reading progress for a specific file
     */
    fun getReadingProgress(fileName: String): Flow<RecentFile?> {
        val fileKey = sanitizeKey(fileName)

        return context.dataStore.data.map { prefs ->
            val uri = prefs[stringPreferencesKey("${fileKey}_uri")]
            val page = prefs[intPreferencesKey("${fileKey}_page")]
            val total = prefs[intPreferencesKey("${fileKey}_total")]
            val time = prefs[longPreferencesKey("${fileKey}_time")]

            if (uri != null && page != null && total != null) {
                RecentFile(
                    fileName = fileName,
                    uri = uri,
                    lastPage = page,
                    totalPages = total,
                    lastOpened = time ?: System.currentTimeMillis()
                )
            } else {
                null
            }
        }
    }

    /**
     * Get all recent files sorted by last opened time
     */
    fun getAllRecentFiles(): Flow<List<RecentFile>> {
        return context.dataStore.data.map { prefs ->
            val recentFiles = mutableListOf<RecentFile>()
            val processedKeys = mutableSetOf<String>()

            // Group preferences by file key
            prefs.asMap().forEach { (key, value) ->
                val keyName = key.name

                if (keyName.endsWith("_uri") && value is String) {
                    val fileKey = keyName.removeSuffix("_uri")

                    if (fileKey !in processedKeys) {
                        processedKeys.add(fileKey)

                        val fileName = desanitizeKey(fileKey)
                        val uri = value
                        val page = prefs[intPreferencesKey("${fileKey}_page")] ?: 0
                        val total = prefs[intPreferencesKey("${fileKey}_total")] ?: 0
                        val time = prefs[longPreferencesKey("${fileKey}_time")]
                            ?: System.currentTimeMillis()

                        recentFiles.add(
                            RecentFile(
                                fileName = fileName,
                                uri = uri,
                                lastPage = page,
                                totalPages = total,
                                lastOpened = time
                            )
                        )
                    }
                }
            }

            // Sort by last opened time (most recent first) and limit to MAX_RECENT_FILES
            recentFiles.sortedByDescending { it.lastOpened }.take(MAX_RECENT_FILES)
        }
    }

    /**
     * Delete reading progress for a file
     */
    suspend fun deleteReadingProgress(fileName: String) {
        val fileKey = sanitizeKey(fileName)

        context.dataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey("${fileKey}_uri"))
            prefs.remove(intPreferencesKey("${fileKey}_page"))
            prefs.remove(intPreferencesKey("${fileKey}_total"))
            prefs.remove(longPreferencesKey("${fileKey}_time"))
        }
    }

    /**
     * Clear all preferences
     */
    suspend fun clearAll() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    /**
     * Sanitize filename to be used as preference key
     * Replace special characters with underscores
     */
    private fun sanitizeKey(fileName: String): String {
        return fileName.replace(Regex("[^A-Za-z0-9]"), "_")
    }

    /**
     * Restore original filename from sanitized key
     * This is approximate since we lose the original special characters
     */
    private fun desanitizeKey(key: String): String {
        // We can't perfectly restore the original, but we can make it readable
        return key.replace("_", " ")
    }
}