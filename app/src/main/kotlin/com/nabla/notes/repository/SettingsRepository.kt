package com.nabla.notes.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nabla.notes.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages persistent app settings using DataStore<Preferences>.
 *
 * Settings:
 *  - folderPath: relative OneDrive folder path (e.g. "Documents/notes")
 *  - folderId:   cached OneDrive item ID for the folder
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_FOLDER_PATH = stringPreferencesKey("folder_path")
        private val KEY_FOLDER_ID = stringPreferencesKey("folder_id")
    }

    /** Observe current settings as a Flow. */
    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            folderPath = prefs[KEY_FOLDER_PATH] ?: "",
            folderId = prefs[KEY_FOLDER_ID] ?: "root"
        )
    }

    /** Save updated settings. */
    suspend fun saveSettings(settings: AppSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_FOLDER_PATH] = settings.folderPath
            prefs[KEY_FOLDER_ID] = settings.folderId
        }
    }
}
