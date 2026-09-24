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
        val rotation = evaluateRotation(clip.keyframes, timeMs, baseTransform.rotation)
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
     * Evaluates continuous and shortest-path angle rotation at [timeMs].
     * Avoids unwanted reverse spins when crossing the 0°/360° boundary,
     * while preserving multi-turn continuous revolutions (e.g. 0° -> 720°).
     */
    fun evaluateRotation(
        keyframes: List<Keyframe>,
        timeMs: Long,
        defaultValue: Float
    ): Float {
        val matchingKeyframes = keyframes.filter { it.property == KeyframeProperty.ROTATION }.sortedBy { it.timeMs }
        if (matchingKeyframes.isEmpty()) return defaultValue

        if (timeMs <= matchingKeyframes.first().timeMs) {
            return matchingKeyframes.first().value
        }
        if (timeMs >= matchingKeyframes.last().timeMs) {
            return matchingKeyframes.last().value
        }

        for (i in 0 until matchingKeyframes.size - 1) {
            val k0 = matchingKeyframes[i]
            val k1 = matchingKeyframes[i + 1]

            if (timeMs in k0.timeMs..k1.timeMs) {
                val duration = k1.timeMs - k0.timeMs
                if (duration <= 0L) return k1.value

                val rawT = (timeMs - k0.timeMs).toFloat() / duration.toFloat()
                val easedT = applyInterpolation(rawT.coerceIn(0f, 1f), k0)

                val v0 = k0.value
                val v1 = k1.value
                val diff = v1 - v0

                // If diff magnitude is strictly within (180, 360), apply shortest path across 0/360 boundary
                val effectiveDiff = if (kotlin.math.abs(diff) in 180.001f..359.999f) {
                    var d = diff % 360f
                    if (d > 180f) d -= 360f
                    if (d < -180f) d += 360f
                    d
                } else {
                    diff
                }

                return v0 + effectiveDiff * easedT
            }
        }

        return defaultValue
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
        if (property == KeyframeProperty.ROTATION) {
            return evaluateRotation(keyframes, timeMs, defaultValue)
        }
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
     * Applies deterministic easing/interpolation curve based on the keyframe's [InterpolationType].
     */
    fun applyInterpolation(t: Float, keyframe: Keyframe): Float {
        val clampedT = t.coerceIn(0f, 1f)
        return when (keyframe.interpolation) {
            InterpolationType.LINEAR -> clampedT
            InterpolationType.HOLD -> if (clampedT < 1f) 0f else 1f
            InterpolationType.EASE_IN -> clampedT * clampedT
            InterpolationType.EASE_OUT -> 1f - (1f - clampedT) * (1f - clampedT)
            InterpolationType.EASE_IN_OUT -> {
                if (clampedT < 0.5f) {
                    2f * clampedT * clampedT
                } else {
                    1f - (-2f * clampedT + 2f).pow(2) / 2f
                }
            }
            InterpolationType.CUBIC_EASE_IN -> clampedT.pow(3)
            InterpolationType.CUBIC_EASE_OUT -> 1f - (1f - clampedT).pow(3)
            InterpolationType.CUBIC_EASE_IN_OUT -> {
                if (clampedT < 0.5f) {
                    4f * clampedT.pow(3)
                } else {
                    1f - (-2f * clampedT + 2f).pow(3) / 2f
                }
            }
            InterpolationType.SMOOTH -> clampedT * clampedT * (3f - 2f * clampedT)
            InterpolationType.BEZIER -> {
                clampedT * clampedT * (3f - 2f * clampedT)
            }
        }
    }

    /**
     * Evaluates the active [com.example.core.model.ClipMask] of a [Clip] at a specific timeline timestamp [timeMs],
     * dynamically interpolating position, size, and feather if keyframes are present.
     */
    fun evaluateMask(clip: Clip, timeMs: Long): com.example.core.model.ClipMask? {
        val baseMask = clip.mask ?: return null
        if (clip.keyframes.isEmpty()) return baseMask

        val x = evaluateProperty(clip.keyframes, KeyframeProperty.MASK_X, timeMs, baseMask.x)
        val y = evaluateProperty(clip.keyframes, KeyframeProperty.MASK_Y, timeMs, baseMask.y)
        val feather = evaluateProperty(clip.keyframes, KeyframeProperty.MASK_FEATHER, timeMs, baseMask.feather)

        return baseMask.copy(x = x, y = y, feather = feather)
    }

    /**
     * Evaluates a clip's effects at [timeMs], interpolating any parameters that have corresponding keyframes.
     */
    fun evaluateEffects(clip: Clip, timeMs: Long): List<com.example.core.model.Effect> {
        if (clip.keyframes.isEmpty()) return clip.effects

        return clip.effects.map { eff ->
            when (eff.type) {
                com.example.core.model.EffectType.BRIGHTNESS -> {
                    val v = evaluateProperty(clip.keyframes, KeyframeProperty.BRIGHTNESS, timeMs, eff.parameters["value"] ?: 0f)
                    eff.copy(parameters = eff.parameters + mapOf("value" to v, "brightness" to v))
                }
                com.example.core.model.EffectType.CONTRAST -> {
                    val v = evaluateProperty(clip.keyframes, KeyframeProperty.CONTRAST, timeMs, eff.parameters["value"] ?: 1f)
                    eff.copy(parameters = eff.parameters + mapOf("value" to v, "contrast" to v))
                }
                com.example.core.model.EffectType.SATURATION -> {
                    val v = evaluateProperty(clip.keyframes, KeyframeProperty.SATURATION, timeMs, eff.parameters["value"] ?: 1f)
                    eff.copy(parameters = eff.parameters + mapOf("value" to v, "saturation" to v))
                }
                com.example.core.model.EffectType.EXPOSURE -> {
                    val v = evaluateProperty(clip.keyframes, KeyframeProperty.EXPOSURE, timeMs, eff.parameters["value"] ?: 0f)
                    eff.copy(parameters = eff.parameters + mapOf("value" to v, "exposure" to v))
                }
                com.example.core.model.EffectType.TEMPERATURE -> {
                    val v = evaluateProperty(clip.keyframes, KeyframeProperty.TEMPERATURE, timeMs, eff.parameters["value"] ?: 0f)
                    eff.copy(parameters = eff.parameters + mapOf("value" to v, "temperature" to v))
                }
                com.example.core.model.EffectType.TINT -> {
                    val v = evaluateProperty(clip.keyframes, KeyframeProperty.TINT, timeMs, eff.parameters["value"] ?: 0f)
                    eff.copy(parameters = eff.parameters + mapOf("value" to v, "tint" to v))
                }
                com.example.core.model.EffectType.HIGHLIGHTS -> {
                    val v = evaluateProperty(clip.keyframes, KeyframeProperty.HIGHLIGHTS, timeMs, eff.parameters["value"] ?: 0f)
                    eff.copy(parameters = eff.parameters + mapOf("value" to v, "highlights" to v))
                }
                com.example.core.model.EffectType.SHADOWS -> {
                    val v = evaluateProperty(clip.keyframes, KeyframeProperty.SHADOWS, timeMs, eff.parameters["value"] ?: 0f)
                    eff.copy(parameters = eff.parameters + mapOf("value" to v, "shadows" to v))
                }
                else -> eff
            }
        }
    }
}
