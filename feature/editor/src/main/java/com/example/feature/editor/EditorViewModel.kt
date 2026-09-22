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
                    EditorTool.EDIT -> { _uiState.update { it.copy(isEditSheetVisible = true) } }
                    EditorTool.AUDIO -> { _uiState.update { it.copy(isVolumeSheetVisible = true) } }
                    EditorTool.TEXT -> { _uiState.update { it.copy(isTextSheetVisible = true) } }
                    EditorTool.FILTERS -> { _uiState.update { it.copy(isFiltersSheetVisible = true) } }
                    EditorTool.SPLIT -> {
                        onTimelineAction(TimelineAction.SplitAtPlayhead(currentClipId))
                    }
                    EditorTool.SPEED -> { _uiState.update { it.copy(isSpeedSheetVisible = true) } }
                    EditorTool.VOLUME -> { _uiState.update { it.copy(isVolumeSheetVisible = true) } }
                    EditorTool.CANVAS -> { _uiState.update { it.copy(isCanvasSheetVisible = true) } }
                    EditorTool.KEYFRAME -> { _uiState.update { it.copy(isKeyframeSheetVisible = true) } }
                    EditorTool.BEATS -> {
                        // Tapping BEATS toggles beat marker at playhead!
                        onTimelineAction(TimelineAction.ToggleBeatMarker(_uiState.value.playheadPositionMs))
                    }
                    EditorTool.TRANSFORM -> { _uiState.update { it.copy(isTransformSheetVisible = true) } }
                    EditorTool.DELETE -> {
                        currentClipId?.let { onTimelineAction(TimelineAction.DeleteClip(it)) }
                    }
                    else -> {
                        _uiState.update { it.copy(isEditSheetVisible = true) }
                    }
                }
            }
            is EditorEvent.SetEditSheetVisible -> {
                _uiState.update { it.copy(isEditSheetVisible = event.visible) }
            }

            
            is EditorEvent.SplitSelectedClip -> onTimelineAction(TimelineAction.SplitAtPlayhead(_uiState.value.selectedClipId))
            is EditorEvent.DeleteSelectedClip -> _uiState.value.selectedClipId?.let { onTimelineAction(TimelineAction.DeleteClip(it)) }
            is EditorEvent.DuplicateSelectedClip -> _uiState.value.selectedClipId?.let { onTimelineAction(TimelineAction.DuplicateClip(it)) }
            is EditorEvent.ApplyTextClip -> {
                _uiState.update { it.copy(isTextSheetVisible = false) }
            }
            is EditorEvent.ChangeAspectRatio -> {
                val proj = _uiState.value.project
                if (proj != null) {
                    val updated = proj.copy(aspectRatio = event.ratio)
                    _uiState.update { it.copy(project = updated) }
                }
            }
            is EditorEvent.ChangeClipSpeed -> {}
            is EditorEvent.ChangeClipTransform -> {}
            is EditorEvent.ChangeClipVolume -> {}
            is EditorEvent.CloseToolPanel -> {}
            is EditorEvent.UpdateFilterSettings -> {}
            is EditorEvent.ResetFilterSettings -> {}
            is EditorEvent.AddMediaClicked -> {}

            is EditorEvent.SaveImmediately -> {
                flushAutosave()
            }

            is EditorEvent.AddMediaClicked -> {
                // Handled via onNavigateMediaPicker
            }

            is EditorEvent.ExportClicked -> {
                flushAutosave()
            }
            else -> {}
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
                is TimelineAction.SelectClip -> false
                is TimelineAction.DeleteClip -> false
                is TimelineAction.SplitAtPlayhead, is TimelineAction.SplitClip, is TimelineAction.DuplicateClip -> false
                else -> current.isEditSheetVisible && timelineEngineState.selectedClipId != null
            }
            current.copy(
                playheadPositionMs = timelineEngineState.playheadPositionMs,
                selectedClipId = timelineEngineState.selectedClipId,
                beatMarkers = timelineEngineState.beatMarkers,
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

                // We group incoming clips by trackId so we can append them to the correct tracks.
                val tracks = currentProject.tracks.toMutableList()
                val newThumbnail = currentProject.thumbnailPath ?: assetsAndClips.firstOrNull()?.first?.thumbnailPath

                val trackUpdates = assetsAndClips.groupBy { it.second.trackId }
                
                trackUpdates.forEach { (trackId, mediaList) ->
                    var trackIndex = tracks.indexOfFirst { it.id == trackId }
                    if (trackIndex < 0) {
                        // Create the track if it doesn't exist
                        val newTrackType = mediaList.first().second.type.let {
                            if (it == com.example.core.model.ClipType.AUDIO) TrackType.AUDIO
                            else if (it == com.example.core.model.ClipType.IMAGE || it == com.example.core.model.ClipType.TEXT) TrackType.OVERLAY
                            else TrackType.VIDEO
                        }
                        val newTrack = Track(
                            id = trackId,
                            projectId = currentProject.id,
                            type = newTrackType,
                            order = tracks.size
                        )
                        tracks.add(newTrack)
                        trackIndex = tracks.size - 1
                    }

                    val targetTrack = tracks[trackIndex]
                    var currentEnd = targetTrack.clips.maxOfOrNull { it.endTimeMs } ?: 0L
                    
                    val positionedClips = mediaList.map { (asset, clip) ->
                        val positionedClip = clip.copy(
                            startTimeMs = currentEnd,
                            assetId = asset.id
                        )
                        currentEnd += positionedClip.durationMs
                        positionedClip
                    }
                    
                    tracks[trackIndex] = targetTrack.copy(
                        clips = targetTrack.clips + positionedClips
                    )
                }

                val newTotalDuration = tracks.maxOfOrNull { t -> t.clips.maxOfOrNull { it.endTimeMs } ?: 0L } ?: 0L
                
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
