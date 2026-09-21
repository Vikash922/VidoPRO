package com.example.core.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.media3.effect.BitmapOverlay
import com.example.core.model.Clip

/**
 * Custom BitmapOverlay for Media3 Transformer that renders text clips dynamically
 * based on the current presentation time (DEV-062, DEV-063).
 */
class TextOverlayGenerator(
    private val textClips: List<Clip>,
    private val videoWidth: Int,
    private val videoHeight: Int
) : BitmapOverlay() {

    private val emptyBitmap by lazy {
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val timeMs = presentationTimeUs / 1000L
        
        // Find if any text clip should be visible at this time
        val activeClip = textClips.find { clip ->
            timeMs >= clip.startTimeMs && timeMs < clip.endTimeMs
        } ?: return emptyBitmap

        val textData = activeClip.textData ?: return emptyBitmap

        // Generate full frame bitmap with text drawn on it
        val bitmap = Bitmap.createBitmap(videoWidth, videoHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseColorSafe(textData.textColor)
            textSize = textData.fontSize * 3f // Scale up for video resolution
            textAlign = when (textData.alignment) {
                "LEFT" -> Paint.Align.LEFT
                "RIGHT" -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
            typeface = when (textData.fontFamily) {
                "Serif" -> Typeface.SERIF
                "SansSerif" -> Typeface.SANS_SERIF
                "Monospace" -> Typeface.MONOSPACE
                else -> Typeface.DEFAULT
            }
        }

        val x = when (paint.textAlign) {
            Paint.Align.LEFT -> 50f
            Paint.Align.RIGHT -> videoWidth - 50f
            else -> videoWidth / 2f
        }
        val y = videoHeight / 2f - (paint.descent() + paint.ascent()) / 2f

        canvas.drawText(textData.text, x, y, paint)
        return bitmap
    }

    private fun parseColorSafe(hex: String): Int {
        return try {
            Color.parseColor(if (!hex.startsWith("#")) "#$hex" else hex)
        } catch (e: Exception) {
            Color.WHITE
        }
    }
}
