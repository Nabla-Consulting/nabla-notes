package com.nabla.notes.ui.settings

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nabla.notes.BuildConfig
import com.nabla.notes.viewmodel.SettingsUiState
import com.nabla.notes.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val activity = LocalContext.current as Activity
    var showFolderDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Section: OneDrive Folder ───────────────────────────────────────

            SettingsSectionHeader(title = "OneDrive Folder")

            SettingsRow(
                label = "Current folder",
                value = settings.folderPath.ifBlank { "Not configured" }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Button(
                    onClick = {
                        showFolderDialog = true
                        viewModel.loadFolders(activity = activity)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Change Folder")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider()

            // ── Section: Account ───────────────────────────────────────────────

            SettingsSectionHeader(title = "Account")

            SettingsRow(
                label = "Status",
                value = "Signed in"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Button(
                    onClick = { showSignOutDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign Out")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider()

            // ── Section: About ─────────────────────────────────────────────────

            SettingsSectionHeader(title = "About")

            SettingsRow(
                label = "App",
                value = "nabla notes"
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ── Folder Picker Dialog ───────────────────────────────────────────────────

    if (showFolderDialog) {
        val folderStack by viewModel.folderStack.collectAsState()
        val currentPath by viewModel.currentPath.collectAsState()
        FolderPickerDialog(
            uiState = uiState,
            currentPath = currentPath,
            folderStack = folderStack,
            onNavigateInto = { id, name ->
                viewModel.navigateIntoFolder(id, name, activity)
            },
            onNavigateUp = { viewModel.navigateUp(activity) },
            onSelectCurrent = {
                viewModel.selectCurrentFolder()
                showFolderDialog = false
            },
            onDismiss = {
                viewModel.resetFolderNav()
                showFolderDialog = false
                viewModel.dismissState()
            }
        )
    }

    // ── Sign-Out Confirmation Dialog ───────────────────────────────────────────

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out? You'll need to sign in again to access your notes.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut { onBackClick() }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ─── Folder Picker Dialog ─────────────────────────────────────────────────────

@Composable
private fun FolderPickerDialog(
    uiState: SettingsUiState,
    currentPath: String,
    folderStack: List<Pair<String, String>>,
    onNavigateInto: (folderId: String, folderName: String) -> Unit,
    onNavigateUp: () -> Unit,
    onSelectCurrent: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose OneDrive Folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Current path breadcrumb
                Text(
                    text = "📂 $currentPath",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // Go up button when inside a subfolder
                if (folderStack.isNotEmpty()) {
                    TextButton(
                        onClick = onNavigateUp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "↑ Go up",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                when (uiState) {
                    is SettingsUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is SettingsUiState.FolderPicker -> {
                        if (uiState.folders.isEmpty()) {
                            Text(
                                text = "No subfolders here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .heightIn(max = 320.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Tap a folder to navigate INTO it (does not select immediately)
                                uiState.folders.sortedBy { it.first.lowercase() }.forEach { (name, id) ->
                                    TextButton(
                                        onClick = { onNavigateInto(id, name) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "📁  $name  ›",
                                            modifier = Modifier.fillMaxWidth(),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    is SettingsUiState.Error -> {
                        Text(
                            text = "Failed to load folders: ${uiState.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    else -> {
                        Text(
                            text = "Loading folders…",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            // Selects the current folder level
            Button(onClick = onSelectCurrent) {
                Text("✓ Select this folder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ─── Reusable Composables ─────────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.6f)
        )
    }
}
