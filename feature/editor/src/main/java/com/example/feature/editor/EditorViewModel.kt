package com.example.feature.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.example.core.data.repository.AssetRepository
import com.example.core.data.repository.ProjectRepository
import com.example.core.media.PreviewPlayerController
import com.example.core.model.Asset
import com.example.core.model.Clip
import com.example.core.model.ClipType
import com.example.core.model.KeyframeProperty
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
import com.example.feature.editor.history.UpdateClipTransformCommand
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
            val visibleTracks = project.tracks.filter { it.isVisible }
            val videoClips = visibleTracks.filter { it.type == TrackType.VIDEO }.flatMap { it.clips }
            val audioClips = visibleTracks.filter { it.type == TrackType.AUDIO }.flatMap { it.clips }
            val overlayClips = visibleTracks.filter { it.type == TrackType.OVERLAY }.flatMap { it.clips }
            val allClips = project.tracks.flatMap { it.clips }
            val assetMap = _uiState.value.assets.toMutableMap()
            assetRepository?.let { repo ->
                allClips.forEach { clip ->
                    clip.assetId?.let { assetId ->
                        if (!assetMap.containsKey(assetId)) {
                            repo.getAssetById(assetId)?.let { asset ->
                                assetMap[asset.id] = asset
                            }
                        }
                    }
                }
            }
            _uiState.update { it.copy(assets = assetMap) }
            previewPlayer?.setClips(videoClips, assetMap)
            previewPlayer?.setAudioClips(audioClips, assetMap)
            previewPlayer?.setOverlayClips(overlayClips, assetMap)
        }
    }

    fun getOverlayPlayer(clipId: String): Player? = previewPlayer?.getOverlayPlayer(clipId)

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
                    EditorTool.FILTERS -> {
                        val targetClip = currentClipId?.let { id ->
                            _uiState.value.project?.tracks?.flatMap { it.clips }?.find { it.id == id }
                        }
                        val settings = targetClip?.let { com.example.feature.editor.filter.FilterSettingsMapper.fromEffects(it.effects) }
                            ?: com.example.feature.editor.filter.FilterSettings()
                        _uiState.update { it.copy(isFiltersSheetVisible = true, filterSettings = settings) }
                    }
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
                    EditorTool.TRANSITION -> {
                        val clips = _uiState.value.project?.tracks?.find { it.type == TrackType.VIDEO }?.clips?.sortedBy { it.startTimeMs } ?: emptyList()
                        val playhead = _uiState.value.playheadPositionMs
                        val activePair = clips.zipWithNext().find { (a, b) ->
                            playhead in a.startTimeMs..b.endTimeMs || kotlin.math.abs(a.endTimeMs - playhead) <= 500L
                        }
                        if (activePair != null) {
                            _uiState.update { it.copy(isTransitionSheetVisible = true, editingTransitionPair = activePair.first.id to activePair.second.id) }
                        }
                    }
                    EditorTool.MASK -> { _uiState.update { it.copy(isMaskSheetVisible = true) } }
                    EditorTool.BLEND -> { _uiState.update { it.copy(isBlendSheetVisible = true) } }
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
            is EditorEvent.SetCanvasSheetVisible -> {
                _uiState.update { it.copy(isCanvasSheetVisible = event.visible) }
            }
            is EditorEvent.SetTextSheetVisible -> {
                _uiState.update { it.copy(isTextSheetVisible = event.visible) }
            }
            is EditorEvent.SetFiltersSheetVisible -> {
                if (event.visible) {
                    val targetClip = _uiState.value.selectedClipId?.let { id ->
                        _uiState.value.project?.tracks?.flatMap { it.clips }?.find { it.id == id }
                    } ?: run {
                        val playhead = _uiState.value.playheadPositionMs
                        _uiState.value.project?.tracks?.flatMap { it.clips }?.find {
                            playhead >= it.startTimeMs && playhead <= it.endTimeMs
                        }
                    }
                    val settings = targetClip?.let { com.example.feature.editor.filter.FilterSettingsMapper.fromEffects(it.effects) }
                        ?: com.example.feature.editor.filter.FilterSettings()
                    _uiState.update { it.copy(isFiltersSheetVisible = true, filterSettings = settings) }
                } else {
                    _uiState.update { it.copy(isFiltersSheetVisible = false) }
                }
            }
            is EditorEvent.SetSpeedSheetVisible -> {
                _uiState.update { it.copy(isSpeedSheetVisible = event.visible) }
            }
            is EditorEvent.SetVolumeSheetVisible -> {
                _uiState.update { it.copy(isVolumeSheetVisible = event.visible) }
            }
            is EditorEvent.SetTransformSheetVisible -> {
                _uiState.update { it.copy(isTransformSheetVisible = event.visible) }
            }
            is EditorEvent.SetKeyframeSheetVisible -> {
                _uiState.update { it.copy(isKeyframeSheetVisible = event.visible) }
            }
            is EditorEvent.SetBeatsSheetVisible -> {
                _uiState.update { it.copy(isBeatsSheetVisible = event.visible) }
            }
            
            is EditorEvent.SplitSelectedClip -> onTimelineAction(TimelineAction.SplitAtPlayhead(_uiState.value.selectedClipId))
            is EditorEvent.DeleteSelectedClip -> {
                val clipId = _uiState.value.selectedClipId ?: return
                val groupId = _uiState.value.selectedClip?.groupId
                if (groupId != null) {
                    // Delete all clips in the same group
                    groupedClipIds(groupId).forEach { onTimelineAction(TimelineAction.DeleteClip(it)) }
                } else {
                    onTimelineAction(TimelineAction.DeleteClip(clipId))
                }
            }
            is EditorEvent.DuplicateSelectedClip -> {
                val clipId = _uiState.value.selectedClipId ?: return
                val groupId = _uiState.value.selectedClip?.groupId
                if (groupId != null) {
                    groupedClipIds(groupId).forEach { onTimelineAction(TimelineAction.DuplicateClip(it)) }
                } else {
                    onTimelineAction(TimelineAction.DuplicateClip(clipId))
                }
            }

            is EditorEvent.ApplyTextClip -> {
                val currentSelected = _uiState.value.selectedClip
                if (currentSelected?.type == ClipType.TEXT && currentSelected.textData != null) {
                    updateTextClip(
                        clipId = currentSelected.id,
                        text = event.text,
                        fontSize = event.fontSize,
                        color = event.color,
                        fontFamily = event.fontFamily,
                        alignment = event.alignment
                    )
                } else {
                    addTextClip(
                        text = event.text,
                        fontSize = event.fontSize,
                        color = event.color,
                        fontFamily = event.fontFamily,
                        alignment = event.alignment
                    )
                }
                _uiState.update { it.copy(isTextSheetVisible = false) }
            }
            is EditorEvent.ChangeAspectRatio -> {
                val proj = _uiState.value.project
                if (proj != null) {
                    val (w, h) = when (event.ratio) {
                        com.example.core.model.AspectRatio.RATIO_9_16 -> 1080 to 1920
                        com.example.core.model.AspectRatio.RATIO_16_9 -> 1920 to 1080
                        com.example.core.model.AspectRatio.RATIO_1_1 -> 1080 to 1080
                        com.example.core.model.AspectRatio.RATIO_4_5 -> 1080 to 1350
                    }
                    val updated = proj.copy(
                        aspectRatio = event.ratio,
                        width = w,
                        height = h,
                        updatedAt = System.currentTimeMillis()
                    )
                    _uiState.update { it.copy(project = updated) }
                    scheduleAutosave(updated)
                }
            }
            is EditorEvent.ChangeClipSpeed -> {
                val clipId = _uiState.value.selectedClipId ?: return
                val groupId = _uiState.value.selectedClip?.groupId
                if (groupId != null) {
                    groupedClipIds(groupId).forEach { onTimelineAction(TimelineAction.UpdateClipSpeed(it, event.speed)) }
                } else {
                    onTimelineAction(TimelineAction.UpdateClipSpeed(clipId, event.speed))
                }
                previewPlayer?.setPlaybackSpeed(event.speed)
            }
            is EditorEvent.ChangeClipTransform -> {
                val clipId = event.clipId ?: _uiState.value.selectedClipId ?: return
                val groupId = _uiState.value.selectedClip?.groupId
                if (groupId != null && event.clipId == null) {
                    // Propagate transform (position/scale/rotation/opacity) to all group members
                    groupedClipIds(groupId).forEach { onTimelineAction(TimelineAction.UpdateClipTransform(it, event.transform)) }
                } else {
                    onTimelineAction(TimelineAction.UpdateClipTransform(clipId, event.transform))
                }
            }
            is EditorEvent.ChangeClipVolume -> {
                val clipId = _uiState.value.selectedClipId ?: return
                val groupId = _uiState.value.selectedClip?.groupId
                if (groupId != null) {
                    groupedClipIds(groupId).forEach { onTimelineAction(TimelineAction.UpdateClipVolume(it, event.volume)) }
                } else {
                    onTimelineAction(TimelineAction.UpdateClipVolume(clipId, event.volume))
                }
                previewPlayer?.setVolume(event.volume)
            }

            is EditorEvent.UndoClicked -> {
                val restored = undoRedoManager.undo(timelineEngineState)
                if (restored != null) {
                    applyEngineState(restored)
                }
            }
            is EditorEvent.RedoClicked -> {
                val restored = undoRedoManager.redo(timelineEngineState)
                if (restored != null) {
                    applyEngineState(restored)
                }
            }
            is EditorEvent.CloseToolPanel -> {}
            is EditorEvent.UpdateFilterSettings -> {
                _uiState.update { it.copy(filterSettings = event.filterSettings) }
                val targetClipId = _uiState.value.selectedClipId ?: run {
                    val playhead = _uiState.value.playheadPositionMs
                    _uiState.value.project?.tracks?.flatMap { it.clips }?.find {
                        playhead >= it.startTimeMs && playhead <= it.endTimeMs
                    }?.id
                }
                if (targetClipId != null) {
                    val effects = com.example.feature.editor.filter.FilterSettingsMapper.toEffects(event.filterSettings, targetClipId)
                    onTimelineAction(TimelineAction.UpdateClipEffects(targetClipId, effects))
                }
            }
            is EditorEvent.ResetFilterSettings -> {
                val defaultSettings = com.example.feature.editor.filter.FilterSettings()
                _uiState.update { it.copy(filterSettings = defaultSettings) }
                val targetClipId = _uiState.value.selectedClipId ?: run {
                    val playhead = _uiState.value.playheadPositionMs
                    _uiState.value.project?.tracks?.flatMap { it.clips }?.find {
                        playhead >= it.startTimeMs && playhead <= it.endTimeMs
                    }?.id
                }
                if (targetClipId != null) {
                    onTimelineAction(TimelineAction.UpdateClipEffects(targetClipId, emptyList()))
                }
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
            is EditorEvent.LongPressClip -> {
                // Alight Motion-style: long-press toggles the clip into/out of multi-selection
                onTimelineAction(TimelineAction.ToggleClipSelection(event.clipId))
                _uiState.update { it.copy(multiSelectedClipIds = timelineEngineState.multiSelectedClipIds) }
            }
            is EditorEvent.GroupSelectedClips -> {
                val groupId = UUID.randomUUID().toString()
                onTimelineAction(TimelineAction.GroupSelectedClips(groupId))
                _uiState.update { it.copy(multiSelectedClipIds = emptySet()) }
            }
            is EditorEvent.UngroupSelectedClips -> {
                val gid = _uiState.value.selectedGroupId ?: return
                onTimelineAction(TimelineAction.UngroupClips(gid))
                _uiState.update { it.copy(multiSelectedClipIds = emptySet()) }
            }
            is EditorEvent.MoveKeyframe -> {
                onTimelineAction(TimelineAction.MoveKeyframe(event.clipId, event.keyframeId, event.newTimeMs))
            }
            is EditorEvent.AddKeyframe -> {
                onTimelineAction(TimelineAction.AddKeyframe(event.clipId, event.property, event.timeMs, event.value, event.interpolation))
            }
            is EditorEvent.UpdateKeyframe -> {
                onTimelineAction(TimelineAction.UpdateKeyframe(event.clipId, event.keyframeId, event.value, event.interpolation))
            }
            is EditorEvent.DeleteKeyframe -> {
                onTimelineAction(TimelineAction.DeleteKeyframe(event.clipId, event.keyframeId))
            }
            is EditorEvent.SetActiveKeyframeProperty -> {
                _uiState.update { it.copy(activeKeyframeProperty = event.property) }
            }
            is EditorEvent.ToggleKeyframeAtPlayhead -> {
                handleToggleKeyframeAtPlayhead()
            }
            is EditorEvent.OpenTransitionEditor -> {
                _uiState.update { it.copy(isTransitionSheetVisible = true, editingTransitionPair = event.firstClipId to event.secondClipId) }
            }
            is EditorEvent.SetTransitionSheetVisible -> {
                _uiState.update { it.copy(isTransitionSheetVisible = event.visible) }
            }
            is EditorEvent.ApplyTransition -> {
                onTimelineAction(TimelineAction.AddTransition(event.transition))
            }
            is EditorEvent.RemoveTransition -> {
                onTimelineAction(TimelineAction.RemoveTransition(event.transitionId))
            }
            is EditorEvent.SetMaskSheetVisible -> {
                _uiState.update { it.copy(isMaskSheetVisible = event.visible) }
            }
            is EditorEvent.SetBlendSheetVisible -> {
                _uiState.update { it.copy(isBlendSheetVisible = event.visible) }
            }
            is EditorEvent.ChangeClipMask -> {
                val clipId = _uiState.value.selectedClipId
                if (clipId != null) {
                    onTimelineAction(TimelineAction.SetClipMask(clipId, event.mask))
                }
            }
            is EditorEvent.ChangeClipBlendMode -> {
                val clipId = _uiState.value.selectedClipId
                if (clipId != null) {
                    onTimelineAction(TimelineAction.SetClipBlendMode(clipId, event.blendMode))
                }
            }
            is EditorEvent.ChangeClipOpacity -> {
                val clipId = _uiState.value.selectedClipId
                if (clipId != null) {
                    onTimelineAction(TimelineAction.SetClipOpacity(clipId, event.opacity))
                }
            }
            is EditorEvent.AddClipEffect -> {
                val clipId = _uiState.value.selectedClipId
                if (clipId != null) {
                    onTimelineAction(TimelineAction.AddClipEffect(clipId, event.effect))
                }
            }
            is EditorEvent.RemoveClipEffect -> {
                val clipId = _uiState.value.selectedClipId
                if (clipId != null) {
                    onTimelineAction(TimelineAction.RemoveClipEffect(clipId, event.effectId))
                }
            }
            else -> {}
        }
    }


    private fun applyEngineState(newState: TimelineEngineState) {
        timelineEngineState = newState
        val currentProject = _uiState.value.project ?: return
        val updatedTracks = newState.tracks
        val updatedDuration = newState.durationMs
        val updatedProject = currentProject.copy(
            tracks = updatedTracks,
            durationMs = updatedDuration,
            updatedAt = System.currentTimeMillis()
        )
        val activeEffects = newState.selectedClipId?.let { id ->
            updatedTracks.flatMap { it.clips }.find { it.id == id }?.effects
        }
        val restoredFilterSettings = if (activeEffects != null && activeEffects.isNotEmpty()) {
            com.example.feature.editor.filter.FilterSettingsMapper.fromEffects(activeEffects)
        } else {
            com.example.feature.editor.filter.FilterSettings()
        }
        _uiState.update { current ->
            current.copy(
                playheadPositionMs = newState.playheadPositionMs,
                selectedClipId = newState.selectedClipId,
                multiSelectedClipIds = newState.multiSelectedClipIds,
                beatMarkers = newState.beatMarkers,
                project = updatedProject,
                filterSettings = restoredFilterSettings
            )
        }
        scheduleAutosave(updatedProject)
        syncClipsToPlayer(updatedProject)
        previewPlayer?.seekTo(newState.playheadPositionMs)
    }

    /**
     * Returns all clip IDs that share the given groupId across all project tracks.
     * Used to propagate edits (speed, volume, transform, delete, duplicate) to grouped clips.
     */
    private fun groupedClipIds(groupId: String): List<String> =
        _uiState.value.project?.tracks
            ?.flatMap { it.clips }
            ?.filter { it.groupId == groupId }
            ?.map { it.id }
            ?: emptyList()

    private fun handleToggleKeyframeAtPlayhead() {
        val state = _uiState.value
        val clip = state.selectedClip ?: run {
            val playhead = state.playheadPositionMs
            val found = state.project?.tracks?.flatMap { it.clips }?.find {
                playhead in it.startTimeMs..it.endTimeMs
            }
            if (found != null) {
                onTimelineAction(TimelineAction.SelectClip(found.id))
            }
            found
        } ?: return

        val playhead = state.playheadPositionMs
        val property = state.activeKeyframeProperty

        // Check if there is an existing keyframe within 50ms tolerance at the playhead
        // Prioritize keyframe matching activeProperty, otherwise any keyframe at playhead
        val existingKf = clip.keyframes.find {
            it.property == property && kotlin.math.abs(it.timeMs - playhead) <= 50L
        } ?: clip.keyframes.find {
            kotlin.math.abs(it.timeMs - playhead) <= 50L
        }

        if (existingKf != null) {
            onTimelineAction(TimelineAction.DeleteKeyframe(clip.id, existingKf.id))
        } else {
            val currentValue = when (property) {
                KeyframeProperty.POSITION_X -> clip.transform.x
                KeyframeProperty.POSITION_Y -> clip.transform.y
                KeyframeProperty.SCALE_X -> clip.transform.scaleX
                KeyframeProperty.SCALE_Y -> clip.transform.scaleY
                KeyframeProperty.ROTATION -> clip.transform.rotation
                KeyframeProperty.OPACITY -> clip.transform.opacity
                KeyframeProperty.VOLUME -> clip.volume ?: 1.0f
                KeyframeProperty.BRIGHTNESS -> clip.effects.find { it.type == com.example.core.model.EffectType.BRIGHTNESS }?.parameters?.get("brightness") ?: 0f
                KeyframeProperty.CONTRAST -> clip.effects.find { it.type == com.example.core.model.EffectType.CONTRAST }?.parameters?.get("contrast") ?: 1f
                KeyframeProperty.SATURATION -> clip.effects.find { it.type == com.example.core.model.EffectType.SATURATION }?.parameters?.get("saturation") ?: 1f
                KeyframeProperty.EXPOSURE -> clip.effects.find { it.type == com.example.core.model.EffectType.EXPOSURE }?.parameters?.get("exposure") ?: 0f
                KeyframeProperty.TEMPERATURE -> clip.effects.find { it.type == com.example.core.model.EffectType.TEMPERATURE }?.parameters?.get("temperature") ?: 0f
                KeyframeProperty.TINT -> clip.effects.find { it.type == com.example.core.model.EffectType.TINT }?.parameters?.get("tint") ?: 0f
                KeyframeProperty.HIGHLIGHTS -> clip.effects.find { it.type == com.example.core.model.EffectType.HIGHLIGHTS }?.parameters?.get("highlights") ?: 0f
                KeyframeProperty.SHADOWS -> clip.effects.find { it.type == com.example.core.model.EffectType.SHADOWS }?.parameters?.get("shadows") ?: 0f
                KeyframeProperty.MASK_X -> clip.mask?.x ?: 0.5f
                KeyframeProperty.MASK_Y -> clip.mask?.y ?: 0.5f
                KeyframeProperty.MASK_FEATHER -> clip.mask?.feather ?: 0f
                else -> 0f
            }
            val clampedTime = playhead.coerceIn(clip.startTimeMs, clip.endTimeMs)
            onTimelineAction(
                TimelineAction.AddKeyframe(
                    clipId = clip.id,
                    property = property,
                    timeMs = clampedTime,
                    value = currentValue
                )
            )
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
                val playhead = preActionState.playheadPositionMs
                val targetClipId = action.clipId
                val clipId = if (targetClipId != null) {
                    val c = preActionState.findClip(targetClipId)
                    if (c != null && playhead >= c.startTimeMs && playhead <= c.endTimeMs) targetClipId
                    else preActionState.tracks.flatMap { it.clips }.find { playhead >= it.startTimeMs && playhead < it.endTimeMs }?.id
                } else {
                    val sel = preActionState.selectedClipId?.let { preActionState.findClip(it) }
                    if (sel != null && playhead >= sel.startTimeMs && playhead <= sel.endTimeMs) sel.id
                    else preActionState.tracks.flatMap { it.clips }.find { playhead >= it.startTimeMs && playhead < it.endTimeMs }?.id
                }
                if (clipId != null) {
                    SplitClipCommand(clipId, playhead, preActionState)
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
            is TimelineAction.UpdateClipSpeed -> {
                StateSnapshotCommand("Change clip speed", action, preActionState)
            }
            is TimelineAction.UpdateClipVolume -> {
                StateSnapshotCommand("Change clip volume", action, preActionState)
            }
            is TimelineAction.GroupSelectedClips -> {
                StateSnapshotCommand("Group clips", action, preActionState)
            }
            is TimelineAction.UngroupClips -> {
                StateSnapshotCommand("Ungroup clips", action, preActionState)
            }
            is TimelineAction.AddKeyframe -> {
                StateSnapshotCommand("Add keyframe", action, preActionState)
            }
            is TimelineAction.UpdateKeyframe -> {
                StateSnapshotCommand("Update keyframe", action, preActionState)
            }
            is TimelineAction.DeleteKeyframe -> {
                StateSnapshotCommand("Delete keyframe", action, preActionState)
            }
            is TimelineAction.MoveKeyframe -> {
                StateSnapshotCommand("Move keyframe", action, preActionState)
            }
            is TimelineAction.UpdateClipTransform -> {
                UpdateClipTransformCommand(action.clipId, action.transform, preActionState)
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
            val selectedClipEffects = timelineEngineState.selectedClipId?.let { id ->
                updatedTracks.flatMap { it.clips }.find { it.id == id }?.effects
            }
            val syncedFilterSettings = if (action is TimelineAction.SelectClip) {
                if (selectedClipEffects != null && selectedClipEffects.isNotEmpty()) {
                    com.example.feature.editor.filter.FilterSettingsMapper.fromEffects(selectedClipEffects)
                } else {
                    com.example.feature.editor.filter.FilterSettings()
                }
            } else {
                current.filterSettings
            }
            current.copy(
                playheadPositionMs = timelineEngineState.playheadPositionMs,
                selectedClipId = timelineEngineState.selectedClipId,
                multiSelectedClipIds = timelineEngineState.multiSelectedClipIds,
                beatMarkers = timelineEngineState.beatMarkers,
                isEditSheetVisible = sheetVisible,
                filterSettings = syncedFilterSettings,
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
        val preState = timelineEngineState

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
                        val newTrackType = if (mediaList.first().second.type == com.example.core.model.ClipType.AUDIO) {
                            TrackType.AUDIO
                        } else if (tracks.any { it.type == TrackType.VIDEO }) {
                            TrackType.OVERLAY
                        } else {
                            TrackType.VIDEO
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
                    val isOverlayTrack = targetTrack.type == TrackType.OVERLAY
                    var currentEnd = if (isOverlayTrack) {
                        mediaList.firstOrNull()?.second?.startTimeMs ?: 0L
                    } else {
                        targetTrack.clips.maxOfOrNull { it.endTimeMs } ?: 0L
                    }
                    
                    val positionedClips = mediaList.map { (asset, clip) ->
                        val start = if (isOverlayTrack) clip.startTimeMs else currentEnd
                        val positionedClip = clip.copy(
                            startTimeMs = start,
                            assetId = asset.id
                        )
                        currentEnd = start + positionedClip.durationMs
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

                // Update in-memory state immediately so UI and Player are instantly responsive
                val newAssetsMap = _uiState.value.assets + assetsAndClips.map { it.first.id to it.first }
                _uiState.update {
                    it.copy(
                        project = updatedProject,
                        assets = newAssetsMap
                    )
                }
                timelineEngineState = timelineEngineState.copy(
                    tracks = tracks,
                    durationMs = newTotalDuration
                )
                undoRedoManager.recordStateChange("Add media", preState, timelineEngineState)

                val visibleTracks = tracks.filter { it.isVisible }
                val videoClips = visibleTracks.filter { it.type == TrackType.VIDEO }.flatMap { it.clips }
                val audioClips = visibleTracks.filter { it.type == TrackType.AUDIO }.flatMap { it.clips }
                previewPlayer?.setClips(videoClips, newAssetsMap)
                previewPlayer?.setAudioClips(audioClips, newAssetsMap)

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

        val preState = timelineEngineState

        timelineEngineState = timelineEngineState.copy(
            tracks = tracks,
            durationMs = newTotalDuration,
            selectedClipId = clipId
        )

        undoRedoManager.recordStateChange("Add text clip", preState, timelineEngineState)

        _uiState.update {
            it.copy(
                project = updatedProject,
                selectedClipId = clipId
            )
        }

        scheduleAutosave(updatedProject)
    }

    /**
     * Updates an existing TextClip's text styling and content (DEV-062, DEV-063).
     */
    fun updateTextClip(
        clipId: String,
        text: String,
        fontSize: Float,
        color: String,
        fontFamily: String,
        alignment: String
    ) {
        val currentProject = _uiState.value.project ?: return
        val tracks = currentProject.tracks.map { track ->
            if (track.type == TrackType.TEXT) {
                val updatedClips = track.clips.map { clip ->
                    if (clip.id == clipId) {
                        val updatedData = clip.textData?.copy(
                            text = text,
                            fontSize = fontSize,
                            textColor = color,
                            fontFamily = fontFamily,
                            alignment = alignment
                        ) ?: com.example.core.model.TextClipData(
                            clipId = clip.id,
                            text = text,
                            fontSize = fontSize,
                            textColor = color,
                            fontFamily = fontFamily,
                            alignment = alignment
                        )
                        clip.copy(textData = updatedData)
                    } else clip
                }
                track.copy(clips = updatedClips)
            } else track
        }

        val preState = timelineEngineState
        val updatedProject = currentProject.copy(
            tracks = tracks,
            updatedAt = System.currentTimeMillis()
        )
        timelineEngineState = timelineEngineState.copy(tracks = tracks)
        undoRedoManager.recordStateChange("Update text clip", preState, timelineEngineState)
        _uiState.update { it.copy(project = updatedProject) }
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
