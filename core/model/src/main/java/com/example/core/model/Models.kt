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
    val projectId: String,
    val type: TrackType,
    val order: Int,
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
    val groupId: String? = null
) {
    val endTimeMs: Long
        get() = startTimeMs + durationMs
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

data class Keyframe(
    val id: String,
    val clipId: String,
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

    val ALL = listOf(POSITION_X, POSITION_Y, SCALE_X, SCALE_Y, ROTATION, OPACITY, VOLUME)
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
    val projectId: String,
    val trackId: String,
    val firstClipId: String,
    val secondClipId: String,
    val type: TransitionType = TransitionType.NONE,
    val durationMs: Long = 500L,
    val parametersJson: String? = null
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
