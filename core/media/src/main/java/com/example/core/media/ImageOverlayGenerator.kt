package com.example.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.media3.effect.BitmapOverlay
import com.example.core.model.Asset
import com.example.core.model.Clip

/**
 * Custom BitmapOverlay for Media3 Transformer that renders PiP / Image clips dynamically.
 * Optimized with a reusable canvas bitmap to prevent frame-by-frame memory churn and OOM.
 */
class ImageOverlayGenerator(
    private val context: Context,
    private val imageClips: List<Clip>,
    private val assets: Map<String, Asset>,
    private val videoWidth: Int,
    private val videoHeight: Int
) : BitmapOverlay(), AutoCloseable {

    private val emptyBitmap by lazy {
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    // Reusable canvas and bitmap allocated ONCE to prevent per-frame OOM
    private val canvasBitmap by lazy {
        Bitmap.createBitmap(videoWidth, videoHeight, Bitmap.Config.ARGB_8888)
    }
    private val canvas by lazy {
        Canvas(canvasBitmap)
    }

    private val cachedBitmaps = mutableMapOf<String, Bitmap>()
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    @Synchronized
    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val timeMs = presentationTimeUs / 1000L

        // Find if any image clip should be visible at this time
        val activeClips = imageClips.filter { clip ->
            clip.isVisible && timeMs >= clip.startTimeMs && timeMs < clip.endTimeMs
        }

        if (activeClips.isEmpty()) return emptyBitmap

        canvasBitmap.eraseColor(Color.TRANSPARENT)

        activeClips.forEach { clip ->
            val asset = assets[clip.assetId] ?: return@forEach
            val sourceBitmap = getSourceBitmap(asset) ?: return@forEach

            val transform = KeyframeEvaluator.evaluateTransform(clip, timeMs)

            val matrix = Matrix()
            // Anchor point in center of image
            val cx = sourceBitmap.width / 2f
            val cy = sourceBitmap.height / 2f

            // Apply scale and rotation
            matrix.postScale(transform.scaleX, transform.scaleY, cx, cy)
            matrix.postRotate(transform.rotation, cx, cy)

            // Translate to center of video, then apply clip translation
            val dx = (videoWidth / 2f - cx) + transform.x
            val dy = (videoHeight / 2f - cy) + transform.y
            matrix.postTranslate(dx, dy)

            paint.alpha = (transform.opacity * 255).toInt().coerceIn(0, 255)

            canvas.drawBitmap(sourceBitmap, matrix, paint)
        }

        return canvasBitmap
    }

    private fun getSourceBitmap(asset: Asset): Bitmap? {
        return cachedBitmaps.getOrPut(asset.id) {
            try {
                val uri = Uri.parse(asset.uri)
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }

                var sampleSize = 1
                val reqWidth = if (videoWidth > 0) videoWidth else 1920
                val reqHeight = if (videoHeight > 0) videoHeight else 1080
                if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                    val halfHeight = options.outHeight / 2
                    val halfWidth = options.outWidth / 2
                    while ((halfHeight / sampleSize) >= reqHeight && (halfWidth / sampleSize) >= reqWidth) {
                        sampleSize *= 2
                    }
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                }
            } catch (e: Exception) {
                null
            } ?: return null
        }
    }

    override fun close() {
        cachedBitmaps.values.forEach { if (!it.isRecycled) it.recycle() }
        cachedBitmaps.clear()
        if (!canvasBitmap.isRecycled) {
            canvasBitmap.recycle()
        }
    }
}
