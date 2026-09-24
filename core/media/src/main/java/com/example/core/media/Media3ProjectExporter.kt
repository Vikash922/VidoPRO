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
import com.example.core.media.render.ExportRenderer
import com.example.core.media.render.RenderExportException
import com.example.core.media.render.RenderScene
import com.example.core.media.render.RenderSceneBuilder
import com.example.core.media.render.VideoRenderLayer
import com.example.core.model.Asset
import com.example.core.model.Clip
import com.example.core.model.ClipType
import com.example.core.model.ExportSettings
import com.example.core.model.Project
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
 * Media3 Transformer implementation of [ProjectExporter] and [ExportRenderer].
 *
 * Consumes the universal [RenderScene] to concatenate and trim video clips,
 * applies hardware-accelerated Media3 GPU shaders for transforms and filters,
 * composites multi-layer PiP video/image overlays and text overlays with exact Z-order,
 * aligns audio tracks with silence gap handling, and encodes to MP4.
 */
class Media3ProjectExporter(
    private val context: Context
) : ProjectExporter, ExportRenderer {

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
    ): Result<File> {
        val scene = RenderSceneBuilder.buildScene(project, assets)
        return render(scene, settings, outputFile, onProgress)
    }

    override suspend fun render(
        scene: RenderScene,
        settings: ExportSettings,
        outputFile: File,
        onProgress: (progressPercent: Int) -> Unit
    ): Result<File> = withContext(Dispatchers.Main) {
        try {
            // 1. Validate main video layers
            val mainVideoLayers = scene.videoLayers.filter { it.isMainVideo && it.isVisible }.sortedBy { it.timelineStartMs }
            if (mainVideoLayers.isEmpty()) {
                return@withContext Result.failure(
                    RenderExportException.InvalidTimelineException("No video clips available in project to export.")
                )
            }

            // Ensure parent directory exists and clean target file
            outputFile.parentFile?.mkdirs()
            if (outputFile.exists()) {
                outputFile.delete()
            }

            // 2. Build MediaItems for each trimmed main video clip
            val editedMediaItems = mutableListOf<EditedMediaItem>()
            for (layer in mainVideoLayers) {
                if (layer.sourceUri.isEmpty()) {
                    return@withContext Result.failure(
                        RenderExportException.MissingSourceException(layer.sourceUri, "Missing source URI for clip ${layer.id}")
                    )
                }

                val uri = Uri.parse(layer.sourceUri)

                val clippingConfig = MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(layer.sourceInPointMs)
                    .setEndPositionMs(layer.sourceOutPointMs)
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setUri(uri)
                    .setClippingConfiguration(clippingConfig)
                    .build()

                val isImage = layer.sourceUri.endsWith(".jpg", true) ||
                              layer.sourceUri.endsWith(".jpeg", true) ||
                              layer.sourceUri.endsWith(".png", true) ||
                              layer.sourceUri.endsWith(".webp", true)

                val editedItemBuilder = EditedMediaItem.Builder(mediaItem)
                    .setRemoveAudio(false)

                if (isImage) {
                    editedItemBuilder.setDurationUs(layer.durationMs * 1000L)
                    editedItemBuilder.setFrameRate(settings.fps)
                }

                // Apply per-clip video effects (brightness, contrast, saturation, exposure, filters)
                val clipVideoEffects = mutableListOf<androidx.media3.common.Effect>()
                clipVideoEffects.addAll(Media3EffectHelper.createMedia3Effects(layer.effects))

                // Apply per-clip transform effect (position, scale, rotation, keyframes, transitions)
                val transitionAsOutgoing = scene.transitions.find { it.firstClipId == layer.id }
                val transitionAsIncoming = scene.transitions.find { it.secondClipId == layer.id }
                val transformEffect = Media3EffectHelper.createTransformEffect(
                    layer = layer,
                    canvasWidth = settings.width,
                    canvasHeight = settings.height,
                    transitionAsOutgoing = transitionAsOutgoing,
                    transitionAsIncoming = transitionAsIncoming
                )
                if (transformEffect != null) {
                    clipVideoEffects.add(transformEffect)
                }

                // Apply per-clip opacity effect (dimming toward background canvas)
                if (layer.opacity < 1.0f) {
                    val opacityEffect = Media3EffectHelper.createOpacityEffect(layer.opacity)
                    if (opacityEffect != null) {
                        clipVideoEffects.add(opacityEffect)
                    }
                }

                if (clipVideoEffects.isNotEmpty()) {
                    editedItemBuilder.setEffects(Effects(emptyList(), clipVideoEffects))
                }

                editedMediaItems.add(editedItemBuilder.build())
            }

            if (editedMediaItems.isEmpty()) {
                return@withContext Result.failure(
                    RenderExportException.PipelineRenderException("Unable to resolve valid media items for export.")
                )
            }

            // 3. Configure Effects (Presentation Scale, Video Overlays, Text Overlays)
            // Stacking Precedence:
            //   1. Presentation (fit to output resolution)
            //   2. Visual Overlays: PiP Videos and Images (Z-Index 100..299)
            //   3. Text Overlays: (Z-Index 300..399) - Drawn ON TOP of Video/Image overlays
            val presentation = Presentation.createForWidthAndHeight(
                settings.width,
                settings.height,
                Presentation.LAYOUT_SCALE_TO_FIT
            )

            val videoEffects = mutableListOf<androidx.media3.common.Effect>(presentation)

            // Step 3a: Visual Overlays (PiP - Moving Videos and Images: Z-Index 100..299)
            val overlayVideoLayers = scene.videoLayers.filter { !it.isMainVideo && it.isVisible }
            val overlayImageLayers = scene.imageLayers.filter { it.isVisible }

            if (overlayVideoLayers.isNotEmpty() || overlayImageLayers.isNotEmpty()) {
                try {
                    // Convert layers to Clips and Asset map for VideoOverlayGenerator compatibility
                    val overlayClips = mutableListOf<Clip>()
                    val overlayAssets = mutableMapOf<String, Asset>()

                    for (vl in overlayVideoLayers) {
                        overlayClips.add(
                            Clip(
                                id = vl.id,
                                trackId = "track_overlay",
                                type = ClipType.VIDEO,
                                assetId = vl.assetId,
                                startTimeMs = vl.timelineStartMs,
                                durationMs = vl.durationMs,
                                inPointMs = vl.sourceInPointMs,
                                outPointMs = vl.sourceOutPointMs,
                                speed = vl.speed,
                                volume = vl.volume,
                                isVisible = vl.isVisible,
                                zIndex = vl.zIndex,
                                transform = vl.transform,
                                keyframes = vl.keyframes,
                                effects = vl.effects,
                                mask = vl.mask,
                                blendMode = vl.blendMode
                            )
                        )
                        overlayAssets[vl.assetId] = Asset(
                            id = vl.assetId,
                            uri = vl.sourceUri,
                            mediaType = com.example.core.model.MediaType.VIDEO,
                            width = vl.sourceWidth,
                            height = vl.sourceHeight
                        )
                    }

                    for (il in overlayImageLayers) {
                        overlayClips.add(
                            Clip(
                                id = il.id,
                                trackId = "track_overlay",
                                type = ClipType.IMAGE,
                                assetId = il.assetId,
                                startTimeMs = il.timelineStartMs,
                                durationMs = il.durationMs,
                                isVisible = il.isVisible,
                                zIndex = il.zIndex,
                                transform = il.transform,
                                keyframes = il.keyframes,
                                effects = il.effects,
                                mask = il.mask,
                                blendMode = il.blendMode
                            )
                        )
                        overlayAssets[il.assetId] = Asset(
                            id = il.assetId,
                            uri = il.sourceUri,
                            mediaType = com.example.core.model.MediaType.IMAGE,
                            width = il.sourceWidth,
                            height = il.sourceHeight
                        )
                    }

                    val videoOverlay = VideoOverlayGenerator(context, overlayClips, overlayAssets, settings.width, settings.height)
                    activeVideoOverlay = videoOverlay
                    val overlayEffect = androidx.media3.effect.OverlayEffect(
                        com.google.common.collect.ImmutableList.of<androidx.media3.effect.TextureOverlay>(videoOverlay)
                    )
                    videoEffects.add(overlayEffect)
                } catch (e: Exception) {
                    android.util.Log.w("Media3ProjectExporter", "Failed to initialize visual overlay effect: ${e.message}")
                }
            }

            // Step 3b: Text Overlays (Z-Index 300..399) - Placed after visual overlays so text appears on top
            val textLayers = scene.textLayers.filter { it.isVisible }
            if (textLayers.isNotEmpty()) {
                try {
                    val textClips = textLayers.map { tl ->
                        Clip(
                            id = tl.id,
                            trackId = "track_text",
                            type = ClipType.TEXT,
                            startTimeMs = tl.timelineStartMs,
                            durationMs = tl.durationMs,
                            isVisible = tl.isVisible,
                            zIndex = tl.zIndex,
                            transform = tl.transform,
                            keyframes = tl.keyframes,
                            effects = tl.effects,
                            textData = tl.textData
                        )
                    }
                    val textOverlay = TextOverlayGenerator(textClips, settings.width, settings.height)
                    activeTextOverlay = textOverlay
                    val overlayEffect = androidx.media3.effect.OverlayEffect(
                        com.google.common.collect.ImmutableList.of<androidx.media3.effect.TextureOverlay>(textOverlay)
                    )
                    videoEffects.add(overlayEffect)
                } catch (e: Exception) {
                    android.util.Log.w("Media3ProjectExporter", "Failed to initialize text overlay effect: ${e.message}")
                }
            }

            val effects = Effects(emptyList(), videoEffects)
            val sequences = mutableListOf<EditedMediaItemSequence>()
            sequences.add(EditedMediaItemSequence.Builder(editedMediaItems).build())

            // 4. Multi-track Audio Sequences with timeline gap preservation
            val audibleAudioLayers = scene.audioLayers.filter { it.isVisible && it.volume > 0f }
            if (audibleAudioLayers.isNotEmpty()) {
                val totalTimelineDurationMs = scene.durationMs
                val audioClips = audibleAudioLayers.map { al ->
                    Clip(
                        id = al.id,
                        trackId = al.trackId,
                        type = ClipType.AUDIO,
                        assetId = al.assetId,
                        startTimeMs = al.timelineStartMs,
                        durationMs = al.durationMs,
                        inPointMs = al.sourceInPointMs,
                        outPointMs = al.sourceOutPointMs,
                        speed = al.speed,
                        volume = al.volume
                    )
                }
                val audioAssets = audibleAudioLayers.associate { al ->
                    al.assetId to Asset(id = al.assetId, uri = al.sourceUri, mediaType = com.example.core.model.MediaType.AUDIO)
                }

                val partitionedLayers = AudioTimelineMapper.partitionIntoNonOverlappingLayers(audioClips)
                val mappedTracksSegments = partitionedLayers.mapNotNull { layerClips ->
                    val segments = AudioTimelineMapper.mapClipsToSegments(layerClips, audioAssets, totalTimelineDurationMs)
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
            }

            val composition = Composition.Builder(sequences)
                .setEffects(effects)
                .build()

            // 5. Run export via Transformer with progress polling
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
                        val typedException = when {
                            exportException.errorCode == ExportException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ||
                            exportException.errorCode == ExportException.ERROR_CODE_ENCODING_FORMAT_UNSUPPORTED ->
                                RenderExportException.UnsupportedCodecException(
                                    codecName = exportException.message ?: "Format unsupported",
                                    message = "Unsupported codec encountered during export: ${exportException.message}"
                                )
                            exportException.message?.contains("OutOfMemory", ignoreCase = true) == true ->
                                RenderExportException.OutOfMemoryRenderException("Export ran out of memory: ${exportException.message}", exportException)
                            else ->
                                RenderExportException.PipelineRenderException(exportException.message ?: "Transformer error", exportException)
                        }
                        if (continuation.isActive) {
                            continuation.resumeWithException(typedException)
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
                    val typed = if (e is OutOfMemoryError) {
                        RenderExportException.OutOfMemoryRenderException("OOM when starting transformer", e)
                    } else {
                        RenderExportException.PipelineRenderException("Failed to start transformer: ${e.message}", e)
                    }
                    if (continuation.isActive) {
                        continuation.resumeWithException(typed)
                    }
                }
            }

            Result.success(outputFile)
        } catch (e: CancellationException) {
            cancel()
            Result.failure(RenderExportException.CancelledExportException("Export cancelled"))
        } catch (e: RenderExportException) {
            Result.failure(e)
        } catch (e: OutOfMemoryError) {
            Result.failure(RenderExportException.OutOfMemoryRenderException("Out of memory during export", e))
        } catch (e: Exception) {
            Result.failure(RenderExportException.PipelineRenderException(e.message ?: "Export failed", e))
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
