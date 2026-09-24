package com.example.feature.timeline.engine

import com.example.core.model.Clip
import com.example.core.model.InterpolationType
import com.example.core.model.Keyframe
import com.example.core.model.Track
import com.example.core.model.TrackType
import com.example.core.model.Transition
import com.example.core.model.TransitionValidator
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
            is TimelineAction.SelectClip -> {
                val newKfId = if (action.clipId != state.selectedClipId) null else state.selectedKeyframeId
                state.copy(selectedClipId = action.clipId, selectedKeyframeId = newKfId, multiSelectedClipIds = emptySet())
            }
            is TimelineAction.SelectKeyframe -> state.copy(selectedKeyframeId = action.keyframeId)
            is TimelineAction.SelectMultipleClips -> state.copy(
                multiSelectedClipIds = action.clipIds,
                selectedClipId = action.clipIds.firstOrNull()
            )
            is TimelineAction.ToggleClipSelection -> {
                val updated = if (state.multiSelectedClipIds.contains(action.clipId)) {
                    state.multiSelectedClipIds - action.clipId
                } else {
                    state.multiSelectedClipIds + action.clipId
                }
                state.copy(multiSelectedClipIds = updated, selectedClipId = updated.firstOrNull() ?: state.selectedClipId)
            }
            is TimelineAction.GroupSelectedClips -> handleGroupClips(state, action.groupId)
            is TimelineAction.UngroupClips -> handleUngroupClips(state, action.groupId)
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
            is TimelineAction.AddKeyframe -> handleAddKeyframe(state, action.clipId, action.property, action.timeMs, action.value, action.interpolation)
            is TimelineAction.UpdateKeyframe -> handleUpdateKeyframe(state, action.clipId, action.keyframeId, action.value, action.interpolation)
            is TimelineAction.DeleteKeyframe -> handleDeleteKeyframe(state, action.clipId, action.keyframeId)
            is TimelineAction.MoveKeyframe -> handleMoveKeyframe(state, action.clipId, action.keyframeId, action.newTimeMs)
            is TimelineAction.UpdateClipEffects -> handleUpdateClipEffects(state, action.clipId, action.effects)
            is TimelineAction.AddClipEffect -> handleAddClipEffect(state, action.clipId, action.effect)
            is TimelineAction.UpdateClipEffect -> handleUpdateClipEffect(state, action.clipId, action.effect)
            is TimelineAction.RemoveClipEffect -> handleRemoveClipEffect(state, action.clipId, action.effectId)
            is TimelineAction.ReorderClipEffects -> handleReorderClipEffects(state, action.clipId, action.effectIdsInOrder)
            is TimelineAction.ResetClipEffects -> handleResetClipEffects(state, action.clipId)
            is TimelineAction.SetClipMask -> handleSetClipMask(state, action.clipId, action.mask)
            is TimelineAction.SetClipBlendMode -> handleSetClipBlendMode(state, action.clipId, action.blendMode)
            is TimelineAction.SetClipOpacity -> handleSetClipOpacity(state, action.clipId, action.opacity)
            is TimelineAction.AddTransition -> handleAddTransition(state, action.transition)
            is TimelineAction.UpdateTransition -> handleUpdateTransition(state, action.transition)
            is TimelineAction.RemoveTransition -> handleRemoveTransition(state, action.transitionId)
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
                    val deltaMs = safeStartTime - sourceClip.startTimeMs
                    val shiftedKeyframes = if (deltaMs != 0L) {
                        sourceClip.keyframes.map { it.copy(timeMs = it.timeMs + deltaMs) }
                    } else sourceClip.keyframes
                    val movedClip = sourceClip.copy(
                        trackId = targetTrackId,
                        startTimeMs = safeStartTime,
                        keyframes = shiftedKeyframes
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
                            inPointMs = newInPoint,
                            keyframes = c.keyframes.filter { it.timeMs in clampedStartTime..originalEnd }
                        )
                    } else c
                }
                val resolved = if (track.type == TrackType.VIDEO) {
                    resolveMainTrackOverlaps(updatedClips)
                } else updatedClips
                val sanitizedTransitions = sanitizeTransitions(track.copy(clips = resolved))
                track.copy(clips = resolved, transitions = sanitizedTransitions)
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
                            outPointMs = newOutPoint,
                            keyframes = c.keyframes.filter { it.timeMs in c.startTimeMs..clampedEndTime }
                        )
                    } else c
                }
                val resolved = if (track.type == TrackType.VIDEO) {
                    resolveMainTrackOverlaps(updatedClips)
                } else updatedClips
                val sanitizedTransitions = sanitizeTransitions(track.copy(clips = resolved))
                track.copy(clips = resolved, transitions = sanitizedTransitions)
            } else track
        }

        val newDurationTotal = TimelineUtils.recalculateProjectDuration(updatedTracks)
        return state.copy(
            tracks = updatedTracks,
            durationMs = newDurationTotal
        )
    }

    private fun handleSplitAtPlayhead(state: TimelineEngineState, targetClipId: String?): TimelineEngineState {
        val playhead = state.playheadPositionMs
        val candidateId = targetClipId ?: state.selectedClipId
        val candidateClip = candidateId?.let { state.findClip(it) }
        val clipIdToSplit = if (candidateClip != null && playhead >= candidateClip.startTimeMs && playhead <= candidateClip.endTimeMs) {
            candidateClip.id
        } else {
            findClipAtPlayhead(state) ?: return state
        }
        return handleSplitClip(state, clipIdToSplit, playhead)
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

        val clampedSplit = splitPointMs
        val firstDuration = clampedSplit - clip.startTimeMs
        val secondDuration = clip.endTimeMs - clampedSplit

        val firstClip = clip.copy(
            durationMs = firstDuration,
            outPointMs = clip.inPointMs + firstDuration,
            keyframes = clip.keyframes.filter { it.timeMs <= clampedSplit }
        )

        val secondClipId = UUID.randomUUID().toString()
        val secondClip = clip.copy(
            id = secondClipId,
            startTimeMs = clampedSplit,
            durationMs = secondDuration,
            inPointMs = clip.inPointMs + firstDuration,
            outPointMs = clip.outPointMs,
            keyframes = clip.keyframes.filter { it.timeMs >= clampedSplit }.map {
                it.copy(id = UUID.randomUUID().toString(), clipId = secondClipId)
            }
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
                val remappedTransitions = track.transitions.map { trans ->
                    if (trans.firstClipId == clipId) trans.copy(firstClipId = secondClipId) else trans
                }
                val sanitizedTransitions = sanitizeTransitions(track.copy(clips = resolvedClips, transitions = remappedTransitions))
                track.copy(clips = resolvedClips, transitions = sanitizedTransitions)
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
                val remainingTransitions = track.transitions.filterNot { it.firstClipId == clipId || it.secondClipId == clipId }
                track.copy(clips = resolved, transitions = remainingTransitions)
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
            startTimeMs = if (targetTrack.type == TrackType.OVERLAY) clip.startTimeMs else clip.endTimeMs
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

    private fun handleUpdateClipEffects(
        state: TimelineEngineState,
        clipId: String,
        effects: List<com.example.core.model.Effect>
    ): TimelineEngineState {
        val updatedTracks = state.tracks.map { track ->
            val updated = track.clips.map { c ->
                if (c.id == clipId) c.copy(effects = effects) else c
            }
            track.copy(clips = updated)
        }
        return state.copy(
            tracks = updatedTracks
        )
    }

    private fun handleAddClipEffect(
        state: TimelineEngineState,
        clipId: String,
        effect: com.example.core.model.Effect
    ): TimelineEngineState {
        val clip = state.findClip(clipId) ?: return state
        val existingIndex = clip.effects.indexOfFirst { it.id == effect.id || it.type == effect.type }
        val updatedEffects = if (existingIndex >= 0) {
            clip.effects.mapIndexed { idx, e -> if (idx == existingIndex) effect else e }
        } else {
            clip.effects + effect.copy(order = clip.effects.size)
        }.sortedBy { it.order }

        return handleUpdateClipEffects(state, clipId, updatedEffects)
    }

    private fun handleUpdateClipEffect(
        state: TimelineEngineState,
        clipId: String,
        effect: com.example.core.model.Effect
    ): TimelineEngineState {
        val clip = state.findClip(clipId) ?: return state
        val updatedEffects = clip.effects.map { if (it.id == effect.id) effect else it }
        return handleUpdateClipEffects(state, clipId, updatedEffects)
    }

    private fun handleRemoveClipEffect(
        state: TimelineEngineState,
        clipId: String,
        effectId: String
    ): TimelineEngineState {
        val clip = state.findClip(clipId) ?: return state
        val updatedEffects = clip.effects.filterNot { it.id == effectId }
        return handleUpdateClipEffects(state, clipId, updatedEffects)
    }

    private fun handleReorderClipEffects(
        state: TimelineEngineState,
        clipId: String,
        effectIdsInOrder: List<String>
    ): TimelineEngineState {
        val clip = state.findClip(clipId) ?: return state
        val effectMap = clip.effects.associateBy { it.id }
        val reordered = effectIdsInOrder.mapIndexedNotNull { index, id ->
            effectMap[id]?.copy(order = index)
        }
        val remaining = clip.effects.filterNot { it.id in effectIdsInOrder }
            .mapIndexed { index, e -> e.copy(order = reordered.size + index) }
        return handleUpdateClipEffects(state, clipId, reordered + remaining)
    }

    private fun handleResetClipEffects(
        state: TimelineEngineState,
        clipId: String
    ): TimelineEngineState {
        val clip = state.findClip(clipId) ?: return state
        val nonFilter = clip.effects.filter {
            it.type == com.example.core.model.EffectType.MASK ||
            it.type == com.example.core.model.EffectType.BLEND_MODE
        }
        return handleUpdateClipEffects(state, clipId, nonFilter)
    }

    private fun handleSetClipMask(
        state: TimelineEngineState,
        clipId: String,
        mask: com.example.core.model.ClipMask?
    ): TimelineEngineState {
        val clip = state.findClip(clipId) ?: return state
        val updatedEffects = clip.effects.filterNot { it.type == com.example.core.model.EffectType.MASK }.toMutableList()
        if (mask != null) {
            updatedEffects.add(mask.toEffect(clipId))
        }
        val updatedTracks = state.tracks.map { track ->
            val updated = track.clips.map { c ->
                if (c.id == clipId) c.copy(mask = mask, effects = updatedEffects) else c
            }
            track.copy(clips = updated)
        }
        return state.copy(tracks = updatedTracks)
    }

    private fun handleSetClipBlendMode(
        state: TimelineEngineState,
        clipId: String,
        blendMode: com.example.core.model.BlendMode
    ): TimelineEngineState {
        val clip = state.findClip(clipId) ?: return state
        val updatedEffects = clip.effects.filterNot { it.type == com.example.core.model.EffectType.BLEND_MODE }.toMutableList()
        if (blendMode != com.example.core.model.BlendMode.NORMAL) {
            updatedEffects.add(
                com.example.core.model.Effect(
                    id = java.util.UUID.randomUUID().toString(),
                    clipId = clipId,
                    type = com.example.core.model.EffectType.BLEND_MODE,
                    order = 999,
                    isEnabled = true,
                    parameters = mapOf("mode" to blendMode.ordinal.toFloat())
                )
            )
        }
        val updatedTracks = state.tracks.map { track ->
            val updated = track.clips.map { c ->
                if (c.id == clipId) c.copy(blendMode = blendMode, effects = updatedEffects) else c
            }
            track.copy(clips = updated)
        }
        return state.copy(tracks = updatedTracks)
    }

    private fun handleSetClipOpacity(
        state: TimelineEngineState,
        clipId: String,
        opacity: Float
    ): TimelineEngineState {
        val clip = state.findClip(clipId) ?: return state
        val clamped = opacity.coerceIn(0f, 1f)
        val newTransform = clip.transform.copy(opacity = clamped)
        return handleUpdateClipTransform(state, clipId, newTransform)
    }

    /**
     * Resolves overlaps and gaps for clips on the primary track:
     * Sorts clips by their current startTimeMs and ripples each clip
     * so it begins immediately at the previous clip's endTimeMs.
     * This creates a strictly gapless main track, which ensures 
     * ExoPlayer sequential playback stays in perfect sync.
     */
    fun resolveMainTrackOverlaps(clips: List<Clip>): List<Clip> {
        if (clips.size <= 1) return clips
        val sorted = clips.sortedWith(compareBy<Clip> { it.startTimeMs }.thenBy { it.id })
        val resolved = mutableListOf<Clip>()
        var currentEndTime = 0L

        sorted.forEach { clip ->
            val actualStart = maxOf(clip.startTimeMs, currentEndTime)
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
        val videoClips = state.tracks.filter { it.type == TrackType.VIDEO }.flatMap { it.clips }
        val videoHit = videoClips.find { playhead >= it.startTimeMs && playhead < it.endTimeMs }
        if (videoHit != null) return videoHit.id

        return state.tracks.flatMap { it.clips }
            .find { playhead >= it.startTimeMs && playhead < it.endTimeMs }?.id
    }

    /** Tags all multiSelectedClips with the given groupId. */
    private fun handleGroupClips(state: TimelineEngineState, groupId: String): TimelineEngineState {
        val ids = if (state.multiSelectedClipIds.isNotEmpty()) state.multiSelectedClipIds
                  else setOfNotNull(state.selectedClipId)
        if (ids.size < 2) return state
        val updatedTracks = state.tracks.map { track ->
            track.copy(clips = track.clips.map { clip ->
                if (ids.contains(clip.id)) clip.copy(groupId = groupId) else clip
            })
        }
        return state.copy(tracks = updatedTracks)
    }

    /** Clears the groupId from all clips in the given group. */
    private fun handleUngroupClips(state: TimelineEngineState, groupId: String): TimelineEngineState {
        val updatedTracks = state.tracks.map { track ->
            track.copy(clips = track.clips.map { clip ->
                if (clip.groupId == groupId) clip.copy(groupId = null) else clip
            })
        }
        return state.copy(tracks = updatedTracks, multiSelectedClipIds = emptySet())
    }

    /** Adds or updates a keyframe on a clip. Enforces clip boundary and uniqueness per (property, timeMs). */
    private fun handleAddKeyframe(
        state: TimelineEngineState,
        clipId: String,
        property: String,
        timeMs: Long,
        value: Float,
        interpolation: InterpolationType
    ): TimelineEngineState {
        val clip = state.findClip(clipId) ?: return state
        val clampedTime = timeMs.coerceIn(clip.startTimeMs, clip.endTimeMs)
        val existingIndex = clip.keyframes.indexOfFirst { it.property == property && it.timeMs == clampedTime }
        val updatedKeyframes = if (existingIndex >= 0) {
            clip.keyframes.mapIndexed { idx, kf ->
                if (idx == existingIndex) kf.copy(value = value, interpolation = interpolation) else kf
            }
        } else {
            val newKf = Keyframe(
                id = UUID.randomUUID().toString(),
                clipId = clipId,
                property = property,
                timeMs = clampedTime,
                value = value,
                interpolation = interpolation
            )
            clip.keyframes + newKf
        }.sortedWith(compareBy({ it.timeMs }, { it.property }))

        val updatedTracks = state.tracks.map { track ->
            track.copy(clips = track.clips.map { c ->
                if (c.id == clipId) c.copy(keyframes = updatedKeyframes) else c
            })
        }
        val targetKfId = if (existingIndex >= 0) clip.keyframes[existingIndex].id else updatedKeyframes.find { it.property == property && it.timeMs == clampedTime }?.id
        return state.copy(tracks = updatedTracks, selectedKeyframeId = targetKfId)
    }

    /** Updates value and/or interpolation on an existing keyframe. */
    private fun handleUpdateKeyframe(
        state: TimelineEngineState,
        clipId: String,
        keyframeId: String,
        value: Float,
        interpolation: InterpolationType
    ): TimelineEngineState {
        val clip = state.findClip(clipId) ?: return state
        val updatedKeyframes = clip.keyframes.map { kf ->
            if (kf.id == keyframeId) kf.copy(value = value, interpolation = interpolation) else kf
        }
        val updatedTracks = state.tracks.map { track ->
            track.copy(clips = track.clips.map { c ->
                if (c.id == clipId) c.copy(keyframes = updatedKeyframes) else c
            })
        }
        return state.copy(tracks = updatedTracks, selectedKeyframeId = keyframeId)
    }

    /** Deletes a keyframe from a clip. */
    private fun handleDeleteKeyframe(
        state: TimelineEngineState,
        clipId: String,
        keyframeId: String
    ): TimelineEngineState {
        val clip = state.findClip(clipId) ?: return state
        val updatedKeyframes = clip.keyframes.filterNot { it.id == keyframeId }
        val updatedTracks = state.tracks.map { track ->
            track.copy(clips = track.clips.map { c ->
                if (c.id == clipId) c.copy(keyframes = updatedKeyframes) else c
            })
        }
        return state.copy(
            tracks = updatedTracks,
            selectedKeyframeId = if (state.selectedKeyframeId == keyframeId) null else state.selectedKeyframeId
        )
    }

    /** Moves a keyframe diamond to a new timeMs on the timeline, respecting clip boundaries. */
    private fun handleMoveKeyframe(
        state: TimelineEngineState,
        clipId: String,
        keyframeId: String,
        newTimeMs: Long
    ): TimelineEngineState {
        val clip = state.findClip(clipId) ?: return state
        val clampedTime = newTimeMs.coerceIn(clip.startTimeMs, clip.endTimeMs)
        val targetKf = clip.keyframes.find { it.id == keyframeId } ?: return state
        val updatedKeyframes = clip.keyframes.map { kf ->
            if (kf.id == keyframeId) kf.copy(timeMs = clampedTime) else kf
        }.distinctBy { it.property to it.timeMs }
         .sortedWith(compareBy({ it.timeMs }, { it.property }))

        val updatedTracks = state.tracks.map { track ->
            track.copy(clips = track.clips.map { c ->
                if (c.id == clipId) c.copy(keyframes = updatedKeyframes) else c
            })
        }
        return state.copy(tracks = updatedTracks, selectedKeyframeId = keyframeId)
    }

    /** Sanitizes and clamps all transitions for a track based on the actual duration of adjacent clips. */
    private fun sanitizeTransitions(track: Track): List<Transition> {
        val clipMap = track.clips.associateBy { it.id }
        return track.transitions.mapNotNull { transition ->
            val clipA = clipMap[transition.firstClipId] ?: return@mapNotNull null
            val clipB = clipMap[transition.secondClipId] ?: return@mapNotNull null
            val clampedDuration = TransitionValidator.validateAndClamp(
                transition.durationMs,
                clipA.durationMs,
                clipB.durationMs
            )
            if (clampedDuration > 0L) {
                transition.copy(durationMs = clampedDuration)
            } else {
                null
            }
        }
    }

    /** Adds or replaces a transition between two adjacent clips on a track. */
    private fun handleAddTransition(state: TimelineEngineState, transition: Transition): TimelineEngineState {
        val targetTrack = state.tracks.find { track ->
            track.clips.any { it.id == transition.firstClipId } &&
            track.clips.any { it.id == transition.secondClipId }
        } ?: return state

        val clipA = targetTrack.clips.find { it.id == transition.firstClipId } ?: return state
        val clipB = targetTrack.clips.find { it.id == transition.secondClipId } ?: return state

        val clampedDuration = TransitionValidator.validateAndClamp(
            transition.durationMs,
            clipA.durationMs,
            clipB.durationMs
        )
        if (clampedDuration <= 0L) return state

        val validatedTransition = transition.copy(durationMs = clampedDuration)

        val updatedTracks = state.tracks.map { track ->
            if (track.id == targetTrack.id) {
                val filteredTransitions = track.transitions.filterNot {
                    (it.firstClipId == transition.firstClipId && it.secondClipId == transition.secondClipId) ||
                    it.id == transition.id
                }
                track.copy(transitions = filteredTransitions + validatedTransition)
            } else {
                track
            }
        }
        return state.copy(tracks = updatedTracks)
    }

    /** Updates an existing transition's parameters (e.g. type, duration, properties). */
    private fun handleUpdateTransition(state: TimelineEngineState, transition: Transition): TimelineEngineState {
        val targetTrack = state.tracks.find { track ->
            track.transitions.any { it.id == transition.id } ||
            (track.clips.any { it.id == transition.firstClipId } && track.clips.any { it.id == transition.secondClipId })
        } ?: return state

        val clipA = targetTrack.clips.find { it.id == transition.firstClipId }
        val clipB = targetTrack.clips.find { it.id == transition.secondClipId }

        val clampedDuration = if (clipA != null && clipB != null) {
            TransitionValidator.validateAndClamp(
                transition.durationMs,
                clipA.durationMs,
                clipB.durationMs
            )
        } else {
            transition.durationMs
        }

        val validatedTransition = transition.copy(durationMs = clampedDuration)

        val updatedTracks = state.tracks.map { track ->
            if (track.id == targetTrack.id) {
                val updatedTransitions = track.transitions.map {
                    if (it.id == transition.id) validatedTransition else it
                }
                track.copy(transitions = updatedTransitions)
            } else {
                track
            }
        }
        return state.copy(tracks = updatedTracks)
    }

    /** Removes a transition by ID across all tracks. */
    private fun handleRemoveTransition(state: TimelineEngineState, transitionId: String): TimelineEngineState {
        val updatedTracks = state.tracks.map { track ->
            track.copy(transitions = track.transitions.filterNot { it.id == transitionId })
        }
        return state.copy(tracks = updatedTracks)
    }
}

