package com.example.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Base Dark Theme Color Scheme matching UI_DESIGN_SYSTEM.md Section 7.1
 */
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7C5CFF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9A7BFF),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF22D3EE),
    onSecondary = Color(0xFF04141A),
    background = Color(0xFF0B0B0F),
    onBackground = Color(0xFFF5F5F7),
    surface = Color(0xFF121218),
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = Color(0xFF1A1A22),
    onSurfaceVariant = Color(0xFFA0A0AB),
    outline = Color(0xFF2A2A35),
    outlineVariant = Color(0xFF3A3A48),
    error = Color(0xFFFF4D4F),
    onError = Color(0xFFFFFFFF)
)

/**
 * Editor-specific Colors matching UI_DESIGN_SYSTEM.md Section 7.2
 */
object EditorColors {
    val timelineBackground = Color(0xFF0E0E13)
    val timelineRuler = Color(0xFF6B6B76)
    val playhead = Color(0xFFFF3B30)
    val playheadGlow = Color(0x66FF3B30)
    val clipVideo = Color(0xFF3B82F6)
    val clipImage = Color(0xFF8B5CF6)
    val clipAudio = Color(0xFF22C55E)
    val clipText = Color(0xFFF59E0B)
    val clipOverlay = Color(0xFF22D3EE)
    val clipSelectedBorder = Color(0xFF7C5CFF)
    val clipSelectedFill = Color(0x227C5CFF)
    val snapLine = Color(0xFF22D3EE)
    val transitionMarker = Color(0xFF9A7BFF)
}

/**
 * State Colors matching UI_DESIGN_SYSTEM.md Section 7.3
 */
object StateColors {
    val pressed = Color(0x14FFFFFF)
    val hover = Color(0x0AFFFFFF)
    val selected = Color(0x227C5CFF)
    val disabled = Color(0x08FFFFFF)
    val errorBackground = Color(0x1AFF4D4F)
    val successBackground = Color(0x1A22C55E)
    val warningBackground = Color(0x1AF59E0B)
}
