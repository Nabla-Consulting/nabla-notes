package com.nabla.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.nabla.notes.ui.browser.FileBrowserScreen
import com.nabla.notes.ui.editor.EditorScreen
import com.nabla.notes.ui.settings.SettingsScreen
import com.nabla.notes.viewmodel.BrowserViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nabla.notes.model.NoteFile
import com.nabla.notes.ui.theme.NotepadTheme
import com.google.gson.Gson
import java.net.URLEncoder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotepadTheme {
                val windowSizeClass = calculateWindowSizeClass(this)
                val isExpandedOrMedium = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

                if (isExpandedOrMedium) {
                    SplitPaneLayout()
                } else {
                    SinglePaneLayout()
                }
            }
        }
    }
}

// ─── Single-Pane Navigation (phones) ─────────────────────────────────────────

@Composable
private fun SinglePaneLayout() {
    val navController = rememberNavController()
    val gson = Gson()

    NavHost(navController = navController, startDestination = "browser") {
        composable("browser") {
            val viewModel: BrowserViewModel = hiltViewModel()
            FileBrowserScreen(
                viewModel = viewModel,
                onFileSelected = { file ->
                    val json = URLEncoder.encode(gson.toJson(file), StandardCharsets.UTF_8.name())
                    navController.navigate("editor/$json")
                },
                onSettingsClick = {
                    navController.navigate("settings")
                }
            )
        }

        composable("editor/{fileJson}") { backStackEntry ->
            val fileJson = backStackEntry.arguments?.getString("fileJson") ?: return@composable
            val decoded = URLDecoder.decode(fileJson, StandardCharsets.UTF_8.name())
            val noteFile = Gson().fromJson(decoded, NoteFile::class.java)
            EditorScreen(
                noteFile = noteFile,
                showBack = true,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

// ─── Split-Pane Layout (tablets / foldables) ─────────────────────────────────

@Composable
private fun SplitPaneLayout() {
    val browserViewModel: BrowserViewModel = hiltViewModel()
    val selectedFile by browserViewModel.selectedFile.collectAsState()
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left pane — file browser (fixed 360dp)
                Box(
                    modifier = Modifier
                        .width(360.dp)
                        .fillMaxHeight()
                ) {
                    FileBrowserScreen(
                        viewModel = browserViewModel,
                        onFileSelected = { file ->
                            browserViewModel.selectFile(file)
                        },
                        onSettingsClick = {
                            navController.navigate("settings")
                        }
                    )
                }

                // Right pane — editor (fills remaining space)
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    val file = selectedFile
                    if (file != null) {
                        EditorScreen(
                            noteFile = file,
                            showBack = false,
                            onBackClick = { browserViewModel.clearSelectedFile() }
                        )
                    }
                }
            }
        }

        composable("settings") {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
