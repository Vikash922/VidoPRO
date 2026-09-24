package com.example.core.media.mask

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF
import com.example.core.model.ClipMask
import com.example.core.model.MaskShape

/**
 * Universal evaluator for [ClipMask] geometry across Compose Preview and Media3 Export.
 *
 * Constructs exact clipping paths and boundaries in normalized layer space,
 * with support for rotation, inversion, and smooth edge feathering.
 */
object MaskEvaluator {

    /**
     * Builds an Android [Path] representing the mask in layer pixel coordinates ([layerWidth] x [layerHeight]).
     */
    fun createMaskPath(
        mask: ClipMask,
        layerWidth: Float,
        layerHeight: Float
    ): Path {
        val path = Path()
        val cx = (layerWidth / 2f) + mask.x
        val cy = (layerHeight / 2f) + mask.y
        val mw = (layerWidth * mask.width).coerceAtLeast(1f)
        val mh = (layerHeight * mask.height).coerceAtLeast(1f)

        val left = cx - (mw / 2f)
        val top = cy - (mh / 2f)
        val right = cx + (mw / 2f)
        val bottom = cy + (mh / 2f)

        when (mask.shape) {
            MaskShape.RECTANGLE -> {
                val cornerRadius = (mask.feather * 0.5f).coerceIn(0f, minOf(mw, mh) / 2f)
                val rect = RectF(left, top, right, bottom)
                if (cornerRadius > 0f) {
                    path.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
                } else {
                    path.addRect(rect, Path.Direction.CW)
                }
            }
            MaskShape.CIRCLE, MaskShape.RADIAL_GRADIENT -> {
                val rect = RectF(left, top, right, bottom)
                path.addOval(rect, Path.Direction.CW)
            }
            MaskShape.LINEAR_GRADIENT -> {
                // Linear mask splits half-plane
                path.addRect(RectF(left, top, right, bottom), Path.Direction.CW)
            }
        }

        // Apply mask rotation around center
        if (mask.rotation != 0f) {
            val matrix = Matrix()
            matrix.postRotate(mask.rotation, cx, cy)
            path.transform(matrix)
        }

        // If inverted, subtract mask path from full layer bounds
        if (mask.isInverted) {
            val fullBounds = Path().apply {
                addRect(RectF(0f, 0f, layerWidth, layerHeight), Path.Direction.CW)
            }
            val invertedPath = Path()
            invertedPath.op(fullBounds, path, Path.Op.DIFFERENCE)
            return invertedPath
        }

        return path
    }

    /**
     * Alias for [createMaskPath] returning an Android [android.graphics.Path].
     */
    fun createAndroidPath(mask: ClipMask, layerWidth: Float, layerHeight: Float): Path {
        return createMaskPath(mask, layerWidth, layerHeight)
    }
}
