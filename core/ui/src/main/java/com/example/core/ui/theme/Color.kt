package com.example.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB8A7FF), // Soft lavender
    onPrimary = Color(0xFF0D0F12),
    secondary = Color(0xFF7DE2D1), // Soft mint
    onSecondary = Color(0xFF0D0F12),
    tertiary = Color(0xFFFFB86B), // Warm accent
    onTertiary = Color(0xFF0D0F12),
    background = Color(0xFF0D0F12), // Primary background
    onBackground = Color(0xFFF5F7FA), // Primary text
    surface = Color(0xFF151920), // Surface
    onSurface = Color(0xFFF5F7FA),
    surfaceVariant = Color(0xFF1B2027), // Elevated surface
    onSurfaceVariant = Color(0xFF9299A5), // Secondary text
    outline = Color(0xFF272D35), // Border
    error = Color(0xFFE57373),
    onError = Color(0xFF0D0F12)
)

object EditorColors {
    val timelineBackground = Color(0xFF111419) // Secondary background
    val timelineRuler = Color(0xFF626A76) // Muted text
    val playhead = Color(0xFFF5F7FA) // Clear but not excessively bright
    val playheadGlow = Color(0x33F5F7FA)
    
    // Muted clip colors based on instructions
    val clipVideo = Color(0xFF4B6B99) // Muted blue
    val clipImage = Color(0xFF7A64A3) // Muted violet
    val clipAudio = Color(0xFF5B8A6E) // Muted green
    val clipText = Color(0xFFB38959) // Muted amber
    val clipOverlay = Color(0xFF4A8B99) // Muted cyan
    
    val clipSelectedBorder = Color(0xFFB8A7FF) // Accent
    val clipSelectedFill = Color(0x33B8A7FF)
    val snapLine = Color(0xFF7DE2D1) // Secondary accent
    val transitionMarker = Color(0xFFB8A7FF)
    
    val mutedText = Color(0xFF626A76)
}

object StateColors {
    val pressed = Color(0x1AFFFFFF)
    val hover = Color(0x0AFFFFFF)
    val selected = Color(0x33B8A7FF)
    val disabled = Color(0x0CFFFFFF)
    val errorBackground = Color(0x1AE57373)
}
