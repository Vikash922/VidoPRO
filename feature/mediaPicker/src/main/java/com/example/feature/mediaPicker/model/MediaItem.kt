package com.example.feature.mediaPicker.model

import android.net.Uri
import com.example.core.model.MediaType

data class MediaItem(
    val id: String,
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val mediaType: MediaType,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0L,
    val dateAdded: Long = 0L
)
