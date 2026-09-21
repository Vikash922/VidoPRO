package com.example.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Main application theme using the dark-first design system
 */
@Composable
fun VideoEditorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
