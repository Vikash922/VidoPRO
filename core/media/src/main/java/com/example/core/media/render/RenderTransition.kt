package com.example.core.media.render

import com.example.core.model.TransitionType

/**
 * Universal transition description inside a [RenderScene].
 *
 * Represents an active transition between [firstClipId] (outgoing) and [secondClipId] (incoming).
 *
 * Boundary: [boundaryTimeMs] is the cut point on the timeline.
 * Centered window:
 *  - [startTimeMs] = boundaryTimeMs - durationMs / 2
 *  - [endTimeMs]   = boundaryTimeMs + durationMs / 2
 *
 * Progress evaluation:
 *  - At [startTimeMs]: progress = 0.0 (first clip 100% visible, second clip entering)
 *  - At [boundaryTimeMs]: progress = 0.5 (exact 50% midpoint blend/movement)
 *  - At [endTimeMs]: progress = 1.0 (first clip exited, second clip 100% visible)
 */
data class RenderTransition(
    val id: String,
    val trackId: String,
    val firstClipId: String,
    val secondClipId: String,
    val type: TransitionType,
    val durationMs: Long,
    val boundaryTimeMs: Long,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val direction: String = "LEFT",
    val parametersJson: String? = null
) {
    /**
     * Calculates deterministic normalized progress in range [0.0, 1.0] at [projectTimeMs].
     */
    fun progressAt(projectTimeMs: Long): Float {
        if (durationMs <= 0L) return 1f
        return ((projectTimeMs - startTimeMs).toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Returns whether the transition window is currently active at [projectTimeMs].
     */
    fun isActiveAt(projectTimeMs: Long): Boolean =
        projectTimeMs >= startTimeMs && projectTimeMs < endTimeMs
}
