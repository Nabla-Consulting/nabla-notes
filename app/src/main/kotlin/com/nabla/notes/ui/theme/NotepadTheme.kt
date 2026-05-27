package com.nabla.notes.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val AppGray = Color(0xFFF0F0F0)

private val LightColors = lightColorScheme(
    background = AppGray,
    surface = AppGray,
    surfaceVariant = Color(0xFFE8E8E8)
)

@Composable
fun NotepadTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicLightColorScheme(context).copy(
            background = AppGray,
            surface = AppGray,
            surfaceVariant = Color(0xFFE8E8E8)
        )
    } else {
        LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
