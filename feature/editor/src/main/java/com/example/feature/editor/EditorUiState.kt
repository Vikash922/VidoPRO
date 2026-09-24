package com.example.feature.editor

import androidx.compose.runtime.Immutable
import com.example.core.model.Clip
import com.example.core.model.Project

enum class EditorTool(val label: String) {
    EDIT("Edit"),
    AUDIO("Audio"),
    TEXT("Text"),
    OVERLAY("Overlay"),
    EFFECTS("Effects"),
    FILTERS("Filter"),
    ADJUST("Adjust"),
    HSL("HSL"),
    AI("AI"),
    
    // Secondary (Edit Panel)
    SPLIT("Split"),
    SPEED("Speed"),
    VOLUME("Volume"),
    DUPLICATE("Duplicate"),
    ANIMATION("Animation"),
    DELETE("Delete"),
    MASK("Mask"),
    BLEND("Blend"),
    CHROMA("Chroma Key"),
    TRANSFORM("Transform"),
    CANVAS("Canvas"),
    KEYFRAME("Keyframe"),
    BEATS("Beat Sync"),
    TRANSITION("Transition")
}

@Immutable
data class EditorUiState(
    val isLoading: Boolean = true,
    val project: Project? = null,
    val selectedClipId: String? = null,
    val multiSelectedClipIds: Set<String> = emptySet(),
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
    val isTransitionSheetVisible: Boolean = false,
    val isMaskSheetVisible: Boolean = false,
    val isBlendSheetVisible: Boolean = false,
    val editingTransitionPair: Pair<String, String>? = null,
    val filterSettings: com.example.feature.editor.filter.FilterSettings = com.example.feature.editor.filter.FilterSettings(),
    val assets: Map<String, com.example.core.model.Asset> = emptyMap(),
    val beatMarkers: Set<Long> = emptySet(),
    val activeKeyframeProperty: String = com.example.core.model.KeyframeProperty.POSITION_X,
    val error: String? = null
) {
    val selectedClip: Clip?
        get() = project?.tracks?.flatMap { it.clips }?.find { it.id == selectedClipId }

    val durationMs: Long
        get() = project?.durationMs ?: 0L

    /** True when 2+ clips are selected for grouping. */
    val isMultiSelectMode: Boolean
        get() = multiSelectedClipIds.size >= 2

    /** Returns the groupId of the currently selected clip (if it belongs to a group). */
    val selectedGroupId: String?
        get() = selectedClip?.groupId
}

