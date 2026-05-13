package com.nabla.notes.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabla.notes.model.NoteFile
import com.nabla.notes.repository.OneDriveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the editor screen. */
sealed class EditorUiState {
    object Idle : EditorUiState()
    object Loading : EditorUiState()
    object Ready : EditorUiState()
    object Saving : EditorUiState()
    object Saved : EditorUiState()
    data class Error(val message: String) : EditorUiState()
}

/**
 * ViewModel for the note editor.
 *
 * Responsibilities:
 *  - Load file content from OneDrive
 *  - Track edits in memory
 *  - Save file content back to OneDrive
 *  - Toggle between edit mode and markdown preview
 */
@HiltViewModel
class EditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val oneDriveRepository: OneDriveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditorUiState>(EditorUiState.Idle)
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    /** Current text content in the editor. */
    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    /** Whether we are in markdown preview mode (vs raw text edit mode). */
    private val _isMarkdownPreview = MutableStateFlow(false)
    val isMarkdownPreview: StateFlow<Boolean> = _isMarkdownPreview.asStateFlow()

    /** The file currently being edited. */
    private val _currentFile = MutableStateFlow<NoteFile?>(null)
    val currentFile: StateFlow<NoteFile?> = _currentFile.asStateFlow()

    /** Whether the content has unsaved changes. */
    private var savedContent: String = ""
    val hasUnsavedChanges: Boolean
        get() = _content.value != savedContent

    // ─── Public actions ──────────────────────────────────────────────────────────

    /**
     * Load a note file from OneDrive.
     * If the same file is already loaded, does nothing (avoids redundant network calls).
     */
    fun loadFile(noteFile: NoteFile, activity: Activity) {
        if (_currentFile.value?.id == noteFile.id &&
            _uiState.value is EditorUiState.Ready
        ) {
            return // Already loaded
        }

        _currentFile.value = noteFile
        _uiState.value = EditorUiState.Loading
        // Default to markdown preview for .md files
        _isMarkdownPreview.value = noteFile.isMarkdown

        viewModelScope.launch {
            oneDriveRepository.downloadFileContent(
                fileId = noteFile.id,
                activity = activity
            ).fold(
                onSuccess = { text ->
                    _content.value = text
                    savedContent = text
                    _uiState.value = EditorUiState.Ready
                },
                onFailure = { e ->
                    _uiState.value = EditorUiState.Error(e.message ?: "Failed to load file")
                }
            )
        }
    }

    /**
     * Update the content in the editor (called on every keystroke).
     */
    fun updateContent(newContent: String) {
        _content.value = newContent
    }

    /**
     * Save the current content back to OneDrive.
     */
    fun saveFile(activity: Activity) {
        val file = _currentFile.value ?: return
        val textToSave = _content.value

        _uiState.value = EditorUiState.Saving

        viewModelScope.launch {
            oneDriveRepository.saveFileContent(
                fileId = file.id,
                content = textToSave,
                activity = activity
            ).fold(
                onSuccess = {
                    savedContent = textToSave
                    _uiState.value = EditorUiState.Saved
                    // Return to Ready state after a brief "Saved" indication
                    kotlinx.coroutines.delay(1500)
                    _uiState.value = EditorUiState.Ready
                },
                onFailure = { e ->
                    _uiState.value = EditorUiState.Error(e.message ?: "Failed to save file")
                }
            )
        }
    }

    /**
     * Toggle between raw text editing and markdown preview.
     */
    fun toggleMarkdownPreview() {
        _isMarkdownPreview.value = !_isMarkdownPreview.value
    }

    /**
     * Reset editor state (e.g. when selecting a new file in split-pane).
     */
    fun reset() {
        _currentFile.value = null
        _content.value = ""
        savedContent = ""
        _uiState.value = EditorUiState.Idle
        _isMarkdownPreview.value = false
    }
}
