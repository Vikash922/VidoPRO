package com.example.core.data.mappers

import com.example.core.database.entities.AssetEntity
import com.example.core.database.entities.ClipEntity
import com.example.core.database.entities.EffectEntity
import com.example.core.database.entities.KeyframeEntity
import com.example.core.database.entities.TextClipEntity
import com.example.core.database.entities.TransformEntity
import com.example.core.database.entities.TransitionEntity
import com.example.core.database.relations.ClipWithDetails
import com.example.core.model.Asset
import com.example.core.model.Clip
import com.example.core.model.ClipType
import com.example.core.model.Effect
import com.example.core.model.EffectType
import com.example.core.model.InterpolationType
import com.example.core.model.Keyframe
import com.example.core.model.MediaType
import com.example.core.model.TextClipData
import com.example.core.model.Transform
import com.example.core.model.Transition
import com.example.core.model.TransitionType
import org.json.JSONObject

// MARK: - Clip Mappers

fun ClipEntity.toDomain(
    transform: Transform? = null,
    effects: List<Effect> = emptyList(),
    keyframes: List<Keyframe> = emptyList(),
    textData: TextClipData? = null
): Clip {
    val maskEffect = effects.find { it.type == EffectType.MASK }
    val blendEffect = effects.find { it.type == EffectType.BLEND_MODE }
    val mask = maskEffect?.let { ClipMask.fromEffect(it) }
    val blendMode = blendEffect?.let {
        val modeOrdinal = (it.parameters["mode"] ?: 0f).toInt().coerceIn(0, com.example.core.model.BlendMode.values().size - 1)
        com.example.core.model.BlendMode.values()[modeOrdinal]
    } ?: com.example.core.model.BlendMode.NORMAL

    return Clip(
        id = id,
        trackId = trackId,
        type = runCatching { ClipType.valueOf(type) }.getOrDefault(ClipType.VIDEO),
        assetId = assetId,
        startTimeMs = startTimeMs,
        durationMs = durationMs,
        inPointMs = inPointMs,
        outPointMs = outPointMs,
        speed = speed,
        volume = volume,
        isVisible = isVisible,
        zIndex = zIndex,
        transform = transform ?: Transform.DEFAULT,
        effects = effects,
        keyframes = keyframes,
        textData = textData,
        groupId = groupId,
        mask = mask,
        blendMode = blendMode
    )
}

fun ClipWithDetails.toDomain(): Clip {
    return clip.toDomain(
        transform = transform?.toDomain(),
        effects = effects.map { it.toDomain() },
        keyframes = keyframes.map { it.toDomain() },
        textData = textClip?.toDomain()
    )
}

fun Clip.toEntity(): ClipEntity {
    return ClipEntity(
        id = id,
        trackId = trackId,
        assetId = assetId,
        type = type.name,
        startTimeMs = startTimeMs,
        durationMs = durationMs,
        inPointMs = inPointMs,
        outPointMs = outPointMs,
        speed = speed,
        volume = volume,
        isVisible = isVisible,
        zIndex = zIndex,
        groupId = groupId,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

// MARK: - Transform Mappers

fun TransformEntity.toDomain(): Transform {
    return Transform(
        x = x,
        y = y,
        scaleX = scaleX,
        scaleY = scaleY,
        rotation = rotation,
        opacity = opacity,
        anchorX = anchorX,
        anchorY = anchorY
    )
}

fun Transform.toEntity(clipId: String): TransformEntity {
    return TransformEntity(
        clipId = clipId,
        x = x,
        y = y,
        scaleX = scaleX,
        scaleY = scaleY,
        rotation = rotation,
        opacity = opacity,
        anchorX = anchorX,
        anchorY = anchorY
    )
}

// MARK: - Effect Mappers

fun EffectEntity.toDomain(): Effect {
    val paramsMap = mutableMapOf<String, Float>()
    if (parametersJson.isNotBlank()) {
        runCatching {
            val json = JSONObject(parametersJson)
            json.keys().forEach { key ->
                paramsMap[key] = json.optDouble(key, 0.0).toFloat()
            }
        }
    }
    return Effect(
        id = id,
        clipId = clipId,
        type = runCatching { EffectType.valueOf(type) }.getOrDefault(EffectType.BRIGHTNESS),
        order = effectOrder,
        isEnabled = isEnabled,
        parameters = paramsMap
    )
}

fun Effect.toEntity(): EffectEntity {
    val json = JSONObject()
    parameters.forEach { (k, v) -> json.put(k, v.toDouble()) }
    return EffectEntity(
        id = id,
        clipId = clipId,
        type = type.name,
        effectOrder = order,
        isEnabled = isEnabled,
        parametersJson = json.toString()
    )
}

// MARK: - Keyframe Mappers

fun KeyframeEntity.toDomain(): Keyframe {
    return Keyframe(
        id = id,
        clipId = clipId,
        property = property,
        timeMs = timeMs,
        value = value,
        interpolation = runCatching { InterpolationType.valueOf(interpolation) }.getOrDefault(InterpolationType.LINEAR),
        bezierX1 = bezierX1,
        bezierY1 = bezierY1,
        bezierX2 = bezierX2,
        bezierY2 = bezierY2
    )
}

fun Keyframe.toEntity(): KeyframeEntity {
    return KeyframeEntity(
        id = id,
        clipId = clipId,
        property = property,
        timeMs = timeMs,
        value = value,
        interpolation = interpolation.name,
        bezierX1 = bezierX1,
        bezierY1 = bezierY1,
        bezierX2 = bezierX2,
        bezierY2 = bezierY2
    )
}

// MARK: - TextClip Mappers

fun TextClipEntity.toDomain(): TextClipData {
    return TextClipData(
        clipId = clipId,
        text = text,
        fontFamily = fontFamily,
        fontSize = fontSize,
        textColor = textColor,
        backgroundColor = backgroundColor,
        alignment = alignment,
        letterSpacing = letterSpacing,
        lineHeight = lineHeight
    )
}

fun TextClipData.toEntity(): TextClipEntity {
    return TextClipEntity(
        clipId = clipId,
        text = text,
        fontFamily = fontFamily,
        fontSize = fontSize,
        textColor = textColor,
        backgroundColor = backgroundColor,
        alignment = alignment,
        letterSpacing = letterSpacing,
        lineHeight = lineHeight
    )
}

// MARK: - Asset Mappers

fun AssetEntity.toDomain(): Asset {
    return Asset(
        id = id,
        uri = uri,
        mimeType = mimeType,
        mediaType = runCatching { MediaType.valueOf(mediaType) }.getOrDefault(MediaType.VIDEO),
        durationMs = durationMs,
        width = width,
        height = height,
        sizeBytes = sizeBytes,
        displayName = displayName,
        thumbnailPath = thumbnailPath,
        createdAt = createdAt
    )
}

fun Asset.toEntity(): AssetEntity {
    return AssetEntity(
        id = id,
        uri = uri,
        mimeType = mimeType,
        mediaType = mediaType.name,
        durationMs = durationMs,
        width = width,
        height = height,
        sizeBytes = sizeBytes,
        displayName = displayName,
        thumbnailPath = thumbnailPath,
        createdAt = createdAt
    )
}

// MARK: - Transition Mappers

fun TransitionEntity.toDomain(): Transition {
    return Transition(
        id = id,
        projectId = projectId,
        trackId = trackId,
        firstClipId = firstClipId,
        secondClipId = secondClipId,
        type = runCatching { TransitionType.valueOf(type) }.getOrDefault(TransitionType.NONE),
        durationMs = durationMs,
        parametersJson = parametersJson
    )
}

fun Transition.toEntity(): TransitionEntity {
    return TransitionEntity(
        id = id,
        projectId = projectId,
        trackId = trackId,
        firstClipId = firstClipId,
        secondClipId = secondClipId,
        type = type.name,
        durationMs = durationMs,
        parametersJson = parametersJson
    )
}
