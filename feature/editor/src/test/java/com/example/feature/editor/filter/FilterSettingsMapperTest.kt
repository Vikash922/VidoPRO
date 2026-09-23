package com.example.feature.editor.filter

import com.example.core.model.EffectType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterSettingsMapperTest {

    @Test
    fun testDefaultSettingsProducesEmptyEffects() {
        val defaultSettings = FilterSettings()
        val effects = FilterSettingsMapper.toEffects(defaultSettings, "clip_1")
        assertTrue(effects.isEmpty())
    }

    @Test
    fun testBasicAdjustmentsProducesCorrespondingEffects() {
        val settings = FilterSettings(
            brightness = 15f,
            contrast = 1.35f,
            saturation = 0.8f,
            exposure = -10f
        )
        val effects = FilterSettingsMapper.toEffects(settings, "clip_1")
        assertEquals(4, effects.size)

        val types = effects.map { it.type }.toSet()
        assertTrue(types.contains(EffectType.BRIGHTNESS))
        assertTrue(types.contains(EffectType.CONTRAST))
        assertTrue(types.contains(EffectType.SATURATION))
        assertTrue(types.contains(EffectType.EXPOSURE))

        val brightnessEff = effects.find { it.type == EffectType.BRIGHTNESS }!!
        assertEquals(15f, brightnessEff.parameters["value"]!!, 0.001f)

        val contrastEff = effects.find { it.type == EffectType.CONTRAST }!!
        assertEquals(1.35f, contrastEff.parameters["value"]!!, 0.001f)

        val saturationEff = effects.find { it.type == EffectType.SATURATION }!!
        assertEquals(0.8f, saturationEff.parameters["value"]!!, 0.001f)

        val exposureEff = effects.find { it.type == EffectType.EXPOSURE }!!
        assertEquals(-10f, exposureEff.parameters["value"]!!, 0.001f)
    }

    @Test
    fun testRoundTripConversionPreservesValues() {
        val original = FilterSettings(
            brightness = -20f,
            contrast = 1.4f,
            saturation = 1.25f,
            exposure = 8f,
            vignette = 30f,
            temperature = 15f,
            tint = -10f
        )

        val effects = FilterSettingsMapper.toEffects(original, "clip_test")
        val restored = FilterSettingsMapper.fromEffects(effects)

        assertEquals(original.brightness, restored.brightness, 0.001f)
        assertEquals(original.contrast, restored.contrast, 0.001f)
        assertEquals(original.saturation, restored.saturation, 0.001f)
        assertEquals(original.exposure, restored.exposure, 0.001f)
        assertEquals(original.vignette, restored.vignette, 0.001f)
        assertEquals(original.temperature, restored.temperature, 0.001f)
        assertEquals(original.tint, restored.tint, 0.001f)
    }

    @Test
    fun testPresetRoundTripConversion() {
        val preset = FilterPreset.CINEMATIC
        val settings = preset.toFilterSettings()

        val effects = FilterSettingsMapper.toEffects(settings, "clip_cine")
        val restored = FilterSettingsMapper.fromEffects(effects)

        assertEquals(FilterPreset.CINEMATIC, restored.selectedPreset)
        assertEquals(settings.contrast, restored.contrast, 0.001f)
        assertEquals(settings.saturation, restored.saturation, 0.001f)
        assertEquals(settings.vignette, restored.vignette, 0.001f)
    }
}
