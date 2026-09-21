package com.example.feature.export

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.core.data.repository.AssetRepository
import com.example.core.data.repository.ProjectRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel managing export configurations, WorkManager orchestration, and state updates (DEV-068 to DEV-072).
 */
class ExportViewModel(
    private val projectId: String,
    private val projectRepository: ProjectRepository,
    private val assetRepository: AssetRepository,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    private val workManager = WorkManager.getInstance(appContext)
    private var currentWorkId: UUID? = null
    private var workObserverJob: Job? = null

    init {
        loadProject()
    }

    private fun loadProject() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingProject = true) }
            val project = projectRepository.getProjectById(projectId)
            _uiState.update {
                it.copy(
                    project = project,
                    isLoadingProject = false
                )
            }
        }
    }

    fun setResolution(preset: ResolutionPreset) {
        if (_uiState.value.isExporting) return
        _uiState.update { it.copy(selectedResolution = preset) }
    }

    fun setFps(preset: FpsPreset) {
        if (_uiState.value.isExporting) return
        _uiState.update { it.copy(selectedFps = preset) }
    }

    fun setQuality(preset: QualityPreset) {
        if (_uiState.value.isExporting) return
        _uiState.update { it.copy(selectedQuality = preset) }
    }

    fun startExport() {
        val state = _uiState.value
        val project = state.project ?: return

        val settings = buildExportSettings(
            resolution = state.selectedResolution,
            fps = state.selectedFps,
            quality = state.selectedQuality,
            aspectRatio = project.aspectRatio
        )

        val inputData = Data.Builder()
            .putString(ExportWorker.KEY_PROJECT_ID, project.id)
            .putInt(ExportWorker.KEY_WIDTH, settings.width)
            .putInt(ExportWorker.KEY_HEIGHT, settings.height)
            .putInt(ExportWorker.KEY_FPS, settings.fps)
            .putInt(ExportWorker.KEY_VIDEO_BITRATE, settings.videoBitrate)
            .putInt(ExportWorker.KEY_AUDIO_BITRATE, settings.audioBitrate)
            .build()

        val exportWorkRequest = OneTimeWorkRequestBuilder<ExportWorker>()
            .setInputData(inputData)
            .addTag("export_${project.id}")
            .build()

        val workId = exportWorkRequest.id
        currentWorkId = workId

        _uiState.update {
            it.copy(
                status = ExportStatus.PREPARING,
                progressPercent = 0,
                statusMessage = "Enqueuing export task...",
                errorMessage = null,
                outputUri = null,
                isWorkManagerRunning = true
            )
        }

        workManager.enqueueUniqueWork(
            "${ExportWorker.WORK_NAME}_${project.id}",
            ExistingWorkPolicy.REPLACE,
            exportWorkRequest
        )

        observeWork(workId)
    }

    private fun observeWork(workId: UUID) {
        workObserverJob?.cancel()
        workObserverJob = viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workId).collect { workInfo ->
                if (workInfo == null) return@collect

                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED -> {
                        _uiState.update {
                            it.copy(
                                status = ExportStatus.PREPARING,
                                statusMessage = "Preparing to export..."
                            )
                        }
                    }
                    WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress.getInt(ExportWorker.KEY_PROGRESS, 0)
                        val message = workInfo.progress.getString(ExportWorker.KEY_STATUS_MESSAGE) ?: "Processing video..."
                        val currentStatus = if (progress >= 90) ExportStatus.SAVING_TO_GALLERY else ExportStatus.EXPORTING
                        _uiState.update {
                            it.copy(
                                status = currentStatus,
                                progressPercent = progress,
                                statusMessage = message,
                                isWorkManagerRunning = true
                            )
                        }
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val outputUri = workInfo.outputData.getString(ExportWorker.KEY_OUTPUT_URI)
                        _uiState.update {
                            it.copy(
                                status = ExportStatus.SUCCESS,
                                progressPercent = 100,
                                statusMessage = "Export complete! Video saved to Gallery.",
                                outputUri = outputUri,
                                isWorkManagerRunning = false
                            )
                        }
                    }
                    WorkInfo.State.FAILED -> {
                        val error = workInfo.outputData.getString(ExportWorker.KEY_ERROR_MESSAGE) ?: "Export failed unexpectedly."
                        _uiState.update {
                            it.copy(
                                status = ExportStatus.ERROR,
                                errorMessage = error,
                                isWorkManagerRunning = false
                            )
                        }
                    }
                    WorkInfo.State.CANCELLED -> {
                        _uiState.update {
                            it.copy(
                                status = ExportStatus.CANCELLED,
                                statusMessage = "Export was cancelled.",
                                isWorkManagerRunning = false
                            )
                        }
                    }
                    WorkInfo.State.BLOCKED -> {
                        // Keep current status
                    }
                }
            }
        }
    }

    fun cancelExport() {
        currentWorkId?.let { id ->
            workManager.cancelWorkById(id)
        }
        _uiState.update {
            it.copy(
                status = ExportStatus.CANCELLED,
                isWorkManagerRunning = false
            )
        }
    }

    fun resetState() {
        _uiState.update {
            it.copy(
                status = ExportStatus.IDLE,
                progressPercent = 0,
                statusMessage = "",
                errorMessage = null,
                outputUri = null
            )
        }
    }

    companion object {
        fun provideFactory(
            projectId: String,
            projectRepository: ProjectRepository,
            assetRepository: AssetRepository,
            context: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ExportViewModel(
                    projectId = projectId,
                    projectRepository = projectRepository,
                    assetRepository = assetRepository,
                    appContext = context.applicationContext
                ) as T
            }
        }
    }
}
