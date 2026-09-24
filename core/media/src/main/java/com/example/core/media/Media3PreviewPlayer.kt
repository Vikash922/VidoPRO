package com.example.core.media

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.SilenceMediaSource
import com.example.core.model.Asset
import com.example.core.model.Clip
import kotlin.math.abs
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
 *
 * Optimisations (media3 1.5.1):
 *  - EXTENSION_RENDERER_MODE_PREFER → hardware codec preference for 4K.
 *  - enableDecoderFallback = true   → graceful fallback if hardware H.265/AV1 unavailable.
 *  - LoadControl tuned for local file editing: low min-buffer, fast seek.
 *  - setClips() preserves playhead after clip-list change (fixes split reset to 0).
 *  - Ticker at 16ms for 60fps smooth playhead updates.
 */
class Media3PreviewPlayer(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job())
) : PreviewPlayerController {

    private val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context.applicationContext)
        .setEnableDecoderFallback(true)
        // Prefer hardware-accelerated codec extensions over platform codecs
        .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

    // Tuned for local file editing: small buffers = fast seek, low latency
    private val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            /* minBufferMs                      */ 5_000,
            /* maxBufferMs                      */ 30_000,
            /* bufferForPlaybackMs              */ 500,
            /* bufferForPlaybackAfterRebufferMs */ 1_000
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()


    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context.applicationContext)
        .setRenderersFactory(renderersFactory)
        .setLoadControl(loadControl)
        .build()

    private val audioPlayers = mutableListOf<ExoPlayer>()

    override val player: Player get() = exoPlayer

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
    private var audioLayersSegments: List<List<AudioTimelineSegment>> = emptyList()
    private val mediaSourceFactory = DefaultMediaSourceFactory(context.applicationContext)
    private var currentPlaybackSpeed: Float = 1.0f
    private var currentVolume: Float = 1.0f
    private var assetsMap: Map<String, Asset> = emptyMap()
    private var tickerJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
            if (playing) {
                startPositionTicker()
                for (i in audioPlayers.indices) {
                    val p = audioPlayers[i]
                    if (p.playbackState == Player.STATE_ENDED) {
                        seekAudioPlayerTo(i, _currentPositionMs.value)
                    }
                    if (!p.isPlaying) p.play()
                }
            } else {
                stopPositionTicker()
                updatePositionFromPlayer()
                for (p in audioPlayers) {
                    if (p.isPlaying) p.pause()
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _isBuffering.value = (playbackState == Player.STATE_BUFFERING)
            if (playbackState == Player.STATE_BUFFERING) {
                for (p in audioPlayers) {
                    if (p.isPlaying) p.pause()
                }
            } else if (playbackState == Player.STATE_READY && exoPlayer.isPlaying) {
                for (p in audioPlayers) {
                    if (!p.isPlaying) p.play()
                }
            } else if (playbackState == Player.STATE_ENDED) {
                _isPlaying.value = false
                stopPositionTicker()
                _currentPositionMs.value = _durationMs.value
                for (p in audioPlayers) {
                    p.pause()
                }
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
            android.util.Log.e("Media3Player", "Error ${error.errorCodeName}: ${error.message}", error)
            _isPlaying.value = false
            _isBuffering.value = false
            stopPositionTicker()
            for (p in audioPlayers) {
                p.pause()
            }
        }
    }

    init {
        exoPlayer.addListener(playerListener)
    }

    override fun setClips(clips: List<Clip>, assets: Map<String, Asset>) {
        val sorted = clips.sortedBy { it.startTimeMs }
        assetsMap = assets

        val wasPlaying = exoPlayer.isPlaying
        // Save current timeline position so split/trim doesn't reset playhead
        val savedPositionMs = _currentPositionMs.value

        clipsList = sorted
        val totalDuration = sorted.maxOfOrNull { it.endTimeMs } ?: 0L
        _durationMs.value = totalDuration

        // Check if underlying media sources actually changed (URIs, clip boundaries, speed, duration).
        // If only visual properties (transform, effects, keyframes) changed, avoid re-preparing ExoPlayer.
        val sourcesUnchanged = clipsList.size == sorted.size && clipsList.zip(sorted).all { (old, new) ->
            old.id == new.id &&
            old.assetId == new.assetId &&
            old.inPointMs == new.inPointMs &&
            old.outPointMs == new.outPointMs &&
            old.speed == new.speed &&
            old.startTimeMs == new.startTimeMs &&
            old.durationMs == new.durationMs
        } && assets == assetsMap

        if (sourcesUnchanged && exoPlayer.mediaItemCount == sorted.size) {
            clipsList = sorted
            return
        }

        val mediaItems = sorted.mapNotNull { clip -> buildMediaItem(clip, assets) }

        exoPlayer.setMediaItems(mediaItems)
        exoPlayer.prepare()

        // Re-map audio segments if audio clips were previously set
        if (audioClipsList.isNotEmpty()) {
            setAudioClips(audioClipsList, assetsMap)
        }

        // Restore playhead to saved position (fixes: split causes reset to 0)
        if (savedPositionMs > 0L && savedPositionMs < totalDuration) {
            seekTo(savedPositionMs)
        }
        if (wasPlaying) {
            exoPlayer.play()
        }
    }

    override fun setAudioClips(clips: List<Clip>, assets: Map<String, Asset>) {
        audioClipsList = clips
        assetsMap = assets

        val effectiveDuration = _durationMs.value.coerceAtLeast(clips.maxOfOrNull { it.endTimeMs } ?: 0L)
        val layers = AudioTimelineMapper.mapClipsToLayers(clips, assets, effectiveDuration)
            .filter { layer -> layer.any { it is AudioTimelineSegment.ClipSegment } }
        audioLayersSegments = layers

        if (layers.isEmpty()) {
            for (player in audioPlayers) {
                player.clearMediaItems()
                player.release()
            }
            audioPlayers.clear()
            return
        }

        // Adjust player count to match layer count
        while (audioPlayers.size < layers.size) {
            val player = ExoPlayer.Builder(context.applicationContext).build()
            audioPlayers.add(player)
        }
        while (audioPlayers.size > layers.size) {
            val removed = audioPlayers.removeAt(audioPlayers.lastIndex)
            removed.clearMediaItems()
            removed.release()
        }

        for (i in layers.indices) {
            val segments = layers[i]
            val player = audioPlayers[i]
            val mediaSources = segments.map { segment ->
                when (segment) {
                    is AudioTimelineSegment.GapSegment -> {
                        SilenceMediaSource(segment.durationMs * 1000L)
                    }
                    is AudioTimelineSegment.ClipSegment -> {
                        val builder = MediaItem.Builder()
                            .setUri(Uri.parse(segment.asset.uri))
                            .setMediaId(segment.clip.id)
                        val isTrimmedStart = segment.sourceInPointMs > 0L
                        val assetDuration = segment.asset.durationMs ?: 0L
                        val isTrimmedEnd = segment.sourceOutPointMs > 0L &&
                                (assetDuration <= 0L || segment.sourceOutPointMs < assetDuration)
                        if (isTrimmedStart || isTrimmedEnd) {
                            builder.setClippingConfiguration(
                                MediaItem.ClippingConfiguration.Builder().apply {
                                    if (isTrimmedStart) setStartPositionMs(segment.sourceInPointMs)
                                    if (isTrimmedEnd) setEndPositionMs(segment.sourceOutPointMs)
                                }.build()
                            )
                        }
                        mediaSourceFactory.createMediaSource(builder.build())
                    }
                }
            }
            player.setMediaSources(mediaSources)
            player.prepare()
        }

        seekAudioTo(_currentPositionMs.value)
        if (exoPlayer.isPlaying) {
            for (player in audioPlayers) player.play()
        }
    }

    override fun play() {
        if (exoPlayer.playbackState == Player.STATE_ENDED) seekTo(0L)
        if (exoPlayer.playbackState == Player.STATE_IDLE) exoPlayer.prepare()
        exoPlayer.play()

        for (i in audioPlayers.indices) {
            val player = audioPlayers[i]
            if (player.playbackState == Player.STATE_ENDED) seekAudioPlayerTo(i, 0L)
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
        }
    }

    override fun pause() {
        exoPlayer.pause()
        for (player in audioPlayers) player.pause()
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
                exoPlayer.seekTo(targetIndex, clamped - clip.startTimeMs)
            } else if (clamped <= (clipsList.firstOrNull()?.startTimeMs ?: 0L)) {
                exoPlayer.seekTo(0, 0L)
            } else {
                val last = clipsList.lastIndex
                exoPlayer.seekTo(last, clipsList[last].durationMs)
            }
        }

        seekAudioTo(clamped)
    }

    private fun seekAudioTo(timelinePositionMs: Long) {
        for (i in audioPlayers.indices) {
            seekAudioPlayerTo(i, timelinePositionMs)
        }
    }

    private fun seekAudioPlayerTo(layerIndex: Int, timelinePositionMs: Long) {
        if (layerIndex !in audioLayersSegments.indices || layerIndex !in audioPlayers.indices) return
        val segments = audioLayersSegments[layerIndex]
        val player = audioPlayers[layerIndex]
        val clamped = timelinePositionMs.coerceIn(0L, _durationMs.value.coerceAtLeast(0L))

        val idx = segments.indexOfFirst { clamped >= it.startTimeMs && clamped < it.endTimeMs }
        if (idx != -1) {
            val segment = segments[idx]
            val offsetMs = clamped - segment.startTimeMs
            player.seekTo(idx, offsetMs)
            applySegmentParameters(player, segment)
        } else if (clamped <= 0L) {
            player.seekTo(0, 0L)
            if (segments.isNotEmpty()) applySegmentParameters(player, segments[0])
        } else {
            val lastIdx = segments.lastIndex
            player.seekTo(lastIdx, segments[lastIdx].durationMs)
            applySegmentParameters(player, segments[lastIdx])
        }
    }

    private fun applySegmentParameters(player: ExoPlayer, segment: AudioTimelineSegment) {
        if (segment is AudioTimelineSegment.ClipSegment) {
            val effectiveSpeed = (segment.speed * currentPlaybackSpeed).coerceIn(0.1f, 4.0f)
            player.playbackParameters = PlaybackParameters(effectiveSpeed)
            player.volume = (segment.volume * currentVolume).coerceIn(0f, 1f)
        } else {
            player.playbackParameters = PlaybackParameters(currentPlaybackSpeed)
            player.volume = currentVolume
        }
    }

    override fun setPlaybackSpeed(speed: Float) {
        val s = speed.coerceIn(0.1f, 4.0f)
        currentPlaybackSpeed = s
        exoPlayer.playbackParameters = PlaybackParameters(s)
        for (i in audioPlayers.indices) {
            val player = audioPlayers[i]
            val curIdx = player.currentMediaItemIndex
            val segments = audioLayersSegments.getOrNull(i)
            if (segments != null && curIdx in segments.indices) {
                applySegmentParameters(player, segments[curIdx])
            } else {
                player.playbackParameters = PlaybackParameters(s)
            }
        }
    }

    override fun setVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        currentVolume = v
        exoPlayer.volume = v
        for (i in audioPlayers.indices) {
            val player = audioPlayers[i]
            val curIdx = player.currentMediaItemIndex
            val segments = audioLayersSegments.getOrNull(i)
            if (segments != null && curIdx in segments.indices) {
                applySegmentParameters(player, segments[curIdx])
            } else {
                player.volume = v
            }
        }
    }

    override fun release() {
        stopPositionTicker()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
        for (player in audioPlayers) {
            player.release()
        }
        audioPlayers.clear()
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private fun buildMediaItem(clip: Clip, assets: Map<String, Asset>): MediaItem? {
        val asset = assets[clip.assetId] ?: return null
        val isImage = asset.mimeType?.startsWith("image") == true ||
                clip.type == com.example.core.model.ClipType.IMAGE
        val builder = MediaItem.Builder()
            .setUri(Uri.parse(asset.uri))
            .setMediaId(clip.id)
        applyClipping(builder, clip, asset)
        if (isImage) builder.setImageDurationMs(clip.durationMs)
        return builder.build()
    }

    private fun applyClipping(builder: MediaItem.Builder, clip: Clip, asset: Asset) {
        val isTrimmedStart = clip.inPointMs > 0L
        val assetDuration = asset.durationMs ?: 0L
        val isTrimmedEnd = clip.outPointMs > 0L &&
                (assetDuration <= 0L || clip.outPointMs < assetDuration)
        if (isTrimmedStart || isTrimmedEnd) {
            builder.setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder().apply {
                    if (isTrimmedStart) setStartPositionMs(clip.inPointMs)
                    if (isTrimmedEnd) setEndPositionMs(clip.outPointMs)
                }.build()
            )
        }
    }

    /** Ticker runs at 16ms (~60fps) for smooth playhead animation. */
    private fun startPositionTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                updatePositionFromPlayer()
                delay(16L) // 60fps
            }
        }
    }

    private fun stopPositionTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun updatePositionFromPlayer() {
        if (clipsList.isEmpty()) return
        val idx = exoPlayer.currentMediaItemIndex
        if (idx in clipsList.indices) {
            val timelinePos = clipsList[idx].startTimeMs + exoPlayer.currentPosition
            _currentPositionMs.value = timelinePos.coerceIn(0L, _durationMs.value)

            // Audio synchronization & drift correction during playback
            if (exoPlayer.isPlaying) {
                for (i in audioPlayers.indices) {
                    val player = audioPlayers[i]
                    val segments = audioLayersSegments.getOrNull(i) ?: continue
                    val audioIdx = player.currentMediaItemIndex
                    if (audioIdx in segments.indices) {
                        val currentSegment = segments[audioIdx]
                        val audioTimelinePos = currentSegment.startTimeMs + player.currentPosition
                        if (abs(audioTimelinePos - timelinePos) > 100L) {
                            seekAudioPlayerTo(i, timelinePos)
                        }
                    }
                }
            }
        }
    }
}
