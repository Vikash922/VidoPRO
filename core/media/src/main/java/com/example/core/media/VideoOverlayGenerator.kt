package com.example.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.effect.BitmapOverlay
import com.example.core.model.Asset
import com.example.core.model.Clip
import com.example.core.model.ClipType
import com.example.core.model.MediaType
import com.example.core.media.render.TimeMapping

/**
 * High-performance, memory-safe [BitmapOverlay] for Media3 Transformer that renders
 * both real moving video overlays (PiP) and image overlays on the project canvas.
 *
 * For video clips:
 * - Dynamically decodes video frames at each timestamp using [MediaMetadataRetriever]
 * - Preserves timeline synchronization: (timeMs - clip.startTimeMs) + clip.inPointMs
 * - Reuses a single target [Bitmap] and [Canvas] to prevent GC pressure and OOM
 *
 * For image clips:
 * - Decodes static bitmaps once and renders with scale, rotation, translation, and opacity
 */
class VideoOverlayGenerator(
    private val context: Context,
    private val overlayClips: List<Clip>,
    private val assets: Map<String, Asset>,
    private val videoWidth: Int,
    private val videoHeight: Int,
    private val frameExtractorFactory: ((Uri) -> FrameExtractor)? = null
) : BitmapOverlay(), AutoCloseable {

    interface FrameExtractor : AutoCloseable {
        fun getFrameAtTime(timeUs: Long): Bitmap?
    }

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

    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    // Cached image bitmaps
    private val cachedImages = mutableMapOf<String, Bitmap>()

    // Video frame extractors per clip/asset
    private val videoExtractors = mutableMapOf<String, FrameExtractor>()

    @Synchronized
    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val timeMs = presentationTimeUs / 1000L

        // Find active overlay clips at this timeline timestamp
        val activeClips = overlayClips.filter { clip ->
            clip.isVisible && timeMs >= clip.startTimeMs && timeMs < clip.endTimeMs
        }

        if (activeClips.isEmpty()) {
            return emptyBitmap
        }

        // Clear canvas for fresh composite frame
        canvasBitmap.eraseColor(Color.TRANSPARENT)

        activeClips.forEach { clip ->
            val asset = assets[clip.assetId] ?: return@forEach
            val isVideo = clip.type == ClipType.VIDEO || asset.mediaType == MediaType.VIDEO

            val sourceBitmap = if (isVideo) {
                // Calculate deterministic source time using unified TimeMapping pipeline
                val sourceOffsetMs = TimeMapping.projectTimeToSourceTime(
                    projectTimeMs = timeMs,
                    layerStartMs = clip.startTimeMs,
                    sourceInPointMs = clip.inPointMs,
                    speed = clip.speed
                )
                val sourceTimeUs = sourceOffsetMs * 1000L
                getVideoFrame(clip, asset, sourceTimeUs)
            } else {
                getImageBitmap(asset)
            } ?: return@forEach

            // Dynamically evaluate keyframe animated or static transform
            val currentT = KeyframeEvaluator.evaluateTransform(clip, timeMs)

            // Compute PiP base sizing proportional to canvas width
            val targetBaseWidth = videoWidth * 0.42f
            val baseScale = targetBaseWidth / sourceBitmap.width.toFloat()
            val totalScaleX = baseScale * currentT.scaleX
            val totalScaleY = baseScale * currentT.scaleY

            val matrix = Matrix()
            val cx = sourceBitmap.width / 2f
            val cy = sourceBitmap.height / 2f

            // Apply scale and rotation around center of bitmap
            matrix.postScale(totalScaleX, totalScaleY, cx, cy)
            matrix.postRotate(currentT.rotation, cx, cy)

            // Translate to center of video canvas, plus clip offset
            val dx = (videoWidth / 2f - cx) + currentT.x
            val dy = (videoHeight / 2f - cy) + currentT.y
            matrix.postTranslate(dx, dy)

            paint.alpha = (currentT.opacity * 255).toInt().coerceIn(0, 255)

            canvas.drawBitmap(sourceBitmap, matrix, paint)
        }

        return canvasBitmap
    }

    private fun getVideoFrame(clip: Clip, asset: Asset, sourceTimeUs: Long): Bitmap? {
        val extractor = videoExtractors.getOrPut(clip.id) {
            val uri = Uri.parse(asset.uri)
            frameExtractorFactory?.invoke(uri) ?: DefaultVideoFrameExtractor(context, uri)
        }
        return extractor.getFrameAtTime(sourceTimeUs)
    }

    private fun getImageBitmap(asset: Asset): Bitmap? {
        return cachedImages.getOrPut(asset.id) {
            try {
                val inputStream = context.contentResolver.openInputStream(Uri.parse(asset.uri))
                BitmapFactory.decodeStream(inputStream)?.also { inputStream?.close() }
            } catch (e: Exception) {
                null
            } ?: return null
        }
    }

    override fun close() {
        videoExtractors.values.forEach { it.close() }
        videoExtractors.clear()
        cachedImages.values.forEach { if (!it.isRecycled) it.recycle() }
        cachedImages.clear()
        if (!canvasBitmap.isRecycled) {
            canvasBitmap.recycle()
        }
    }

    /**
     * Default frame extractor using [MediaMetadataRetriever].
     */
    class DefaultVideoFrameExtractor(
        private val context: Context,
        private val uri: Uri
    ) : FrameExtractor {
        private var retriever: MediaMetadataRetriever? = null
        private var lastTimeUs: Long = -1L
        private var cachedFrame: Bitmap? = null

        init {
            try {
                retriever = MediaMetadataRetriever().apply {
                    setDataSource(context, uri)
                }
            } catch (_: Throwable) {
                retriever = null
            }
        }

        override fun getFrameAtTime(timeUs: Long): Bitmap? {
            val r = retriever ?: return cachedFrame

            // Reuse cached frame if within ~15ms (15,000 us) to avoid redundant native decodes
            if (cachedFrame != null && Math.abs(timeUs - lastTimeUs) < 15_000L) {
                return cachedFrame
            }

            return try {
                val frame = r.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                if (frame != null) {
                    cachedFrame = frame
                    lastTimeUs = timeUs
                }
                frame ?: cachedFrame
            } catch (_: Throwable) {
                cachedFrame
            }
        }

        override fun close() {
            try {
                retriever?.release()
            } catch (_: Throwable) {}
            retriever = null
            cachedFrame = null
        }
    }
}
