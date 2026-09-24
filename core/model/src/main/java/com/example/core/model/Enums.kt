package com.example.core.model

enum class TrackType {
    VIDEO,
    OVERLAY,
    TEXT,
    AUDIO
}

enum class ClipType {
    VIDEO,
    IMAGE,
    AUDIO,
    TEXT,
    COLOR,
    SHAPE
}

enum class MediaType {
    VIDEO,
    IMAGE,
    AUDIO
}

enum class AspectRatio(val widthRatio: Int, val heightRatio: Int, val label: String) {
    RATIO_9_16(9, 16, "9:16"),
    RATIO_16_9(16, 9, "16:9"),
    RATIO_1_1(1, 1, "1:1"),
    RATIO_4_5(4, 5, "4:5");

    val floatRatio: Float
        get() = widthRatio.toFloat() / heightRatio.toFloat()
}

enum class EffectType {
    BRIGHTNESS,
    CONTRAST,
    SATURATION,
    EXPOSURE,
    TEMPERATURE,
    TINT,
    HIGHLIGHTS,
    SHADOWS,
    BLUR,
    VIGNETTE,
    SHARPEN,
    OPACITY,
    MASK,
    BLEND_MODE,
    GLITCH,
    RGB_SPLIT,
    PIXELATE
}

enum class MaskShape(val label: String) {
    RECTANGLE("Rectangle"),
    CIRCLE("Circle"),
    LINEAR_GRADIENT("Linear"),
    RADIAL_GRADIENT("Radial")
}

enum class BlendMode(val label: String) {
    NORMAL("Normal"),
    MULTIPLY("Multiply"),
    SCREEN("Screen"),
    OVERLAY("Overlay"),
    DARKEN("Darken"),
    LIGHTEN("Lighten"),
    ADD("Add")
}

enum class InterpolationType {
    LINEAR,
    HOLD,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    CUBIC_EASE_IN,
    CUBIC_EASE_OUT,
    CUBIC_EASE_IN_OUT,
    SMOOTH,
    BEZIER
}

enum class TransitionType {
    NONE,
    FADE,
    SLIDE,
    ZOOM,
    WIPE,
    BLUR,
    SPIN,
    GLITCH
}

enum class OutputFormat {
    MP4
}

enum class VideoCodec {
    H264,
    H265
}

enum class AudioCodec {
    AAC
}
