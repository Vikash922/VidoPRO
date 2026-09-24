package com.example.core.media

import com.example.core.model.Asset
import com.example.core.model.Clip
import com.example.core.model.Project
import com.example.core.model.Track
import com.example.core.model.TrackType

/**
 * Represents a single audio segment along the linear timeline.
 * An audio track is composed of [ClipSegment]s and [GapSegment]s.
 */
sealed class AudioTimelineSegment {
    abstract val startTimeMs: Long
    abstract val durationMs: Long
    val endTimeMs: Long get() = startTimeMs + durationMs

    data class ClipSegment(
        override val startTimeMs: Long,
        override val durationMs: Long,
        val clip: Clip,
        val asset: Asset,
        val sourceInPointMs: Long,
        val sourceOutPointMs: Long,
        val speed: Float,
        val volume: Float
    ) : AudioTimelineSegment()

    data class GapSegment(
        override val startTimeMs: Long,
        override val durationMs: Long
    ) : AudioTimelineSegment()
}

/**
 * Deterministic timeline-to-audio mapping engine (DEV-070, FIX-06).
 *
 * Translates project audio tracks and clips into strictly contiguous
 * sequences of [ClipSegment] and [GapSegment] instances.
 *
 * Ensures timeline semantics are identical between Preview and Export:
 * - Leading gaps: silence before the first clip starts
 * - Middle gaps: silence between consecutive clips
 * - Trailing gaps: silence after the last clip until project end
 * - Overlapping clips: partitioned into independent concurrent tracks/layers
 * - Speed changes: properly factored into source trimming and playback parameters
 * - Trim: precise in-point and out-point mapping
 * - Muted tracks / muted clips: treated as silent gaps
 */
object AudioTimelineMapper {

    /**
     * Maps a list of audio clips into a contiguous timeline of [AudioTimelineSegment]s.
     * All gaps (leading, middle, trailing) are filled with [AudioTimelineSegment.GapSegment].
     */
    fun mapClipsToSegments(
        clips: List<Clip>,
        assets: Map<String, Asset>,
        totalDurationMs: Long = 0L
    ): List<AudioTimelineSegment> {
        val validClips = clips
            .filter { clip ->
                clip.isVisible &&
                (clip.volume ?: 1f) > 0f &&
                clip.durationMs > 0L &&
                clip.assetId != null &&
                assets.containsKey(clip.assetId)
            }
            .sortedBy { it.startTimeMs }

        if (validClips.isEmpty()) {
            return if (totalDurationMs > 0L) {
                listOf(AudioTimelineSegment.GapSegment(0L, totalDurationMs))
            } else {
                emptyList()
            }
        }

        val segments = mutableListOf<AudioTimelineSegment>()
        var currentTimeMs = 0L

        for (clip in validClips) {
            val asset = assets[clip.assetId] ?: continue

            // 1. Fill leading or middle gap
            if (clip.startTimeMs > currentTimeMs) {
                val gapDuration = clip.startTimeMs - currentTimeMs
                segments.add(AudioTimelineSegment.GapSegment(currentTimeMs, gapDuration))
                currentTimeMs = clip.startTimeMs
            }

            // 2. Build Clip Segment
            val inPoint = clip.inPointMs.coerceAtLeast(0L)
            val sourceDuration = (clip.durationMs * clip.speed.coerceAtLeast(0.1f)).toLong()
            val outPoint = if (clip.outPointMs > inPoint) {
                clip.outPointMs
            } else {
                inPoint + sourceDuration
            }

            val segmentDuration = clip.durationMs
            segments.add(
                AudioTimelineSegment.ClipSegment(
                    startTimeMs = clip.startTimeMs,
                    durationMs = segmentDuration,
                    clip = clip,
                    asset = asset,
                    sourceInPointMs = inPoint,
                    sourceOutPointMs = outPoint,
                    speed = clip.speed.coerceIn(0.1f, 4.0f),
                    volume = (clip.volume ?: 1f).coerceIn(0f, 1f)
                )
            )
        currentTimeMs = maxOf(currentTimeMs, clip.startTimeMs + segmentDuration)
    }

    // 3. Fill trailing gap up to totalDurationMs
    if (totalDurationMs > currentTimeMs) {
        segments.add(AudioTimelineSegment.GapSegment(currentTimeMs, totalDurationMs - currentTimeMs))
    }

    return segments
}

    /**
     * Maps a list of audio clips (which may contain overlapping clips) into one or more
     * independent non-overlapping layers of [AudioTimelineSegment]s.
     * Each layer covers the complete timeline from 0 to [totalDurationMs].
     */
    fun mapClipsToLayers(
        clips: List<Clip>,
        assets: Map<String, Asset>,
        totalDurationMs: Long = 0L
    ): List<List<AudioTimelineSegment>> {
        val validClips = clips
            .filter { clip ->
                clip.isVisible &&
                (clip.volume ?: 1f) > 0f &&
                clip.durationMs > 0L &&
                clip.assetId != null &&
                assets.containsKey(clip.assetId)
            }
            .sortedBy { it.startTimeMs }

        if (validClips.isEmpty()) {
            return if (totalDurationMs > 0L) {
                listOf(listOf(AudioTimelineSegment.GapSegment(0L, totalDurationMs)))
            } else {
                emptyList()
            }
        }

        val layers = partitionIntoNonOverlappingLayers(validClips)
        return layers.map { layer ->
            mapClipsToSegments(layer, assets, totalDurationMs)
        }
    }

    /**
     * Maps a single [Track] into contiguous timeline segments.
     * Returns empty list if track is muted or not an audio track.
     */
    fun mapTrackToSegments(
        track: Track,
        assets: Map<String, Asset>,
        totalDurationMs: Long = 0L
    ): List<AudioTimelineSegment> {
        if (!track.isVisible || track.type != TrackType.AUDIO) {
            return emptyList()
        }
        return mapClipsToSegments(track.clips, assets, totalDurationMs)
    }

    /**
     * Partitions a list of clips (potentially overlapping) into minimum non-overlapping layers.
     * Each layer is guaranteed to be chronologically sequential with no overlapping clips.
     */
    fun partitionIntoNonOverlappingLayers(clips: List<Clip>): List<List<Clip>> {
        val sorted = clips.sortedBy { it.startTimeMs }
        val layers = mutableListOf<MutableList<Clip>>()

        for (clip in sorted) {
            var placed = false
            for (layer in layers) {
                val lastClip = layer.last()
                if (clip.startTimeMs >= lastClip.endTimeMs) {
                    layer.add(clip)
                    placed = true
                    break
                }
            }
            if (!placed) {
                layers.add(mutableListOf(clip))
            }
        }
        return layers
    }

    /**
     * Maps all visible audio tracks in a [Project] to a list of contiguous segment sequences,
     * one sequence per track (or non-overlapping layer).
     */
    fun mapProjectAudioTracks(
        project: Project,
        assets: Map<String, Asset>,
        timelineDurationMs: Long = project.durationMs
    ): List<List<AudioTimelineSegment>> {
        val effectiveDuration = if (timelineDurationMs > 0L) timelineDurationMs else project.durationMs
        val visibleAudioTracks = project.tracks.filter { it.type == TrackType.AUDIO && it.isVisible }

        val result = mutableListOf<List<AudioTimelineSegment>>()
        for (track in visibleAudioTracks) {
            val layers = partitionIntoNonOverlappingLayers(track.clips)
            for (layer in layers) {
                val segments = mapClipsToSegments(layer, assets, effectiveDuration)
                if (segments.isNotEmpty() && segments.any { it is AudioTimelineSegment.ClipSegment }) {
                    result.add(segments)
                }
            }
        }
        return result
    }
}
