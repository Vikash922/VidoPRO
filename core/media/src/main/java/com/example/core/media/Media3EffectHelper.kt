package com.example.core.media

import android.graphics.Matrix
import androidx.media3.common.Effect
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.RgbFilter
import com.example.core.model.Clip
import com.example.core.model.EffectType
import com.example.core.model.KeyframeProperty
import com.example.core.model.Transform
import kotlin.math.pow

/**
 * Helper to convert domain [com.example.core.model.Effect] models into Media3 [Effect] instances
 * for video export processing (DEV-069).
 *
 * Maps brightness, contrast, saturation, exposure, and color balance to exact Media3 1.5.1
 * GPU-accelerated video effects.
 */
object Media3EffectHelper {

    /**
     * Converts a list of domain [com.example.core.model.Effect] models into Media3 [Effect] instances.
     * Only enabled effects are processed.
     */
    fun createMedia3Effects(effects: List<com.example.core.model.Effect>): List<Effect> {
        val activeEffects = effects.filter { it.isEnabled }
        if (activeEffects.isEmpty()) return emptyList()

        val result = mutableListOf<Effect>()

        var brightness = 0f
        var contrast = 1f
        var saturation = 1f
        var exposure = 0f
        var hue = 0f
        var redBalance = 0f
        var greenBalance = 0f
        var blueBalance = 0f

        for (effect in activeEffects) {
            val p = effect.parameters
            val v = p["value"] ?: 0f

            when (effect.type) {
                EffectType.BRIGHTNESS -> brightness = p["brightness"] ?: v
                EffectType.CONTRAST -> contrast = p["contrast"] ?: v
                EffectType.SATURATION -> saturation = p["saturation"] ?: v
                EffectType.EXPOSURE -> exposure = p["exposure"] ?: v
                else -> {}
            }

            p["hue"]?.let { hue = it }
            p["redBalance"]?.let { redBalance = it }
            p["greenBalance"]?.let { greenBalance = it }
            p["blueBalance"]?.let { blueBalance = it }
        }

        // 1. Contrast Adjustment
        // Media3 Contrast takes a float in range [-1.0f, 1.0f] where 0.0f is unchanged.
        // Domain contrast default is 1.0f (range 0.5f to 2.0f).
        if (contrast != 1f) {
            val contrastNorm = (contrast - 1f).coerceIn(-1f, 1f)
            result.add(Contrast(contrastNorm))
        }

        // 2. Grayscale Filter if saturation is completely 0 (B&W look)
        if (saturation == 0f) {
            result.add(RgbFilter.createGrayscaleFilter())
        }

        // 3. HSL Adjustments (Brightness/Lightness, Saturation, Hue)
        val hasBrightness = brightness != 0f
        val hasSaturation = saturation != 1f && saturation > 0f
        val hasHue = hue != 0f

        if (hasBrightness || hasSaturation || hasHue) {
            val hslBuilder = HslAdjustment.Builder()
            if (hasBrightness) {
                // adjustLightness accepts [-100, 100]
                hslBuilder.adjustLightness(brightness.coerceIn(-100f, 100f))
            }
            if (hasSaturation) {
                // adjustSaturation accepts [-100, 100] where 0 is no change
                val satAdj = ((saturation - 1f) * 100f).coerceIn(-100f, 100f)
                hslBuilder.adjustSaturation(satAdj)
            }
            if (hasHue) {
                // adjustHue accepts degrees [-180, 180]
                hslBuilder.adjustHue(hue.coerceIn(-180f, 180f))
            }
            result.add(hslBuilder.build())
        }

        // 4. RGB Adjustments (Exposure and Color Balance)
        val hasExposure = exposure != 0f
        val hasColorBalance = redBalance != 0f || greenBalance != 0f || blueBalance != 0f

        if (hasExposure || hasColorBalance) {
            val exposureScale = if (hasExposure) {
                2.0.pow((exposure / 50.0).toDouble()).toFloat()
            } else {
                1.0f
            }
            val rScale = (exposureScale * (1f + redBalance / 100f)).coerceAtLeast(0f)
            val gScale = (exposureScale * (1f + greenBalance / 100f)).coerceAtLeast(0f)
            val bScale = (exposureScale * (1f + blueBalance / 100f)).coerceAtLeast(0f)

            if (rScale != 1f || gScale != 1f || bScale != 1f) {
                val rgbAdjustment = RgbAdjustment.Builder()
                    .setRedScale(rScale)
                    .setGreenScale(gScale)
                    .setBlueScale(bScale)
                    .build()
                result.add(rgbAdjustment)
            }
        }

        return result
    }

    /**
     * Creates an [Effect] representing a clip's 2D transform (position, scale, rotation),
     * dynamically evaluated from keyframes or static transform.
     *
     * Returns null if the clip has default transform and no keyframes.
     */
    fun createTransformEffect(
        clip: Clip,
        canvasWidth: Int,
        canvasHeight: Int
    ): Effect? {
        val hasKeyframes = clip.keyframes.any { it.property in KeyframeProperty.ALL }
        val baseTransform = clip.transform
        if (!hasKeyframes && baseTransform == Transform.DEFAULT) {
            return null
        }

        return MatrixTransformation { presentationTimeUs ->
            val timeMs = clip.startTimeMs + (presentationTimeUs / 1000L)
            val currentT = if (hasKeyframes) {
                KeyframeEvaluator.evaluateTransform(clip, timeMs)
            } else {
                baseTransform
            }

            val matrix = Matrix()
            // 1. Scale around center (0, 0)
            matrix.postScale(currentT.scaleX, currentT.scaleY)
            // 2. Rotate around center (0, 0)
            // Screen clockwise rotation -> NDC counter-clockwise (-rotation)
            matrix.postRotate(-currentT.rotation)
            // 3. Translate in NDC coordinates
            val (ndcDx, ndcDy) = CanvasCoordinateHelper.toNdcCoordinates(currentT, canvasWidth, canvasHeight)
            matrix.postTranslate(ndcDx, ndcDy)

            matrix
        }
    }

    /**
     * Creates an [Effect] representing a [com.example.core.media.render.VideoRenderLayer]'s 2D transform,
     * evaluated dynamically from keyframes, static transform, and active transitions.
     */
    fun createTransformEffect(
        layer: com.example.core.media.render.VideoRenderLayer,
        canvasWidth: Int,
        canvasHeight: Int,
        transitionAsOutgoing: com.example.core.media.render.RenderTransition? = null,
        transitionAsIncoming: com.example.core.media.render.RenderTransition? = null
    ): Effect? {
        val hasKeyframes = layer.keyframes.any { it.property in KeyframeProperty.ALL }
        val baseTransform = layer.transform
        val hasTransition = transitionAsOutgoing != null || transitionAsIncoming != null
        if (!hasKeyframes && baseTransform == Transform.DEFAULT && !hasTransition) {
            return null
        }

        return MatrixTransformation { presentationTimeUs ->
            val timeMs = layer.timelineStartMs + (presentationTimeUs / 1000L)
            var currentT = layer.evaluateTransformAt(timeMs)

            if (transitionAsOutgoing != null && transitionAsOutgoing.isActiveAt(timeMs)) {
                val progress = transitionAsOutgoing.progressAt(timeMs)
                currentT = com.example.core.media.transition.TransitionEvaluator.evaluateOutgoingTransform(
                    transition = transitionAsOutgoing,
                    rawProgress = progress,
                    baseTransform = currentT,
                    canvasWidth = canvasWidth.toFloat(),
                    canvasHeight = canvasHeight.toFloat()
                )
            } else if (transitionAsIncoming != null && transitionAsIncoming.isActiveAt(timeMs)) {
                val progress = transitionAsIncoming.progressAt(timeMs)
                currentT = com.example.core.media.transition.TransitionEvaluator.evaluateIncomingTransform(
                    transition = transitionAsIncoming,
                    rawProgress = progress,
                    baseTransform = currentT,
                    canvasWidth = canvasWidth.toFloat(),
                    canvasHeight = canvasHeight.toFloat()
                )
            }

            val matrix = Matrix()
            matrix.postScale(currentT.scaleX, currentT.scaleY)
            matrix.postRotate(-currentT.rotation)
            val (ndcDx, ndcDy) = CanvasCoordinateHelper.toNdcCoordinates(currentT.toDomainTransform(), canvasWidth, canvasHeight)
            matrix.postTranslate(ndcDx, ndcDy)

            matrix
        }
    }

    /**
     * Creates an [Effect] to adjust clip opacity by scaling RGB values toward the background.
     * Returns null if opacity is 1.0f (fully opaque).
     */
    fun createOpacityEffect(opacity: Float): Effect? {
        val clamped = opacity.coerceIn(0f, 1f)
        if (clamped >= 1f) return null

        return RgbAdjustment.Builder()
            .setRedScale(clamped)
            .setGreenScale(clamped)
            .setBlueScale(clamped)
            .build()
    }
}
