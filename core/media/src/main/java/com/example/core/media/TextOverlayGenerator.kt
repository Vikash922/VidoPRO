package com.example.core.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Typeface
import androidx.media3.effect.BitmapOverlay
import com.example.core.model.Clip
import java.io.Closeable

/**
 * High-performance, memory-safe [BitmapOverlay] for Media3 Transformer that renders text clips.
 *
 * ROOT-CAUSE FIX (DEV-062, DEV-063, FIX-05):
 * Previously, this class allocated `Bitmap.createBitmap(videoWidth, videoHeight)` on every single frame,
 * producing ~8.3 MB per frame at 1080p, resulting in gigabytes of garbage allocations and OOM crashes during export.
 *
 * This implementation uses an instance-scoped reusable Bitmap & Canvas buffer with dirty-state detection.
 * When text has not changed between frames (the common case across seconds of footage), no allocations or
 * draw calls take place. When text changes or transitions, the buffer is cleared and redrawn in-place.
 */
class TextOverlayGenerator(
    private val textClips: List<Clip>,
    videoWidth: Int,
    videoHeight: Int
) : BitmapOverlay(), Closeable {

    val targetWidth: Int = videoWidth.coerceAtLeast(1)
    val targetHeight: Int = videoHeight.coerceAtLeast(1)

    private val emptyBitmap by lazy {
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    @Volatile
    private var reusableBitmap: Bitmap? = null
    private var reusableCanvas: Canvas? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private data class ClipRenderState(
        val clipId: String,
        val text: String,
        val textColor: String,
        val fontSize: Float,
        val alignment: String,
        val fontFamily: String,
        val transformX: Float,
        val transformY: Float,
        val opacity: Float
    )

    private var lastRenderedStates: List<ClipRenderState>? = null

    // Tracking metrics for diagnostic and testing verification
    var allocationCount: Int = 0
        private set
    var drawCount: Int = 0
        private set

    @Synchronized
    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val timeMs = presentationTimeUs / 1000L

        // Find all active, visible text clips at the current timestamp
        val activeClips = textClips.filter { clip ->
            clip.isVisible && timeMs >= clip.startTimeMs && timeMs < clip.endTimeMs && clip.textData != null
        }

        if (activeClips.isEmpty()) {
            lastRenderedStates = null
            return if (!emptyBitmap.isRecycled) emptyBitmap else Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }

        // Build snapshot of active text states to check if redrawing is needed
        val currentStates = activeClips.map { clip ->
            val td = clip.textData!!
            ClipRenderState(
                clipId = clip.id,
                text = td.text,
                textColor = td.textColor,
                fontSize = td.fontSize,
                alignment = td.alignment,
                fontFamily = td.fontFamily,
                transformX = clip.transform.x,
                transformY = clip.transform.y,
                opacity = clip.transform.opacity
            )
        }

        val existingBitmap = reusableBitmap
        // Fast-path: Text has not changed from previous frame, reuse existing buffer directly
        if (currentStates == lastRenderedStates && existingBitmap != null && !existingBitmap.isRecycled) {
            return existingBitmap
        }

        // Allocate reusable buffer lazily on first visible text frame (only once per instance)
        val bitmap = if (existingBitmap == null || existingBitmap.isRecycled) {
            allocationCount++
            Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888).also {
                reusableBitmap = it
                reusableCanvas = Canvas(it)
            }
        } else {
            existingBitmap
        }

        val canvas = reusableCanvas ?: Canvas(bitmap).also { reusableCanvas = it }

        // Clear previous frame contents
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        drawCount++

        for (clip in activeClips) {
            val textData = clip.textData ?: continue
            val baseColor = parseColorSafe(textData.textColor)
            val alphaInt = (clip.transform.opacity.coerceIn(0f, 1f) * 255).toInt()

            paint.apply {
                color = baseColor
                alpha = alphaInt
                textSize = textData.fontSize * 3f
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

            val baseX = when (paint.textAlign) {
                Paint.Align.LEFT -> 50f
                Paint.Align.RIGHT -> targetWidth - 50f
                else -> targetWidth / 2f
            }
            val baseY = targetHeight / 2f - (paint.descent() + paint.ascent()) / 2f
            val x = baseX + clip.transform.x
            val y = baseY + clip.transform.y

            canvas.drawText(textData.text, x, y, paint)
        }

        lastRenderedStates = currentStates
        return bitmap
    }

    private fun parseColorSafe(hex: String): Int {
        return try {
            Color.parseColor(if (!hex.startsWith("#")) "#$hex" else hex)
        } catch (_: Exception) {
            Color.WHITE
        }
    }

    /**
     * Recycles the reusable bitmap and clears cached state to eliminate memory leaks.
     */
    @Synchronized
    fun release() {
        lastRenderedStates = null
        reusableCanvas = null
        reusableBitmap?.let {
            if (!it.isRecycled) {
                it.recycle()
            }
        }
        reusableBitmap = null
        if (!emptyBitmap.isRecycled) {
            emptyBitmap.recycle()
        }
    }

    override fun close() {
        release()
    }
}
