package com.example.feature.editor.filter

import com.example.core.model.Effect
import com.example.core.model.EffectType
import java.util.UUID

/**
 * Bidirectional mapper between UI [FilterSettings] and domain [Effect] models for clips.
 *
 * Ensures complete parity between real-time editor preview adjustments and persisted
 * per-clip effects that are exported via Media3 Transformer.
 */
object FilterSettingsMapper {

    /**
     * Converts a [FilterSettings] into domain [Effect] models for the given [clipId].
     */
    fun toEffects(settings: FilterSettings, clipId: String): List<Effect> {
        val eff = if (settings.selectedPreset != FilterPreset.NONE && settings.isDefault) {
            settings.selectedPreset.toFilterSettings()
        } else {
            settings
        }

        if (eff.isDefault && eff.selectedPreset == FilterPreset.NONE) {
            return emptyList()
        }

        val effects = mutableListOf<Effect>()
        var order = 0

        val presetParam = if (eff.selectedPreset != FilterPreset.NONE) {
            mapOf("preset" to eff.selectedPreset.ordinal.toFloat())
        } else {
            emptyMap()
        }

        if (eff.brightness != 0f) {
            effects.add(
                Effect(
                    id = UUID.randomUUID().toString(),
                    clipId = clipId,
                    type = EffectType.BRIGHTNESS,
                    order = order++,
                    isEnabled = true,
                    parameters = mapOf("value" to eff.brightness, "brightness" to eff.brightness) + presetParam
                )
            )
        }

        if (eff.contrast != 1f) {
            effects.add(
                Effect(
                    id = UUID.randomUUID().toString(),
                    clipId = clipId,
                    type = EffectType.CONTRAST,
                    order = order++,
                    isEnabled = true,
                    parameters = mapOf("value" to eff.contrast, "contrast" to eff.contrast) + presetParam
                )
            )
        }

        if (eff.saturation != 1f) {
            effects.add(
                Effect(
                    id = UUID.randomUUID().toString(),
                    clipId = clipId,
                    type = EffectType.SATURATION,
                    order = order++,
                    isEnabled = true,
                    parameters = mapOf("value" to eff.saturation, "saturation" to eff.saturation) + presetParam
                )
            )
        }

        if (eff.exposure != 0f) {
            effects.add(
                Effect(
                    id = UUID.randomUUID().toString(),
                    clipId = clipId,
                    type = EffectType.EXPOSURE,
                    order = order++,
                    isEnabled = true,
                    parameters = mapOf("value" to eff.exposure, "exposure" to eff.exposure) + presetParam
                )
            )
        }

        if (eff.vignette != 0f) {
            effects.add(
                Effect(
                    id = UUID.randomUUID().toString(),
                    clipId = clipId,
                    type = EffectType.VIGNETTE,
                    order = order++,
                    isEnabled = true,
                    parameters = mapOf("value" to eff.vignette, "vignette" to eff.vignette)
                )
            )
        }

        if (eff.blur != 0f) {
            effects.add(
                Effect(
                    id = UUID.randomUUID().toString(),
                    clipId = clipId,
                    type = EffectType.BLUR,
                    order = order++,
                    isEnabled = true,
                    parameters = mapOf("value" to eff.blur, "blur" to eff.blur)
                )
            )
        }

        if (eff.sharpness != 0f) {
            effects.add(
                Effect(
                    id = UUID.randomUUID().toString(),
                    clipId = clipId,
                    type = EffectType.SHARPEN,
                    order = order++,
                    isEnabled = true,
                    parameters = mapOf("value" to eff.sharpness, "sharpness" to eff.sharpness)
                )
            )
        }

        // Store additional color tuning parameters (temperature, tint, hue, balances, fade, etc.)
        val additionalParams = mutableMapOf<String, Float>()
        if (eff.temperature != 0f) additionalParams["temperature"] = eff.temperature
        if (eff.tint != 0f) additionalParams["tint"] = eff.tint
        if (eff.hue != 0f) additionalParams["hue"] = eff.hue
        if (eff.redBalance != 0f) additionalParams["redBalance"] = eff.redBalance
        if (eff.greenBalance != 0f) additionalParams["greenBalance"] = eff.greenBalance
        if (eff.blueBalance != 0f) additionalParams["blueBalance"] = eff.blueBalance
        if (eff.fade != 0f) additionalParams["fade"] = eff.fade
        if (eff.glow != 0f) additionalParams["glow"] = eff.glow
        if (eff.opacity != 100f) additionalParams["opacity"] = eff.opacity
        if (eff.selectedPreset != FilterPreset.NONE) additionalParams["preset"] = eff.selectedPreset.ordinal.toFloat()

        if (additionalParams.isNotEmpty()) {
            if (effects.isEmpty()) {
                effects.add(
                    Effect(
                        id = UUID.randomUUID().toString(),
                        clipId = clipId,
                        type = EffectType.BRIGHTNESS,
                        order = order++,
                        isEnabled = true,
                        parameters = additionalParams + mapOf("value" to 0f)
                    )
                )
            } else {
                val first = effects[0]
                effects[0] = first.copy(parameters = first.parameters + additionalParams)
            }
        }

        return effects
    }

    /**
     * Reconstructs [FilterSettings] from a list of domain [Effect] models on a clip.
     */
    fun fromEffects(effects: List<Effect>): FilterSettings {
        if (effects.isEmpty()) return FilterSettings()

        var brightness = 0f
        var contrast = 1f
        var saturation = 1f
        var exposure = 0f
        var vignette = 0f
        var blur = 0f
        var sharpness = 0f
        var temperature = 0f
        var tint = 0f
        var hue = 0f
        var redBalance = 0f
        var greenBalance = 0f
        var blueBalance = 0f
        var fade = 0f
        var glow = 0f
        var opacity = 100f
        var presetOrdinal: Int? = null

        for (effect in effects) {
            if (!effect.isEnabled) continue
            val p = effect.parameters
            val v = p["value"] ?: 0f

            when (effect.type) {
                EffectType.BRIGHTNESS -> brightness = p["brightness"] ?: v
                EffectType.CONTRAST -> contrast = p["contrast"] ?: v
                EffectType.SATURATION -> saturation = p["saturation"] ?: v
                EffectType.EXPOSURE -> exposure = p["exposure"] ?: v
                EffectType.VIGNETTE -> vignette = p["vignette"] ?: v
                EffectType.BLUR -> blur = p["blur"] ?: v
                EffectType.SHARPEN -> sharpness = p["sharpness"] ?: v
                else -> {}
            }

            p["temperature"]?.let { temperature = it }
            p["tint"]?.let { tint = it }
            p["hue"]?.let { hue = it }
            p["redBalance"]?.let { redBalance = it }
            p["greenBalance"]?.let { greenBalance = it }
            p["blueBalance"]?.let { blueBalance = it }
            p["fade"]?.let { fade = it }
            p["glow"]?.let { glow = it }
            p["opacity"]?.let { opacity = it }
            p["preset"]?.let { presetOrdinal = it.toInt() }
        }

        val preset = presetOrdinal?.let { ord ->
            FilterPreset.entries.getOrNull(ord)
        } ?: FilterPreset.NONE

        return FilterSettings(
            brightness = brightness,
            contrast = contrast,
            exposure = exposure,
            saturation = saturation,
            vignette = vignette,
            blur = blur,
            sharpness = sharpness,
            temperature = temperature,
            tint = tint,
            hue = hue,
            redBalance = redBalance,
            greenBalance = greenBalance,
            blueBalance = blueBalance,
            fade = fade,
            glow = glow,
            opacity = opacity,
            selectedPreset = preset
        )
    }
}
