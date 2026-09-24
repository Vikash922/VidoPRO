package com.example.core.model

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

    fun maxTransitionDuration(clipADurationMs: Long, clipBDurationMs: Long): Long {
        return minOf(clipADurationMs, clipBDurationMs)
    }

    fun getMaxDurationMs(firstClip: Clip, secondClip: Clip): Long {
        return maxTransitionDuration(firstClip.durationMs, secondClip.durationMs)
    }

    fun validateAndClamp(durationMs: Long, clipADurationMs: Long, clipBDurationMs: Long): Long {
        val maxDuration = maxTransitionDuration(clipADurationMs, clipBDurationMs)
        if (maxDuration < MIN_TRANSITION_DURATION_MS) return 0L
        return durationMs.coerceIn(MIN_TRANSITION_DURATION_MS, maxDuration)
    }

    fun isValidTransitionDuration(durationMs: Long, clipADurationMs: Long, clipBDurationMs: Long): Boolean {
        val maxDuration = maxTransitionDuration(clipADurationMs, clipBDurationMs)
        if (maxDuration < MIN_TRANSITION_DURATION_MS) return false
        return durationMs in MIN_TRANSITION_DURATION_MS..maxDuration
    }

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
