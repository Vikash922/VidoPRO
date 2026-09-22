package com.example.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.common.TimeUtils
import com.example.core.model.ClipType
import com.example.core.ui.components.LoadingView
import com.example.core.ui.theme.AppSpacing
import com.example.core.ui.theme.EditorColors

import com.example.feature.timeline.engine.TimelineEngineState
import com.example.feature.timeline.engine.TimelineAction
import com.example.feature.timeline.ui.TimelineContainer
import com.example.core.model.TrackType
import androidx.media3.common.Player

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    uiState: EditorUiState,
    player: Player? = null,
    onEvent: (EditorEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateExport: (projectId: String) -> Unit,
    onNavigateMediaPicker: (TrackType) -> Unit = {},
    onTimelineAction: (TimelineAction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Resolution Dropdown
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("1080P", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                        }
                    }
                    
                    // Export Button
                    Button(
                        onClick = onNavigateToExport,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Export", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingView(modifier = Modifier.fillMaxSize().padding(innerPadding))
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Preview Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // Placeholder for actual video preview player
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            // Timeline Controls Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EditorColors.timelineBackground)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${TimeUtils.formatDuration(uiState.playheadPositionMs)} / ${TimeUtils.formatDuration(uiState.durationMs)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (uiState.isPlaying) onEvent(EditorEvent.PlayPauseClickedPauseClicked) else onEvent(EditorEvent.PlayPauseClicked) }
                    ) {
                        Icon(
                            if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { onEvent(EditorEvent.UndoClicked) }, enabled = uiState.canUndo) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = if(uiState.canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onEvent(EditorEvent.RedoClicked) }, enabled = uiState.canRedo) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = if(uiState.canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { /* Fullscreen */ }) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Timeline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(EditorColors.timelineBackground)
            ) {
                if (uiState.project != null) {
                    val timelineEngineState = androidx.compose.runtime.remember(uiState.project, uiState.playheadPositionMs, uiState.selectedClipId, uiState.durationMs) {
                        com.example.feature.timeline.engine.TimelineEngineState(
                            tracks = uiState.project?.tracks ?: emptyList(),
                            playheadPositionMs = uiState.playheadPositionMs,
                            durationMs = uiState.durationMs,
                            selectedClipId = uiState.selectedClipId
                        )
                    }

                    com.example.feature.timeline.ui.TimelineContainer(
                        state = timelineEngineState,
                        isPlaying = uiState.isPlaying,
                        onAction = onTimelineAction,
                        onPlayPause = { onEvent(EditorEvent.PlayPauseClicked) },
                        onAddMedia = { onNavigateMediaPicker(com.example.core.model.TrackType.VIDEO) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            // Bottom Toolbar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(vertical = 12.dp)
            ) {
                val scrollState = rememberScrollState()
                
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (uiState.selectedClipId == null) {
                        // Main Tools
                        EditorToolButton(EditorTool.EDIT, Icons.Default.ContentCut) { onEvent(EditorEvent.ToolClicked(EditorTool.EDIT)) }
                        EditorToolButton(EditorTool.AUDIO, Icons.Default.Audiotrack) { onEvent(EditorEvent.ToolClicked(EditorTool.AUDIO)) }
                        EditorToolButton(EditorTool.TEXT, Icons.Default.Title) { onEvent(EditorEvent.ToolClicked(EditorTool.TEXT)) }
                        EditorToolButton(EditorTool.OVERLAY, Icons.Default.Layers) { onEvent(EditorEvent.ToolClicked(EditorTool.OVERLAY)) }
                        EditorToolButton(EditorTool.EFFECTS, Icons.Default.AutoFixHigh) { onEvent(EditorEvent.ToolClicked(EditorTool.EFFECTS)) }
                        EditorToolButton(EditorTool.FILTERS, Icons.Default.Brush) { onEvent(EditorEvent.ToolClicked(EditorTool.FILTERS)) }
                        EditorToolButton(EditorTool.ADJUST, Icons.Default.Settings) { onEvent(EditorEvent.ToolClicked(EditorTool.ADJUST)) }
                        EditorToolButton(EditorTool.HSL, Icons.Default.Edit) { onEvent(EditorEvent.ToolClicked(EditorTool.HSL)) }
                        EditorToolButton(EditorTool.AI, Icons.Default.Star) { onEvent(EditorEvent.ToolClicked(EditorTool.AI)) }
                    } else {
                        // Clip Specific Tools
                        val clip = uiState.selectedClip
                        EditorToolButton(EditorTool.SPLIT, Icons.Default.CallSplit) { onEvent(EditorEvent.ToolClicked(EditorTool.SPLIT)) }
                        if (clip?.type == ClipType.VIDEO) {
                            EditorToolButton(EditorTool.SPEED, Icons.Default.PlayArrow) { onEvent(EditorEvent.ToolClicked(EditorTool.SPEED)) }
                        }
                        if (clip?.type == ClipType.VIDEO || clip?.type == ClipType.AUDIO) {
                            EditorToolButton(EditorTool.VOLUME, Icons.Default.VolumeUp) { onEvent(EditorEvent.ToolClicked(EditorTool.VOLUME)) }
                        }
                        EditorToolButton(EditorTool.ANIMATION, Icons.Default.Build) { onEvent(EditorEvent.ToolClicked(EditorTool.ANIMATION)) }
                        EditorToolButton(EditorTool.DELETE, Icons.Default.Delete) { onEvent(EditorEvent.ToolClicked(EditorTool.DELETE)) }
                        
                        EditorToolButton(EditorTool.MASK, Icons.Default.Layers) { onEvent(EditorEvent.ToolClicked(EditorTool.MASK)) }
                        EditorToolButton(EditorTool.BLEND, Icons.Default.ViewHeadline) { onEvent(EditorEvent.ToolClicked(EditorTool.BLEND)) }
                        EditorToolButton(EditorTool.TRANSFORM, Icons.Default.Refresh) { onEvent(EditorEvent.ToolClicked(EditorTool.TRANSFORM)) }
                        EditorToolButton(EditorTool.KEYFRAME, Icons.Default.Star) { onEvent(EditorEvent.ToolClicked(EditorTool.KEYFRAME)) }
                        EditorToolButton(EditorTool.BEATS, Icons.Default.Share) { onEvent(EditorEvent.ToolClicked(EditorTool.BEATS)) }
                    }
                }
            }
        }

        // Bottom Sheets (Omitted for brevity, but they will be mapped here)
                if (uiState.isEditSheetVisible && uiState.selectedClip != null) { 
            EditBottomSheet(
                clip = uiState.selectedClip!!,
                playheadPositionMs = uiState.playheadPositionMs,
                onDismiss = { onEvent(EditorEvent.SetEditSheetVisible(false)) }, 
                onSplit = { onEvent(EditorEvent.ToolClicked(EditorTool.SPLIT)) },
                onDuplicate = { onEvent(EditorEvent.DuplicateSelectedClip) },
                onDelete = { onEvent(EditorEvent.DeleteSelectedClip) },
                onSpeed = { onEvent(EditorEvent.ToolClicked(EditorTool.SPEED)) },
                onVolume = { onEvent(EditorEvent.ToolClicked(EditorTool.VOLUME)) },
                onAudio = { onEvent(EditorEvent.ToolClicked(EditorTool.AUDIO)) },
                onText = { onEvent(EditorEvent.ToolClicked(EditorTool.TEXT)) },
                onOverlay = { onEvent(EditorEvent.ToolClicked(EditorTool.OVERLAY)) },
                onFilters = { onEvent(EditorEvent.ToolClicked(EditorTool.FILTERS)) },
                onTransform = { onEvent(EditorEvent.ToolClicked(EditorTool.TRANSFORM)) },
                onCanvas = { onEvent(EditorEvent.ToolClicked(EditorTool.CANVAS)) },
                onKeyframe = { onEvent(EditorEvent.ToolClicked(EditorTool.KEYFRAME)) },
                onBeats = { onEvent(EditorEvent.ToolClicked(EditorTool.BEATS)) }
            ) 
        }
        if (uiState.isKeyframeSheetVisible) { KeyframeBottomSheet(onDismiss = { onEvent(EditorEvent.SetKeyframeSheetVisible(false)) }) }
        if (uiState.isBeatsSheetVisible) { BeatsBottomSheet(onDismiss = { onEvent(EditorEvent.SetBeatsSheetVisible(false)) }) }
        if (uiState.isSpeedSheetVisible) { SpeedBottomSheet(currentSpeed = 1.0f, onSpeedChanged = {}, onDismiss = { onEvent(EditorEvent.SetSpeedSheetVisible(false)) }) }
        if (uiState.isTransformSheetVisible) { TransformBottomSheet(currentTransform = com.example.core.model.Transform.DEFAULT, onTransformChanged = {}, onDismiss = { onEvent(EditorEvent.SetTransformSheetVisible(false)) }) }
        if (uiState.isVolumeSheetVisible) { VolumeBottomSheet(currentVolume = 1.0f, onVolumeChanged = {}, onDismiss = { onEvent(EditorEvent.SetVolumeSheetVisible(false)) }) }
        if (uiState.isCanvasSheetVisible) { CanvasBottomSheet(currentRatio = com.example.core.model.AspectRatio.RATIO_9_16, onRatioSelected = {}, onDismiss = { onEvent(EditorEvent.SetCanvasSheetVisible(false)) }) }
    }
}

@Composable
fun EditorToolButton(tool: EditorTool, icon: ImageVector, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(imageVector = icon, contentDescription = tool.label, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = tool.label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
