package com.example.core.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Universal Haptic Feedback helper for tactile editor interactions:
 * - Scrubbing tick
 * - Button click
 * - Long-press multi-select
 * - Keyframe dragging
 * - Split / cut tactile snap
 */
object HapticHelper {

    fun tick(context: Context) {
        vibrate(context, 10L, 40, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_TICK else -1)
    }

    fun click(context: Context) {
        vibrate(context, 18L, 80, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_CLICK else -1)
    }

    fun heavyClick(context: Context) {
        vibrate(context, 35L, 200, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_HEAVY_CLICK else -1)
    }

    fun longPress(context: Context) {
        vibrate(context, 55L, 255, -1)
    }

    fun doubleTick(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 15, 35, 15)
            val amplitudes = intArrayOf(0, 70, 0, 90)
            try {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } catch (_: Exception) {}
        } else {
            @Suppress("DEPRECATION")
            try {
                vibrator.vibrate(25L)
            } catch (_: Exception) {}
        }
    }

    private fun vibrate(context: Context, durationMs: Long, amplitude: Int, predefinedEffect: Int) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && predefinedEffect != -1) {
                vibrator.vibrate(VibrationEffect.createPredefined(predefinedEffect))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    private fun getVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }
}
