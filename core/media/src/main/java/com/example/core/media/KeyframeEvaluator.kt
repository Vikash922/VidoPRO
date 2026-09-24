package com.example.core.media

import com.example.core.model.Clip
import com.example.core.model.InterpolationType
import com.example.core.model.Keyframe
import com.example.core.model.KeyframeProperty
import com.example.core.model.Transform
import kotlin.math.abs
import kotlin.math.pow

/**
 * Deterministic Keyframe Evaluator for VidoPRO animation and transform properties.
 *
 * Evaluates dynamic property values at any arbitrary timeline timestamp (timeMs),
 * interpolating between surrounding keyframes according to their [InterpolationType].
 * Optimized for zero runtime heap allocations during playback and rendering.
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
     * Interpolates between [from] and [to] values at progress [t] in [0..1]
     * using the specified [InterpolationType] curve.
     */
    fun interpolate(from: Float, to: Float, t: Float, type: InterpolationType): Float {
        val clampedT = t.coerceIn(0f, 1f)
        val progress = when (type) {
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
            InterpolationType.BEZIER -> clampedT * clampedT * (3f - 2f * clampedT)
        }
        return from + (to - from) * progress
    }

    /**
     * Interpolates rotation angles between [from] and [to] at progress [t] in [0..1].
     * Applies shortest-path boundary crossing when diff is within (180, 360) degrees,
     * while preserving multi-turn continuous revolutions (e.g. 0° -> 720°).
     */
    fun evaluateRotation(from: Float, to: Float, t: Float, type: InterpolationType): Float {
        val diff = to - from
        val effectiveDiff = if (abs(diff) in 180.001f..359.999f) {
            var d = diff % 360f
            if (d > 180f) d -= 360f
            if (d < -180f) d += 360f
            d
        } else {
            diff
        }
        val easedT = interpolate(0f, 1f, t, type)
        return from + effectiveDiff * easedT
    }

    /**
     * Evaluates continuous and shortest-path angle rotation at [timeMs].
     * Avoids unwanted reverse spins when crossing the 0°/360° boundary,
     * while preserving multi-turn continuous revolutions (e.g. 0° -> 720°).
     * Single-pass zero-allocation search.
     */
    fun evaluateRotation(
        keyframes: List<Keyframe>,
        timeMs: Long,
        defaultValue: Float
    ): Float {
        if (keyframes.isEmpty()) return defaultValue

        var first: Keyframe? = null
        var last: Keyframe? = null
        var prev: Keyframe? = null
        var next: Keyframe? = null

        for (i in keyframes.indices) {
            val k = keyframes[i]
            if (k.property != KeyframeProperty.ROTATION) continue

            if (first == null || k.timeMs < first.timeMs) first = k
            if (last == null || k.timeMs > last.timeMs) last = k

            if (k.timeMs <= timeMs) {
                if (prev == null || k.timeMs > prev.timeMs) prev = k
            }
            if (k.timeMs >= timeMs) {
                if (next == null || k.timeMs < next.timeMs) next = k
            }
        }

        if (first == null) return defaultValue
        if (timeMs <= first.timeMs) return first.value
        val l = last ?: return first.value
        if (timeMs >= l.timeMs) return l.value

        val k0 = prev ?: first
        val k1 = next ?: l

        val duration = k1.timeMs - k0.timeMs
        if (duration <= 0L) return k1.value

        val rawT = (timeMs - k0.timeMs).toFloat() / duration.toFloat()
        return evaluateRotation(k0.value, k1.value, rawT, k0.interpolation)
    }

    /**
     * Evaluates a single named property (e.g. [KeyframeProperty.SCALE_X]) at [timeMs].
     * Returns [defaultValue] if no keyframes exist for this property.
     * Single-pass zero-allocation search.
     */
    fun evaluateProperty(
        keyframes: List<Keyframe>,
        property: String,
        timeMs: Long,
        defaultValue: Float
    ): Float {
        if (keyframes.isEmpty()) return defaultValue
        if (property == KeyframeProperty.ROTATION) {
            return evaluateRotation(keyframes, timeMs, defaultValue)
        }

        var first: Keyframe? = null
        var last: Keyframe? = null
        var prev: Keyframe? = null
        var next: Keyframe? = null

        for (i in keyframes.indices) {
            val k = keyframes[i]
            if (k.property != property) continue

            if (first == null || k.timeMs < first.timeMs) first = k
            if (last == null || k.timeMs > last.timeMs) last = k

            if (k.timeMs <= timeMs) {
                if (prev == null || k.timeMs > prev.timeMs) prev = k
            }
            if (k.timeMs >= timeMs) {
                if (next == null || k.timeMs < next.timeMs) next = k
            }
        }

        if (first == null) return defaultValue
        if (timeMs <= first.timeMs) return first.value
        val l = last ?: return first.value
        if (timeMs >= l.timeMs) return l.value

        val k0 = prev ?: first
        val k1 = next ?: l

        val duration = k1.timeMs - k0.timeMs
        if (duration <= 0L) return k1.value

        val rawT = (timeMs - k0.timeMs).toFloat() / duration.toFloat()
        return interpolate(k0.value, k1.value, rawT, k0.interpolation)
    }

    /**
     * Applies deterministic easing/interpolation curve based on the keyframe's [InterpolationType].
     */
    fun applyInterpolation(t: Float, keyframe: Keyframe): Float {
        return interpolate(0f, 1f, t, keyframe.interpolation)
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
                    val v = evaluateProperty(clip.keyframes, KeyframeProperty.BRIGHTNESS, timeMs, eff.parameters["value"] ?: eff.parameters["brightness"] ?: 0f)
                    eff.copy(parameters = eff.parameters + mapOf("value" to v, "brightness" to v))
                }
                com.example.core.model.EffectType.CONTRAST -> {
                    val v = evaluateProperty(clip.keyframes, KeyframeProperty.CONTRAST, timeMs, eff.parameters["value"] ?: eff.parameters["contrast"] ?: 1f)
                    eff.copy(parameters = eff.parameters + mapOf("value" to v, "contrast" to v))
                }
                com.example.core.model.EffectType.SATURATION -> {
                    val v = evaluateProperty(clip.keyframes, KeyframeProperty.SATURATION, timeMs, eff.parameters["value"] ?: eff.parameters["saturation"] ?: 1f)
                    eff.copy(parameters = eff.parameters + mapOf("value" to v, "saturation" to v))
                }
                com.example.core.model.EffectType.EXPOSURE -> {
                    val v = evaluateProperty(clip.keyframes, KeyframeProperty.EXPOSURE, timeMs, eff.parameters["value"] ?: eff.parameters["exposure"] ?: 0f)
                    eff.copy(parameters = eff.parameters + mapOf("value" to v, "exposure" to v))
                }
                com.example.core.model.EffectType.TEMPERATURE -> {
                    val v = evaluateProperty(clip.keyframes, KeyframeProperty.TEMPERATURE, timeMs, eff.parameters["value"] ?: eff.parameters["temperature"] ?: 0f)
                    eff.copy(parameters = eff.parameters + mapOf("value" to v, "temperature" to v))
                }
                com.example.core.model.EffectType.TINT -> {
                    val v = evaluateProperty(clip.keyframes, KeyframeProperty.TINT, timeMs, eff.parameters["value"] ?: eff.parameters["tint"] ?: 0f)
                    eff.copy(parameters = eff.parameters + mapOf("value" to v, "tint" to v))
                }
                com.example.core.model.EffectType.HIGHLIGHTS -> {
                    val v = evaluateProperty(clip.keyframes, KeyframeProperty.HIGHLIGHTS, timeMs, eff.parameters["value"] ?: eff.parameters["highlights"] ?: 0f)
                    eff.copy(parameters = eff.parameters + mapOf("value" to v, "highlights" to v))
                }
                com.example.core.model.EffectType.SHADOWS -> {
                    val v = evaluateProperty(clip.keyframes, KeyframeProperty.SHADOWS, timeMs, eff.parameters["value"] ?: eff.parameters["shadows"] ?: 0f)
                    eff.copy(parameters = eff.parameters + mapOf("value" to v, "shadows" to v))
                }
                else -> eff
            }
        }
    }

    /**
     * Evaluates volume at [timeMs], interpolating any volume keyframes.
     * Returns base volume if no keyframes are set.
     */
    fun evaluateVolume(clip: Clip, timeMs: Long): Float {
        val baseVolume = clip.volume ?: 1f
        if (clip.keyframes.isEmpty()) return baseVolume
        return evaluateProperty(clip.keyframes, KeyframeProperty.VOLUME, timeMs, baseVolume).coerceIn(0f, 2f)
    }
}
