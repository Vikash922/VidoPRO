package com.example.core.media

import com.example.core.model.Clip
import com.example.core.model.ClipMask
import com.example.core.model.ClipType
import com.example.core.model.Effect
import com.example.core.model.EffectType
import com.example.core.model.InterpolationType
import com.example.core.model.Keyframe
import com.example.core.model.KeyframeProperty
import com.example.core.model.MaskShape
import com.example.core.model.Transform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdvancedKeyframeMotionTest {

    @Test
    fun testAllInterpolationCurvesAtProgressPoints() {
        val from = 0f
        val to = 100f

        // HOLD: stays at 'from' until t=1.0f
        assertEquals(0f, KeyframeEvaluator.interpolate(from, to, 0.0f, InterpolationType.HOLD), 0.001f)
        assertEquals(0f, KeyframeEvaluator.interpolate(from, to, 0.5f, InterpolationType.HOLD), 0.001f)
        assertEquals(0f, KeyframeEvaluator.interpolate(from, to, 0.999f, InterpolationType.HOLD), 0.001f)
        assertEquals(100f, KeyframeEvaluator.interpolate(from, to, 1.0f, InterpolationType.HOLD), 0.001f)

        // LINEAR: exact midpoint
        assertEquals(0f, KeyframeEvaluator.interpolate(from, to, 0.0f, InterpolationType.LINEAR), 0.001f)
        assertEquals(50f, KeyframeEvaluator.interpolate(from, to, 0.5f, InterpolationType.LINEAR), 0.001f)
        assertEquals(100f, KeyframeEvaluator.interpolate(from, to, 1.0f, InterpolationType.LINEAR), 0.001f)

        // EASE_IN (Quad): t^2 -> 0.5^2 = 0.25 -> 25f
        assertEquals(25f, KeyframeEvaluator.interpolate(from, to, 0.5f, InterpolationType.EASE_IN), 0.001f)

        // EASE_OUT (Quad): 1 - (1-t)^2 -> 1 - 0.25 = 0.75 -> 75f
        assertEquals(75f, KeyframeEvaluator.interpolate(from, to, 0.5f, InterpolationType.EASE_OUT), 0.001f)

        // EASE_IN_OUT (Quad): midpoint is 50f
        assertEquals(50f, KeyframeEvaluator.interpolate(from, to, 0.5f, InterpolationType.EASE_IN_OUT), 0.001f)
        // at t=0.25: 2 * 0.25^2 = 0.125 -> 12.5f
        assertEquals(12.5f, KeyframeEvaluator.interpolate(from, to, 0.25f, InterpolationType.EASE_IN_OUT), 0.001f)

        // CUBIC_EASE_IN: t^3 -> 0.5^3 = 0.125 -> 12.5f
        assertEquals(12.5f, KeyframeEvaluator.interpolate(from, to, 0.5f, InterpolationType.CUBIC_EASE_IN), 0.001f)

        // CUBIC_EASE_OUT: 1 - (1-t)^3 -> 1 - 0.125 = 0.875 -> 87.5f
        assertEquals(87.5f, KeyframeEvaluator.interpolate(from, to, 0.5f, InterpolationType.CUBIC_EASE_OUT), 0.001f)

        // CUBIC_EASE_IN_OUT: midpoint is 50f
        assertEquals(50f, KeyframeEvaluator.interpolate(from, to, 0.5f, InterpolationType.CUBIC_EASE_IN_OUT), 0.001f)

        // SMOOTH (Hermite Smoothstep: 3t^2 - 2t^3): midpoint is 50f
        assertEquals(50f, KeyframeEvaluator.interpolate(from, to, 0.5f, InterpolationType.SMOOTH), 0.001f)
        // at t=0.25: 3*(0.0625) - 2*(0.015625) = 0.1875 - 0.03125 = 0.15625 -> 15.625f
        assertEquals(15.625f, KeyframeEvaluator.interpolate(from, to, 0.25f, InterpolationType.SMOOTH), 0.001f)
    }

    @Test
    fun testRotationShortestPathAndContinuousSpins() {
        // 1. Boundary crossing: 350 deg to 10 deg
        // Shortest path: +20 degrees clockwise.
        // Midpoint should pass through 0 deg (or 360 deg), i.e., 360f % 360f = 0f.
        val boundaryMid = KeyframeEvaluator.evaluateRotation(350f, 10f, 0.5f, InterpolationType.LINEAR)
        val normalizedMid = (boundaryMid % 360f + 360f) % 360f
        assertEquals(0f, normalizedMid, 0.001f)

        // 2. Multi-turn spins: 0 deg to 720 deg (two full revolutions)
        // Must preserve full continuous multi-turn rotation!
        val multiTurnQuarter = KeyframeEvaluator.evaluateRotation(0f, 720f, 0.25f, InterpolationType.LINEAR)
        assertEquals(180f, multiTurnQuarter, 0.001f)

        val multiTurnMid = KeyframeEvaluator.evaluateRotation(0f, 720f, 0.5f, InterpolationType.LINEAR)
        assertEquals(360f, multiTurnMid, 0.001f)

        val multiTurnEnd = KeyframeEvaluator.evaluateRotation(0f, 720f, 1.0f, InterpolationType.LINEAR)
        assertEquals(720f, multiTurnEnd, 0.001f)

        // 3. Multi-turn spins: 0 deg to 360 deg
        val oneTurnMid = KeyframeEvaluator.evaluateRotation(0f, 360f, 0.5f, InterpolationType.LINEAR)
        assertEquals(180f, oneTurnMid, 0.001f)
    }

    @Test
    fun testKeyframedTransformEvaluation() {
        val clip = Clip(
            id = "c1",
            trackId = "t1",
            assetId = "a1",
            type = ClipType.VIDEO,
            startTimeMs = 0L,
            endTimeMs = 5000L,
            transform = Transform(x = 0f, y = 0f, scaleX = 1f, scaleY = 1f, rotation = 0f, opacity = 1f),
            keyframes = listOf(
                Keyframe("k1", "c1", KeyframeProperty.POSITION_X, 1000L, 100f, InterpolationType.LINEAR),
                Keyframe("k2", "c1", KeyframeProperty.POSITION_X, 3000L, 500f, InterpolationType.LINEAR),
                Keyframe("k3", "c1", KeyframeProperty.OPACITY, 0L, 0f, InterpolationType.EASE_IN),
                Keyframe("k4", "c1", KeyframeProperty.OPACITY, 2000L, 1f, InterpolationType.EASE_IN),
                Keyframe("k5", "c1", KeyframeProperty.SCALE_X, 1000L, 1f, InterpolationType.CUBIC_EASE_IN_OUT),
                Keyframe("k6", "c1", KeyframeProperty.SCALE_X, 3000L, 2f, InterpolationType.CUBIC_EASE_IN_OUT)
            )
        )

        // Before first position keyframe (< 1000ms): clamped to first keyframe value (100f)
        val t0 = KeyframeEvaluator.evaluateTransform(clip, 500L)
        assertEquals(100f, t0.x, 0.001f)

        // At exactly first keyframe (1000ms)
        val t1 = KeyframeEvaluator.evaluateTransform(clip, 1000L)
        assertEquals(100f, t1.x, 0.001f)
        assertEquals(1f, t1.scaleX, 0.001f)

        // Midpoint of position X (2000ms): linear between 100f and 500f = 300f
        val t2 = KeyframeEvaluator.evaluateTransform(clip, 2000L)
        assertEquals(300f, t2.x, 0.001f)
        assertEquals(1f, t2.opacity, 0.001f) // at 2000ms opacity keyframe is 1f
        assertEquals(1.5f, t2.scaleX, 0.001f) // midpoint cubic ease in-out is 1.5f

        // After last position keyframe (> 3000ms): clamped to last keyframe value (500f)
        val t3 = KeyframeEvaluator.evaluateTransform(clip, 4000L)
        assertEquals(500f, t3.x, 0.001f)
        assertEquals(2f, t3.scaleX, 0.001f)
    }

    @Test
    fun testKeyframedEffectsEvaluation() {
        val clip = Clip(
            id = "c1",
            trackId = "t1",
            assetId = "a1",
            type = ClipType.VIDEO,
            startTimeMs = 0L,
            endTimeMs = 4000L,
            effects = listOf(
                Effect("e1", "c1", EffectType.BRIGHTNESS, parameters = mapOf("brightness" to 0f)),
                Effect("e2", "c1", EffectType.CONTRAST, parameters = mapOf("contrast" to 1f))
            ),
            keyframes = listOf(
                Keyframe("k1", "c1", KeyframeProperty.BRIGHTNESS, 0L, 0f, InterpolationType.LINEAR),
                Keyframe("k2", "c1", KeyframeProperty.BRIGHTNESS, 2000L, 0.5f, InterpolationType.LINEAR),
                Keyframe("k3", "c1", KeyframeProperty.CONTRAST, 1000L, 1.0f, InterpolationType.EASE_OUT),
                Keyframe("k4", "c1", KeyframeProperty.CONTRAST, 3000L, 2.0f, InterpolationType.EASE_OUT),
                Keyframe("k5", "c1", KeyframeProperty.HIGHLIGHTS, 0L, 0.2f, InterpolationType.LINEAR),
                Keyframe("k6", "c1", KeyframeProperty.HIGHLIGHTS, 2000L, 0.8f, InterpolationType.LINEAR),
                Keyframe("k7", "c1", KeyframeProperty.SHADOWS, 0L, -0.4f, InterpolationType.LINEAR),
                Keyframe("k8", "c1", KeyframeProperty.SHADOWS, 2000L, 0.4f, InterpolationType.LINEAR)
            )
        )

        // Evaluate at 1000ms (midpoint for brightness)
        val effectsAt1000 = KeyframeEvaluator.evaluateEffects(clip, 1000L)
        val brightness = effectsAt1000.find { it.type == EffectType.BRIGHTNESS }?.parameters?.get("brightness")
        assertNotNull(brightness)
        assertEquals(0.25f, brightness!!, 0.001f)

        // Highlights and Shadows at 1000ms
        val highlights = effectsAt1000.find { it.type == EffectType.HIGHLIGHTS }?.parameters?.get("highlights")
        assertNotNull(highlights)
        assertEquals(0.5f, highlights!!, 0.001f)

        val shadows = effectsAt1000.find { it.type == EffectType.SHADOWS }?.parameters?.get("shadows")
        assertNotNull(shadows)
        assertEquals(0.0f, shadows!!, 0.001f)

        // Contrast at 2000ms: midpoint of ease_out between 1.0 and 2.0 is 1.75
        val effectsAt2000 = KeyframeEvaluator.evaluateEffects(clip, 2000L)
        val contrast = effectsAt2000.find { it.type == EffectType.CONTRAST }?.parameters?.get("contrast")
        assertNotNull(contrast)
        assertEquals(1.75f, contrast!!, 0.001f)
    }

    @Test
    fun testKeyframedMaskEvaluation() {
        val clip = Clip(
            id = "c1",
            trackId = "t1",
            assetId = "a1",
            type = ClipType.VIDEO,
            startTimeMs = 0L,
            endTimeMs = 4000L,
            mask = ClipMask(shape = MaskShape.RECTANGLE, x = 0.5f, y = 0.5f, feather = 0f),
            keyframes = listOf(
                Keyframe("k1", "c1", KeyframeProperty.MASK_X, 0L, 0.2f, InterpolationType.LINEAR),
                Keyframe("k2", "c1", KeyframeProperty.MASK_X, 2000L, 0.8f, InterpolationType.LINEAR),
                Keyframe("k3", "c1", KeyframeProperty.MASK_FEATHER, 1000L, 0.1f, InterpolationType.LINEAR),
                Keyframe("k4", "c1", KeyframeProperty.MASK_FEATHER, 3000L, 0.5f, InterpolationType.LINEAR)
            )
        )

        val evaluatedMask = KeyframeEvaluator.evaluateMask(clip, 1000L)
        assertNotNull(evaluatedMask)
        assertEquals(0.5f, evaluatedMask!!.x, 0.001f)
        assertEquals(0.1f, evaluatedMask.feather, 0.001f)

        val evaluatedMask2 = KeyframeEvaluator.evaluateMask(clip, 2000L)
        assertNotNull(evaluatedMask2)
        assertEquals(0.8f, evaluatedMask2!!.x, 0.001f)
        assertEquals(0.3f, evaluatedMask2.feather, 0.001f)
    }

    @Test
    fun testKeyframedVolumeEvaluation() {
        val clip = Clip(
            id = "c1",
            trackId = "t1",
            assetId = "a1",
            type = ClipType.AUDIO,
            startTimeMs = 0L,
            endTimeMs = 5000L,
            volume = 1.0f,
            keyframes = listOf(
                Keyframe("k1", "c1", KeyframeProperty.VOLUME, 0L, 0f, InterpolationType.LINEAR),
                Keyframe("k2", "c1", KeyframeProperty.VOLUME, 2000L, 1f, InterpolationType.LINEAR),
                Keyframe("k3", "c1", KeyframeProperty.VOLUME, 4000L, 0.2f, InterpolationType.LINEAR)
            )
        )

        assertEquals(0f, KeyframeEvaluator.evaluateVolume(clip, 0L), 0.001f)
        assertEquals(0.5f, KeyframeEvaluator.evaluateVolume(clip, 1000L), 0.001f)
        assertEquals(1.0f, KeyframeEvaluator.evaluateVolume(clip, 2000L), 0.001f)
        assertEquals(0.6f, KeyframeEvaluator.evaluateVolume(clip, 3000L), 0.001f)
        assertEquals(0.2f, KeyframeEvaluator.evaluateVolume(clip, 4000L), 0.001f)
        assertEquals(0.2f, KeyframeEvaluator.evaluateVolume(clip, 5000L), 0.001f)
    }

    @Test
    fun testFrameRateIndependence() {
        val keyframes = listOf(
            Keyframe("k1", "c1", KeyframeProperty.POSITION_X, 0L, 0f, InterpolationType.EASE_IN_OUT),
            Keyframe("k2", "c1", KeyframeProperty.POSITION_X, 1000L, 100f, InterpolationType.EASE_IN_OUT)
        )

        // 30 FPS timestamp (e.g. 500ms) vs 60 FPS timestamp (500ms) yield identical value
        val valAt500 = KeyframeEvaluator.evaluateProperty(keyframes, KeyframeProperty.POSITION_X, 500L, 0f)
        assertEquals(50f, valAt500, 0.001f)

        // Microsecond parity test (arbitrary continuous timestamps)
        val valAt250 = KeyframeEvaluator.evaluateProperty(keyframes, KeyframeProperty.POSITION_X, 250L, 0f)
        assertEquals(12.5f, valAt250, 0.001f)

        val valAt750 = KeyframeEvaluator.evaluateProperty(keyframes, KeyframeProperty.POSITION_X, 750L, 0f)
        assertEquals(87.5f, valAt750, 0.001f)
    }
}
