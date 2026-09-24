package com.example.core.media.render

import com.example.core.media.KeyframeEvaluator
import com.example.core.model.Keyframe
import com.example.core.model.KeyframeProperty
import com.example.core.model.Transform

/**
 * Evaluated 2D geometric and optical transform for a render layer at a specific timestamp.
 *
 * Distinguishes:
 * - Translation (x, y in project canvas coordinates)
 * - Scale (scaleX, scaleY multipliers)
 * - Rotation (degrees clockwise, 0..360)
 * - Opacity (normalized 0.0..1.0)
 * - Anchor point (anchorX, anchorY, normalized 0.0..1.0, default center 0.5, 0.5)
 */
data class RenderTransform(
    val x: Float = 0f,
    val y: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotation: Float = 0f,
    val opacity: Float = 1f,
    val anchorX: Float = 0.5f,
    val anchorY: Float = 0.5f
) {
    fun toDomainTransform(): Transform = Transform(
        x = x,
        y = y,
        scaleX = scaleX,
        scaleY = scaleY,
        rotation = rotation,
        opacity = opacity,
        anchorX = anchorX,
        anchorY = anchorY
    )

    companion object {
        val IDENTITY = RenderTransform()

        fun fromDomainTransform(transform: Transform): RenderTransform = RenderTransform(
            x = transform.x,
            y = transform.y,
            scaleX = transform.scaleX,
            scaleY = transform.scaleY,
            rotation = transform.rotation,
            opacity = transform.opacity,
            anchorX = transform.anchorX,
            anchorY = transform.anchorY
        )
    }
}

/**
 * Shared transform evaluation pipeline across Preview and Export.
 *
 * Evaluates dynamic animated keyframe properties (position, scale, rotation, opacity)
 * at any timeline timestamp, smoothly interpolating values. Falls back to static transform
 * when no keyframes are defined.
 */
object RenderTransformEvaluator {

    /**
     * Evaluates the active [RenderTransform] for a layer at [projectTimeMs].
     */
    fun evaluate(
        baseTransform: Transform,
        keyframes: List<Keyframe>,
        projectTimeMs: Long
    ): RenderTransform {
        if (keyframes.isEmpty()) {
            return RenderTransform.fromDomainTransform(baseTransform)
        }

        val posX = KeyframeEvaluator.evaluateProperty(keyframes, KeyframeProperty.POSITION_X, projectTimeMs, baseTransform.x)
        val posY = KeyframeEvaluator.evaluateProperty(keyframes, KeyframeProperty.POSITION_Y, projectTimeMs, baseTransform.y)
        val scaleX = KeyframeEvaluator.evaluateProperty(keyframes, KeyframeProperty.SCALE_X, projectTimeMs, baseTransform.scaleX)
        val scaleY = KeyframeEvaluator.evaluateProperty(keyframes, KeyframeProperty.SCALE_Y, projectTimeMs, baseTransform.scaleY)
        val rotation = KeyframeEvaluator.evaluateProperty(keyframes, KeyframeProperty.ROTATION, projectTimeMs, baseTransform.rotation)
        val opacity = KeyframeEvaluator.evaluateProperty(keyframes, KeyframeProperty.OPACITY, projectTimeMs, baseTransform.opacity)

        return RenderTransform(
            x = posX,
            y = posY,
            scaleX = scaleX,
            scaleY = scaleY,
            rotation = rotation,
            opacity = opacity.coerceIn(0f, 1f),
            anchorX = baseTransform.anchorX,
            anchorY = baseTransform.anchorY
        )
    }
}
