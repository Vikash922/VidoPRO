package com.example.core.media

import com.example.core.media.blend.BlendModeHelper
import com.example.core.media.mask.MaskEvaluator
import com.example.core.model.BlendMode
import com.example.core.model.Clip
import com.example.core.model.ClipMask
import com.example.core.model.ClipType
import com.example.core.model.Effect
import com.example.core.model.EffectStack
import com.example.core.model.EffectType
import com.example.core.model.InterpolationType
import com.example.core.model.Keyframe
import com.example.core.model.KeyframeProperty
import com.example.core.model.MaskShape
import com.example.core.model.Transform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdvancedEffectsMaskBlendTest {

    @Test
    fun testEffectStackOperations() {
        val eff1 = Effect("e1", "c1", EffectType.BRIGHTNESS, parameters = mapOf("brightness" to 10f))
        val eff2 = Effect("e2", "c1", EffectType.CONTRAST, parameters = mapOf("contrast" to 1.2f))
        val eff3 = Effect("e3", "c1", EffectType.SATURATION, parameters = mapOf("saturation" to 1.5f))

        var stack = EffectStack()
        assertEquals(0, stack.size)

        stack = stack.add(eff1).add(eff2).add(eff3)
        assertEquals(3, stack.size)
        assertEquals(listOf(eff1, eff2, eff3), stack.effects)

        // Update effect
        val updatedEff2 = eff2.copy(parameters = mapOf("contrast" to 1.8f))
        stack = stack.update(updatedEff2)
        assertEquals(1.8f, stack.get("e2")?.parameters?.get("contrast"))

        // Reorder effect: move e3 from index 2 to index 0
        stack = stack.reorder(2, 0)
        assertEquals(listOf(eff3, eff1, updatedEff2), stack.effects)

        // Remove effect
        stack = stack.remove("e1")
        assertEquals(2, stack.size)
        assertNull(stack.get("e1"))

        // Reset
        stack = stack.reset()
        assertEquals(0, stack.size)
    }

    @Test
    fun testMaskEvaluatorGeometry() {
        val canvasW = 1080f
        val canvasH = 1920f

        val rectMask = ClipMask(
            shape = MaskShape.RECTANGLE,
            x = 0.5f,
            y = 0.5f,
            width = 0.5f,
            height = 0.5f,
            rotation = 45f,
            feather = 0.2f,
            isInverted = true,
            opacity = 0.8f
        )
        val rectPath = MaskEvaluator.createAndroidPath(rectMask, canvasW, canvasH)
        assertNotNull(rectPath)
        assertFalse(rectPath.isEmpty)

        val circleMask = ClipMask(
            shape = MaskShape.CIRCLE,
            x = 0.3f,
            y = 0.4f,
            width = 0.4f,
            height = 0.4f
        )
        val circlePath = MaskEvaluator.createAndroidPath(circleMask, canvasW, canvasH)
        assertNotNull(circlePath)
        assertFalse(circlePath.isEmpty)

        val linearMask = ClipMask(
            shape = MaskShape.LINEAR_GRADIENT,
            x = 0.5f,
            y = 0.5f,
            width = 1.0f,
            height = 0.5f,
            rotation = 90f
        )
        val linearPath = MaskEvaluator.createAndroidPath(linearMask, canvasW, canvasH)
        assertNotNull(linearPath)
        assertFalse(linearPath.isEmpty)

        val radialMask = ClipMask(
            shape = MaskShape.RADIAL_GRADIENT,
            x = 0.5f,
            y = 0.5f,
            width = 0.6f,
            height = 0.6f
        )
        val radialPath = MaskEvaluator.createAndroidPath(radialMask, canvasW, canvasH)
        assertNotNull(radialPath)
        assertFalse(radialPath.isEmpty)
    }

    @Test
    fun testBlendModeHelperMappings() {
        assertEquals(android.graphics.PorterDuff.Mode.SRC_OVER, BlendModeHelper.toPorterDuffMode(BlendMode.NORMAL))
        assertEquals(android.graphics.PorterDuff.Mode.MULTIPLY, BlendModeHelper.toPorterDuffMode(BlendMode.MULTIPLY))
        assertEquals(android.graphics.PorterDuff.Mode.SCREEN, BlendModeHelper.toPorterDuffMode(BlendMode.SCREEN))
        assertEquals(android.graphics.PorterDuff.Mode.OVERLAY, BlendModeHelper.toPorterDuffMode(BlendMode.OVERLAY))
        assertEquals(android.graphics.PorterDuff.Mode.DARKEN, BlendModeHelper.toPorterDuffMode(BlendMode.DARKEN))
        assertEquals(android.graphics.PorterDuff.Mode.LIGHTEN, BlendModeHelper.toPorterDuffMode(BlendMode.LIGHTEN))
        assertEquals(android.graphics.PorterDuff.Mode.ADD, BlendModeHelper.toPorterDuffMode(BlendMode.ADD))

        assertEquals(androidx.compose.ui.graphics.BlendMode.SrcOver, BlendModeHelper.toComposeBlendMode(BlendMode.NORMAL))
        assertEquals(androidx.compose.ui.graphics.BlendMode.Multiply, BlendModeHelper.toComposeBlendMode(BlendMode.MULTIPLY))
        assertEquals(androidx.compose.ui.graphics.BlendMode.Screen, BlendModeHelper.toComposeBlendMode(BlendMode.SCREEN))
        assertEquals(androidx.compose.ui.graphics.BlendMode.Overlay, BlendModeHelper.toComposeBlendMode(BlendMode.OVERLAY))
        assertEquals(androidx.compose.ui.graphics.BlendMode.Darken, BlendModeHelper.toComposeBlendMode(BlendMode.DARKEN))
        assertEquals(androidx.compose.ui.graphics.BlendMode.Lighten, BlendModeHelper.toComposeBlendMode(BlendMode.LIGHTEN))
        assertEquals(androidx.compose.ui.graphics.BlendMode.Plus, BlendModeHelper.toComposeBlendMode(BlendMode.ADD))
    }

    @Test
    fun testKeyframedMaskAndEffectEvaluation() {
        val clip = Clip(
            id = "c1",
            trackId = "t1",
            type = ClipType.VIDEO,
            startTimeMs = 0L,
            durationMs = 2000L,
            mask = ClipMask(shape = MaskShape.CIRCLE, x = 0.2f, y = 0.2f, feather = 0.0f),
            effects = listOf(
                Effect("e_b", "c1", EffectType.BRIGHTNESS, parameters = mapOf("brightness" to 0f))
            ),
            keyframes = listOf(
                Keyframe("k1", "c1", KeyframeProperty.MASK_X, 0L, 0.2f, InterpolationType.LINEAR),
                Keyframe("k2", "c1", KeyframeProperty.MASK_X, 2000L, 0.8f, InterpolationType.LINEAR),
                Keyframe("k3", "c1", KeyframeProperty.BRIGHTNESS, 0L, 0f, InterpolationType.LINEAR),
                Keyframe("k4", "c1", KeyframeProperty.BRIGHTNESS, 2000L, 100f, InterpolationType.LINEAR)
            )
        )

        // Midpoint at 1000ms: mask.x should be 0.5f, brightness should be 50f
        val evaluatedMask = KeyframeEvaluator.evaluateMask(clip, 1000L)
        assertNotNull(evaluatedMask)
        assertEquals(0.5f, evaluatedMask!!.x, 0.01f)
        assertEquals(0.2f, evaluatedMask.y, 0.01f)

        val evaluatedEffects = KeyframeEvaluator.evaluateEffects(clip, 1000L)
        val evaluatedBrightness = evaluatedEffects.find { it.type == EffectType.BRIGHTNESS }
        assertNotNull(evaluatedBrightness)
        assertEquals(50f, evaluatedBrightness!!.parameters["brightness"] ?: 0f, 0.5f)
    }

    @Test
    fun testClipIsolation() {
        val clipA = Clip(
            id = "clip_a",
            trackId = "t1",
            type = ClipType.VIDEO,
            startTimeMs = 0L,
            durationMs = 2000L,
            mask = ClipMask(shape = MaskShape.RECTANGLE, width = 0.3f),
            blendMode = BlendMode.MULTIPLY,
            effects = listOf(Effect("e1", "clip_a", EffectType.BRIGHTNESS, parameters = mapOf("brightness" to 25f)))
        )

        val clipB = Clip(
            id = "clip_b",
            trackId = "t1",
            type = ClipType.VIDEO,
            startTimeMs = 2000L,
            durationMs = 2000L,
            mask = null,
            blendMode = BlendMode.NORMAL,
            effects = emptyList()
        )

        // Verify Clip A changes do not mutate Clip B
        assertEquals(BlendMode.MULTIPLY, clipA.blendMode)
        assertEquals(BlendMode.NORMAL, clipB.blendMode)
        assertNotNull(clipA.mask)
        assertNull(clipB.mask)
        assertEquals(1, clipA.effects.size)
        assertEquals(0, clipB.effects.size)
    }
}
