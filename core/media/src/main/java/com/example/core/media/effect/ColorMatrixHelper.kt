package com.example.core.media.effect

import com.example.core.model.Effect
import com.example.core.model.EffectType
import kotlin.math.pow

/**
 * Pure 20-element FloatArray ColorMatrix generator for [android.graphics.ColorMatrixColorFilter].
 * Evaluates active clip effects (brightness, contrast, saturation, exposure, temperature, tint, highlights, shadows).
 */
object ColorMatrixHelper {

    fun createColorMatrix(effects: List<Effect>): FloatArray {
        val activeEffects = effects.filter { it.isEnabled }
        val m = FloatArray(20) { 0f }
        // Identity matrix
        m[0] = 1f
        m[6] = 1f
        m[12] = 1f
        m[18] = 1f

        if (activeEffects.isEmpty()) return m

        var brightness = 0f
        var contrast = 1f
        var saturation = 1f
        var exposure = 0f
        var temperature = 0f
        var tint = 0f
        var highlights = 0f
        var shadows = 0f

        for (eff in activeEffects) {
            val p = eff.parameters
            val v = p["value"] ?: 0f
            when (eff.type) {
                EffectType.BRIGHTNESS -> brightness = p["brightness"] ?: v
                EffectType.CONTRAST -> contrast = p["contrast"] ?: v
                EffectType.SATURATION -> saturation = p["saturation"] ?: v
                EffectType.EXPOSURE -> exposure = p["exposure"] ?: v
                EffectType.TEMPERATURE -> temperature = p["temperature"] ?: v
                EffectType.TINT -> tint = p["tint"] ?: v
                EffectType.HIGHLIGHTS -> highlights = p["highlights"] ?: v
                EffectType.SHADOWS -> shadows = p["shadows"] ?: v
                else -> {}
            }
        }

        // 1. Exposure scale: 2^(exposure / 50.0)
        val exposureScale = 2.0.pow((exposure / 50.0).toDouble()).toFloat()

        // 2. Contrast scale & offset
        val contrastScale = contrast.coerceIn(0.2f, 3.0f)
        val contrastOffset = (1f - contrastScale) * 64f

        // 3. Brightness offset (-100..100 -> -128..128)
        val brightnessOffset = (brightness / 100f) * 128f

        // 4. Highlights and shadows
        val highlightScale = 1f + (highlights / 100f) * 0.25f
        val shadowOffset = (shadows / 100f) * 25f

        val totalScale = exposureScale * contrastScale * highlightScale
        val totalOffset = contrastOffset + brightnessOffset + shadowOffset

        // Apply RGB gain and offset
        m[0] = totalScale
        m[6] = totalScale
        m[12] = totalScale
        m[4] = totalOffset
        m[9] = totalOffset
        m[14] = totalOffset

        // Temperature & Tint RGB adjustments
        val redGain = 1f + (temperature / 100f) * 0.3f
        val blueGain = 1f - (temperature / 100f) * 0.3f
        val greenGain = 1f - (tint / 100f) * 0.3f

        m[0] *= redGain
        m[6] *= greenGain
        m[12] *= blueGain

        // Saturation adjustment using luminance weights (Rec. 709)
        if (saturation != 1f) {
            val sat = saturation.coerceIn(0f, 3f)
            val invSat = 1f - sat
            val lr = 0.2126f * invSat
            val lg = 0.7152f * invSat
            val lb = 0.0722f * invSat

            val r0 = m[0] * (lr + sat) + m[6] * lg + m[12] * lb
            val g0 = m[0] * lr + m[6] * (lg + sat) + m[12] * lb
            val b0 = m[0] * lr + m[6] * lg + m[12] * (lb + sat)

            m[0] = r0
            m[6] = g0
            m[12] = b0
        }

        return m
    }
}
