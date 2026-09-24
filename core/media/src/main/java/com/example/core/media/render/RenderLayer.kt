package com.example.core.media.render

import com.example.core.model.BlendMode
import com.example.core.model.ClipMask
import com.example.core.model.Effect
import com.example.core.model.EffectStack
import com.example.core.model.Keyframe
import com.example.core.model.TextClipData
import com.example.core.model.Transform

/**
 * Common base abstraction for all visual compositing layers in VidoPRO.
 *
 * Each layer describes WHAT should be rendered at any project timestamp:
 * - Timeline boundaries (startTimeMs .. endTimeMs)
 * - Layer Z-order (stacking precedence)
 * - Geometric and optical transform (position, scale, rotation, opacity, keyframes)
 * - Color and filter effects stack
 * - Optional clip mask (shape, feather, opacity)
 * - Composite blend mode
 */
sealed interface RenderLayer {
    val id: String
    val timelineStartMs: Long
    val timelineEndMs: Long
    val zIndex: Int
    val isVisible: Boolean
    val transform: Transform
    val keyframes: List<Keyframe>
    val effects: List<Effect>
    val mask: ClipMask?
        get() = null
    val blendMode: BlendMode
        get() = BlendMode.NORMAL

    val effectStack: EffectStack
        get() = EffectStack(effects)

    val durationMs: Long
        get() = (timelineEndMs - timelineStartMs).coerceAtLeast(0L)

    val opacity: Float
        get() = transform.opacity

    fun isActiveAt(projectTimeMs: Long): Boolean =
        isVisible && TimeMapping.isLayerActive(projectTimeMs, timelineStartMs, timelineEndMs)

    fun evaluateTransformAt(projectTimeMs: Long): RenderTransform =
        RenderTransformEvaluator.evaluate(transform, keyframes, projectTimeMs)
}

/**
 * Visual video layer (supports both primary video track and PiP / video overlays).
 */
data class VideoRenderLayer(
    override val id: String,
    override val timelineStartMs: Long,
    override val timelineEndMs: Long,
    override val zIndex: Int,
    override val isVisible: Boolean = true,
    override val transform: Transform = Transform.DEFAULT,
    override val keyframes: List<Keyframe> = emptyList(),
    override val effects: List<Effect> = emptyList(),
    override val mask: ClipMask? = null,
    override val blendMode: BlendMode = BlendMode.NORMAL,
    val assetId: String,
    val sourceUri: String,
    val sourceInPointMs: Long = 0L,
    val sourceOutPointMs: Long = 0L,
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val isMainVideo: Boolean = false,
    val sourceWidth: Int? = null,
    val sourceHeight: Int? = null
) : RenderLayer {

    fun sourceTimeAt(projectTimeMs: Long): Long =
        TimeMapping.projectTimeToSourceTime(projectTimeMs, timelineStartMs, sourceInPointMs, speed)
}

/**
 * Visual image overlay layer.
 */
data class ImageRenderLayer(
    override val id: String,
    override val timelineStartMs: Long,
    override val timelineEndMs: Long,
    override val zIndex: Int,
    override val isVisible: Boolean = true,
    override val transform: Transform = Transform.DEFAULT,
    override val keyframes: List<Keyframe> = emptyList(),
    override val effects: List<Effect> = emptyList(),
    override val mask: ClipMask? = null,
    override val blendMode: BlendMode = BlendMode.NORMAL,
    val assetId: String,
    val sourceUri: String,
    val sourceWidth: Int? = null,
    val sourceHeight: Int? = null
) : RenderLayer

/**
 * Visual text overlay layer.
 */
data class TextRenderLayer(
    override val id: String,
    override val timelineStartMs: Long,
    override val timelineEndMs: Long,
    override val zIndex: Int,
    override val isVisible: Boolean = true,
    override val transform: Transform = Transform.DEFAULT,
    override val keyframes: List<Keyframe> = emptyList(),
    override val effects: List<Effect> = emptyList(),
    override val mask: ClipMask? = null,
    override val blendMode: BlendMode = BlendMode.NORMAL,
    val textData: TextClipData
) : RenderLayer

/**
 * Non-visual audio rendering layer (for background tracks and audible video clips).
 */
data class AudioRenderLayer(
    val id: String,
    val trackId: String,
    val timelineStartMs: Long,
    val timelineEndMs: Long,
    val isVisible: Boolean = true,
    val assetId: String,
    val sourceUri: String,
    val sourceInPointMs: Long = 0L,
    val sourceOutPointMs: Long = 0L,
    val speed: Float = 1.0f,
    val volume: Float = 1.0f
) {
    val durationMs: Long
        get() = (timelineEndMs - timelineStartMs).coerceAtLeast(0L)

    fun isActiveAt(projectTimeMs: Long): Boolean =
        isVisible && TimeMapping.isLayerActive(projectTimeMs, timelineStartMs, timelineEndMs)

    fun sourceTimeAt(projectTimeMs: Long): Long =
        TimeMapping.projectTimeToSourceTime(projectTimeMs, timelineStartMs, sourceInPointMs, speed)
}
