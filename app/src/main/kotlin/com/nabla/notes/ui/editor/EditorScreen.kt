package com.nabla.notes.ui.editor

import android.app.Activity
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.nabla.notes.model.MarkdownAction
import com.nabla.notes.model.NoteFile
import com.nabla.notes.viewmodel.EditorUiState
import com.nabla.notes.viewmodel.EditorViewModel
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    noteFile: NoteFile,
    showBack: Boolean,
    onBackClick: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val textFieldValue by viewModel.textFieldValue.collectAsState()
    val isMarkdownPreview by viewModel.isMarkdownPreview.collectAsState()
    val activity = LocalContext.current as Activity
    val snackbarHostState = remember { SnackbarHostState() }

    // Load file on first composition or when file changes
    LaunchedEffect(noteFile.id) {
        viewModel.loadFile(noteFile, activity)
    }

    // Show "Saved" snackbar on explicit (non-silent) save
    LaunchedEffect(uiState) {
        if (uiState is EditorUiState.Saved) {
            snackbarHostState.showSnackbar("Saved")
        }
    }

    val hasUnsaved = viewModel.hasUnsavedChanges
    val titleText = buildString {
        append(noteFile.name)
        if (hasUnsaved) append(" \u25CF")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = titleText,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    // Toggle preview / edit mode (only for .md files or when in preview)
                    if (noteFile.isMarkdown || isMarkdownPreview) {
                        IconButton(onClick = { viewModel.toggleMarkdownPreview() }) {
                            Icon(
                                imageVector = if (isMarkdownPreview) Icons.Filled.Edit else Icons.Filled.Visibility,
                                contentDescription = if (isMarkdownPreview) "Edit" else "Preview"
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        // No imePadding here — applied to the content Column instead so the
        // toolbar hugs the keyboard edge without an extra gap.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is EditorUiState.Idle,
                is EditorUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is EditorUiState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                }

                is EditorUiState.Ready,
                is EditorUiState.Saving,
                is EditorUiState.Saved -> {
                    // imePadding on Column: the whole column shifts up with the keyboard,
                    // so the toolbar lands right above it with no extra space.
                    Column(modifier = Modifier.fillMaxSize().imePadding()) {
                        if (isMarkdownPreview) {
                            MarkdownPreview(
                                content = textFieldValue.text,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        } else {
                            NoteEditor(
                                textFieldValue = textFieldValue,
                                onValueChange = { viewModel.updateTextFieldValue(it, activity) },
                                modifier = Modifier
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.surface)
                            )
                            // Toolbar only visible in edit mode
                            MarkdownToolbar(
                                onAction = { action ->
                                    when (action) {
                                        MarkdownAction.UNDO -> viewModel.undo()
                                        MarkdownAction.REDO -> viewModel.redo()
                                        else -> viewModel.insertMarkdown(action)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Plain Text Editor ────────────────────────────────────────────────────────

@Composable
private fun NoteEditor(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    BasicTextField(
        value = textFieldValue,
        onValueChange = onValueChange,
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 300.dp),
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            if (textFieldValue.text.isEmpty()) {
                Text(
                    text = "Start writing\u2026",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            innerTextField()
        }
    )
}

// ─── Markdown Toolbar ─────────────────────────────────────────────────────────

@Composable
private fun MarkdownToolbar(
    onAction: (MarkdownAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        LazyRow(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(MarkdownAction.entries.toList()) { action ->
                TextButton(
                    onClick = { onAction(action) },
                    modifier = Modifier.defaultMinSize(minWidth = 40.dp, minHeight = 36.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = action.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = if (action == MarkdownAction.CODE_BLOCK) FontFamily.Monospace else FontFamily.Default
                    )
                }
            }
        }
    }
}

// ─── Markdown Preview ─────────────────────────────────────────────────────────

@Composable
private fun MarkdownPreview(
    content: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(context))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(GlideImagesPlugin.create(context))
            .build()
    }

    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                setPadding(0, 0, 0, 0)
                textSize = 15f
            }
        },
        update = { textView ->
            markwon.setMarkdown(textView, content)
        },
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 300.dp)
    )
}
