package com.nabla.notes.model

/**
 * User-configurable app settings persisted in DataStore.
 *
 * @param folderPath  OneDrive folder path relative to root (e.g. "Documents/notes")
 *                    Use empty string to browse root.
 * @param folderId    Resolved OneDrive item ID (cached to avoid repeated lookups).
 *                    "root" means the drive root.
 */
data class AppSettings(
    val folderPath: String = "",
    val folderId: String = "root"
)
