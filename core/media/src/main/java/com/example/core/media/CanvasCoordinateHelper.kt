package com.example.core.media

import com.example.core.model.AspectRatio
import com.example.core.model.Transform

/**
 * Deterministic coordinate conversion pipeline for VidoPRO transforms.
 *
 * Converts between:
 *  1. Project/Timeline coordinates (pixels relative to Project width/height, e.g. 1080x1920)
 *  2. Canvas/Preview coordinates (pixels on the Compose screen canvas)
 *  3. Media/Export coordinates (Normalized Device Coordinates [-1.0, 1.0] for Media3 MatrixTransformation)
 */
object CanvasCoordinateHelper {

    /**
     * Converts project coordinates into Media3 Normalized Device Coordinates (NDC) in range [-1.0, 1.0].
     *
     * In NDC:
     * - (0, 0) is the center of the output video frame.
     * - X spans [-1.0, 1.0] from left to right.
     * - Y spans [-1.0, 1.0] from bottom to top (inverted compared to screen/canvas Y).
     *
     * @param transform The clip's domain transform.
     * @param projectWidth Width of the project/canvas in pixels (e.g. 1080).
     * @param projectHeight Height of the project/canvas in pixels (e.g. 1920).
     * @return Pair(ndcDx, ndcDy) representing normalized translations in [-1.0, 1.0].
     */
    fun toNdcCoordinates(
        transform: Transform,
        projectWidth: Int,
        projectHeight: Int
    ): Pair<Float, Float> {
        val safeW = if (projectWidth > 0) projectWidth.toFloat() else 1080f
        val safeH = if (projectHeight > 0) projectHeight.toFloat() else 1920f

        val ndcDx = (2f * transform.x) / safeW
        val ndcDy = -(2f * transform.y) / safeH // Y is inverted in NDC
        return Pair(ndcDx, ndcDy)
    }

    /**
     * Converts a domain [Transform] (stored in project coordinates) to preview coordinates (pixels)
     * for rendering in Jetpack Compose [androidx.compose.ui.graphics.graphicsLayer].
     *
     * @param transform The clip's domain transform.
     * @param canvasWidthPx Width of the preview canvas in screen pixels.
     * @param canvasHeightPx Height of the preview canvas in screen pixels.
     * @param projectWidth Width of the project in project pixels.
     * @param projectHeight Height of the project in project pixels.
     */
    fun toPreviewCoordinates(
        transform: Transform,
        canvasWidthPx: Float,
        canvasHeightPx: Float,
        projectWidth: Int,
        projectHeight: Int
    ): Transform {
        val safeW = if (projectWidth > 0) projectWidth.toFloat() else 1080f
        val safeH = if (projectHeight > 0) projectHeight.toFloat() else 1920f

        val previewX = transform.x * (canvasWidthPx / safeW)
        val previewY = transform.y * (canvasHeightPx / safeH)

        return transform.copy(
            x = previewX,
            y = previewY
        )
    }

    /**
     * Converts a gesture pan delta (in screen pixels on the preview canvas) to project coordinate delta.
     *
     * @param panX Horizontal pan delta in screen pixels.
     * @param panY Vertical pan delta in screen pixels.
     * @param canvasWidthPx Width of the preview canvas in screen pixels.
     * @param canvasHeightPx Height of the preview canvas in screen pixels.
     * @param projectWidth Width of the project in project pixels.
     * @param projectHeight Height of the project in project pixels.
     * @return Pair(deltaX, deltaY) in project coordinates.
     */
    fun fromPreviewPan(
        panX: Float,
        panY: Float,
        canvasWidthPx: Float,
        canvasHeightPx: Float,
        projectWidth: Int,
        projectHeight: Int
    ): Pair<Float, Float> {
        val safeCanvasW = if (canvasWidthPx > 0f) canvasWidthPx else 1f
        val safeCanvasH = if (canvasHeightPx > 0f) canvasHeightPx else 1f
        val safeW = if (projectWidth > 0) projectWidth.toFloat() else 1080f
        val safeH = if (projectHeight > 0) projectHeight.toFloat() else 1920f

        val deltaX = panX * (safeW / safeCanvasW)
        val deltaY = panY * (safeH / safeCanvasH)
        return Pair(deltaX, deltaY)
    }

    /**
     * Resolves standard project dimensions for a given [AspectRatio].
     */
    fun getDimensionsForAspectRatio(aspectRatio: AspectRatio): Pair<Int, Int> {
        return when (aspectRatio) {
            AspectRatio.RATIO_9_16 -> 1080 to 1920
            AspectRatio.RATIO_16_9 -> 1920 to 1080
            AspectRatio.RATIO_1_1 -> 1080 to 1080
            AspectRatio.RATIO_4_5 -> 1080 to 1350
        }
    }
}
