package com.example.core.media.render

import com.example.core.media.CanvasCoordinateHelper
import com.example.core.model.AspectRatio
import com.example.core.model.Transform

/**
 * Universal Canvas Configuration and Coordinate Transformation Pipeline.
 *
 * Distinguishes:
 * 1. PROJECT / CANVAS COORDINATES: Logical project pixels (e.g. 1080x1920, 1920x1080, 1080x1080, 1080x1350)
 * 2. PREVIEW SCREEN PIXELS: Dynamic device viewport pixels inside Jetpack Compose UI
 * 3. EXPORT FRAME PIXELS: Final output video pixels (720p, 1080p, 1440p, 4K)
 * 4. MEDIA3 NDC: Normalized Device Coordinates [-1.0, 1.0] for GPU MatrixTransformation shaders
 */
data class CanvasConfig(
    val width: Int = 1080,
    val height: Int = 1920,
    val aspectRatio: AspectRatio = AspectRatio.RATIO_9_16
) {
    val floatRatio: Float
        get() = aspectRatio.floatRatio

    /**
     * Converts project coordinates into Preview Screen Pixels for Compose rendering.
     */
    fun projectToPreview(
        transform: RenderTransform,
        previewWidthPx: Float,
        previewHeightPx: Float
    ): RenderTransform {
        val safeW = width.toFloat().coerceAtLeast(1f)
        val safeH = height.toFloat().coerceAtLeast(1f)

        val previewX = transform.x * (previewWidthPx / safeW)
        val previewY = transform.y * (previewHeightPx / safeH)

        return transform.copy(
            x = previewX,
            y = previewY
        )
    }

    /**
     * Converts a gesture pan delta (in screen pixels) on the preview canvas to project coordinate delta.
     */
    fun previewPanToProject(
        panX: Float,
        panY: Float,
        previewWidthPx: Float,
        previewHeightPx: Float
    ): Pair<Float, Float> {
        val safePreviewW = previewWidthPx.coerceAtLeast(1f)
        val safePreviewH = previewHeightPx.coerceAtLeast(1f)
        val safeW = width.toFloat().coerceAtLeast(1f)
        val safeH = height.toFloat().coerceAtLeast(1f)

        val deltaX = panX * (safeW / safePreviewW)
        val deltaY = panY * (safeH / safePreviewH)
        return Pair(deltaX, deltaY)
    }

    /**
     * Converts project coordinates into Media3 Normalized Device Coordinates (NDC) in range [-1.0, 1.0].
     * (0, 0) is the center of the frame; X is [-1, 1] left-to-right, Y is [-1, 1] bottom-to-top.
     */
    fun projectToExportNdc(transform: RenderTransform, exportWidth: Int, exportHeight: Int): Pair<Float, Float> {
        val safeW = exportWidth.toFloat().coerceAtLeast(1f)
        val safeH = exportHeight.toFloat().coerceAtLeast(1f)

        val ndcDx = (2f * transform.x) / safeW
        val ndcDy = -(2f * transform.y) / safeH // Y is inverted in OpenGL NDC
        return Pair(ndcDx, ndcDy)
    }

    /**
     * Converts project coordinates into Export Frame Pixels for custom bitmap compositing.
     */
    fun projectToExportPixels(transform: RenderTransform, exportWidth: Int, exportHeight: Int): RenderTransform {
        val safeW = width.toFloat().coerceAtLeast(1f)
        val safeH = height.toFloat().coerceAtLeast(1f)

        val exportX = transform.x * (exportWidth.toFloat() / safeW)
        val exportY = transform.y * (exportHeight.toFloat() / safeH)

        return transform.copy(
            x = exportX,
            y = exportY
        )
    }

    companion object {
        val DEFAULT_9_16 = CanvasConfig(1080, 1920, AspectRatio.RATIO_9_16)
        val DEFAULT_16_9 = CanvasConfig(1920, 1080, AspectRatio.RATIO_16_9)
        val DEFAULT_1_1 = CanvasConfig(1080, 1080, AspectRatio.RATIO_1_1)
        val DEFAULT_4_5 = CanvasConfig(1080, 1350, AspectRatio.RATIO_4_5)

        fun forAspectRatio(aspectRatio: AspectRatio): CanvasConfig = when (aspectRatio) {
            AspectRatio.RATIO_9_16 -> DEFAULT_9_16
            AspectRatio.RATIO_16_9 -> DEFAULT_16_9
            AspectRatio.RATIO_1_1 -> DEFAULT_1_1
            AspectRatio.RATIO_4_5 -> DEFAULT_4_5
        }
    }
}
