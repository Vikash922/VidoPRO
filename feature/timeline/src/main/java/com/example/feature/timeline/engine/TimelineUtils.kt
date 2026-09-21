package com.example.feature.timeline.engine

import com.example.core.model.Track
import kotlin.math.abs

/**
 * Pure Kotlin utilities for timeline math, coordinate conversions, and snapping logic.
 * Conforms to TIMELINE_ENGINE_SPEC.md.
 */
object TimelineUtils {

    const val BASE_PIXELS_PER_SECOND = 60f

    /**
     * Calculates the horizontal scale factor (pixels per millisecond) based on current zoom level.
     */
    fun calculatePixelsPerMs(
        zoomLevel: Float,
        basePixelsPerSecond: Float = BASE_PIXELS_PER_SECOND
    ): Float {
        val clampedZoom = zoomLevel.coerceIn(TimelineEngineState.MIN_ZOOM, TimelineEngineState.MAX_ZOOM)
        return (basePixelsPerSecond * clampedZoom) / 1000f
    }

    /**
     * Converts a millisecond timestamp to horizontal pixel coordinate X.
     */
    fun timeToX(timeMs: Long, pixelsPerMs: Float): Float {
        return timeMs.coerceAtLeast(0L) * pixelsPerMs
    }

    /**
     * Converts a horizontal pixel coordinate X to millisecond timestamp.
     */
    fun xToTime(x: Float, pixelsPerMs: Float): Long {
        if (pixelsPerMs <= 0f) return 0L
        return (x / pixelsPerMs).toLong().coerceAtLeast(0L)
    }

    /**
     * Finds the closest snap point within [thresholdMs] of [targetTimeMs].
     * If no snap point is within the threshold, returns [targetTimeMs] unchanged.
     */
    fun snapTime(
        targetTimeMs: Long,
        snapPoints: List<Long>,
        thresholdMs: Long = TimelineEngineState.SNAP_THRESHOLD_MS
    ): Long {
        var closest = targetTimeMs
        var minDiff = thresholdMs + 1

        for (point in snapPoints) {
            val diff = abs(targetTimeMs - point)
            if (diff <= thresholdMs && diff < minDiff) {
                minDiff = diff
                closest = point
            }
        }
        return closest
    }

    /**
     * Extracts all deterministic snap points (clip start, clip end, 0ms) across all tracks,
     * optionally excluding a clip currently being edited.
     */
    fun calculateSnapPoints(
        tracks: List<Track>,
        excludeClipId: String? = null
    ): List<Long> {
        val points = mutableSetOf(0L)

        tracks.forEach { track ->
            track.clips.forEach { clip ->
                if (clip.id != excludeClipId) {
                    points.add(clip.startTimeMs)
                    points.add(clip.endTimeMs)
                }
            }
        }

        return points.sorted()
    }

    /**
     * Recalculates total project duration as the maximum endTimeMs across all tracks and clips.
     */
    fun recalculateProjectDuration(tracks: List<Track>): Long {
        return tracks.maxOfOrNull { track ->
            track.clips.maxOfOrNull { it.endTimeMs } ?: 0L
        } ?: 0L
    }

    /**
     * Clamps playhead position within valid range [0, durationMs].
     */
    fun clampPlayhead(positionMs: Long, durationMs: Long): Long {
        return positionMs.coerceIn(0L, durationMs.coerceAtLeast(0L))
    }
}
