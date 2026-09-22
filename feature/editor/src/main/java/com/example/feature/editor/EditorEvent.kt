package com.example.feature.editor

import com.example.core.model.TrackType

/**
 * User and system events in the Editor screen.
 */
sealed interface EditorEvent {
    data object PlayPauseClicked : EditorEvent
    data class SeekTo(val positionMs: Long) : EditorEvent
    data class SelectClip(val clipId: String?) : EditorEvent
    data object UndoClicked : EditorEvent
    data object RedoClicked : EditorEvent
    data object ExportClicked : EditorEvent
    data class ToolClicked(val tool: EditorTool) : EditorEvent
    data object CloseToolPanel : EditorEvent
    data object SplitSelectedClip : EditorEvent
    data object DeleteSelectedClip : EditorEvent
    data object DuplicateSelectedClip : EditorEvent
    data class SetEditSheetVisible(val visible: Boolean) : EditorEvent
    data class SetTextSheetVisible(val visible: Boolean) : EditorEvent
    data class SetFiltersSheetVisible(val visible: Boolean) : EditorEvent
    data class SetSpeedSheetVisible(val visible: Boolean) : EditorEvent
    data class SetVolumeSheetVisible(val visible: Boolean) : EditorEvent
    data class SetCanvasSheetVisible(val visible: Boolean) : EditorEvent
    data class SetTransformSheetVisible(val visible: Boolean) : EditorEvent
    data class SetKeyframeSheetVisible(val visible: Boolean) : EditorEvent
    data class SetBeatsSheetVisible(val visible: Boolean) : EditorEvent
    data class ChangeClipSpeed(val speed: Float) : EditorEvent
    data class ChangeClipVolume(val volume: Float) : EditorEvent
    data class ChangeAspectRatio(val ratio: com.example.core.model.AspectRatio) : EditorEvent
    data class ChangeClipTransform(val transform: com.example.core.model.Transform, val clipId: String? = null) : EditorEvent
    data class ApplyTextClip(
        val text: String,
        val fontSize: Float,
        val color: String,
        val fontFamily: String,
        val alignment: String
    ) : EditorEvent
    data class UpdateFilterSettings(val filterSettings: com.example.feature.editor.filter.FilterSettings) : EditorEvent
    data object ResetFilterSettings : EditorEvent
    data class AddMediaClicked(val trackType: TrackType) : EditorEvent
    data object SaveImmediately : EditorEvent
}
