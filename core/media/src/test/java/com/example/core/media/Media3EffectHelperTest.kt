package com.example.core.media

import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.RgbFilter
import com.example.core.model.Effect
import com.example.core.model.EffectType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Media3EffectHelperTest {

    @Test
    fun testEmptyEffectsReturnsEmptyList() {
        val effects = emptyList<Effect>()
        val media3Effects = Media3EffectHelper.createMedia3Effects(effects)
        assertTrue(media3Effects.isEmpty())
    }

    @Test
    fun testDisabledEffectsIgnored() {
        val effects = listOf(
            Effect(
                id = "eff_1",
                clipId = "clip_1",
                type = EffectType.BRIGHTNESS,
                isEnabled = false,
                parameters = mapOf("value" to 20f)
            )
        )
        val media3Effects = Media3EffectHelper.createMedia3Effects(effects)
        assertTrue(media3Effects.isEmpty())
    }

    @Test
    fun testContrastEffectGenerated() {
        val effects = listOf(
            Effect(
                id = "eff_c",
                clipId = "clip_1",
                type = EffectType.CONTRAST,
                isEnabled = true,
                parameters = mapOf("value" to 1.5f)
            )
        )
        val media3Effects = Media3EffectHelper.createMedia3Effects(effects)
        assertEquals(1, media3Effects.size)
        assertTrue(media3Effects[0] is Contrast)
    }

    @Test
    fun testBrightnessEffectGeneratesHslAdjustment() {
        val effects = listOf(
            Effect(
                id = "eff_b",
                clipId = "clip_1",
                type = EffectType.BRIGHTNESS,
                isEnabled = true,
                parameters = mapOf("value" to 25f)
            )
        )
        val media3Effects = Media3EffectHelper.createMedia3Effects(effects)
        assertEquals(1, media3Effects.size)
        assertTrue(media3Effects[0] is HslAdjustment)
    }

    @Test
    fun testZeroSaturationGeneratesGrayscaleFilter() {
        val effects = listOf(
            Effect(
                id = "eff_s",
                clipId = "clip_1",
                type = EffectType.SATURATION,
                isEnabled = true,
                parameters = mapOf("value" to 0f)
            )
        )
        val media3Effects = Media3EffectHelper.createMedia3Effects(effects)
        assertEquals(1, media3Effects.size)
        assertTrue(media3Effects[0] is RgbFilter)
    }

    @Test
    fun testExposureEffectGeneratesRgbAdjustment() {
        val effects = listOf(
            Effect(
                id = "eff_e",
                clipId = "clip_1",
                type = EffectType.EXPOSURE,
                isEnabled = true,
                parameters = mapOf("value" to 15f)
            )
        )
        val media3Effects = Media3EffectHelper.createMedia3Effects(effects)
        assertEquals(1, media3Effects.size)
        assertTrue(media3Effects[0] is RgbAdjustment)
    }

    @Test
    fun testCombinedEffectsGeneratesAllPipelineStages() {
        val effects = listOf(
            Effect(
                id = "eff_c",
                clipId = "clip_1",
                type = EffectType.CONTRAST,
                isEnabled = true,
                parameters = mapOf("value" to 1.3f)
            ),
            Effect(
                id = "eff_b",
                clipId = "clip_1",
                type = EffectType.BRIGHTNESS,
                isEnabled = true,
                parameters = mapOf("value" to 10f)
            ),
            Effect(
                id = "eff_s",
                clipId = "clip_1",
                type = EffectType.SATURATION,
                isEnabled = true,
                parameters = mapOf("value" to 1.2f)
            ),
            Effect(
                id = "eff_e",
                clipId = "clip_1",
                type = EffectType.EXPOSURE,
                isEnabled = true,
                parameters = mapOf("value" to 5f)
            )
        )
        val media3Effects = Media3EffectHelper.createMedia3Effects(effects)
        // Expected: Contrast, HslAdjustment (brightness + saturation), RgbAdjustment (exposure)
        assertEquals(3, media3Effects.size)
        assertTrue(media3Effects.any { it is Contrast })
        assertTrue(media3Effects.any { it is HslAdjustment })
        assertTrue(media3Effects.any { it is RgbAdjustment })
    }
}
