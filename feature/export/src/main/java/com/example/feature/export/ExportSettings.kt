package com.example.feature.export

import com.example.core.model.AspectRatio
import com.example.core.model.AudioCodec
import com.example.core.model.ExportSettings
import com.example.core.model.OutputFormat
import com.example.core.model.VideoCodec

/**
 * Resolution preset options for export (DEV-068).
 */
enum class ResolutionPreset(
    val label: String,
    val shortLabel: String,
    val standardHeight: Int
) {
    RES_720P("HD (720p)", "720p", 720),
    RES_1080P("Full HD (1080p)", "1080p", 1080),
    RES_4K("Ultra HD (4K)", "4K", 2160);

    fun getDimensions(aspectRatio: AspectRatio): Pair<Int, Int> {
        return when (aspectRatio) {
            AspectRatio.RATIO_9_16 -> {
                // Vertical (portrait)
                val w = (standardHeight * 9) / 16
                // Ensure even dimensions
                val evenW = (w / 2) * 2
                val evenH = (standardHeight / 2) * 2
                Pair(evenW, evenH)
            }
            AspectRatio.RATIO_16_9 -> {
                // Horizontal (landscape)
                val h = (standardHeight * 9) / 16
                val evenW = (standardHeight / 2) * 2
                val evenH = (h / 2) * 2
                Pair(evenW, evenH)
            }
            AspectRatio.RATIO_1_1 -> {
                val even = (standardHeight / 2) * 2
                Pair(even, even)
            }
            AspectRatio.RATIO_4_5 -> {
                val h = standardHeight
                val w = (standardHeight * 4) / 5
                val evenW = (w / 2) * 2
                val evenH = (h / 2) * 2
                Pair(evenW, evenH)
            }
        }
    }
}

/**
 * FPS presets for export (DEV-068).
 */
enum class FpsPreset(val fps: Int, val label: String) {
    FPS_24(24, "24 fps (Cinematic)"),
    FPS_30(30, "30 fps (Standard)"),
    FPS_60(60, "60 fps (Smooth)");

    val shortLabel: String get() = "${fps}fps"
}

/**
 * Video bitrate / quality presets (DEV-068).
 */
enum class QualityPreset(val label: String, val bitrateMultiplier: Float) {
    LOW("Standard", 0.6f),
    MEDIUM("High", 1.0f),
    HIGH("Ultra", 1.5f);

    fun calculateBitrate(resolution: ResolutionPreset): Int {
        val baseBitrate = when (resolution) {
            ResolutionPreset.RES_720P -> 5_000_000
            ResolutionPreset.RES_1080P -> 10_000_000
            ResolutionPreset.RES_4K -> 35_000_000
        }
        return (baseBitrate * bitrateMultiplier).toInt()
    }
}

/**
 * Helper to construct [ExportSettings] domain model from chosen presets.
 */
fun buildExportSettings(
    resolution: ResolutionPreset,
    fps: FpsPreset,
    quality: QualityPreset,
    aspectRatio: AspectRatio
): ExportSettings {
    val (width, height) = resolution.getDimensions(aspectRatio)
    val videoBitrate = quality.calculateBitrate(resolution)

    return ExportSettings(
        width = width,
        height = height,
        fps = fps.fps,
        videoBitrate = videoBitrate,
        audioBitrate = 192_000,
        format = OutputFormat.MP4,
        videoCodec = VideoCodec.H264,
        audioCodec = AudioCodec.AAC
    )
}

/**
 * Calculates estimated file size in Megabytes based on duration and bitrate.
 */
fun estimateFileSizeMb(durationMs: Long, videoBitrate: Int, audioBitrate: Int = 192_000): Float {
    if (durationMs <= 0L) return 0f
    val durationSeconds = durationMs / 1000f
    val totalBits = (videoBitrate + audioBitrate) * durationSeconds
    val totalBytes = totalBits / 8f
    return totalBytes / (1024f * 1024f)
}
