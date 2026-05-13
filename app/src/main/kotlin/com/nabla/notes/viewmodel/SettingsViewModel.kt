package com.nabla.notes.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabla.notes.auth.MsalManager
import com.nabla.notes.model.AppSettings
import com.nabla.notes.repository.OneDriveRepository
import com.nabla.notes.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the Settings screen. */
sealed class SettingsUiState {
    object Idle : SettingsUiState()
    object Loading : SettingsUiState()
    object Saved : SettingsUiState()
    data class FolderPicker(val folders: List<Pair<String, String>>) : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}

/**
 * ViewModel for the Settings screen.
 *
 * Responsibilities:
 *  - Read/write app settings via [SettingsRepository]
 *  - Sign out via [MsalManager]
 *  - Browse OneDrive folders for folder picker
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val msalManager: MsalManager,
    private val oneDriveRepository: OneDriveRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Folder navigation stack: List<Pair<folderId, folderName>>
    private val _folderStack = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val folderStack: StateFlow<List<Pair<String, String>>> = _folderStack.asStateFlow()

    val currentPath: StateFlow<String> = _folderStack.map { stack ->
        if (stack.isEmpty()) "OneDrive" else "OneDrive / " + stack.joinToString(" / ") { it.second }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "OneDrive")

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings()
    )

    // ─── Actions ─────────────────────────────────────────────────────────────────

    /** Save updated folder path and resolved folder ID. */
    fun saveSettings(folderPath: String, folderId: String) {
        viewModelScope.launch {
            settingsRepository.saveSettings(
                AppSettings(folderPath = folderPath, folderId = folderId)
            )
            _uiState.value = SettingsUiState.Saved
            kotlinx.coroutines.delay(1500)
            _uiState.value = SettingsUiState.Idle
        }
    }

    /** Browse OneDrive folders to pick a target folder. */
    fun loadFolders(parentId: String = "root", activity: Activity) {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            oneDriveRepository.listFolders(parentId, activity).fold(
                onSuccess = { folders -> _uiState.value = SettingsUiState.FolderPicker(folders) },
                onFailure = { e -> _uiState.value = SettingsUiState.Error(e.message ?: "Failed") }
            )
        }
    }

    /** Navigate into a subfolder; updates the stack and loads its contents. */
    fun navigateIntoFolder(folderId: String, folderName: String, activity: Activity) {
        _folderStack.value = _folderStack.value + (folderId to folderName)
        loadFolders(parentId = folderId, activity = activity)
    }

    /** Navigate up one level in the folder stack. */
    fun navigateUp(activity: Activity) {
        val stack = _folderStack.value.dropLast(1)
        _folderStack.value = stack
        val parentId = stack.lastOrNull()?.first ?: "root"
        loadFolders(parentId = parentId, activity = activity)
    }

    /** Save the current folder level as the selected folder and reset nav. */
    fun selectCurrentFolder() {
        val stack = _folderStack.value
        val folderId = stack.lastOrNull()?.first ?: "root"
        val folderPath = if (stack.isEmpty()) "/" else "/" + stack.joinToString("/") { it.second }
        saveSettings(folderPath, folderId)
        _folderStack.value = emptyList()
    }

    /** Reset folder navigation state (e.g. on dialog dismiss). */
    fun resetFolderNav() {
        _folderStack.value = emptyList()
    }

    /** Sign out the current Microsoft account. */
    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            msalManager.signOut()
            onComplete()
        }
    }

    /** Dismiss any transient state back to Idle. */
    fun dismissState() {
        _uiState.value = SettingsUiState.Idle
    }
}
