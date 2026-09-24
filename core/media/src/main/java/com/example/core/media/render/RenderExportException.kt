package com.example.core.media.render

/**
 * Structured taxonomy of rendering and export exceptions for VidoPRO.
 *
 * Provides typed, actionable error categories instead of generic failures.
 */
sealed class RenderExportException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    class InvalidTimelineException(message: String) :
        RenderExportException("Invalid timeline: $message")

    class MissingSourceException(val sourceUri: String, message: String = "Source media file could not be found: $sourceUri") :
        RenderExportException(message)

    class UnsupportedCodecException(val codecName: String, message: String = "Unsupported media codec: $codecName") :
        RenderExportException(message)

    class UnsupportedEffectException(val effectType: String, message: String = "Unsupported effect requested: $effectType") :
        RenderExportException(message)

    class OutOfMemoryRenderException(message: String = "Out of memory during frame rendering/compositing", cause: Throwable? = null) :
        RenderExportException(message, cause)

    class CancelledExportException(message: String = "Export was cancelled by user or system") :
        RenderExportException(message)

    class PipelineRenderException(message: String, cause: Throwable? = null) :
        RenderExportException("Pipeline rendering error: $message", cause)
}
