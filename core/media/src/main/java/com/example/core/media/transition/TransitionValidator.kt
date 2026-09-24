package com.example.core.media.transition

import com.example.core.model.Clip

data class TransitionValidationResult(
    val isValid: Boolean,
    val clampedDurationMs: Long,
    val errorMessage: String? = null
)

/**
 * Validates transition timing, overlap constraints, and clip duration boundaries.
 */
object TransitionValidator {
    const val MIN_TRANSITION_DURATION_MS = 100L
    const val DEFAULT_TRANSITION_DURATION_MS = 1000L

    /**
     * Determines maximum valid transition duration between two adjacent clips.
     * Transition duration cannot exceed the duration of either clip.
     */
    fun getMaxDurationMs(firstClip: Clip, secondClip: Clip): Long {
        return minOf(firstClip.durationMs, secondClip.durationMs)
    }

    /**
     * Validates and safely clamps transition duration to safe boundaries.
     */
    fun validate(
        firstClip: Clip,
        secondClip: Clip,
        requestedDurationMs: Long
    ): TransitionValidationResult {
        val maxDuration = getMaxDurationMs(firstClip, secondClip)
        if (maxDuration < MIN_TRANSITION_DURATION_MS) {
            return TransitionValidationResult(
                isValid = false,
                clampedDurationMs = maxDuration,
                errorMessage = "Clips are too short for a transition (minimum ${MIN_TRANSITION_DURATION_MS}ms required)."
            )
        }

        val clamped = requestedDurationMs.coerceIn(MIN_TRANSITION_DURATION_MS, maxDuration)
        return TransitionValidationResult(
            isValid = true,
            clampedDurationMs = clamped,
            errorMessage = if (requestedDurationMs > maxDuration) "Duration clamped to maximum allowed (${maxDuration}ms)." else null
        )
    }
}
