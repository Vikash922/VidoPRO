package com.example.feature.timeline.engine

import androidx.compose.runtime.Immutable
import com.example.core.model.Clip
import com.example.core.model.Track

/**
 * Pure Kotlin immutable state representation of the timeline engine.
 * Defined according to TIMELINE_ENGINE_SPEC.md.
 */
@Immutable
data class TimelineEngineState(
    val tracks: List<Track> = emptyList(),
    val playheadPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val selectedClipId: String? = null,
    val zoomLevel: Float = 1.0f,
    val isSnappingEnabled: Boolean = true,
    val scrollOffsetPx: Float = 0f,
    val beatMarkers: Set<Long> = emptySet()
) {
    val selectedClip: Clip?
        get() = tracks.flatMap { it.clips }.find { it.id == selectedClipId }

    val selectedTrack: Track?
        get() = tracks.find { track -> track.clips.any { it.id == selectedClipId } }

    fun findClip(clipId: String): Clip? {
        return tracks.flatMap { it.clips }.find { it.id == clipId }
    }

    fun findTrackForClip(clipId: String): Track? {
        return tracks.find { track -> track.clips.any { it.id == clipId } }
    }

    companion object {
        const val MIN_CLIP_DURATION_MS = 500L
        const val DEFAULT_ZOOM = 1.0f
        const val MIN_ZOOM = 0.25f
        const val MAX_ZOOM = 4.0f
        const val SNAP_THRESHOLD_MS = 150L
    }
}
