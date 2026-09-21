package com.example.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.media3.effect.BitmapOverlay
import com.example.core.model.Asset
import com.example.core.model.Clip

/**
 * Custom BitmapOverlay for Media3 Transformer that renders PiP / Image clips dynamically.
 */
class ImageOverlayGenerator(
    private val context: Context,
    private val imageClips: List<Clip>,
    private val assets: Map<String, Asset>,
    private val videoWidth: Int,
    private val videoHeight: Int
) : BitmapOverlay() {

    private val emptyBitmap by lazy {
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    private val cachedBitmaps = mutableMapOf<String, Bitmap>()
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val timeMs = presentationTimeUs / 1000L
        
        // Find if any image clip should be visible at this time
        val activeClips = imageClips.filter { clip ->
            clip.isVisible && timeMs >= clip.startTimeMs && timeMs < clip.endTimeMs
        }
        
        if (activeClips.isEmpty()) return emptyBitmap

        val frameBitmap = Bitmap.createBitmap(videoWidth, videoHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(frameBitmap)

        activeClips.forEach { clip ->
            val asset = assets[clip.assetId] ?: return@forEach
            val sourceBitmap = getSourceBitmap(asset) ?: return@forEach

            val matrix = Matrix()
            // Anchor point in center of image
            val cx = sourceBitmap.width / 2f
            val cy = sourceBitmap.height / 2f
            
            // Apply scale and rotation
            matrix.postScale(clip.transform.scaleX, clip.transform.scaleY, cx, cy)
            matrix.postRotate(clip.transform.rotation, cx, cy)
            
            // Translate to center of video, then apply clip translation
            val dx = (videoWidth / 2f - cx) + clip.transform.x
            val dy = (videoHeight / 2f - cy) + clip.transform.y
            matrix.postTranslate(dx, dy)
            
            paint.alpha = (clip.transform.opacity * 255).toInt().coerceIn(0, 255)
            
            canvas.drawBitmap(sourceBitmap, matrix, paint)
        }

        return frameBitmap
    }

    private fun getSourceBitmap(asset: Asset): Bitmap? {
        return cachedBitmaps.getOrPut(asset.id) {
            try {
                val inputStream = context.contentResolver.openInputStream(Uri.parse(asset.uri))
                BitmapFactory.decodeStream(inputStream)?.also { inputStream?.close() }
            } catch (e: Exception) {
                null
            } ?: return null
        }
    }
}
