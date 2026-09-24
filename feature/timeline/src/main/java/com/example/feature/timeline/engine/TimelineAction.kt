package com.example.feature.timeline.engine

import com.example.core.model.Clip
import com.example.core.model.Effect
import com.example.core.model.InterpolationType
import com.example.core.model.Track
import com.example.core.model.Transform

/**
 * Pure sealed interface of timeline engine actions according to TIMELINE_ENGINE_SPEC.md.
 */
sealed interface TimelineAction {
    data class LoadProject(val tracks: List<Track>, val durationMs: Long? = null) : TimelineAction
    data class SetTracks(val tracks: List<Track>) : TimelineAction
    data class Seek(val positionMs: Long) : TimelineAction
    data class SeekPlayhead(val positionMs: Long) : TimelineAction
    data class SelectClip(val clipId: String?) : TimelineAction
    data class SelectMultipleClips(val clipIds: Set<String>) : TimelineAction
    data class ToggleClipSelection(val clipId: String) : TimelineAction
    data class GroupSelectedClips(val groupId: String) : TimelineAction
    data class UngroupClips(val groupId: String) : TimelineAction
    data class AddClip(val trackId: String, val clip: Clip, val atTimeMs: Long? = null) : TimelineAction
    data class MoveClip(val clipId: String, val targetTrackId: String, val newStartTimeMs: Long) : TimelineAction
    data class TrimStart(val clipId: String, val newStartTimeMs: Long) : TimelineAction
    data class TrimClipStart(val clipId: String, val newStartTimeMs: Long) : TimelineAction
    data class TrimEnd(val clipId: String, val newEndTimeMs: Long) : TimelineAction
    data class TrimClipEnd(val clipId: String, val newEndTimeMs: Long) : TimelineAction
    data class SplitAtPlayhead(val clipId: String? = null) : TimelineAction
    data class SplitClip(val clipId: String, val splitPointMs: Long) : TimelineAction
    data class DeleteClip(val clipId: String) : TimelineAction
    data class RemoveClip(val clipId: String) : TimelineAction
    data class DuplicateClip(val clipId: String) : TimelineAction
    data class UpdateClipSpeed(val clipId: String, val speed: Float) : TimelineAction
    data class UpdateClipVolume(val clipId: String, val volume: Float) : TimelineAction
    data class UpdateClipTransform(val clipId: String, val transform: Transform) : TimelineAction
    data class SetZoom(val zoom: Float) : TimelineAction
    data class SetSnapping(val enabled: Boolean) : TimelineAction
    data class SetScrollOffset(val offsetPx: Float) : TimelineAction
    data class ToggleBeatMarker(val positionMs: Long) : TimelineAction
    /** Adds or updates a keyframe on a clip. */
    data class AddKeyframe(
        val clipId: String,
        val property: String,
        val timeMs: Long,
        val value: Float,
        val interpolation: InterpolationType = InterpolationType.LINEAR
    ) : TimelineAction
    /** Updates the value or interpolation of an existing keyframe. */
    data class UpdateKeyframe(
        val clipId: String,
        val keyframeId: String,
        val value: Float,
        val interpolation: InterpolationType = InterpolationType.LINEAR
    ) : TimelineAction
    /** Deletes a keyframe from a clip. */
    data class DeleteKeyframe(val clipId: String, val keyframeId: String) : TimelineAction
    /** Moves a keyframe diamond to a new time position (drag-on-timeline). */
    data class MoveKeyframe(val clipId: String, val keyframeId: String, val newTimeMs: Long) : TimelineAction
    /** Updates the effects applied to a clip. */
    data class UpdateClipEffects(val clipId: String, val effects: List<Effect>) : TimelineAction
    /** Adds a transition between two adjacent clips on a track. */
    data class AddTransition(val transition: com.example.core.model.Transition) : TimelineAction
    /** Updates an existing transition's type, duration, or parameters. */
    data class UpdateTransition(val transition: com.example.core.model.Transition) : TimelineAction
    /** Removes a transition. */
    data class RemoveTransition(val transitionId: String) : TimelineAction
}

