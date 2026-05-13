package com.nabla.notes.model

/**
 * Represents a note file stored on OneDrive.
 *
 * @param id                OneDrive item ID
 * @param name              File name (including extension)
 * @param lastModified      ISO-8601 timestamp string from Graph API
 */
data class NoteFile(
    val id: String,
    val name: String,
    val lastModified: String = ""
) {
    val isMarkdown: Boolean
        get() = name.endsWith(".md", ignoreCase = true)

    val displayName: String
        get() = name.removeSuffix(".md").removeSuffix(".txt")
}

/**
 * Represents an OneDrive folder shown in the file browser.
 *
 * @param id    OneDrive item ID
 * @param name  Folder display name
 */
data class FolderItem(
    val id: String,
    val name: String
)

/**
 * Combined browser entry — either a folder or a note file.
 * Used to display a mixed list in [com.nabla.notes.ui.browser.FileBrowserScreen].
 */
sealed class BrowserEntry {
    data class Folder(val item: FolderItem) : BrowserEntry()
    data class File(val note: NoteFile) : BrowserEntry()

    val displayName: String
        get() = when (this) {
            is Folder -> item.name
            is File -> note.name
        }
}
