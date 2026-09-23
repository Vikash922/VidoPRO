package com.example.feature.timeline.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/**
 * Encapsulates timeline selection state and visual styling for single and multi-clip selection.
 */
@Immutable
data class TimelineSelectionState(
    val selectedClipId: String? = null,
    val multiSelectedClipIds: Set<String> = emptySet(),
    val activeGroupId: String? = null
) {
    val isMultiSelectActive: Boolean
        get() = multiSelectedClipIds.isNotEmpty()

    fun isClipSelected(clipId: String): Boolean = clipId == selectedClipId

    fun isClipMultiSelected(clipId: String): Boolean = multiSelectedClipIds.contains(clipId)

    fun isClipInSelection(clipId: String): Boolean = isClipSelected(clipId) || isClipMultiSelected(clipId)
}

object TimelineSelectionDefaults {
    val singleSelectedBorderColor = Color(0xFF00D2FF)
    val multiSelectedBorderColor = Color(0xFFFFD600)
    val groupedBorderColor = Color(0xFF00E676)
    val unselectedBorderColor = Color(0xFF262C3D)
}

@Composable
fun rememberTimelineSelection(
    selectedClipId: String?,
    multiSelectedClipIds: Set<String> = emptySet(),
    activeGroupId: String? = null
): TimelineSelectionState = remember(selectedClipId, multiSelectedClipIds, activeGroupId) {
    TimelineSelectionState(
        selectedClipId = selectedClipId,
        multiSelectedClipIds = multiSelectedClipIds,
        activeGroupId = activeGroupId
    )
}
