package com.nabla.notes.viewmodel

import android.app.Activity
import android.content.Context
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabla.notes.model.MarkdownAction
import com.nabla.notes.model.NoteFile
import com.nabla.notes.repository.OneDriveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
 *  - Track edits in memory (with cursor/selection via TextFieldValue)
 *  - Save file content back to OneDrive (with debounced autosave)
 *  - Toggle between edit mode and markdown preview
 *  - Insert markdown formatting via toolbar actions
 *  - Undo/redo history stack
 */
@HiltViewModel
class EditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val oneDriveRepository: OneDriveRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditorUiState>(EditorUiState.Idle)
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    /** Current text content with cursor/selection tracking. */
    private val _textFieldValue = MutableStateFlow(TextFieldValue(""))
    val textFieldValue: StateFlow<TextFieldValue> = _textFieldValue.asStateFlow()

    /** Whether we are in markdown preview mode (vs raw text edit mode). */
    private val _isMarkdownPreview = MutableStateFlow(false)
    val isMarkdownPreview: StateFlow<Boolean> = _isMarkdownPreview.asStateFlow()

    /** The file currently being edited. */
    private val _currentFile = MutableStateFlow<NoteFile?>(null)
    val currentFile: StateFlow<NoteFile?> = _currentFile.asStateFlow()

    /** Snapshot of content at last save, used to track unsaved changes. */
    private var savedContent: String = ""

    /** Whether the content has unsaved changes. */
    val hasUnsavedChanges: Boolean
        get() = _textFieldValue.value.text != savedContent

    // ─── Autosave ────────────────────────────────────────────────────────────────

    private var autosaveJob: Job? = null

    private fun scheduleAutosave(activity: Activity) {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(2000L)
            if (hasUnsavedChanges) saveFile(activity, silent = true)
        }
    }

    // ─── Undo / Redo ─────────────────────────────────────────────────────────────

    private val history = ArrayDeque<TextFieldValue>()   // undo stack
    private val future = ArrayDeque<TextFieldValue>()    // redo stack
    private val MAX_HISTORY = 50

    /** Push current state onto the undo stack before applying a new value. */
    private fun pushHistory(value: TextFieldValue) {
        if (history.lastOrNull()?.text == value.text) return  // skip if text unchanged
        history.addLast(value)
        if (history.size > MAX_HISTORY) history.removeFirst()
        future.clear()  // new change invalidates redo
    }

    fun undo() {
        if (history.isEmpty()) return
        future.addFirst(_textFieldValue.value)
        _textFieldValue.value = history.removeLast()
        autosaveJob?.cancel()  // don't autosave mid-undo
    }

    fun redo() {
        if (future.isEmpty()) return
        history.addLast(_textFieldValue.value)
        _textFieldValue.value = future.removeFirst()
        autosaveJob?.cancel()  // don't autosave mid-redo
    }

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
                    setContent(text)
                    _uiState.value = EditorUiState.Ready
                },
                onFailure = { e ->
                    _uiState.value = EditorUiState.Error(e.message ?: "Failed to load file")
                }
            )
        }
    }

    /**
     * Set content from a string source (e.g. loaded from OneDrive).
     * Resets cursor to start and clears history.
     */
    fun setContent(text: String) {
        _textFieldValue.value = TextFieldValue(text)
        savedContent = text
        history.clear()
        future.clear()
    }

    /**
     * Called by BasicTextField on every keystroke to update text + cursor/selection.
     * Schedules a debounced autosave 2 seconds after the last keystroke.
     */
    fun updateTextFieldValue(value: TextFieldValue, activity: Activity) {
        pushHistory(_textFieldValue.value)
        _textFieldValue.value = value
        scheduleAutosave(activity)
    }

    /**
     * Save the current content back to OneDrive.
     *
     * @param silent If true, no Saving/Saved UI state transitions (for autosave calls).
     */
    fun saveFile(activity: Activity, silent: Boolean = false) {
        val file = _currentFile.value ?: return
        val textToSave = _textFieldValue.value.text

        if (!silent) {
            _uiState.value = EditorUiState.Saving
        }

        viewModelScope.launch {
            oneDriveRepository.saveFileContent(
                fileId = file.id,
                content = textToSave,
                activity = activity
            ).fold(
                onSuccess = {
                    savedContent = textToSave
                    if (!silent) {
                        _uiState.value = EditorUiState.Saved
                        delay(1500)
                    }
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
     * Insert markdown formatting at the current cursor position or around the current selection.
     *
     * - Block actions (isBlock=true): insert prefix at the start of the current line.
     * - Inline with no selection: insert prefix+placeholder+suffix, select the placeholder.
     * - Inline with selection: wrap selected text with prefix/suffix.
     */
    fun insertMarkdown(action: MarkdownAction) {
        val current = _textFieldValue.value
        val text = current.text
        val selection = current.selection

        val newValue: TextFieldValue = if (action.isBlock) {
            // Find the start of the current line
            val lineStart = text.lastIndexOf('\n', selection.start - 1) + 1
            val newText = text.substring(0, lineStart) + action.prefix + text.substring(lineStart)
            val newCursor = lineStart + action.prefix.length + (selection.start - lineStart)
            TextFieldValue(newText, TextRange(newCursor))
        } else if (selection.start == selection.end) {
            // No selection: insert prefix+placeholder+suffix, select the placeholder
            val insert = action.prefix + action.placeholder + action.suffix
            val newText = text.substring(0, selection.start) + insert + text.substring(selection.end)
            val selectStart = selection.start + action.prefix.length
            val selectEnd = selectStart + action.placeholder.length
            TextFieldValue(newText, TextRange(selectStart, selectEnd))
        } else {
            // Has selection: wrap selected text with prefix/suffix
            val selected = text.substring(selection.start, selection.end)
            val wrapped = action.prefix + selected + action.suffix
            val newText = text.substring(0, selection.start) + wrapped + text.substring(selection.end)
            val newStart = selection.start + action.prefix.length
            val newEnd = newStart + selected.length
            TextFieldValue(newText, TextRange(newStart, newEnd))
        }

        _textFieldValue.value = newValue
    }

    /**
     * Reset editor state (e.g. when selecting a new file in split-pane).
     */
    fun reset() {
        autosaveJob?.cancel()
        _currentFile.value = null
        _textFieldValue.value = TextFieldValue("")
        savedContent = ""
        _uiState.value = EditorUiState.Idle
        _isMarkdownPreview.value = false
        history.clear()
        future.clear()
    }
}
