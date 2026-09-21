package com.example.feature.mediaPicker

import com.example.core.model.MediaType
import com.example.feature.mediaPicker.model.MediaItem

enum class MediaFilter(val label: String, val mediaType: MediaType?) {
    ALL("All", null),
    VIDEOS("Videos", MediaType.VIDEO),
    PHOTOS("Photos", MediaType.IMAGE)
}

data class MediaPickerUiState(
    val isLoading: Boolean = true,
    val mediaItems: List<MediaItem> = emptyList(),
    val selectedItems: List<MediaItem> = emptyList(),
    val currentFilter: MediaFilter = MediaFilter.ALL,
    val hasPermission: Boolean = false,
    val error: String? = null
) {
    val selectedCount: Int
        get() = selectedItems.size
}
