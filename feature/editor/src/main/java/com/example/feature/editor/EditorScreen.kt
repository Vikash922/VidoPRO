package com.example.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CameraFilter
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.core.model.TrackType
import com.example.core.ui.components.LoadingView
import com.example.feature.timeline.engine.TimelineAction
import androidx.media3.common.Player

/**
 * Editor Screen matching the reference image exactly.
 * Layout: TopBar → Preview → TimeControls → Timeline → BottomToolbar
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val bgColor = Color(0xFF0A0D14)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = bgColor,
        topBar = {
            // Top Bar: X (close) ... 1080P ▼ ... [Export]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp).clickable { onNavigateBack() }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Resolution picker
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("1080P", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }

                    // Export button — purple gradient, rounded
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF6B4BFF), Color(0xFF9E84FF))))
                            .clickable { uiState.project?.id?.let { onNavigateExport(it) } }
                            .padding(horizontal = 20.dp),
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

            // Preview Area — black box with placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.White.copy(alpha = 0.15f)
                )
            }

            // Time Controls Row: 00:08 / 00:32  ▶  ↶ ↷ ⛶
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timecode left
                Text(
                    text = "${TimeUtils.formatDuration(uiState.playheadPositionMs)} / ${TimeUtils.formatDuration(uiState.durationMs)}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )

                // Play/Pause center
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp).clickable { onEvent(EditorEvent.PlayPauseClicked) }
                )

                // Undo/Redo/Fullscreen right
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (uiState.canUndo) Color.White else Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(20.dp).clickable(enabled = uiState.canUndo) { onEvent(EditorEvent.UndoClicked) }
                    )
                    Spacer(Modifier.width(14.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (uiState.canRedo) Color.White else Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(20.dp).clickable(enabled = uiState.canRedo) { onEvent(EditorEvent.RedoClicked) }
                    )
                    Spacer(Modifier.width(14.dp))
                    Icon(
                        Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Timeline Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color(0xFF0A0D14))
            ) {
                if (uiState.project != null) {
                    val timelineEngineState = remember(uiState.project, uiState.playheadPositionMs, uiState.selectedClipId, uiState.durationMs) {
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
                        onAddMedia = { onNavigateMediaPicker(TrackType.VIDEO) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Bottom Toolbar — matching reference: Edit, Audio, Text, Overlay, Effects, Filters
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor)
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                val scrollState = rememberScrollState()

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (uiState.selectedClipId == null) {
                        // Main tools — exactly matching reference image icons
                        EditorToolButton(EditorTool.EDIT, Icons.Default.ContentCut) { onEvent(EditorEvent.ToolClicked(EditorTool.EDIT)) }
                        Spacer(Modifier.width(24.dp))
                        EditorToolButton(EditorTool.AUDIO, Icons.Default.Audiotrack) { onEvent(EditorEvent.ToolClicked(EditorTool.AUDIO)) }
                        Spacer(Modifier.width(24.dp))
                        EditorToolButton(EditorTool.TEXT, Icons.Default.Title) { onEvent(EditorEvent.ToolClicked(EditorTool.TEXT)) }
                        Spacer(Modifier.width(24.dp))
                        EditorToolButton(EditorTool.OVERLAY, Icons.Default.Layers) { onEvent(EditorEvent.ToolClicked(EditorTool.OVERLAY)) }
                        Spacer(Modifier.width(24.dp))
                        EditorToolButton(EditorTool.EFFECTS, Icons.Default.AutoFixHigh) { onEvent(EditorEvent.ToolClicked(EditorTool.EFFECTS)) }
                        Spacer(Modifier.width(24.dp))
                        EditorToolButton(EditorTool.FILTERS, Icons.Default.CameraFilter) { onEvent(EditorEvent.ToolClicked(EditorTool.FILTERS)) }
                    } else {
                        // Clip-specific tools
                        val clip = uiState.selectedClip
                        EditorToolButton(EditorTool.SPLIT, Icons.Default.CallSplit) { onEvent(EditorEvent.ToolClicked(EditorTool.SPLIT)) }
                        Spacer(Modifier.width(20.dp))
                        if (clip?.type == ClipType.VIDEO) {
                            EditorToolButton(EditorTool.SPEED, Icons.Default.Speed) { onEvent(EditorEvent.ToolClicked(EditorTool.SPEED)) }
                            Spacer(Modifier.width(20.dp))
                        }
                        if (clip?.type == ClipType.VIDEO || clip?.type == ClipType.AUDIO) {
                            EditorToolButton(EditorTool.VOLUME, Icons.Default.VolumeUp) { onEvent(EditorEvent.ToolClicked(EditorTool.VOLUME)) }
                            Spacer(Modifier.width(20.dp))
                        }
                        EditorToolButton(EditorTool.DELETE, Icons.Default.Delete) { onEvent(EditorEvent.ToolClicked(EditorTool.DELETE)) }
                        Spacer(Modifier.width(20.dp))
                        EditorToolButton(EditorTool.TRANSFORM, Icons.Default.Tune) { onEvent(EditorEvent.ToolClicked(EditorTool.TRANSFORM)) }
                        Spacer(Modifier.width(20.dp))
                        EditorToolButton(EditorTool.KEYFRAME, Icons.Default.Star) { onEvent(EditorEvent.ToolClicked(EditorTool.KEYFRAME)) }
                        Spacer(Modifier.width(20.dp))
                        EditorToolButton(EditorTool.BEATS, Icons.Default.GraphicEq) { onEvent(EditorEvent.ToolClicked(EditorTool.BEATS)) }
                    }
                }
            }
        }

        // Bottom Sheets
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
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tool.label,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tool.label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}
