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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.common.TimeUtils
import com.example.core.model.ClipType
import com.example.core.ui.components.LoadingView
import com.example.feature.timeline.engine.TimelineAction
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
    val bgColor = Color(0xFF0F111A)
    val cardColor = Color(0xFF161925)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = bgColor,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp).clickable { onNavigateBack() }
                )
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("1080P", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF6B4BFF), Color(0xFF9E84FF))))
                            .clickable { uiState.project?.id?.let { onNavigateExport(it) } }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Export", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
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
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.White.copy(alpha = 0.2f)
                )
            }

            // Timeline Controls Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${TimeUtils.formatDuration(uiState.playheadPositionMs)} / ${TimeUtils.formatDuration(uiState.durationMs)}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp).clickable { onEvent(EditorEvent.PlayPauseClicked) }
                )
                
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if(uiState.canUndo) Color.White else Color.White.copy(alpha=0.3f),
                        modifier = Modifier.size(20.dp).clickable(enabled = uiState.canUndo) { onEvent(EditorEvent.UndoClicked) }
                    )
                    Spacer(Modifier.width(16.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if(uiState.canRedo) Color.White else Color.White.copy(alpha=0.3f),
                        modifier = Modifier.size(20.dp).clickable(enabled = uiState.canRedo) { onEvent(EditorEvent.RedoClicked) }
                    )
                    Spacer(Modifier.width(16.dp))
                    Icon(
                        Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Timeline Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(Color(0xFF0F111A))
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
                    .background(bgColor)
                    .padding(vertical = 12.dp)
            ) {
                val scrollState = rememberScrollState()
                
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState).padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    if (uiState.selectedClipId == null) {
                        EditorToolButton(EditorTool.EDIT, Icons.Default.ContentCut) { onEvent(EditorEvent.ToolClicked(EditorTool.EDIT)) }
                        EditorToolButton(EditorTool.AUDIO, Icons.Default.Audiotrack) { onEvent(EditorEvent.ToolClicked(EditorTool.AUDIO)) }
                        EditorToolButton(EditorTool.TEXT, Icons.Default.Title) { onEvent(EditorEvent.ToolClicked(EditorTool.TEXT)) }
                        EditorToolButton(EditorTool.OVERLAY, Icons.Default.Layers) { onEvent(EditorEvent.ToolClicked(EditorTool.OVERLAY)) }
                        EditorToolButton(EditorTool.EFFECTS, Icons.Default.AutoFixHigh) { onEvent(EditorEvent.ToolClicked(EditorTool.EFFECTS)) }
                        EditorToolButton(EditorTool.FILTERS, Icons.Default.CameraFilter) { onEvent(EditorEvent.ToolClicked(EditorTool.FILTERS)) }
                    } else {
                        val clip = uiState.selectedClip
                        EditorToolButton(EditorTool.SPLIT, Icons.Default.CallSplit) { onEvent(EditorEvent.ToolClicked(EditorTool.SPLIT)) }
                        if (clip?.type == ClipType.VIDEO) {
                            EditorToolButton(EditorTool.SPEED, Icons.Default.Speed) { onEvent(EditorEvent.ToolClicked(EditorTool.SPEED)) }
                        }
                        if (clip?.type == ClipType.VIDEO || clip?.type == ClipType.AUDIO) {
                            EditorToolButton(EditorTool.VOLUME, Icons.Default.VolumeUp) { onEvent(EditorEvent.ToolClicked(EditorTool.VOLUME)) }
                        }
                        EditorToolButton(EditorTool.DELETE, Icons.Default.Delete) { onEvent(EditorEvent.ToolClicked(EditorTool.DELETE)) }
                        EditorToolButton(EditorTool.MASK, Icons.Default.Layers) { onEvent(EditorEvent.ToolClicked(EditorTool.MASK)) }
                        EditorToolButton(EditorTool.BLEND, Icons.Default.ViewHeadline) { onEvent(EditorEvent.ToolClicked(EditorTool.BLEND)) }
                        EditorToolButton(EditorTool.TRANSFORM, Icons.Default.Refresh) { onEvent(EditorEvent.ToolClicked(EditorTool.TRANSFORM)) }
                        EditorToolButton(EditorTool.KEYFRAME, Icons.Default.Star) { onEvent(EditorEvent.ToolClicked(EditorTool.KEYFRAME)) }
                        EditorToolButton(EditorTool.BEATS, Icons.Default.GraphicEq) { onEvent(EditorEvent.ToolClicked(EditorTool.BEATS)) }
                    }
                }
            }
        }

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
        Icon(imageVector = icon, contentDescription = tool.label, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = tool.label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
    }
}
