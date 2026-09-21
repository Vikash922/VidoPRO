package com.example.feature.editor

import androidx.compose.runtime.Immutable
import com.example.core.model.Clip
import com.example.core.model.Project

enum class EditorTool(val label: String) {
    SPLIT("Split"),
    SPEED("Speed"),
    VOLUME("Volume"),
    AUDIO("Audio"),
    TEXT("Text"),
    OVERLAY("Overlay"),
    EFFECTS("Effects"),
    FILTERS("Filters"),
    TRANSFORM("Transform"),
    CANVAS("Canvas"),
    KEYFRAME("Keyframes"),
    BEATS("Beats"),
    DELETE("Delete")
}

/**
 * UI State for the Video Editor screen matching UI_DESIGN_SYSTEM.md Section 15.2
 */
@Immutable
data class EditorUiState(
    val isLoading: Boolean = true,
    val project: Project? = null,
    val selectedClipId: String? = null,
    val playheadPositionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val activeTool: EditorTool? = null,
    val isEditSheetVisible: Boolean = false,
    val isTextSheetVisible: Boolean = false,
    val isFiltersSheetVisible: Boolean = false,
    val isSpeedSheetVisible: Boolean = false,
    val isVolumeSheetVisible: Boolean = false,
    val isCanvasSheetVisible: Boolean = false,
    val isTransformSheetVisible: Boolean = false,
    val isKeyframeSheetVisible: Boolean = false,
    val isBeatsSheetVisible: Boolean = false,
    val filterSettings: com.example.feature.editor.filter.FilterSettings = com.example.feature.editor.filter.FilterSettings(),
    val assets: Map<String, com.example.core.model.Asset> = emptyMap(),
    val error: String? = null
) {
    val selectedClip: Clip?
        get() = project?.tracks?.flatMap { it.clips }?.find { it.id == selectedClipId }

    val durationMs: Long
        get() = project?.durationMs ?: 0L
}
