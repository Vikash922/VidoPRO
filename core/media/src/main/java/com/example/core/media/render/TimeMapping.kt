package com.example.core.media.render

/**
 * Single deterministic time-mapping pipeline for VidoPRO timeline compositing.
 *
 * Maps:
 * Project Timeline Time -> Layer Time -> Source Media Time
 *
 * Evaluated uniformly by:
 * - Preview playback & seeking
 * - Export frame rendering
 * - Scrubbing & playhead synchronization
 * - Keyframe evaluation
 * - Video overlays / PiP
 */
object TimeMapping {

    /**
     * Calculates the local layer playback time (elapsed time since layer started on timeline).
     *
     * @param projectTimeMs Current project timeline position in milliseconds.
     * @param layerStartMs Timeline start position of the layer in milliseconds.
     * @return Layer elapsed time in milliseconds (clamped to non-negative).
     */
    fun projectTimeToLayerTime(projectTimeMs: Long, layerStartMs: Long): Long {
        return (projectTimeMs - layerStartMs).coerceAtLeast(0L)
    }

    /**
     * Calculates the exact source media timestamp (accounting for trim inPoint and playback speed).
     *
     * Example:
     * Layer starts at 5s (5000ms), sourceInPoint is 12s (12000ms), speed is 1.0x.
     * At project time 8s (8000ms):
     * layerTime = 3000ms
     * sourceMediaTime = 12000ms + (3000ms * 1.0) = 15000ms (15s)
     *
     * If speed is 2.0x:
     * sourceMediaTime = 12000ms + (3000ms * 2.0) = 18000ms (18s)
     *
     * @param projectTimeMs Current project timeline position in milliseconds.
     * @param layerStartMs Timeline start position of the layer in milliseconds.
     * @param sourceInPointMs Trim start point inside the source media file in milliseconds.
     * @param speed Playback speed multiplier (default 1.0f).
     * @return Exact source media timestamp in milliseconds.
     */
    fun projectTimeToSourceTime(
        projectTimeMs: Long,
        layerStartMs: Long,
        sourceInPointMs: Long,
        speed: Float = 1.0f
    ): Long {
        val layerOffsetMs = (projectTimeMs - layerStartMs).coerceAtLeast(0L)
        val scaledOffsetMs = (layerOffsetMs * speed.coerceAtLeast(0.01f)).toLong()
        return sourceInPointMs + scaledOffsetMs
    }

    /**
     * Calculates project timeline time from a given layer elapsed time.
     */
    fun layerTimeToProjectTime(layerTimeMs: Long, layerStartMs: Long): Long {
        return layerStartMs + layerTimeMs.coerceAtLeast(0L)
    }

    /**
     * Determines whether a layer is active at a given project timeline timestamp.
     *
     * A layer is active if: timelineStartMs <= projectTimeMs < timelineEndMs.
     */
    fun isLayerActive(projectTimeMs: Long, timelineStartMs: Long, timelineEndMs: Long): Boolean {
        return projectTimeMs >= timelineStartMs && projectTimeMs < timelineEndMs
    }
}
