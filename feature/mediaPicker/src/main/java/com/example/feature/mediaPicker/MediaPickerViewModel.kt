package com.example.feature.mediaPicker

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.feature.mediaPicker.data.MediaRepository
import com.example.feature.mediaPicker.model.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MediaPickerViewModel(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaPickerUiState())
    val uiState: StateFlow<MediaPickerUiState> = _uiState.asStateFlow()

    fun updatePermissionStatus(hasPermission: Boolean) {
        _uiState.update { it.copy(hasPermission = hasPermission) }
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val filter = _uiState.value.currentFilter.mediaType
                val items = mediaRepository.getLocalMedia(filter)
                _uiState.update { it.copy(isLoading = false, mediaItems = items) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load media") }
            }
        }
    }

    fun setFilter(filter: MediaFilter) {
        if (_uiState.value.currentFilter == filter) return
        _uiState.update { it.copy(currentFilter = filter) }
        loadMedia()
    }

    fun toggleSelection(item: MediaItem) {
        _uiState.update { current ->
            val exists = current.selectedItems.any { it.id == item.id }
            val newSelected = if (exists) {
                current.selectedItems.filterNot { it.id == item.id }
            } else {
                current.selectedItems + item
            }
            current.copy(selectedItems = newSelected)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedItems = emptyList()) }
    }

    fun handlePickedUris(uris: List<Uri>, onCompleted: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val items = uris.mapNotNull { mediaRepository.getMediaItemFromUri(it) }
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    selectedItems = current.selectedItems + items
                )
            }
            onCompleted()
        }
    }

    companion object {
        fun provideFactory(mediaRepository: MediaRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MediaPickerViewModel(mediaRepository) as T
                }
            }
    }
}
