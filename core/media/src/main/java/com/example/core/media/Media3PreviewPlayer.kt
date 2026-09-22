package com.example.core.media

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.core.model.Asset
import com.example.core.model.Clip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Media3 ExoPlayer implementation of PreviewPlayerController.
 * Conforms to DEV-051, DEV-052.
 */
class Media3PreviewPlayer(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job())
) : PreviewPlayerController {

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build()
    private val audioPlayer: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build()

    override val player: Player
        get() = exoPlayer

    private val _currentPositionMs = MutableStateFlow(0L)
    override val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    override val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    override val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private var clipsList: List<Clip> = emptyList()
    private var audioClipsList: List<Clip> = emptyList()
    private var tickerJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
            if (playing) {
                startPositionTicker()
            } else {
                stopPositionTicker()
                updatePositionFromPlayer()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _isBuffering.value = (playbackState == Player.STATE_BUFFERING)
            if (playbackState == Player.STATE_ENDED) {
                _isPlaying.value = false
                stopPositionTicker()
                _currentPositionMs.value = _durationMs.value
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            updatePositionFromPlayer()
        }

        override fun onPlayerError(error: PlaybackException) {
            android.util.Log.e("Media3PreviewPlayer", "ExoPlayer Error: ${error.errorCodeName} (${error.errorCode}): ${error.message}", error)
            _isPlaying.value = false
            _isBuffering.value = false
            stopPositionTicker()
        }
    }

    init {
        exoPlayer.addListener(playerListener)
    }

    override fun setClips(clips: List<Clip>, assets: Map<String, Asset>) {
        val sorted = clips.sortedBy { it.startTimeMs }
        clipsList = sorted

        val totalDuration = sorted.maxOfOrNull { it.endTimeMs } ?: 0L
        _durationMs.value = totalDuration

        val mediaItems = sorted.mapNotNull { clip ->
            val asset = assets[clip.assetId] ?: return@mapNotNull null

            val isImage = asset.mimeType?.startsWith("image") == true || clip.type == com.example.core.model.ClipType.IMAGE

            val builder = MediaItem.Builder()
                .setUri(Uri.parse(asset.uri))
                .setMediaId(clip.id)

            // ONLY apply clipping configuration if the clip is actually trimmed.
            // Avoid setting endPositionMs if not trimmed, preventing IllegalClippingException.
            val isTrimmedStart = clip.inPointMs > 0L
            val assetDuration = asset.durationMs ?: 0L
            val isTrimmedEnd = clip.outPointMs > 0L && (assetDuration > 0L && clip.outPointMs < assetDuration)
            if (isTrimmedStart || isTrimmedEnd) {
                val clippingConfig = MediaItem.ClippingConfiguration.Builder().apply {
                    if (isTrimmedStart) {
                        setStartPositionMs(clip.inPointMs)
                    }
                    if (isTrimmedEnd) {
                        setEndPositionMs(clip.outPointMs)
                    }
                }.build()
                builder.setClippingConfiguration(clippingConfig)
            }

            if (isImage) {
                builder.setImageDurationMs(clip.durationMs)
            }

            builder.build()
        }

        exoPlayer.setMediaItems(mediaItems)
        exoPlayer.prepare()
        updatePositionFromPlayer()
    }

    override fun setAudioClips(clips: List<Clip>, assets: Map<String, Asset>) {
        val sorted = clips.sortedBy { it.startTimeMs }
        audioClipsList = sorted

        val mediaItems = sorted.mapNotNull { clip ->
            val asset = assets[clip.assetId] ?: return@mapNotNull null
            val builder = MediaItem.Builder()
                .setUri(Uri.parse(asset.uri))
                .setMediaId(clip.id)

            val isTrimmedStart = clip.inPointMs > 0L
            val assetDuration = asset.durationMs ?: 0L
            val isTrimmedEnd = clip.outPointMs > 0L && (assetDuration > 0L && clip.outPointMs < assetDuration)
            if (isTrimmedStart || isTrimmedEnd) {
                val clippingConfig = MediaItem.ClippingConfiguration.Builder().apply {
                    if (isTrimmedStart) {
                        setStartPositionMs(clip.inPointMs)
                    }
                    if (isTrimmedEnd) {
                        setEndPositionMs(clip.outPointMs)
                    }
                }.build()
                builder.setClippingConfiguration(clippingConfig)
            }
            builder.build()
        }

        audioPlayer.setMediaItems(mediaItems)
        audioPlayer.prepare()
    }

    override fun play() {
        if (exoPlayer.playbackState == Player.STATE_ENDED) {
            seekTo(0L)
        }
        if (exoPlayer.playbackState == Player.STATE_IDLE) {
            exoPlayer.prepare()
        }
        exoPlayer.play()

        if (audioClipsList.isNotEmpty()) {
            if (audioPlayer.playbackState == Player.STATE_ENDED) {
                audioPlayer.seekTo(0L)
            }
            if (audioPlayer.playbackState == Player.STATE_IDLE) {
                audioPlayer.prepare()
            }
            audioPlayer.play()
        }
    }

    override fun pause() {
        exoPlayer.pause()
        if (audioClipsList.isNotEmpty()) {
            audioPlayer.pause()
        }
    }

    override fun seekTo(timelinePositionMs: Long) {
        val clamped = timelinePositionMs.coerceIn(0L, _durationMs.value.coerceAtLeast(0L))
        _currentPositionMs.value = clamped

        if (clipsList.isEmpty()) {
            exoPlayer.seekTo(0L)
        } else {
            val targetIndex = clipsList.indexOfFirst { clamped >= it.startTimeMs && clamped < it.endTimeMs }
            if (targetIndex != -1) {
                val clip = clipsList[targetIndex]
                val relativeOffset = clamped - clip.startTimeMs
                exoPlayer.seekTo(targetIndex, relativeOffset)
            } else {
                if (clamped <= (clipsList.firstOrNull()?.startTimeMs ?: 0L)) {
                    exoPlayer.seekTo(0, 0L)
                } else {
                    val lastIdx = clipsList.lastIndex
                    val lastClip = clipsList[lastIdx]
                    exoPlayer.seekTo(lastIdx, lastClip.durationMs)
                }
            }
        }

        if (audioClipsList.isNotEmpty()) {
            val audioIndex = audioClipsList.indexOfFirst { clamped >= it.startTimeMs && clamped < it.endTimeMs }
            if (audioIndex != -1) {
                val audioClip = audioClipsList[audioIndex]
                val offset = clamped - audioClip.startTimeMs
                audioPlayer.seekTo(audioIndex, offset)
            } else {
                audioPlayer.pause()
            }
        }
    }

    override fun setPlaybackSpeed(speed: Float) {
        val safeSpeed = speed.coerceIn(0.1f, 4.0f)
        exoPlayer.playbackParameters = PlaybackParameters(safeSpeed, exoPlayer.playbackParameters.pitch)
        if (audioClipsList.isNotEmpty()) {
            audioPlayer.playbackParameters = PlaybackParameters(safeSpeed, audioPlayer.playbackParameters.pitch)
        }
    }

    override fun setVolume(volume: Float) {
        exoPlayer.volume = volume.coerceIn(0f, 1f)
        if (audioClipsList.isNotEmpty()) {
            audioPlayer.volume = volume.coerceIn(0f, 1f)
        }
    }

    override fun release() {
        stopPositionTicker()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
        audioPlayer.release()
    }

    private fun startPositionTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                updatePositionFromPlayer()
                delay(33L) // ~30 fps updates
            }
        }
    }

    private fun stopPositionTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun updatePositionFromPlayer() {
        if (clipsList.isEmpty()) return
        val currentWindow = exoPlayer.currentMediaItemIndex
        if (currentWindow in clipsList.indices) {
            val currentClip = clipsList[currentWindow]
            val posInClip = exoPlayer.currentPosition
            val timelinePos = currentClip.startTimeMs + posInClip
            _currentPositionMs.value = timelinePos.coerceIn(0L, _durationMs.value)
        }
    }
}
