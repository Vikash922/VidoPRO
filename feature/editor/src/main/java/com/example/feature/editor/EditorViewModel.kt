package com.example.feature.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.data.repository.AssetRepository
import com.example.core.data.repository.ProjectRepository
import com.example.core.media.PreviewPlayerController
import com.example.core.model.Asset
import com.example.core.model.Clip
import com.example.core.model.Project
import com.example.core.model.Track
import com.example.core.model.TrackType
import com.example.feature.editor.history.DeleteClipCommand
import com.example.feature.editor.history.EditorCommand
import com.example.feature.editor.history.MoveClipCommand
import com.example.feature.editor.history.SplitClipCommand
import com.example.feature.editor.history.StateSnapshotCommand
import com.example.feature.editor.history.TrimEndCommand
import com.example.feature.editor.history.TrimStartCommand
import com.example.feature.editor.history.UndoRedoManager
import com.example.feature.timeline.engine.TimelineAction
import com.example.feature.timeline.engine.TimelineEngineState
import com.example.feature.timeline.engine.TimelineReducer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for Video Editor managing project state, playhead, selection, tools,
 * Command-pattern undo/redo manager, debounced autosave, and syncing with Media3 Preview Player.
 */
class EditorViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
    private val assetRepository: AssetRepository? = null,
    val previewPlayer: PreviewPlayerController? = null
) : ViewModel() {

    val projectId: String? = savedStateHandle["projectId"]

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val undoRedoManager = UndoRedoManager()
    private var timelineEngineState = TimelineEngineState()

    // Debounced autosave job (1500ms delay) - DEV-060, DEV-061
    private var autosaveJob: Job? = null
    private var pendingProjectSave: Project? = null

    init {
        loadProject()
        observePlayerState()
        observeUndoRedoState()
    }

    private fun observeUndoRedoState() {
        undoRedoManager.canUndo
            .onEach { canUndo ->
                _uiState.update { it.copy(canUndo = canUndo) }
            }
            .launchIn(viewModelScope)

        undoRedoManager.canRedo
            .onEach { canRedo ->
                _uiState.update { it.copy(canRedo = canRedo) }
            }
            .launchIn(viewModelScope)
    }

    private fun observePlayerState() {
        previewPlayer?.currentPositionMs
            ?.onEach { pos ->
                if (_uiState.value.isPlaying) {
                    _uiState.update { it.copy(playheadPositionMs = pos) }
                    timelineEngineState = timelineEngineState.copy(playheadPositionMs = pos)
                }
            }
            ?.launchIn(viewModelScope)

        previewPlayer?.isPlaying
            ?.onEach { playing ->
                _uiState.update { it.copy(isPlaying = playing) }
            }
            ?.launchIn(viewModelScope)
    }

    private fun loadProject() {
        val id = projectId
        if (id.isNullOrBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "No project ID provided") }
            return
        }

        projectRepository.observeProjectById(id)
            .onEach { project ->
                if (project != null) {
                    timelineEngineState = timelineEngineState.copy(
                        tracks = project.tracks,
                        durationMs = project.durationMs
                    )
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            project = project,
                            error = null
                        )
                    }
                    syncClipsToPlayer(project)
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Project not found") }
                }
            }
            .catch { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "Failed to load project"
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun syncClipsToPlayer(project: com.example.core.model.Project) {
        viewModelScope.launch {
            val allClips = project.tracks.flatMap { it.clips }
            val videoClips = project.tracks.filter { it.type == TrackType.VIDEO }.flatMap { it.clips }
            val assetMap = mutableMapOf<String, Asset>()
            assetRepository?.let { repo ->
                allClips.forEach { clip ->
                    clip.assetId?.let { assetId ->
                        repo.getAssetById(assetId)?.let { asset ->
                            assetMap[asset.id] = asset
                        }
                    }
                }
            }
            _uiState.update { it.copy(assets = assetMap) }
            previewPlayer?.setClips(videoClips, assetMap)
        }
    }

    fun onEvent(event: EditorEvent) {
        when (event) {
            is EditorEvent.PlayPauseClicked -> {
                if (_uiState.value.isPlaying) {
                    previewPlayer?.pause()
                } else {
                    previewPlayer?.play()
                }
            }

            is EditorEvent.SeekTo -> {
                previewPlayer?.seekTo(event.positionMs)
                onTimelineAction(TimelineAction.Seek(event.positionMs))
            }

            is EditorEvent.SelectClip -> {
                onTimelineAction(TimelineAction.SelectClip(event.clipId))
            }

            is EditorEvent.ToolClicked -> {
                // If no clip is explicitly selected for clip-specific tools, auto-select clip under playhead
                val currentClipId = _uiState.value.selectedClipId ?: run {
                    val playhead = _uiState.value.playheadPositionMs
                    val found = _uiState.value.project?.tracks?.flatMap { it.clips }?.find {
                        playhead >= it.startTimeMs && playhead <= it.endTimeMs
                    }
                    found?.id?.also { id ->
                        onTimelineAction(TimelineAction.SelectClip(id))
                    }
                }

                when (event.tool) {
                    EditorTool.SPLIT -> {
                        _uiState.update { it.copy(isEditSheetVisible = true) }
                    }
                    EditorTool.SPEED -> {
                        _uiState.update { it.copy(isSpeedSheetVisible = true) }
                    }
                    EditorTool.VOLUME -> {
                        _uiState.update { it.copy(isVolumeSheetVisible = true) }
                    }
                    EditorTool.CANVAS -> {
                        _uiState.update { it.copy(isCanvasSheetVisible = true) }
                    }
                    EditorTool.TRANSFORM -> {
                        _uiState.update { it.copy(isTransformSheetVisible = true) }
                    }
                    EditorTool.TEXT -> {
                        _uiState.update { it.copy(isTextSheetVisible = true) }
                    }
                    EditorTool.FILTERS, EditorTool.EFFECTS -> {
                        _uiState.update { it.copy(isFiltersSheetVisible = true) }
                    }
                    EditorTool.DELETE -> {
                        currentClipId?.let { onTimelineAction(TimelineAction.DeleteClip(it)) }
                    }
                    else -> {
                        _uiState.update { current ->
                            val newTool = if (current.activeTool == event.tool) null else event.tool
                            current.copy(activeTool = newTool)
                        }
                    }
                }
            }

            is EditorEvent.SetTextSheetVisible -> {
                _uiState.update { it.copy(isTextSheetVisible = event.visible) }
            }

            is EditorEvent.SetFiltersSheetVisible -> {
                _uiState.update { it.copy(isFiltersSheetVisible = event.visible) }
            }

            is EditorEvent.SetSpeedSheetVisible -> {
                _uiState.update { it.copy(isSpeedSheetVisible = event.visible) }
            }

            is EditorEvent.SetVolumeSheetVisible -> {
                _uiState.update { it.copy(isVolumeSheetVisible = event.visible) }
            }

            is EditorEvent.SetCanvasSheetVisible -> {
                _uiState.update { it.copy(isCanvasSheetVisible = event.visible) }
            }

            is EditorEvent.SetTransformSheetVisible -> {
                _uiState.update { it.copy(isTransformSheetVisible = event.visible) }
            }

            is EditorEvent.ChangeClipSpeed -> {
                _uiState.value.selectedClipId?.let { clipId ->
                    onTimelineAction(TimelineAction.UpdateClipSpeed(clipId, event.speed))
                }
            }

            is EditorEvent.ChangeClipVolume -> {
                _uiState.value.selectedClipId?.let { clipId ->
                    onTimelineAction(TimelineAction.UpdateClipVolume(clipId, event.volume))
                }
            }

            is EditorEvent.ChangeClipTransform -> {
                _uiState.value.selectedClipId?.let { clipId ->
                    onTimelineAction(TimelineAction.UpdateClipTransform(clipId, event.transform))
                }
            }

            is EditorEvent.ChangeAspectRatio -> {
                val currentProject = _uiState.value.project ?: return
                val updated = currentProject.copy(
                    aspectRatio = event.ratio,
                    updatedAt = System.currentTimeMillis()
                )
                _uiState.update { it.copy(project = updated) }
                scheduleAutosave(updated)
            }

            is EditorEvent.UpdateFilterSettings -> {
                _uiState.update { it.copy(filterSettings = event.filterSettings) }
            }

            is EditorEvent.ResetFilterSettings -> {
                _uiState.update { it.copy(filterSettings = com.example.feature.editor.filter.FilterSettings()) }
            }

            is EditorEvent.ApplyTextClip -> {
                addTextClip(
                    text = event.text,
                    fontSize = event.fontSize,
                    color = event.color,
                    fontFamily = event.fontFamily,
                    alignment = event.alignment
                )
                _uiState.update { it.copy(isTextSheetVisible = false) }
            }

            is EditorEvent.CloseToolPanel -> {
                _uiState.update { it.copy(activeTool = null) }
            }

            is EditorEvent.UndoClicked -> {
                val previous = undoRedoManager.undo(timelineEngineState)
                if (previous != null) {
                    timelineEngineState = previous
                    val currentProj = _uiState.value.project ?: return
                    val updatedProj = currentProj.copy(
                        tracks = previous.tracks,
                        durationMs = previous.durationMs,
                        updatedAt = System.currentTimeMillis()
                    )
                    _uiState.update {
                        it.copy(
                            project = updatedProj,
                            playheadPositionMs = previous.playheadPositionMs,
                            selectedClipId = previous.selectedClipId
                        )
                    }
                    scheduleAutosave(updatedProj)
                    syncClipsToPlayer(updatedProj)
                }
            }

            is EditorEvent.RedoClicked -> {
                val next = undoRedoManager.redo(timelineEngineState)
                if (next != null) {
                    timelineEngineState = next
                    val currentProj = _uiState.value.project ?: return
                    val updatedProj = currentProj.copy(
                        tracks = next.tracks,
                        durationMs = next.durationMs,
                        updatedAt = System.currentTimeMillis()
                    )
                    _uiState.update {
                        it.copy(
                            project = updatedProj,
                            playheadPositionMs = next.playheadPositionMs,
                            selectedClipId = next.selectedClipId
                        )
                    }
                    scheduleAutosave(updatedProj)
                    syncClipsToPlayer(updatedProj)
                }
            }

            is EditorEvent.SplitSelectedClip -> {
                onTimelineAction(TimelineAction.SplitAtPlayhead(_uiState.value.selectedClipId))
            }

            is EditorEvent.DeleteSelectedClip -> {
                _uiState.value.selectedClipId?.let { onTimelineAction(TimelineAction.DeleteClip(it)) }
            }

            is EditorEvent.DuplicateSelectedClip -> {
                _uiState.value.selectedClipId?.let { onTimelineAction(TimelineAction.DuplicateClip(it)) }
            }

            is EditorEvent.SetEditSheetVisible -> {
                _uiState.update { it.copy(isEditSheetVisible = event.visible) }
            }

            is EditorEvent.SaveImmediately -> {
                flushAutosave()
            }

            is EditorEvent.AddMediaClicked -> {
                // Handled via onNavigateMediaPicker
            }

            is EditorEvent.ExportClicked -> {
                flushAutosave()
            }
        }
    }

    /**
     * Schedules a debounced autosave of the project (DEV-060, DEV-061).
     * Waits 1500ms after the last edit before persisting to Room via ProjectRepository.
     */
    private fun scheduleAutosave(project: Project) {
        pendingProjectSave = project
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(1500L)
            flushAutosave()
        }
    }

    /**
     * Immediately flushes any pending autosave changes to the repository.
     */
    fun flushAutosave() {
        autosaveJob?.cancel()
        val toSave = pendingProjectSave ?: _uiState.value.project
        if (toSave != null) {
            viewModelScope.launch {
                projectRepository.updateProject(toSave)
            }
            pendingProjectSave = null
        }
    }

    fun onTimelineAction(action: TimelineAction) {
        val currentProject = _uiState.value.project ?: return
        val preActionState = timelineEngineState

        // Wrap structural actions in specific commands (DEV-058, DEV-059)
        val command: EditorCommand? = when (action) {
            is TimelineAction.MoveClip -> {
                val clip = preActionState.tracks.flatMap { it.clips }.find { it.id == action.clipId }
                if (clip != null) {
                    MoveClipCommand(
                        clipId = action.clipId,
                        targetTrackId = action.targetTrackId,
                        newStartTimeMs = action.newStartTimeMs,
                        oldTrackId = clip.trackId,
                        oldStartTimeMs = clip.startTimeMs
                    )
                } else null
            }
            is TimelineAction.TrimStart -> {
                val clip = preActionState.tracks.flatMap { it.clips }.find { it.id == action.clipId }
                if (clip != null) TrimStartCommand(action.clipId, action.newStartTimeMs, clip.startTimeMs) else null
            }
            is TimelineAction.TrimClipStart -> {
                val clip = preActionState.tracks.flatMap { it.clips }.find { it.id == action.clipId }
                if (clip != null) TrimStartCommand(action.clipId, action.newStartTimeMs, clip.startTimeMs) else null
            }
            is TimelineAction.TrimEnd -> {
                val clip = preActionState.tracks.flatMap { it.clips }.find { it.id == action.clipId }
                if (clip != null) TrimEndCommand(action.clipId, action.newEndTimeMs, clip.endTimeMs) else null
            }
            is TimelineAction.TrimClipEnd -> {
                val clip = preActionState.tracks.flatMap { it.clips }.find { it.id == action.clipId }
                if (clip != null) TrimEndCommand(action.clipId, action.newEndTimeMs, clip.endTimeMs) else null
            }
            is TimelineAction.SplitClip -> {
                SplitClipCommand(action.clipId, action.splitPointMs, preActionState)
            }
            is TimelineAction.SplitAtPlayhead -> {
                val clipId = action.clipId ?: preActionState.selectedClipId
                if (clipId != null) {
                    SplitClipCommand(clipId, preActionState.playheadPositionMs, preActionState)
                } else null
            }
            is TimelineAction.DeleteClip -> {
                DeleteClipCommand(action.clipId, preActionState)
            }
            is TimelineAction.RemoveClip -> {
                DeleteClipCommand(action.clipId, preActionState)
            }
            is TimelineAction.DuplicateClip -> {
                StateSnapshotCommand("Duplicate clip", action, preActionState)
            }
            is TimelineAction.AddClip -> {
                StateSnapshotCommand("Add clip", action, preActionState)
            }
            else -> null
        }

        timelineEngineState = if (command != null) {
            undoRedoManager.executeCommand(command, preActionState)
        } else {
            TimelineReducer.reduce(preActionState, action)
        }

        if (action is TimelineAction.Seek) {
            previewPlayer?.seekTo(action.positionMs)
        } else if (action is TimelineAction.SeekPlayhead) {
            previewPlayer?.seekTo(action.positionMs)
        }

        val updatedTracks = timelineEngineState.tracks
        val updatedDuration = timelineEngineState.durationMs
        val tracksChanged = updatedTracks != currentProject.tracks

        _uiState.update { current ->
            val sheetVisible = when (action) {
                is TimelineAction.SelectClip -> action.clipId != null
                is TimelineAction.DeleteClip -> false
                is TimelineAction.SplitAtPlayhead, is TimelineAction.SplitClip, is TimelineAction.DuplicateClip -> true
                else -> current.isEditSheetVisible && timelineEngineState.selectedClipId != null
            }
            current.copy(
                playheadPositionMs = timelineEngineState.playheadPositionMs,
                selectedClipId = timelineEngineState.selectedClipId,
                isEditSheetVisible = sheetVisible,
                project = if (tracksChanged) current.project?.copy(
                    tracks = updatedTracks,
                    durationMs = updatedDuration,
                    updatedAt = System.currentTimeMillis()
                ) else current.project
            )
        }

        if (tracksChanged) {
            val updatedProject = currentProject.copy(
                tracks = updatedTracks,
                durationMs = updatedDuration,
                updatedAt = System.currentTimeMillis()
            )
            scheduleAutosave(updatedProject)
            syncClipsToPlayer(updatedProject)
        }
    }

    fun addMedia(assetsAndClips: List<Pair<Asset, Clip>>) {
        val currentProject = _uiState.value.project ?: return
        if (assetsAndClips.isEmpty()) return

        viewModelScope.launch {
            try {
                // Insert assets
                assetRepository?.let { repo ->
                    assetsAndClips.forEach { (asset, _) ->
                        repo.insertAsset(asset)
                    }
                }

                // Find or create main video track
                val tracks = currentProject.tracks.toMutableList()
                val targetTrackIndex = tracks.indexOfFirst { it.type == TrackType.VIDEO }
                val targetTrack = if (targetTrackIndex >= 0) {
                    tracks[targetTrackIndex]
                } else {
                    val newTrack = Track(
                        id = UUID.randomUUID().toString(),
                        projectId = currentProject.id,
                        type = TrackType.VIDEO,
                        order = 0
                    )
                    tracks.add(newTrack)
                    newTrack
                }

                var currentEnd = targetTrack.clips.maxOfOrNull { it.endTimeMs } ?: 0L
                val newClips = assetsAndClips.map { (asset, clip) ->
                    val positionedClip = clip.copy(
                        trackId = targetTrack.id,
                        startTimeMs = currentEnd,
                        assetId = asset.id
                    )
                    currentEnd += positionedClip.durationMs
                    positionedClip
                }

                val updatedClips = targetTrack.clips + newClips
                val updatedTrack = targetTrack.copy(clips = updatedClips)
                val finalTrackIndex = tracks.indexOfFirst { it.id == targetTrack.id }
                tracks[finalTrackIndex] = updatedTrack

                val newTotalDuration = tracks.maxOfOrNull { t -> t.clips.maxOfOrNull { it.endTimeMs } ?: 0L } ?: 0L
                val newThumbnail = currentProject.thumbnailPath ?: assetsAndClips.firstOrNull()?.first?.thumbnailPath

                val updatedProject = currentProject.copy(
                    tracks = tracks,
                    durationMs = newTotalDuration,
                    thumbnailPath = newThumbnail,
                    updatedAt = System.currentTimeMillis()
                )

                projectRepository.updateProject(updatedProject)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to add media") }
            }
        }
    }

    /**
     * Adds a TextClip on a TEXT track at the current playhead position (DEV-062, DEV-063).
     */
    fun addTextClip(
        text: String,
        fontSize: Float,
        color: String,
        fontFamily: String,
        alignment: String
    ) {
        val currentProject = _uiState.value.project ?: return
        val currentPlayhead = _uiState.value.playheadPositionMs
        val durationMs = 3000L // Default text clip duration: 3 seconds

        val clipId = UUID.randomUUID().toString()
        val textData = com.example.core.model.TextClipData(
            clipId = clipId,
            text = text,
            fontSize = fontSize,
            textColor = color,
            fontFamily = fontFamily,
            alignment = alignment
        )

        val tracks = currentProject.tracks.toMutableList()
        val textTrackIndex = tracks.indexOfFirst { it.type == TrackType.TEXT }
        val textTrack = if (textTrackIndex >= 0) {
            tracks[textTrackIndex]
        } else {
            val newTrack = Track(
                id = UUID.randomUUID().toString(),
                projectId = currentProject.id,
                type = TrackType.TEXT,
                order = tracks.size
            )
            tracks.add(newTrack)
            newTrack
        }

        val textClip = Clip(
            id = clipId,
            trackId = textTrack.id,
            type = com.example.core.model.ClipType.TEXT,
            startTimeMs = currentPlayhead,
            durationMs = durationMs,
            inPointMs = 0L,
            outPointMs = durationMs,
            textData = textData
        )

        val updatedClips = (textTrack.clips + textClip).sortedBy { it.startTimeMs }
        val updatedTrack = textTrack.copy(clips = updatedClips)
        val finalTrackIndex = tracks.indexOfFirst { it.id == textTrack.id }
        tracks[finalTrackIndex] = updatedTrack

        val newTotalDuration = tracks.maxOfOrNull { t -> t.clips.maxOfOrNull { it.endTimeMs } ?: 0L } ?: currentProject.durationMs

        val updatedProject = currentProject.copy(
            tracks = tracks,
            durationMs = newTotalDuration,
            updatedAt = System.currentTimeMillis()
        )

        timelineEngineState = timelineEngineState.copy(
            tracks = tracks,
            durationMs = newTotalDuration,
            selectedClipId = clipId
        )

        _uiState.update {
            it.copy(
                project = updatedProject,
                selectedClipId = clipId
            )
        }

        scheduleAutosave(updatedProject)
    }

    override fun onCleared() {
        super.onCleared()
        flushAutosave()
        previewPlayer?.release()
    }

    companion object {
        fun provideFactory(
            savedStateHandle: SavedStateHandle,
            projectRepository: ProjectRepository,
            assetRepository: AssetRepository? = null,
            previewPlayer: PreviewPlayerController? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EditorViewModel(savedStateHandle, projectRepository, assetRepository, previewPlayer) as T
            }
        }
    }
}
