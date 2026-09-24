package com.example.core.media.render

import com.example.core.model.Asset
import com.example.core.model.ClipType
import com.example.core.model.MediaType
import com.example.core.model.Project
import com.example.core.model.TrackType

/**
 * Pure builder that derives a [RenderScene] from the persistent [Project] domain model.
 *
 * Enforces:
 * 1. Single source of truth (Project / Timeline -> RenderScene)
 * 2. Deterministic Z-Ordering:
 *    - Main Video: 0..99
 *    - Video Overlays (PiP): 100..199
 *    - Image Overlays: 200..299
 *    - Text Overlays: 300..399
 * 3. Consistent speed, trim points, and audio extraction across both preview and export.
 */
object RenderSceneBuilder {

    const val Z_INDEX_MAIN_VIDEO = 0
    const val Z_INDEX_VIDEO_OVERLAY = 100
    const val Z_INDEX_IMAGE_OVERLAY = 200
    const val Z_INDEX_TEXT_OVERLAY = 300

    fun buildScene(project: Project, assets: Map<String, Asset>): RenderScene {
        val canvasConfig = CanvasConfig(
            width = project.width,
            height = project.height,
            aspectRatio = project.aspectRatio
        )

        val videoLayers = mutableListOf<VideoRenderLayer>()
        val imageLayers = mutableListOf<ImageRenderLayer>()
        val textLayers = mutableListOf<TextRenderLayer>()
        val audioLayers = mutableListOf<AudioRenderLayer>()

        // 1. Main Video Track
        val mainVideoTrack = project.tracks.firstOrNull { it.type == TrackType.VIDEO && it.isVisible }
        if (mainVideoTrack != null) {
            val mainClips = mainVideoTrack.clips.filter { it.isVisible }.sortedBy { it.startTimeMs }
            for (clip in mainClips) {
                val asset = assets[clip.assetId]
                val uri = asset?.uri ?: ""
                videoLayers.add(
                    VideoRenderLayer(
                        id = clip.id,
                        timelineStartMs = clip.startTimeMs,
                        timelineEndMs = clip.endTimeMs,
                        zIndex = Z_INDEX_MAIN_VIDEO + clip.zIndex,
                        isVisible = clip.isVisible,
                        transform = clip.transform,
                        keyframes = clip.keyframes,
                        effects = clip.effects,
                        assetId = clip.assetId ?: "",
                        sourceUri = uri,
                        sourceInPointMs = clip.inPointMs,
                        sourceOutPointMs = clip.outPointMs,
                        speed = clip.speed,
                        volume = clip.volume ?: 1.0f,
                        isMainVideo = true,
                        sourceWidth = asset?.width,
                        sourceHeight = asset?.height
                    )
                )
            }
        }

        // 2. Overlays Tracks (PiP Video and Images)
        val overlayTracks = project.tracks.filter { it.type == TrackType.OVERLAY && it.isVisible }
        for (track in overlayTracks) {
            val clips = track.clips.filter { it.isVisible }.sortedBy { it.startTimeMs }
            for (clip in clips) {
                val asset = assets[clip.assetId]
                val uri = asset?.uri ?: ""
                val isVideo = clip.type == ClipType.VIDEO || asset?.mediaType == MediaType.VIDEO

                if (isVideo) {
                    videoLayers.add(
                        VideoRenderLayer(
                            id = clip.id,
                            timelineStartMs = clip.startTimeMs,
                            timelineEndMs = clip.endTimeMs,
                            zIndex = Z_INDEX_VIDEO_OVERLAY + clip.zIndex,
                            isVisible = clip.isVisible,
                            transform = clip.transform,
                            keyframes = clip.keyframes,
                            effects = clip.effects,
                            assetId = clip.assetId ?: "",
                            sourceUri = uri,
                            sourceInPointMs = clip.inPointMs,
                            sourceOutPointMs = clip.outPointMs,
                            speed = clip.speed,
                            volume = clip.volume ?: 1.0f,
                            isMainVideo = false,
                            sourceWidth = asset?.width,
                            sourceHeight = asset?.height
                        )
                    )

                    // If overlay video has audio, add to audioLayers
                    val vol = clip.volume ?: 1.0f
                    if (vol > 0f && uri.isNotEmpty()) {
                        audioLayers.add(
                            AudioRenderLayer(
                                id = "audio_${clip.id}",
                                trackId = track.id,
                                timelineStartMs = clip.startTimeMs,
                                timelineEndMs = clip.endTimeMs,
                                isVisible = clip.isVisible,
                                assetId = clip.assetId ?: "",
                                sourceUri = uri,
                                sourceInPointMs = clip.inPointMs,
                                sourceOutPointMs = clip.outPointMs,
                                speed = clip.speed,
                                volume = vol
                            )
                        )
                    }
                } else {
                    imageLayers.add(
                        ImageRenderLayer(
                            id = clip.id,
                            timelineStartMs = clip.startTimeMs,
                            timelineEndMs = clip.endTimeMs,
                            zIndex = Z_INDEX_IMAGE_OVERLAY + clip.zIndex,
                            isVisible = clip.isVisible,
                            transform = clip.transform,
                            keyframes = clip.keyframes,
                            effects = clip.effects,
                            assetId = clip.assetId ?: "",
                            sourceUri = uri,
                            sourceWidth = asset?.width,
                            sourceHeight = asset?.height
                        )
                    )
                }
            }
        }

        // 3. Text Tracks
        val textTracks = project.tracks.filter { it.type == TrackType.TEXT && it.isVisible }
        for (track in textTracks) {
            val clips = track.clips.filter { it.isVisible && it.textData != null }.sortedBy { it.startTimeMs }
            for (clip in clips) {
                textLayers.add(
                    TextRenderLayer(
                        id = clip.id,
                        timelineStartMs = clip.startTimeMs,
                        timelineEndMs = clip.endTimeMs,
                        zIndex = Z_INDEX_TEXT_OVERLAY + clip.zIndex,
                        isVisible = clip.isVisible,
                        transform = clip.transform,
                        keyframes = clip.keyframes,
                        effects = clip.effects,
                        textData = clip.textData!!
                    )
                )
            }
        }

        // 4. Audio Tracks
        val audioTracks = project.tracks.filter { it.type == TrackType.AUDIO && it.isVisible }
        for (track in audioTracks) {
            val clips = track.clips.filter { it.isVisible }.sortedBy { it.startTimeMs }
            for (clip in clips) {
                val asset = assets[clip.assetId]
                val uri = asset?.uri ?: ""
                audioLayers.add(
                    AudioRenderLayer(
                        id = clip.id,
                        trackId = track.id,
                        timelineStartMs = clip.startTimeMs,
                        timelineEndMs = clip.endTimeMs,
                        isVisible = clip.isVisible,
                        assetId = clip.assetId ?: "",
                        sourceUri = uri,
                        sourceInPointMs = clip.inPointMs,
                        sourceOutPointMs = clip.outPointMs,
                        speed = clip.speed,
                        volume = clip.volume ?: 1.0f
                    )
                )
            }
        }

        val totalDurationMs = maxOf(
            project.durationMs,
            (videoLayers.map { it.timelineEndMs } +
             imageLayers.map { it.timelineEndMs } +
             textLayers.map { it.timelineEndMs } +
             audioLayers.map { it.timelineEndMs }).maxOrNull() ?: 0L
        )

        return RenderScene(
            canvasConfig = canvasConfig,
            durationMs = totalDurationMs,
            videoLayers = videoLayers,
            imageLayers = imageLayers,
            textLayers = textLayers,
            audioLayers = audioLayers
        )
    }
}
