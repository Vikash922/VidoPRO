package com.example.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsViewModel(
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        calculateCacheSize()
    }

    fun setDefaultResolution(resolution: String) {
        _uiState.update { it.copy(defaultResolution = resolution) }
    }

    fun setDefaultFps(fps: Int) {
        _uiState.update { it.copy(defaultFps = fps) }
    }

    fun setDefaultQuality(quality: String) {
        _uiState.update { it.copy(defaultQuality = quality) }
    }

    fun toggleSnapping(enabled: Boolean) {
        _uiState.update { it.copy(timelineSnapping = enabled) }
    }

    fun toggleAutosave(enabled: Boolean) {
        _uiState.update { it.copy(autosaveEnabled = enabled) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun clearCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearingCache = true) }
            withContext(Dispatchers.IO) {
                try {
                    context.cacheDir.deleteRecursively()
                    val importedMediaDir = File(context.filesDir, "imported_media")
                    if (importedMediaDir.exists()) {
                        importedMediaDir.listFiles()?.forEach { it.delete() }
                    }
                } catch (_: Exception) {
                }
            }
            calculateCacheSize()
            _uiState.update {
                it.copy(
                    isClearingCache = false,
                    message = "Cache cleared successfully"
                )
            }
        }
    }

    private fun calculateCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val cacheSize = getDirSize(context.cacheDir)
            val importedSize = getDirSize(File(context.filesDir, "imported_media"))
            val totalMb = (cacheSize + importedSize) / (1024.0 * 1024.0)
            _uiState.update { it.copy(cacheSizeMb = totalMb) }
        }
    }

    private fun getDirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) getDirSize(file) else file.length()
        }
        return size
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(context.applicationContext) as T
                }
            }
    }
}
