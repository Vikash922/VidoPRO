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
    /** Alight Motion-style long-press toggles this clip into/out of multi-selection. */
    data class LongPressClip(val clipId: String) : EditorEvent
    /** Groups all currently multi-selected clips under a new shared groupId. */
    data object GroupSelectedClips : EditorEvent
    /** Ungroups clips that belong to the same group as the currently selected clip. */
    data object UngroupSelectedClips : EditorEvent
    /** Drags a keyframe diamond to a new position on the timeline. */
    data class MoveKeyframe(val clipId: String, val keyframeId: String, val newTimeMs: Long) : EditorEvent
    /** Adds a keyframe to a clip. */
    data class AddKeyframe(
        val clipId: String,
        val property: String,
        val timeMs: Long,
        val value: Float,
        val interpolation: com.example.core.model.InterpolationType = com.example.core.model.InterpolationType.LINEAR
    ) : EditorEvent
    /** Updates an existing keyframe. */
    data class UpdateKeyframe(
        val clipId: String,
        val keyframeId: String,
        val value: Float,
        val interpolation: com.example.core.model.InterpolationType = com.example.core.model.InterpolationType.LINEAR
    ) : EditorEvent
    /** Deletes a keyframe. */
    data class DeleteKeyframe(val clipId: String, val keyframeId: String) : EditorEvent
    /** Changes the currently active animation property for keyframing. */
    data class SetActiveKeyframeProperty(val property: String) : EditorEvent
    /** Toggles (adds or removes) a keyframe at the current playhead position on the selected clip. */
    data object ToggleKeyframeAtPlayhead : EditorEvent
    /** Opens the transition editor bottom sheet for the given adjacent clips. */
    data class OpenTransitionEditor(val firstClipId: String, val secondClipId: String) : EditorEvent
    /** Controls visibility of the transition bottom sheet. */
    data class SetTransitionSheetVisible(val visible: Boolean) : EditorEvent
    /** Applies or updates a transition. */
    data class ApplyTransition(val transition: com.example.core.model.Transition) : EditorEvent
    /** Removes a transition. */
    data class RemoveTransition(val transitionId: String) : EditorEvent
}

