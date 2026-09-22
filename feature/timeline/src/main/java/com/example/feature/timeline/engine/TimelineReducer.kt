package com.example.feature.timeline.engine

import com.example.core.model.Clip
import com.example.core.model.Track
import com.example.core.model.TrackType
import java.util.UUID

/**
 * Pure reducer function for the Timeline Engine following TIMELINE_ENGINE_SPEC.md.
 * Ensures immutability, duration recalculation, overlap prevention, and boundary checks.
 */
object TimelineReducer {

    fun reduce(state: TimelineEngineState, action: TimelineAction): TimelineEngineState {
        return when (action) {
            is TimelineAction.LoadProject -> handleLoadProject(state, action.tracks, action.durationMs)
            is TimelineAction.SetTracks -> handleLoadProject(state, action.tracks, null)
            is TimelineAction.Seek -> handleSeek(state, action.positionMs)
            is TimelineAction.SeekPlayhead -> handleSeek(state, action.positionMs)
            is TimelineAction.SelectClip -> state.copy(selectedClipId = action.clipId)
            is TimelineAction.AddClip -> handleAddClip(state, action.trackId, action.clip, action.atTimeMs)
            is TimelineAction.MoveClip -> handleMoveClip(state, action.clipId, action.targetTrackId, action.newStartTimeMs)
            is TimelineAction.TrimStart -> handleTrimStart(state, action.clipId, action.newStartTimeMs)
            is TimelineAction.TrimClipStart -> handleTrimStart(state, action.clipId, action.newStartTimeMs)
            is TimelineAction.TrimEnd -> handleTrimEnd(state, action.clipId, action.newEndTimeMs)
            is TimelineAction.TrimClipEnd -> handleTrimEnd(state, action.clipId, action.newEndTimeMs)
            is TimelineAction.SplitAtPlayhead -> handleSplitAtPlayhead(state, action.clipId)
            is TimelineAction.SplitClip -> handleSplitClip(state, action.clipId, action.splitPointMs)
            is TimelineAction.DeleteClip -> handleDeleteClip(state, action.clipId)
            is TimelineAction.RemoveClip -> handleDeleteClip(state, action.clipId)
            is TimelineAction.DuplicateClip -> handleDuplicateClip(state, action.clipId)
            is TimelineAction.UpdateClipSpeed -> handleUpdateClipSpeed(state, action.clipId, action.speed)
            is TimelineAction.UpdateClipVolume -> handleUpdateClipVolume(state, action.clipId, action.volume)
            is TimelineAction.UpdateClipTransform -> handleUpdateClipTransform(state, action.clipId, action.transform)
            is TimelineAction.SetZoom -> state.copy(zoomLevel = action.zoom.coerceIn(TimelineEngineState.MIN_ZOOM, TimelineEngineState.MAX_ZOOM))
            is TimelineAction.SetSnapping -> state.copy(isSnappingEnabled = action.enabled)
            is TimelineAction.SetScrollOffset -> state.copy(scrollOffsetPx = action.offsetPx.coerceAtLeast(0f))
            is TimelineAction.ToggleBeatMarker -> {
                val existing = state.beatMarkers
                val updated = if (existing.contains(action.positionMs)) {
                    existing - action.positionMs
                } else {
                    existing + action.positionMs
                }
                state.copy(beatMarkers = updated)
            }
        }
    }

    private fun handleLoadProject(state: TimelineEngineState, tracks: List<Track>, explicitDuration: Long?): TimelineEngineState {
        val totalDuration = explicitDuration ?: TimelineUtils.recalculateProjectDuration(tracks)
        val clampedPlayhead = TimelineUtils.clampPlayhead(state.playheadPositionMs, totalDuration)
        return state.copy(
            tracks = tracks,
            durationMs = totalDuration,
            playheadPositionMs = clampedPlayhead,
            selectedClipId = if (tracks.flatMap { it.clips }.any { it.id == state.selectedClipId }) state.selectedClipId else null
        )
    }

    private fun handleSeek(state: TimelineEngineState, positionMs: Long): TimelineEngineState {
        val clamped = TimelineUtils.clampPlayhead(positionMs, state.durationMs)
        return state.copy(playheadPositionMs = clamped)
    }

    private fun handleAddClip(
        state: TimelineEngineState,
        trackId: String,
        clip: Clip,
        atTimeMs: Long?
    ): TimelineEngineState {
        val tracks = state.tracks.map { track ->
            if (track.id == trackId) {
                val insertTime = atTimeMs ?: (track.clips.maxOfOrNull { it.endTimeMs } ?: 0L)
                val newClip = clip.copy(
                    trackId = track.id,
                    startTimeMs = insertTime.coerceAtLeast(0L)
                )
                val combinedClips = track.clips + newClip
                val adjustedClips = if (track.type == TrackType.VIDEO) {
                    resolveMainTrackOverlaps(combinedClips)
                } else {
                    combinedClips.sortedBy { it.startTimeMs }
                }
                track.copy(clips = adjustedClips)
            } else {
                track
            }
        }
        val newDuration = TimelineUtils.recalculateProjectDuration(tracks)
        return state.copy(
            tracks = tracks,
            durationMs = newDuration,
            selectedClipId = clip.id
        )
    }

    private fun handleMoveClip(
        state: TimelineEngineState,
        clipId: String,
        targetTrackId: String,
        newStartTimeMs: Long
    ): TimelineEngineState {
        val sourceClip = state.findClip(clipId) ?: return state
        val safeStartTime = newStartTimeMs.coerceAtLeast(0L)

        val updatedTracks = state.tracks.map { track ->
            when (track.id) {
                targetTrackId -> {
                    val remainingClips = track.clips.filterNot { it.id == clipId }
                    val movedClip = sourceClip.copy(
                        trackId = targetTrackId,
                        startTimeMs = safeStartTime
                    )
                    val combined = remainingClips + movedClip
                    val resolved = if (track.type == TrackType.VIDEO) {
                        resolveMainTrackOverlaps(combined)
                    } else {
                        combined.sortedBy { it.startTimeMs }
                    }
                    track.copy(clips = resolved)
                }
                sourceClip.trackId -> {
                    // Removed from source track
                    val remaining = track.clips.filterNot { it.id == clipId }
                    track.copy(clips = remaining)
                }
                else -> track
            }
        }

        val newDuration = TimelineUtils.recalculateProjectDuration(updatedTracks)
        return state.copy(
            tracks = updatedTracks,
            durationMs = newDuration
        )
    }

    private fun handleTrimStart(
        state: TimelineEngineState,
        clipId: String,
        newStartTimeMs: Long
    ): TimelineEngineState {
        val clip = state.findClip(clipId) ?: return state
        val originalEnd = clip.endTimeMs
        val minDuration = TimelineEngineState.MIN_CLIP_DURATION_MS

        // Boundary safety: newStartTimeMs must not push duration below minDuration
        val maxAllowedStartTime = originalEnd - minDuration
        val clampedStartTime = newStartTimeMs.coerceIn(0L, maxAllowedStartTime)
        val deltaMs = clampedStartTime - clip.startTimeMs

        val newInPoint = (clip.inPointMs + deltaMs).coerceAtLeast(0L)
        val newDuration = (originalEnd - clampedStartTime).coerceAtLeast(minDuration)

        val updatedTracks = state.tracks.map { track ->
            if (track.clips.any { it.id == clipId }) {
                val updatedClips = track.clips.map { c ->
                    if (c.id == clipId) {
                        c.copy(
                            startTimeMs = clampedStartTime,
                            durationMs = newDuration,
                            inPointMs = newInPoint
                        )
                    } else c
                }
                val resolved = if (track.type == TrackType.VIDEO) {
                    resolveMainTrackOverlaps(updatedClips)
                } else updatedClips
                track.copy(clips = resolved)
            } else track
        }

        val newDurationTotal = TimelineUtils.recalculateProjectDuration(updatedTracks)
        return state.copy(
            tracks = updatedTracks,
            durationMs = newDurationTotal
        )
    }

    private fun handleTrimEnd(
        state: TimelineEngineState,
        clipId: String,
        newEndTimeMs: Long
    ): TimelineEngineState {
        val clip = state.findClip(clipId) ?: return state
        val minDuration = TimelineEngineState.MIN_CLIP_DURATION_MS
        val minAllowedEndTime = clip.startTimeMs + minDuration

        val clampedEndTime = newEndTimeMs.coerceAtLeast(minAllowedEndTime)
        val newDuration = clampedEndTime - clip.startTimeMs
        val newOutPoint = clip.inPointMs + newDuration

        val updatedTracks = state.tracks.map { track ->
            if (track.clips.any { it.id == clipId }) {
                val updatedClips = track.clips.map { c ->
                    if (c.id == clipId) {
                        c.copy(
                            durationMs = newDuration,
                            outPointMs = newOutPoint
                        )
                    } else c
                }
                val resolved = if (track.type == TrackType.VIDEO) {
                    resolveMainTrackOverlaps(updatedClips)
                } else updatedClips
                track.copy(clips = resolved)
            } else track
        }

        val newDurationTotal = TimelineUtils.recalculateProjectDuration(updatedTracks)
        return state.copy(
            tracks = updatedTracks,
            durationMs = newDurationTotal
        )
    }

    private fun handleSplitAtPlayhead(state: TimelineEngineState, targetClipId: String?): TimelineEngineState {
        val clipIdToSplit = targetClipId ?: state.selectedClipId ?: findClipAtPlayhead(state) ?: return state
        return handleSplitClip(state, clipIdToSplit, state.playheadPositionMs)
    }

    private fun handleSplitClip(
        state: TimelineEngineState,
        clipId: String,
        splitPointMs: Long
    ): TimelineEngineState {
        val clip = state.findClip(clipId) ?: return state
        val minDuration = TimelineEngineState.MIN_CLIP_DURATION_MS

        // Validation: split point must be within clip bounds and not too close to ends
        val minValidSplit = clip.startTimeMs + minDuration
        val maxValidSplit = clip.endTimeMs - minDuration

        if (splitPointMs < minValidSplit || splitPointMs > maxValidSplit) {
            return state // Safe no-op if split point is invalid
        }

        val firstDuration = splitPointMs - clip.startTimeMs
        val secondDuration = clip.endTimeMs - splitPointMs

        val firstClip = clip.copy(
            durationMs = firstDuration,
            outPointMs = clip.inPointMs + firstDuration
        )

        val secondClip = clip.copy(
            id = UUID.randomUUID().toString(),
            startTimeMs = splitPointMs,
            durationMs = secondDuration,
            inPointMs = clip.inPointMs + firstDuration,
            outPointMs = clip.outPointMs
        )

        val updatedTracks = state.tracks.map { track ->
            if (track.clips.any { it.id == clipId }) {
                val newClips = mutableListOf<Clip>()
                track.clips.forEach { c ->
                    if (c.id == clipId) {
                        newClips.add(firstClip)
                        newClips.add(secondClip)
                    } else {
                        newClips.add(c)
                    }
                }
                val resolvedClips = if (track.type == TrackType.VIDEO) resolveMainTrackOverlaps(newClips) else newClips
                track.copy(clips = resolvedClips)
            } else track
        }

        return state.copy(
            tracks = updatedTracks,
            selectedClipId = secondClip.id
        )
    }

    private fun handleDeleteClip(state: TimelineEngineState, clipId: String): TimelineEngineState {
        val updatedTracks = state.tracks.map { track ->
            if (track.clips.any { it.id == clipId }) {
                val remaining = track.clips.filterNot { it.id == clipId }
                val resolved = if (track.type == TrackType.VIDEO) {
                    resolveMainTrackOverlaps(remaining)
                } else remaining
                track.copy(clips = resolved)
            } else track
        }

        val newDuration = TimelineUtils.recalculateProjectDuration(updatedTracks)
        val clampedPlayhead = TimelineUtils.clampPlayhead(state.playheadPositionMs, newDuration)

        return state.copy(
            tracks = updatedTracks,
            durationMs = newDuration,
            playheadPositionMs = clampedPlayhead,
            selectedClipId = if (state.selectedClipId == clipId) null else state.selectedClipId
        )
    }

    private fun handleDuplicateClip(state: TimelineEngineState, clipId: String): TimelineEngineState {
        val clip = state.findClip(clipId) ?: return state
        val targetTrack = state.findTrackForClip(clipId) ?: return state

        val duplicateClip = clip.copy(
            id = UUID.randomUUID().toString(),
            startTimeMs = clip.endTimeMs
        )

        val updatedTracks = state.tracks.map { track ->
            if (track.id == targetTrack.id) {
                val combined = track.clips + duplicateClip
                val resolved = if (track.type == TrackType.VIDEO) {
                    resolveMainTrackOverlaps(combined)
                } else {
                    combined.sortedBy { it.startTimeMs }
                }
                track.copy(clips = resolved)
            } else track
        }

        val newDuration = TimelineUtils.recalculateProjectDuration(updatedTracks)
        return state.copy(
            tracks = updatedTracks,
            durationMs = newDuration,
            selectedClipId = duplicateClip.id
        )
    }

    private fun handleUpdateClipSpeed(state: TimelineEngineState, clipId: String, speed: Float): TimelineEngineState {
        val safeSpeed = speed.coerceIn(0.1f, 10.0f)
        val updatedTracks = state.tracks.map { track ->
            val updated = track.clips.map { c ->
                if (c.id == clipId) {
                    // Recalculate timeline duration based on the speed
                    val rawDuration = c.outPointMs - c.inPointMs
                    val newDuration = (rawDuration / safeSpeed).toLong()
                    c.copy(speed = safeSpeed, durationMs = newDuration)
                } else c
            }
            if (track.type == com.example.core.model.TrackType.VIDEO && track.order == 0) {
                track.copy(clips = resolveMainTrackOverlaps(updated))
            } else {
                track.copy(clips = updated)
            }
        }
        return state.copy(tracks = updatedTracks, durationMs = TimelineUtils.recalculateProjectDuration(updatedTracks))
    }

    private fun handleUpdateClipVolume(state: TimelineEngineState, clipId: String, volume: Float): TimelineEngineState {
        val safeVolume = volume.coerceIn(0f, 2.0f)
        val updatedTracks = state.tracks.map { track ->
            val updated = track.clips.map { c ->
                if (c.id == clipId) c.copy(volume = safeVolume) else c
            }
            track.copy(clips = updated)
        }
        return state.copy(tracks = updatedTracks)
    }

    private fun handleUpdateClipTransform(
        state: TimelineEngineState,
        clipId: String,
        transform: com.example.core.model.Transform
    ): TimelineEngineState {
        val updatedTracks = state.tracks.map { track ->
            val updated = track.clips.map { c ->
                if (c.id == clipId) c.copy(transform = transform) else c
            }
            track.copy(clips = updated)
        }
        return state.copy(tracks = updatedTracks)
    }

    /**
     * Resolves overlaps and gaps for clips on the primary track:
     * Sorts clips by their current startTimeMs and ripples each clip
     * so it begins immediately at the previous clip's endTimeMs.
     * This creates a strictly gapless main track, which ensures 
     * ExoPlayer sequential playback stays in perfect sync.
     */
    fun resolveMainTrackOverlaps(clips: List<Clip>): List<Clip> {
        if (clips.isEmpty()) return clips
        val sorted = clips.sortedWith(compareBy<Clip> { it.startTimeMs }.thenBy { it.id })
        val resolved = mutableListOf<Clip>()
        var currentEndTime = 0L

        sorted.forEach { clip ->
            val actualStart = currentEndTime
            val updated = if (actualStart != clip.startTimeMs) {
                clip.copy(startTimeMs = actualStart)
            } else {
                clip
            }
            resolved.add(updated)
            currentEndTime = updated.endTimeMs
        }

        return resolved
    }

    private fun findClipAtPlayhead(state: TimelineEngineState): String? {
        val playhead = state.playheadPositionMs
        return state.tracks.flatMap { it.clips }
            .find { playhead >= it.startTimeMs && playhead < it.endTimeMs }?.id
    }
}
