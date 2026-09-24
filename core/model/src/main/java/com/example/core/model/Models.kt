package com.example.core.model

data class Project(
    val id: String,
    val name: String,
    val width: Int = 1080,
    val height: Int = 1920,
    val fps: Int = 30,
    val durationMs: Long = 0L,
    val aspectRatio: AspectRatio = AspectRatio.RATIO_9_16,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val thumbnailPath: String? = null,
    val tracks: List<Track> = emptyList()
)

data class Track(
    val id: String,
    val projectId: String = "p1",
    val type: TrackType,
    val order: Int = 0,
    val name: String = "",
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val clips: List<Clip> = emptyList(),
    val transitions: List<Transition> = emptyList()
)

data class Clip(
    val id: String,
    val trackId: String,
    val type: ClipType,
    val assetId: String? = null,
    val startTimeMs: Long = 0L,
    val durationMs: Long = 0L,
    val inPointMs: Long = 0L,
    val outPointMs: Long = 0L,
    val speed: Float = 1.0f,
    val volume: Float? = 1.0f,
    val isVisible: Boolean = true,
    val zIndex: Int = 0,
    val transform: Transform = Transform.DEFAULT,
    val effects: List<Effect> = emptyList(),
    val keyframes: List<Keyframe> = emptyList(),
    val textData: TextClipData? = null,
    /** Alight-Motion-style clip group. Clips sharing the same non-null groupId
     *  receive bulk edits together (speed, volume, transform, delete, duplicate). */
    val groupId: String? = null,
    val mask: ClipMask? = null,
    val blendMode: BlendMode = BlendMode.NORMAL
) {
    val endTimeMs: Long
        get() = startTimeMs + durationMs

    val effectStack: EffectStack
        get() = EffectStack(effects)
}

data class Asset(
    val id: String,
    val uri: String,
    val mimeType: String? = null,
    val mediaType: MediaType = MediaType.VIDEO,
    val durationMs: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val sizeBytes: Long? = null,
    val displayName: String? = null,
    val thumbnailPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class Transform(
    val x: Float = 0f,
    val y: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotation: Float = 0f,
    val opacity: Float = 1f,
    val anchorX: Float = 0.5f,
    val anchorY: Float = 0.5f
) {
    companion object {
        val DEFAULT = Transform()
    }
}

data class Effect(
    val id: String,
    val clipId: String,
    val type: EffectType,
    val order: Int = 0,
    val isEnabled: Boolean = true,
    val parameters: Map<String, Float> = emptyMap()
)

data class ClipMask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val shape: MaskShape = MaskShape.RECTANGLE,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0.8f,
    val height: Float = 0.8f,
    val feather: Float = 0f,
    val opacity: Float = 1.0f,
    val rotation: Float = 0f,
    val isInverted: Boolean = false
) {
    fun toEffect(clipId: String): Effect {
        return Effect(
            id = id,
            clipId = clipId,
            type = EffectType.MASK,
            order = 998,
            isEnabled = true,
            parameters = mapOf(
                "shape" to shape.ordinal.toFloat(),
                "x" to x,
                "y" to y,
                "width" to width,
                "height" to height,
                "feather" to feather,
                "opacity" to opacity,
                "rotation" to rotation,
                "isInverted" to if (isInverted) 1f else 0f
            )
        )
    }

    companion object {
        fun fromEffect(effect: Effect): ClipMask {
            val p = effect.parameters
            val shapeOrdinal = (p["shape"] ?: 0f).toInt().coerceIn(0, MaskShape.values().size - 1)
            return ClipMask(
                id = effect.id,
                shape = MaskShape.values()[shapeOrdinal],
                x = p["x"] ?: 0f,
                y = p["y"] ?: 0f,
                width = p["width"] ?: 0.8f,
                height = p["height"] ?: 0.8f,
                feather = p["feather"] ?: 0f,
                opacity = p["opacity"] ?: 1.0f,
                rotation = p["rotation"] ?: 0f,
                isInverted = (p["isInverted"] ?: 0f) > 0.5f
            )
        }
    }
}

data class EffectStack(
    val effects: List<Effect> = emptyList()
) {
    val size: Int get() = effects.size
    fun activeEffects(): List<Effect> = effects.filter { it.isEnabled }.sortedBy { it.order }
    fun getEffect(type: EffectType): Effect? = effects.find { it.type == type && it.isEnabled }
    fun get(effectId: String): Effect? = effects.find { it.id == effectId }
    fun withEffect(effect: Effect): EffectStack {
        val existingIndex = effects.indexOfFirst { it.type == effect.type || it.id == effect.id }
        val updated = if (existingIndex >= 0) {
            effects.mapIndexed { idx, e -> if (idx == existingIndex) effect else e }
        } else {
            effects + effect
        }
        return EffectStack(updated.sortedBy { it.order })
    }
    fun add(effect: Effect): EffectStack = withEffect(effect)
    fun update(effect: Effect): EffectStack = withEffect(effect)
    fun withoutEffect(effectId: String): EffectStack =
        EffectStack(effects.filterNot { it.id == effectId })
    fun remove(effectId: String): EffectStack = withoutEffect(effectId)
    fun withoutEffectType(type: EffectType): EffectStack =
        EffectStack(effects.filterNot { it.type == type })
    fun reset(): EffectStack = EffectStack(emptyList())
    fun reorder(fromIndex: Int, toIndex: Int): EffectStack {
        if (fromIndex !in effects.indices || toIndex !in effects.indices) return this
        val mutable = effects.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        return EffectStack(mutable.mapIndexed { idx, e -> e.copy(order = idx) })
    }
}

data class Keyframe(
    val id: String,
    val clipId: String = "",
    val property: String,
    val timeMs: Long,
    val value: Float,
    val interpolation: InterpolationType = InterpolationType.LINEAR,
    val bezierX1: Float? = null,
    val bezierY1: Float? = null,
    val bezierX2: Float? = null,
    val bezierY2: Float? = null
)

object KeyframeProperty {
    const val POSITION_X = "positionX"
    const val POSITION_Y = "positionY"
    const val SCALE_X = "scaleX"
    const val SCALE_Y = "scaleY"
    const val ROTATION = "rotation"
    const val OPACITY = "opacity"
    const val VOLUME = "volume"

    // Keyframeable Effect & Mask properties
    const val BRIGHTNESS = "brightness"
    const val CONTRAST = "contrast"
    const val SATURATION = "saturation"
    const val EXPOSURE = "exposure"
    const val TEMPERATURE = "temperature"
    const val TINT = "tint"
    const val HIGHLIGHTS = "highlights"
    const val SHADOWS = "shadows"
    const val MASK_X = "maskX"
    const val MASK_Y = "maskY"
    const val MASK_FEATHER = "maskFeather"

    val ALL = listOf(
        POSITION_X, POSITION_Y, SCALE_X, SCALE_Y, ROTATION, OPACITY, VOLUME,
        BRIGHTNESS, CONTRAST, SATURATION, EXPOSURE, TEMPERATURE, TINT, HIGHLIGHTS, SHADOWS,
        MASK_X, MASK_Y, MASK_FEATHER
    )
}

data class TextClipData(
    val clipId: String,
    val text: String,
    val fontFamily: String = "Default",
    val fontSize: Float = 24f,
    val textColor: String = "#FFFFFF",
    val backgroundColor: String? = null,
    val alignment: String = "CENTER",
    val letterSpacing: Float = 0f,
    val lineHeight: Float = 1.2f
)

data class Transition(
    val id: String,
    val projectId: String = "p1",
    val trackId: String = "t1",
    val firstClipId: String,
    val secondClipId: String,
    val type: TransitionType = TransitionType.NONE,
    val durationMs: Long = 500L,
    val parametersJson: String? = null,
    val properties: Map<String, Any> = emptyMap()
)

data class ExportSettings(
    val width: Int = 1080,
    val height: Int = 1920,
    val fps: Int = 30,
    val videoBitrate: Int = 10_000_000,
    val audioBitrate: Int = 192_000,
    val format: OutputFormat = OutputFormat.MP4,
    val videoCodec: VideoCodec = VideoCodec.H264,
    val audioCodec: AudioCodec = AudioCodec.AAC
) {
    companion object {
        fun default(): ExportSettings = ExportSettings()
    }
}
