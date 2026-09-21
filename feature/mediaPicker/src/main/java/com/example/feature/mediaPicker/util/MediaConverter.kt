package com.example.feature.mediaPicker.util

import com.example.core.model.Asset
import com.example.core.model.Clip
import com.example.core.model.ClipType
import com.example.core.model.MediaType
import com.example.core.model.Transform
import com.example.feature.mediaPicker.model.MediaItem
import java.util.UUID

object MediaConverter {

    fun toAssetAndClip(
        mediaItem: MediaItem,
        trackId: String,
        startTimeMs: Long
    ): Pair<Asset, Clip> {
        val assetId = UUID.randomUUID().toString()
        val clipId = UUID.randomUUID().toString()
        val duration = if (mediaItem.durationMs > 0) mediaItem.durationMs else 3000L
        val clipType = if (mediaItem.mediaType == MediaType.VIDEO) ClipType.VIDEO else ClipType.IMAGE

        val asset = Asset(
            id = assetId,
            uri = mediaItem.uri.toString(),
            mimeType = mediaItem.mimeType,
            mediaType = mediaItem.mediaType,
            durationMs = duration,
            width = mediaItem.width,
            height = mediaItem.height,
            sizeBytes = mediaItem.sizeBytes,
            displayName = mediaItem.displayName,
            thumbnailPath = mediaItem.uri.toString(),
            createdAt = System.currentTimeMillis()
        )

        val clip = Clip(
            id = clipId,
            trackId = trackId,
            type = clipType,
            assetId = assetId,
            startTimeMs = startTimeMs,
            durationMs = duration,
            inPointMs = 0L,
            outPointMs = duration,
            speed = 1.0f,
            volume = 1.0f,
            isVisible = true,
            zIndex = 0,
            transform = Transform.DEFAULT,
            effects = emptyList(),
            keyframes = emptyList(),
            textData = null
        )

        return Pair(asset, clip)
    }
}
