package com.example.core.media.nativeengine

import android.graphics.Bitmap
import android.util.Log

/**
 * High-performance C++ Native Video and Image Processing Engine.
 * 
 * Provides hardware-accelerated, multi-threaded C++ processing for:
 * - Real-time color grading and professional adjustments
 * - 3D LUT (Look-Up Table) color transformation
 * - Multi-pass box and stack blur
 * - 3x3 Convolution sharpening
 * - High-speed radial vignette and organic film grain
 * - Multi-layer overlay blending (13 blend modes)
 * - Native audio FFT beat detection
 * - High-precision cubic Bézier keyframe curve evaluation
 */
object NativeVideoEngine {

    private const val TAG = "NativeVideoEngine"

    val isAvailable: Boolean = try {
        System.loadLibrary("vidopro_native")
        Log.i(TAG, "Successfully loaded native library vidopro_native (C++ SIMD accelerated)")
        true
    } catch (e: Throwable) {
        Log.w(TAG, "Native library vidopro_native not loaded: ${e.message}")
        false
    }

    /**
     * Applies full adjustment pipeline to a bitmap in-place using multi-threaded C++.
     * 
     * @param params 20-float array:
     *  [0] brightness (-100..100)
     *  [1] contrast (0.2..3.0)
     *  [2] exposure (-100..100)
     *  [3] highlights (-100..100)
     *  [4] shadows (-100..100)
     *  [5] whites (-100..100)
     *  [6] blacks (-100..100)
     *  [7] saturation (0.0..3.0)
     *  [8] vibrance (-100..100)
     *  [9] temperature (-100..100)
     *  [10] tint (-100..100)
     *  [11] hue (-180..180)
     *  [12] redBalance (-100..100)
     *  [13] greenBalance (-100..100)
     *  [14] blueBalance (-100..100)
     *  [15] gamma (0.5..2.5)
     *  [16] midtones (-100..100)
     *  [17] vignette (0..100)
     *  [18] fade (0..100)
     *  [19] opacity (0.0..1.0)
     */
    fun applyAdjustments(bitmap: Bitmap, params: FloatArray): Boolean {
        if (!isAvailable || bitmap.isRecycled) return false
        return nativeApplyAdjustments(bitmap, params)
    }

    /**
     * Fast C++ multi-pass blur on bitmap.
     */
    fun applyBlur(bitmap: Bitmap, radius: Int): Boolean {
        if (!isAvailable || bitmap.isRecycled || radius <= 0) return false
        return nativeApplyBlur(bitmap, radius)
    }

    /**
     * Fast C++ 3x3 convolution sharpening.
     */
    fun applySharpen(bitmap: Bitmap, amount: Float): Boolean {
        if (!isAvailable || bitmap.isRecycled || amount <= 0.01f) return false
        return nativeApplySharpen(bitmap, amount)
    }

    /**
     * Organic film grain using fast C++ PRNG.
     */
    fun applyGrain(bitmap: Bitmap, amount: Float, seed: Long = System.currentTimeMillis()): Boolean {
        if (!isAvailable || bitmap.isRecycled || amount <= 0.01f) return false
        return nativeApplyGrain(bitmap, amount, seed)
    }

    /**
     * 3D LUT (Look-Up Table) trilinear interpolation.
     */
    fun applyLut(bitmap: Bitmap, lutData: ByteArray, lutSize: Int, intensity: Float): Boolean {
        if (!isAvailable || bitmap.isRecycled || intensity <= 0.01f) return false
        return nativeApplyLut(bitmap, lutData, lutSize, intensity)
    }

    /**
     * Blends an overlay bitmap onto a base bitmap using native C++ blend modes.
     * 
     * Modes:
     * 0: Normal, 1: Multiply, 2: Screen, 3: Overlay, 4: Darken, 5: Lighten,
     * 6: Color Dodge, 7: Color Burn, 8: Hard Light, 9: Soft Light,
     * 10: Difference, 11: Exclusion, 12: Add
     */
    fun blendBitmaps(base: Bitmap, overlay: Bitmap, mode: Int, opacity: Float): Boolean {
        if (!isAvailable || base.isRecycled || overlay.isRecycled || opacity <= 0.001f) return false
        return nativeBlendBitmaps(base, overlay, mode, opacity)
    }

    /**
     * Detects audio musical beats and rhythm drops using native C++ spectral onset detection.
     * Returns an array of timestamps in milliseconds.
     */
    fun detectBeats(
        pcmSamples: ShortArray,
        sampleRate: Int,
        channels: Int,
        sensitivity: Float = 1.3f
    ): LongArray {
        if (!isAvailable || pcmSamples.isEmpty()) return LongArray(0)
        return nativeDetectBeats(pcmSamples, pcmSamples.size, sampleRate, channels, sensitivity)
    }

    /**
     * High precision cubic Bézier evaluation in C++ using Newton-Raphson method.
     */
    fun evaluateBezier(x1: Float, y1: Float, x2: Float, y2: Float, time: Float): Float {
        if (!isAvailable) {
            // Fallback linear
            return time.coerceIn(0f, 1f)
        }
        return nativeEvaluateBezier(x1, y1, x2, y2, time)
    }

    /**
     * Keyframe interpolation in C++.
     */
    fun interpolate(
        t: Float,
        startVal: Float,
        endVal: Float,
        type: Int,
        x1: Float = 0.25f,
        y1: Float = 0.1f,
        x2: Float = 0.25f,
        y2: Float = 1.0f
    ): Float {
        if (!isAvailable) {
            return startVal + (endVal - startVal) * t.coerceIn(0f, 1f)
        }
        return nativeInterpolate(t, startVal, endVal, type, x1, y1, x2, y2)
    }

    // --- Native JNI External Methods ---

    private external fun nativeApplyAdjustments(bitmap: Bitmap, params: FloatArray): Boolean
    private external fun nativeApplyBlur(bitmap: Bitmap, radius: Int): Boolean
    private external fun nativeApplySharpen(bitmap: Bitmap, amount: Float): Boolean
    private external fun nativeApplyGrain(bitmap: Bitmap, amount: Float, seed: Long): Boolean
    private external fun nativeApplyLut(bitmap: Bitmap, lutData: ByteArray, lutSize: Int, intensity: Float): Boolean
    private external fun nativeBlendBitmaps(base: Bitmap, overlay: Bitmap, mode: Int, opacity: Float): Boolean
    private external fun nativeDetectBeats(
        pcmSamples: ShortArray,
        totalSamples: Int,
        sampleRate: Int,
        channels: Int,
        sensitivity: Float
    ): LongArray
    private external fun nativeEvaluateBezier(x1: Float, y1: Float, x2: Float, y2: Float, time: Float): Float
    private external fun nativeInterpolate(
        t: Float,
        startVal: Float,
        endVal: Float,
        type: Int,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float
    ): Float
}
