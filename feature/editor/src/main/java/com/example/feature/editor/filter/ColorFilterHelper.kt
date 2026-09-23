package com.example.feature.editor.filter

import androidx.compose.ui.graphics.ColorMatrix
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * High-performance GPU ColorMatrix builder for real-time video preview filters and adjustments.
 * Computes a standard 20-float color matrix (4x5) for native Skia / Android hardware layer rendering
 * and Jetpack Compose ColorMatrix.
 */
object ColorFilterHelper {

    /**
     * Generates a 20-element FloatArray suitable for [android.graphics.ColorMatrixColorFilter].
     */
    fun createColorMatrixArray(settings: FilterSettings): FloatArray {
        val eff = if (settings.selectedPreset != FilterPreset.NONE && settings.isDefault) {
            settings.selectedPreset.toFilterSettings()
        } else {
            settings
        }

        // Start with identity matrix
        // [ 0..4  : R row ]
        // [ 5..9  : G row ]
        // [ 10..14: B row ]
        // [ 15..19: A row ]
        val m = FloatArray(20) { 0f }
        m[0] = 1f
        m[6] = 1f
        m[12] = 1f
        m[18] = 1f

        // 1. Exposure: 2^(exposure / 50.0)
        val exposureScale = 2.0.pow((eff.exposure / 50.0).toDouble()).toFloat()

        // 2. Contrast: 0.5 to 2.0
        val contrastScale = eff.contrast.coerceIn(0.2f, 3.0f)
        val contrastOffset = (1f - contrastScale) * 64f

        // 3. Brightness: -100 to +100 -> -128 to +128
        val brightnessOffset = (eff.brightness / 100f) * 128f

        // 4. Whites and Blacks
        val whiteGain = 1f + (eff.whites / 100f) * 0.35f
        val blackOffset = (eff.blacks / 100f) * 35f

        // 5. Highlights and Shadows
        val highlightScale = 1f + (eff.highlights / 100f) * 0.25f
        val shadowOffset = (eff.shadows / 100f) * 25f

        // 6. Gamma & Midtones
        val gammaScale = (1f / eff.gamma.coerceIn(0.5f, 2.5f))
        val midtoneOffset = (eff.midtones / 100f) * 30f

        // 7. Dehaze & Clarity
        val dehazeGain = 1f + (eff.dehaze / 100f) * 0.25f
        val dehazeOffset = -(eff.dehaze / 100f) * 15f
        val clarityGain = 1f + (eff.clarity / 100f) * 0.15f

        // Combined RGB base scales and offsets
        val baseScale = exposureScale * contrastScale * whiteGain * highlightScale * gammaScale * dehazeGain * clarityGain
        val baseOffset = brightnessOffset + contrastOffset + blackOffset + shadowOffset + midtoneOffset + dehazeOffset

        // 8. Temperature (Cool / Warm)
        val tempNorm = (eff.temperature / 100f).coerceIn(-1f, 1f)
        val tempR = if (tempNorm > 0) tempNorm * 35f else tempNorm * 15f
        val tempB = if (tempNorm < 0) -tempNorm * 40f else -tempNorm * 20f

        // 9. Tint (Green / Magenta)
        val tintNorm = (eff.tint / 100f).coerceIn(-1f, 1f)
        val tintG = if (tintNorm < 0) -tintNorm * 35f else -tintNorm * 10f
        val tintM = if (tintNorm > 0) tintNorm * 25f else 0f

        // 10. Color Balance (Red, Green, Blue)
        val balR = (eff.redBalance / 100f) * 35f
        val balG = (eff.greenBalance / 100f) * 35f
        val balB = (eff.blueBalance / 100f) * 35f

        // 11. 3-Way Color Grading: Shadows Tint, Highlights Tint
        val (shR, shG, shB) = hueSatToRgbDelta(eff.shadowHue, eff.shadowSat, 35f)
        val (hlR, hlG, hlB) = hueSatToRgbGain(eff.highlightHue, eff.highlightSat, 0.35f)
        val (midR, midG, midB) = hueSatToRgbDelta(eff.midtoneHue, eff.midtoneSat, 20f)

        m[0] = baseScale * hlR
        m[6] = baseScale * hlG
        m[12] = baseScale * hlB

        m[4] = baseOffset + tempR + tintM + balR + shR + midR
        m[9] = baseOffset + tintG + balG + shG + midG
        m[14] = baseOffset + tempB + tintM + balB + shB + midB

        // 12. Hue Rotation Matrix
        var result = m
        if (eff.hue != 0f) {
            val hueMat = createHueMatrix(eff.hue)
            result = multiplyMatrices(hueMat, result)
        }

        // 13. Saturation & Vibrance Matrix
        val effectiveSat = (eff.saturation * (1f + (eff.vibrance / 100f) * 0.5f)).coerceIn(0f, 3f)
        if (effectiveSat != 1f) {
            val satMat = createSaturationMatrix(effectiveSat)
            result = multiplyMatrices(satMat, result)
        }

        // 14. Opacity
        result[18] = (eff.opacity / 100f).coerceIn(0f, 1f)

        return result
    }

    /**
     * Generates a Compose [ColorMatrix].
     */
    fun createColorMatrix(settings: FilterSettings): ColorMatrix {
        return ColorMatrix(createColorMatrixArray(settings))
    }

    private fun createHueMatrix(degrees: Float): FloatArray {
        val rad = degrees * (PI / 180.0)
        val cosVal = cos(rad).toFloat()
        val sinVal = sin(rad).toFloat()

        val mat = FloatArray(20) { 0f }
        mat[0] = 0.213f + cosVal * 0.787f - sinVal * 0.213f
        mat[1] = 0.715f - cosVal * 0.715f - sinVal * 0.715f
        mat[2] = 0.072f - cosVal * 0.072f + sinVal * 0.928f

        mat[5] = 0.213f - cosVal * 0.213f + sinVal * 0.143f
        mat[6] = 0.715f + cosVal * 0.285f + sinVal * 0.140f
        mat[7] = 0.072f - cosVal * 0.072f - sinVal * 0.283f

        mat[10] = 0.213f - cosVal * 0.213f - sinVal * 0.787f
        mat[11] = 0.715f - cosVal * 0.715f + sinVal * 0.715f
        mat[12] = 0.072f + cosVal * 0.928f + sinVal * 0.072f

        mat[18] = 1f
        return mat
    }

    private fun createSaturationMatrix(sat: Float): FloatArray {
        val mat = FloatArray(20) { 0f }
        val invSat = 1f - sat
        val r = 0.2126f * invSat
        val g = 0.7152f * invSat
        val b = 0.0722f * invSat

        mat[0] = r + sat
        mat[1] = g
        mat[2] = b

        mat[5] = r
        mat[6] = g + sat
        mat[7] = b

        mat[10] = r
        mat[11] = g
        mat[12] = b + sat

        mat[18] = 1f
        return mat
    }

    private fun hueSatToRgbDelta(hue: Float, sat: Float, maxDelta: Float): Triple<Float, Float, Float> {
        if (sat <= 0f) return Triple(0f, 0f, 0f)
        val s = (sat / 100f).coerceIn(0f, 1f)
        val h = (hue % 360f + 360f) % 360f
        val c = s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val (r1, g1, b1) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val factor = maxDelta
        return Triple(
            (r1 - 0.33f) * factor,
            (g1 - 0.33f) * factor,
            (b1 - 0.33f) * factor
        )
    }

    private fun hueSatToRgbGain(hue: Float, sat: Float, maxGain: Float): Triple<Float, Float, Float> {
        if (sat <= 0f) return Triple(1f, 1f, 1f)
        val s = (sat / 100f).coerceIn(0f, 1f)
        val h = (hue % 360f + 360f) % 360f
        val c = s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val (r1, g1, b1) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Triple(
            1f + (r1 - 0.33f) * maxGain,
            1f + (g1 - 0.33f) * maxGain,
            1f + (b1 - 0.33f) * maxGain
        )
    }

    private fun multiplyMatrices(a: FloatArray, b: FloatArray): FloatArray {
        val result = FloatArray(20) { 0f }
        for (y in 0 until 4) {
            for (x in 0 until 4) {
                var sum = 0f
                for (i in 0 until 4) {
                    sum += a[y * 5 + i] * b[i * 5 + x]
                }
                result[y * 5 + x] = sum
            }
            // Add translation offset
            result[y * 5 + 4] = a[y * 5 + 0] * b[4] +
                               a[y * 5 + 1] * b[9] +
                               a[y * 5 + 2] * b[14] +
                               a[y * 5 + 3] * b[19] +
                               a[y * 5 + 4]
        }
        return result
    }

    /**
     * Applies filter and color grading adjustments to a bitmap using multi-threaded C++ (SIMD accelerated).
     */
    fun applyNativeAdjustments(bitmap: android.graphics.Bitmap, settings: FilterSettings): Boolean {
        if (!com.example.core.media.nativeengine.NativeVideoEngine.isAvailable) return false
        val eff = if (settings.selectedPreset != FilterPreset.NONE && settings.isDefault) {
            settings.selectedPreset.toFilterSettings()
        } else {
            settings
        }
        val params = floatArrayOf(
            eff.brightness,
            eff.contrast,
            eff.exposure,
            eff.highlights,
            eff.shadows,
            eff.whites,
            eff.blacks,
            eff.saturation,
            eff.vibrance,
            eff.temperature,
            eff.tint,
            eff.hue,
            eff.redBalance,
            eff.greenBalance,
            eff.blueBalance,
            eff.gamma,
            eff.midtones,
            eff.vignette,
            eff.fade,
            eff.opacity / 100f
        )
        val success = com.example.core.media.nativeengine.NativeVideoEngine.applyAdjustments(bitmap, params)
        if (eff.blur > 0.1f) {
            com.example.core.media.nativeengine.NativeVideoEngine.applyBlur(bitmap, eff.blur.toInt())
        }
        if (eff.sharpness > 0.1f) {
            com.example.core.media.nativeengine.NativeVideoEngine.applySharpen(bitmap, eff.sharpness)
        }
        if (eff.grain > 0.1f) {
            com.example.core.media.nativeengine.NativeVideoEngine.applyGrain(bitmap, eff.grain)
        }
        return success
    }
}
