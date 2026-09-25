package com.example.core.media

import androidx.media3.common.Player
import com.example.core.model.Asset
import com.example.core.model.Clip
import kotlinx.coroutines.flow.StateFlow

/**
 * Controller interface for preview player operations.
 * Defined according to DEV-050.
 */
interface PreviewPlayerController {
    val currentPositionMs: StateFlow<Long>
    val isPlaying: StateFlow<Boolean>
    val durationMs: StateFlow<Long>
    val isBuffering: StateFlow<Boolean>

    val player: Player

    fun setClips(clips: List<Clip>, assets: Map<String, Asset>)
    fun setAudioClips(clips: List<Clip>, assets: Map<String, Asset>) {}
    fun setOverlayClips(clips: List<Clip>, assets: Map<String, Asset>) {}
    fun getOverlayPlayer(clipId: String): Player? = null
    fun play()
    fun pause()
    fun seekTo(timelinePositionMs: Long)
    fun setPlaybackSpeed(speed: Float)
    /**
     * Caps the decoded preview size without changing the project or export settings.
     * Keeping a 4K source at 720p/1080p while editing is substantially cheaper on
     * devices whose hardware decoder cannot sustain 4K60.
     */
    fun setPreviewQuality(quality: PreviewQuality) {}
    fun setVolume(volume: Float)
    fun release()
}

/** Decode cap used by the editor preview. Export always retains source quality. */
enum class PreviewQuality(val label: String, val maxWidth: Int, val maxHeight: Int) {
    AUTO("Auto", Int.MAX_VALUE, Int.MAX_VALUE),
    P1080("1080p", 1920, 1080),
    P720("720p", 1280, 720),
    P540("540p", 960, 540)
}
