package com.example.feature.export

import com.example.core.model.AspectRatio
import com.example.core.model.Project

/**
 * Status stages of the export pipeline (DEV-068).
 */
enum class ExportStatus {
    IDLE,
    PREPARING,
    EXPORTING,
    SAVING_TO_GALLERY,
    SUCCESS,
    ERROR,
    CANCELLED
}

/**
 * UI State for the Export Screen (DEV-068).
 */
data class ExportUiState(
    val project: Project? = null,
    val isLoadingProject: Boolean = true,
    val status: ExportStatus = ExportStatus.IDLE,
    val selectedResolution: ResolutionPreset = ResolutionPreset.RES_1080P,
    val selectedFps: FpsPreset = FpsPreset.FPS_30,
    val selectedQuality: QualityPreset = QualityPreset.MEDIUM,
    val progressPercent: Int = 0,
    val statusMessage: String = "",
    val outputUri: String? = null,
    val errorMessage: String? = null,
    val isWorkManagerRunning: Boolean = false
) {
    val isExporting: Boolean
        get() = status == ExportStatus.PREPARING ||
                status == ExportStatus.EXPORTING ||
                status == ExportStatus.SAVING_TO_GALLERY

    val progressFraction: Float
        get() = (progressPercent / 100f).coerceIn(0f, 1f)

    val aspectRatio: AspectRatio
        get() = project?.aspectRatio ?: AspectRatio.RATIO_9_16

    val dimensions: Pair<Int, Int>
        get() = selectedResolution.getDimensions(aspectRatio)

    val estimatedSizeMb: Float
        get() {
            val duration = project?.durationMs ?: 0L
            val bitrate = selectedQuality.calculateBitrate(selectedResolution)
            return estimateFileSizeMb(duration, bitrate)
        }
}
