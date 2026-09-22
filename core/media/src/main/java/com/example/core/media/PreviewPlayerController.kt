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
    fun play()
    fun pause()
    fun seekTo(timelinePositionMs: Long)
    fun setPlaybackSpeed(speed: Float)
    fun setVolume(volume: Float)
    fun release()
}
