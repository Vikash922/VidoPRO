package com.example.core.media.transition

import com.example.core.media.render.RenderTransform
import com.example.core.media.render.RenderTransition
import com.example.core.model.InterpolationType
import com.example.core.model.TransitionType
import kotlin.math.pow

/**
 * Universal evaluator for transition transforms and opacity across Preview and Export.
 *
 * Ensures:
 * 1. Deterministic progress calculation in [0.0, 1.0]
 * 2. Proper composition: Base Transform -> Transition Transform -> Final Render Transform
 * 3. Exact visual parity between Compose preview and Media3 export
 */
object TransitionEvaluator {

    fun applyEasing(t: Float, interpolation: InterpolationType = InterpolationType.LINEAR): Float = when (interpolation) {
        InterpolationType.LINEAR -> t
        InterpolationType.EASE_IN -> t * t
        InterpolationType.EASE_OUT -> 1f - (1f - t) * (1f - t)
        InterpolationType.EASE_IN_OUT -> {
            if (t < 0.5f) 2f * t * t
            else 1f - (-2f * t + 2f).pow(2) / 2f
        }
        else -> t
    }

    /**
     * Evaluates the render transform for the OUTGOING clip at the given transition progress.
     */
    fun evaluateOutgoingTransform(
        transition: RenderTransition,
        rawProgress: Float,
        baseTransform: RenderTransform,
        canvasWidth: Float,
        canvasHeight: Float,
        interpolation: InterpolationType = InterpolationType.LINEAR
    ): RenderTransform {
        val p = applyEasing(rawProgress.coerceIn(0f, 1f), interpolation)

        return when (transition.type) {
            TransitionType.NONE -> {
                val opacity = if (p < 0.5f) baseTransform.opacity else 0f
                baseTransform.copy(opacity = opacity)
            }
            TransitionType.FADE -> {
                val opacity = (baseTransform.opacity * (1.0f - p)).coerceIn(0f, 1f)
                baseTransform.copy(opacity = opacity)
            }
            TransitionType.SLIDE -> {
                val (dx, dy) = when (transition.direction.uppercase()) {
                    "RIGHT" -> (p * canvasWidth) to 0f
                    "UP" -> 0f to (-p * canvasHeight)
                    "DOWN" -> 0f to (p * canvasHeight)
                    else -> (-p * canvasWidth) to 0f // Default LEFT
                }
                baseTransform.copy(
                    x = baseTransform.x + dx,
                    y = baseTransform.y + dy
                )
            }
            TransitionType.ZOOM -> {
                val scaleFactor = 1.0f + (p * 0.5f)
                val opacity = (baseTransform.opacity * (1.0f - p)).coerceIn(0f, 1f)
                baseTransform.copy(
                    scaleX = baseTransform.scaleX * scaleFactor,
                    scaleY = baseTransform.scaleY * scaleFactor,
                    opacity = opacity
                )
            }
            TransitionType.WIPE -> {
                // Wipe slides outgoing half-distance while fading
                val dx = -p * canvasWidth * 0.5f
                val opacity = (baseTransform.opacity * (1.0f - p)).coerceIn(0f, 1f)
                baseTransform.copy(
                    x = baseTransform.x + dx,
                    opacity = opacity
                )
            }
            else -> {
                // Fallback fade
                val opacity = (baseTransform.opacity * (1.0f - p)).coerceIn(0f, 1f)
                baseTransform.copy(opacity = opacity)
            }
        }
    }

    /**
     * Evaluates the render transform for the INCOMING clip at the given transition progress.
     */
    fun evaluateIncomingTransform(
        transition: RenderTransition,
        rawProgress: Float,
        baseTransform: RenderTransform,
        canvasWidth: Float,
        canvasHeight: Float,
        interpolation: InterpolationType = InterpolationType.LINEAR
    ): RenderTransform {
        val p = applyEasing(rawProgress.coerceIn(0f, 1f), interpolation)

        return when (transition.type) {
            TransitionType.NONE -> {
                val opacity = if (p >= 0.5f) baseTransform.opacity else 0f
                baseTransform.copy(opacity = opacity)
            }
            TransitionType.FADE -> {
                val opacity = (baseTransform.opacity * p).coerceIn(0f, 1f)
                baseTransform.copy(opacity = opacity)
            }
            TransitionType.SLIDE -> {
                val (dx, dy) = when (transition.direction.uppercase()) {
                    "RIGHT" -> (-(1.0f - p) * canvasWidth) to 0f
                    "UP" -> 0f to ((1.0f - p) * canvasHeight)
                    "DOWN" -> 0f to (-(1.0f - p) * canvasHeight)
                    else -> ((1.0f - p) * canvasWidth) to 0f // Default LEFT
                }
                baseTransform.copy(
                    x = baseTransform.x + dx,
                    y = baseTransform.y + dy
                )
            }
            TransitionType.ZOOM -> {
                val scaleFactor = 0.5f + (p * 0.5f)
                val opacity = (baseTransform.opacity * p).coerceIn(0f, 1f)
                baseTransform.copy(
                    scaleX = baseTransform.scaleX * scaleFactor,
                    scaleY = baseTransform.scaleY * scaleFactor,
                    opacity = opacity
                )
            }
            TransitionType.WIPE -> {
                // Wipe enters from right edge
                val dx = (1.0f - p) * canvasWidth
                baseTransform.copy(
                    x = baseTransform.x + dx,
                    opacity = baseTransform.opacity
                )
            }
            else -> {
                // Fallback fade
                val opacity = (baseTransform.opacity * p).coerceIn(0f, 1f)
                baseTransform.copy(opacity = opacity)
            }
        }
    }
}
