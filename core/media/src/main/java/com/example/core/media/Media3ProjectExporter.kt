package com.example.core.media

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.core.model.Asset
import com.example.core.model.ExportSettings
import com.example.core.model.Project
import com.example.core.model.TrackType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Media3 Transformer implementation of [ProjectExporter].
 * Concatenates and trims video clips, applies resolution and format settings,
 * and exports to an MP4 container (DEV-069).
 */
class Media3ProjectExporter(
    private val context: Context
) : ProjectExporter {

    private var activeTransformer: Transformer? = null
    private var progressJob: Job? = null
    private var activeTextOverlay: TextOverlayGenerator? = null
    private var activeVideoOverlay: VideoOverlayGenerator? = null
    private var activeSilenceFile: File? = null

    override suspend fun export(
        project: Project,
        assets: Map<String, Asset>,
        settings: ExportSettings,
        outputFile: File,
        onProgress: (progressPercent: Int) -> Unit
    ): Result<File> = withContext(Dispatchers.Main) {
        try {
            // 1. Validate tracks and clips
            val videoTrack = project.tracks.firstOrNull { it.type == TrackType.VIDEO }
            val clips = videoTrack?.clips?.filter { it.isVisible }?.sortedBy { it.startTimeMs } ?: emptyList()

            if (clips.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("No video clips available in project to export."))
            }

            // Ensure parent directory exists
            outputFile.parentFile?.mkdirs()
            if (outputFile.exists()) {
                outputFile.delete()
            }

            // 2. Build MediaItems for each trimmed clip
            val editedMediaItems = mutableListOf<EditedMediaItem>()
            for (clip in clips) {
                val asset = assets[clip.assetId] ?: continue
                val uri = Uri.parse(asset.uri)

                val clippingConfig = MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.inPointMs)
                    .setEndPositionMs(clip.outPointMs)
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setUri(uri)
                    .setClippingConfiguration(clippingConfig)
                    .build()

                val isImage = asset.mimeType?.startsWith("image") == true || clip.type == com.example.core.model.ClipType.IMAGE
                val editedItemBuilder = EditedMediaItem.Builder(mediaItem)
                    .setRemoveAudio(false)
                
                if (isImage) {
                    editedItemBuilder.setDurationUs(clip.durationMs * 1000L)
                    editedItemBuilder.setFrameRate(settings.fps)
                }

                // Apply per-clip video effects (brightness, contrast, saturation, exposure, filters)
                val clipVideoEffects = mutableListOf<androidx.media3.common.Effect>()
                clipVideoEffects.addAll(Media3EffectHelper.createMedia3Effects(clip.effects))

                // Apply per-clip transform effect (position, scale, rotation, keyframes)
                val transformEffect = Media3EffectHelper.createTransformEffect(clip, settings.width, settings.height)
                if (transformEffect != null) {
                    clipVideoEffects.add(transformEffect)
                }

                // Apply per-clip opacity effect (dimming toward background canvas)
                if (clip.transform.opacity < 1.0f) {
                    val opacityEffect = Media3EffectHelper.createOpacityEffect(clip.transform.opacity)
                    if (opacityEffect != null) {
                        clipVideoEffects.add(opacityEffect)
                    }
                }

                if (clipVideoEffects.isNotEmpty()) {
                    editedItemBuilder.setEffects(Effects(emptyList(), clipVideoEffects))
                }
                
                val editedItem = editedItemBuilder.build()
                editedMediaItems.add(editedItem)
            }

            if (editedMediaItems.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("Unable to resolve valid media items for export."))
            }

            // 3. Configure Effects (Resolution, Text Overlays, Filters)
            val presentation = Presentation.createForWidthAndHeight(
                settings.width,
                settings.height,
                Presentation.LAYOUT_SCALE_TO_FIT
            )
            
            val videoEffects = mutableListOf<androidx.media3.common.Effect>(presentation)

            // Parse Text Overlays
            val textTracks = project.tracks.filter { it.type == TrackType.TEXT && it.isVisible }
            val textClips = textTracks.flatMap { it.clips }
            if (textClips.isNotEmpty()) {
                try {
                    val textOverlay = TextOverlayGenerator(textClips, settings.width, settings.height)
                    activeTextOverlay = textOverlay
                    val overlayEffect = androidx.media3.effect.OverlayEffect(com.google.common.collect.ImmutableList.of<androidx.media3.effect.TextureOverlay>(textOverlay))
                    videoEffects.add(overlayEffect)
                } catch (e: Exception) {
                    // Fallback if OverlayEffect fails
                }
            }

            // Parse Visual Overlays (PiP - Moving Videos and Images)
            val overlayTracks = project.tracks.filter { it.type == TrackType.OVERLAY && it.isVisible }
            val overlayClips = overlayTracks.flatMap { it.clips }
            if (overlayClips.isNotEmpty()) {
                try {
                    val videoOverlay = VideoOverlayGenerator(context, overlayClips, assets, settings.width, settings.height)
                    activeVideoOverlay = videoOverlay
                    val overlayEffect = androidx.media3.effect.OverlayEffect(com.google.common.collect.ImmutableList.of<androidx.media3.effect.TextureOverlay>(videoOverlay))
                    videoEffects.add(overlayEffect)
                } catch (e: Exception) {
                    // Fallback if OverlayEffect fails
                }
            }

            val effects = Effects(emptyList(), videoEffects)

            val sequences = mutableListOf<EditedMediaItemSequence>()
            sequences.add(EditedMediaItemSequence.Builder(editedMediaItems).build())

            // Include multi-track Audio Sequences with precise timeline gap handling (DEV-069, DEV-070, FIX-06)
            // Includes both audio tracks and audible video overlay clips (PiP with volume > 0)
            val audioTracks = project.tracks.filter { it.type == TrackType.AUDIO && it.isVisible }
            val videoDurationMs = clips.maxOfOrNull { it.endTimeMs } ?: project.durationMs
            val totalTimelineDurationMs = maxOf(videoDurationMs, project.durationMs)

            val audioTrackLayers = audioTracks.flatMap { track ->
                AudioTimelineMapper.partitionIntoNonOverlappingLayers(track.clips)
            }
            val audibleOverlayClips = overlayTracks.flatMap { it.clips }.filter {
                (it.volume ?: 1f) > 0f && (it.type == ClipType.VIDEO || assets[it.assetId]?.mediaType == MediaType.VIDEO)
            }
            val overlayAudioLayers = if (audibleOverlayClips.isNotEmpty()) {
                AudioTimelineMapper.partitionIntoNonOverlappingLayers(audibleOverlayClips)
            } else {
                emptyList()
            }
            val allAudioLayers = audioTrackLayers + overlayAudioLayers

            val mappedTracksSegments = allAudioLayers.mapNotNull { layer ->
                val segments = AudioTimelineMapper.mapClipsToSegments(layer, assets, totalTimelineDurationMs)
                if (segments.any { it is AudioTimelineSegment.ClipSegment }) segments else null
            }

            if (mappedTracksSegments.isNotEmpty()) {
                val maxGapMs = mappedTracksSegments.flatten()
                    .filterIsInstance<AudioTimelineSegment.GapSegment>()
                    .maxOfOrNull { it.durationMs } ?: 0L

                val silenceFile = if (maxGapMs > 0L) {
                    val file = File(context.cacheDir, "export_silence_${System.currentTimeMillis()}.wav")
                    SilentAudioGenerator.createSilenceWavFile(file, maxGapMs)
                    activeSilenceFile = file
                    file
                } else null

                for (trackSegments in mappedTracksSegments) {
                    val audioItems = mutableListOf<EditedMediaItem>()
                    for (segment in trackSegments) {
                        when (segment) {
                            is AudioTimelineSegment.ClipSegment -> {
                                val clippingConfig = MediaItem.ClippingConfiguration.Builder().apply {
                                    if (segment.sourceInPointMs > 0L) setStartPositionMs(segment.sourceInPointMs)
                                    if (segment.sourceOutPointMs > 0L) setEndPositionMs(segment.sourceOutPointMs)
                                }.build()
                                val mediaItem = MediaItem.Builder()
                                    .setUri(Uri.parse(segment.asset.uri))
                                    .setClippingConfiguration(clippingConfig)
                                    .build()
                                val editedItem = EditedMediaItem.Builder(mediaItem)
                                    .setRemoveVideo(true)
                                    .build()
                                audioItems.add(editedItem)
                            }
                            is AudioTimelineSegment.GapSegment -> {
                                if (silenceFile != null && silenceFile.exists()) {
                                    val clippingConfig = MediaItem.ClippingConfiguration.Builder()
                                        .setStartPositionMs(0L)
                                        .setEndPositionMs(segment.durationMs)
                                        .build()
                                    val mediaItem = MediaItem.Builder()
                                        .setUri(Uri.fromFile(silenceFile))
                                        .setClippingConfiguration(clippingConfig)
                                        .build()
                                    val editedItem = EditedMediaItem.Builder(mediaItem)
                                        .setRemoveVideo(true)
                                        .build()
                                    audioItems.add(editedItem)
                                }
                            }
                        }
                    }
                    if (audioItems.isNotEmpty()) {
                        sequences.add(EditedMediaItemSequence.Builder(audioItems).build())
                    }
                }
            }

            val composition = Composition.Builder(sequences)
                .setEffects(effects)
                .build()

            // 4. Run export via Transformer with progress polling
            suspendCancellableCoroutine { continuation ->
                val listener = object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        stopProgressTicker()
                        onProgress(100)
                        if (continuation.isActive) {
                            continuation.resume(outputFile)
                        }
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        stopProgressTicker()
                        if (continuation.isActive) {
                            continuation.resumeWithException(exportException)
                        }
                    }
                }

                val transformer = Transformer.Builder(context.applicationContext)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .addListener(listener)
                    .build()

                activeTransformer = transformer

                continuation.invokeOnCancellation {
                    cancel()
                    if (outputFile.exists()) {
                        outputFile.delete()
                    }
                }

                try {
                    transformer.start(composition, outputFile.absolutePath)
                    startProgressTicker(transformer, onProgress)
                } catch (e: Exception) {
                    stopProgressTicker()
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }
            }

            Result.success(outputFile)
        } catch (e: CancellationException) {
            cancel()
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            stopProgressTicker()
            activeTransformer = null
            activeTextOverlay?.release()
            activeTextOverlay = null
            activeVideoOverlay?.close()
            activeVideoOverlay = null
            activeSilenceFile?.let { if (it.exists()) it.delete() }
            activeSilenceFile = null
        }
    }

    override fun cancel() {
        stopProgressTicker()
        try {
            activeTransformer?.cancel()
        } catch (_: Exception) {
            // Ignore cancel exceptions
        }
        activeTransformer = null
        activeTextOverlay?.release()
        activeTextOverlay = null
        activeVideoOverlay?.close()
        activeVideoOverlay = null
        activeSilenceFile?.let { if (it.exists()) it.delete() }
        activeSilenceFile = null
    }

    private fun startProgressTicker(
        transformer: Transformer,
        onProgress: (Int) -> Unit
    ) {
        stopProgressTicker()
        val progressHolder = ProgressHolder()
        progressJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                val state = transformer.getProgress(progressHolder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    onProgress(progressHolder.progress.coerceIn(0, 100))
                }
                delay(200L)
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }
}
