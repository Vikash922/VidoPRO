package com.example.core.media.render

import androidx.media3.common.Player
import kotlinx.coroutines.flow.StateFlow

/**
 * Preview renderer boundary.
 *
 * Consumes the universal [RenderScene] and coordinates real-time playback
 * across Media3 ExoPlayer and Compose preview surfaces.
 */
interface PreviewRenderer {
    val currentPositionMs: StateFlow<Long>
    val isPlaying: StateFlow<Boolean>
    val durationMs: StateFlow<Long>
    val isBuffering: StateFlow<Boolean>

    val player: Player

    /**
     * Updates the active scene to be rendered.
     */
    fun updateScene(scene: RenderScene)

    /**
     * Retrieves the dedicated overlay player for a PiP video clip, if active.
     */
    fun getOverlayPlayer(clipId: String): Player? = null

    fun play()
    fun pause()
    fun seekTo(timelinePositionMs: Long)
    fun setPlaybackSpeed(speed: Float)
    fun setVolume(volume: Float)
    fun release()
}
