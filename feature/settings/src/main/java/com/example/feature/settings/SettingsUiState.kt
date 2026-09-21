package com.example.feature.settings

import androidx.compose.runtime.Immutable

@Immutable
data class SettingsUiState(
    val defaultResolution: String = "1080p (FHD)",
    val defaultFps: Int = 30,
    val defaultQuality: String = "High",
    val timelineSnapping: Boolean = true,
    val autosaveEnabled: Boolean = true,
    val cacheSizeMb: Double = 0.0,
    val isClearingCache: Boolean = false,
    val appVersion: String = "1.0.0",
    val message: String? = null
)
