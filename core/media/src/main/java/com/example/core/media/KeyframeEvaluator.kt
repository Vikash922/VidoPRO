package com.example.core.media

import com.example.core.model.Clip
import com.example.core.model.InterpolationType
import com.example.core.model.Keyframe
import com.example.core.model.KeyframeProperty
import com.example.core.model.Transform
import kotlin.math.pow

/**
 * Deterministic Keyframe Evaluator for VidoPRO animation and transform properties.
 *
 * Evaluates dynamic property values at any arbitrary timeline timestamp (timeMs),
 * interpolating between surrounding keyframes according to their [InterpolationType].
 */
object KeyframeEvaluator {

    /**
     * Evaluates the active [Transform] of a [Clip] at a specific timeline timestamp [timeMs].
     *
     * If the clip has keyframes for transform properties (position, scale, rotation, opacity),
     * this dynamically interpolates those properties. Properties without keyframes fall back
     * to the clip's base static [Clip.transform].
     */
    fun evaluateTransform(clip: Clip, timeMs: Long): Transform {
        if (clip.keyframes.isEmpty()) {
            return clip.transform
        }

        val baseTransform = clip.transform

        val posX = evaluateProperty(clip.keyframes, KeyframeProperty.POSITION_X, timeMs, baseTransform.x)
        val posY = evaluateProperty(clip.keyframes, KeyframeProperty.POSITION_Y, timeMs, baseTransform.y)
        val scaleX = evaluateProperty(clip.keyframes, KeyframeProperty.SCALE_X, timeMs, baseTransform.scaleX)
        val scaleY = evaluateProperty(clip.keyframes, KeyframeProperty.SCALE_Y, timeMs, baseTransform.scaleY)
        val rotation = evaluateProperty(clip.keyframes, KeyframeProperty.ROTATION, timeMs, baseTransform.rotation)
        val opacity = evaluateProperty(clip.keyframes, KeyframeProperty.OPACITY, timeMs, baseTransform.opacity)

        return baseTransform.copy(
            x = posX,
            y = posY,
            scaleX = scaleX,
            scaleY = scaleY,
            rotation = rotation,
            opacity = opacity.coerceIn(0f, 1f)
        )
    }

    /**
     * Evaluates a single named property (e.g. [KeyframeProperty.SCALE_X]) at [timeMs].
     * Returns [defaultValue] if no keyframes exist for this property.
     */
    fun evaluateProperty(
        keyframes: List<Keyframe>,
        property: String,
        timeMs: Long,
        defaultValue: Float
    ): Float {
        val matchingKeyframes = keyframes.filter { it.property == property }.sortedBy { it.timeMs }
        if (matchingKeyframes.isEmpty()) return defaultValue

        // Before first keyframe
        if (timeMs <= matchingKeyframes.first().timeMs) {
            return matchingKeyframes.first().value
        }

        // After last keyframe
        if (timeMs >= matchingKeyframes.last().timeMs) {
            return matchingKeyframes.last().value
        }

        // Between two keyframes
        for (i in 0 until matchingKeyframes.size - 1) {
            val k0 = matchingKeyframes[i]
            val k1 = matchingKeyframes[i + 1]

            if (timeMs in k0.timeMs..k1.timeMs) {
                val duration = k1.timeMs - k0.timeMs
                if (duration <= 0L) return k1.value

                val rawT = (timeMs - k0.timeMs).toFloat() / duration.toFloat()
                val easedT = applyInterpolation(rawT.coerceIn(0f, 1f), k0)
                return k0.value + (k1.value - k0.value) * easedT
            }
        }

        return defaultValue
    }

    /**
     * Applies easing/interpolation curve based on the keyframe's [InterpolationType].
     */
    private fun applyInterpolation(t: Float, keyframe: Keyframe): Float {
        return when (keyframe.interpolation) {
            InterpolationType.LINEAR -> t
            InterpolationType.EASE_IN -> t * t
            InterpolationType.EASE_OUT -> 1f - (1f - t) * (1f - t)
            InterpolationType.EASE_IN_OUT -> {
                if (t < 0.5f) {
                    2f * t * t
                } else {
                    1f - (-2f * t + 2f).pow(2) / 2f
                }
            }
            InterpolationType.HOLD -> 0f
            InterpolationType.BEZIER -> {
                // If custom cubic bezier control points are provided, use smooth cubic curve
                val y1 = keyframe.bezierY1 ?: 0.42f
                val y2 = keyframe.bezierY2 ?: 0.58f
                // Standard cubic Hermite approximation
                t * t * (3f - 2f * t)
            }
        }
    }
}
