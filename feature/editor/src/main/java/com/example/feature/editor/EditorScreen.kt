package com.example.feature.editor

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.example.core.common.TimeUtils
import com.example.core.model.ClipType
import com.example.core.model.TrackType
import com.example.core.ui.components.LoadingView
import com.example.feature.timeline.engine.TimelineAction

/**
 * Editor Screen — Clean solid dark mode UI without gradients.
 * Features:
 * - Live Video playback with Media3 PlayerView (plays real video when media is added)
 * - Tapping video preview plays/pauses
 * - Interactive Resolution Dropdown (720P, 1080P, 2K, 4K)
 * - Undo / Redo / Fullscreen controls
 * - Fully working tool buttons
 * - Clicking timeline does NOT open bottom sheet
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
    var isFullscreen by remember { mutableStateOf(false) }
    var selectedResolution by remember { mutableStateOf("1080P") }
    var showResolutionMenu by remember { mutableStateOf(false) }

    val hasClips = remember(uiState.project) {
        uiState.project?.tracks?.any { it.clips.isNotEmpty() } == true
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = bgColor,
        topBar = {
            if (!isFullscreen) {
                // Top Bar: Close (X) | 1080P Dropdown | Solid Purple Export Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(bgColor)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onNavigateBack() }
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Resolution selector dropdown (NO GRADIENTS)
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E2230))
                                    .clickable { showResolutionMenu = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    selectedResolution,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showResolutionMenu,
                                onDismissRequest = { showResolutionMenu = false },
                                modifier = Modifier.background(Color(0xFF1E2230))
                            ) {
                                listOf("720P", "1080P", "2K", "4K").forEach { res ->
                                    DropdownMenuItem(
                                        text = { Text(res, color = Color.White) },
                                        onClick = {
                                            selectedResolution = res
                                            showResolutionMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Export button — SOLID PURPLE (NO GRADIENT)
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF6B4BFF))
                                .clickable { uiState.project?.id?.let { onNavigateExport(it) } }
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Export",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
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

            // VIDEO PREVIEW AREA — Real Live PlayerView (Media3 ExoPlayer)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isFullscreen) 1f else 0.52f)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
                    .border(1.dp, Color(0xFF1F2432), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (player != null && hasClips) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                this.player = player
                                useController = false
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        update = { view ->
                            if (view.player != player) {
                                view.player = player
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                onEvent(EditorEvent.PlayPauseClicked)
                            }
                    )
                } else {
                    // Empty state: Tap to add video
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onNavigateMediaPicker(TrackType.VIDEO) }
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircleOutline,
                            contentDescription = "Add Media",
                            modifier = Modifier.size(52.dp),
                            tint = Color(0xFF6B4BFF).copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap to Add Video",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (!isFullscreen) {
                // Time Controls Bar: 00:08 / 00:32 | Play/Pause | Undo | Redo | Fullscreen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Timecode display
                    Text(
                        text = "${TimeUtils.formatDuration(uiState.playheadPositionMs)} / ${TimeUtils.formatDuration(uiState.durationMs)}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )

                    // Play/Pause button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E2230))
                            .clickable { onEvent(EditorEvent.PlayPauseClicked) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Undo, Redo, Fullscreen icons
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (uiState.canUndo) Color.White else Color.White.copy(alpha = 0.2f),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(enabled = uiState.canUndo) { onEvent(EditorEvent.UndoClicked) }
                        )
                        Spacer(Modifier.width(16.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (uiState.canRedo) Color.White else Color.White.copy(alpha = 0.2f),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(enabled = uiState.canRedo) { onEvent(EditorEvent.RedoClicked) }
                        )
                        Spacer(Modifier.width(16.dp))
                        Icon(
                            Icons.Default.Fullscreen,
                            contentDescription = "Fullscreen",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { isFullscreen = true }
                        )
                    }
                }

                // TIMELINE AREA (Height 210dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .background(bgColor)
                ) {
                    if (uiState.project != null) {
                        val timelineEngineState = remember(
                            uiState.project,
                            uiState.playheadPositionMs,
                            uiState.selectedClipId,
                            uiState.durationMs,
                            uiState.beatMarkers
                        ) {
                            com.example.feature.timeline.engine.TimelineEngineState(
                                tracks = uiState.project?.tracks ?: emptyList(),
                                playheadPositionMs = uiState.playheadPositionMs,
                                durationMs = uiState.durationMs,
                                selectedClipId = uiState.selectedClipId,
                                beatMarkers = uiState.beatMarkers
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

                // BOTTOM TOOLBAR (Solid flat colors, NO GRADIENTS)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                        .padding(vertical = 10.dp, horizontal = 4.dp)
                ) {
                    val scrollState = rememberScrollState()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        if (uiState.selectedClipId == null) {
                            // Primary tools
                            EditorToolButton(EditorTool.EDIT, Icons.Default.ContentCut) { onEvent(EditorEvent.SetEditSheetVisible(true)) }
                            Spacer(Modifier.width(20.dp))
                            EditorToolButton(EditorTool.AUDIO, Icons.Default.Audiotrack) { onNavigateMediaPicker(TrackType.AUDIO) }
                            Spacer(Modifier.width(20.dp))
                            EditorToolButton(EditorTool.TEXT, Icons.Default.Title) { onEvent(EditorEvent.SetTextSheetVisible(true)) }
                            Spacer(Modifier.width(20.dp))
                            EditorToolButton(EditorTool.OVERLAY, Icons.Default.Layers) { onNavigateMediaPicker(TrackType.OVERLAY) }
                            Spacer(Modifier.width(20.dp))
                            EditorToolButton(EditorTool.EFFECTS, Icons.Default.AutoFixHigh) { onEvent(EditorEvent.SetFiltersSheetVisible(true)) }
                            Spacer(Modifier.width(20.dp))
                            EditorToolButton(EditorTool.FILTERS, Icons.Default.ColorLens) { onEvent(EditorEvent.SetFiltersSheetVisible(true)) }
                        } else {
                            // Clip-specific tools for selected clip
                            val clip = uiState.selectedClip
                            EditorToolButton(EditorTool.SPLIT, Icons.Default.CallSplit) {
                                onEvent(EditorEvent.SplitSelectedClip)
                            }
                            Spacer(Modifier.width(18.dp))
                            if (clip?.type == ClipType.VIDEO) {
                                EditorToolButton(EditorTool.SPEED, Icons.Default.Speed) {
                                    onEvent(EditorEvent.SetSpeedSheetVisible(true))
                                }
                                Spacer(Modifier.width(18.dp))
                            }
                            if (clip?.type == ClipType.VIDEO || clip?.type == ClipType.AUDIO) {
                                EditorToolButton(EditorTool.VOLUME, Icons.Default.VolumeUp) {
                                    onEvent(EditorEvent.SetVolumeSheetVisible(true))
                                }
                                Spacer(Modifier.width(18.dp))
                            }
                            EditorToolButton(EditorTool.DELETE, Icons.Default.Delete) {
                                onEvent(EditorEvent.DeleteSelectedClip)
                            }
                            Spacer(Modifier.width(18.dp))
                            EditorToolButton(EditorTool.TRANSFORM, Icons.Default.CropRotate) {
                                onEvent(EditorEvent.SetTransformSheetVisible(true))
                            }
                            Spacer(Modifier.width(18.dp))
                            EditorToolButton(EditorTool.KEYFRAME, Icons.Default.Star) {
                                onEvent(EditorEvent.SetKeyframeSheetVisible(true))
                            }
                            Spacer(Modifier.width(18.dp))
                            EditorToolButton(EditorTool.BEATS, Icons.Default.GraphicEq) {
                                // Tapping BEATS toggles pink beat marker line at playhead!
                                onTimelineAction(TimelineAction.ToggleBeatMarker(uiState.playheadPositionMs))
                            }
                        }
                    }
                }
            } else {
                // Exit Fullscreen overlay button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E2230))
                            .clickable { isFullscreen = false }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FullscreenExit, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Exit Fullscreen", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Bottom Sheets (only open when clicked by user)
        if (uiState.isEditSheetVisible && uiState.selectedClip != null) {
            EditBottomSheet(
                clip = uiState.selectedClip!!,
                playheadPositionMs = uiState.playheadPositionMs,
                onDismiss = { onEvent(EditorEvent.SetEditSheetVisible(false)) },
                onSplit = { onEvent(EditorEvent.SplitSelectedClip) },
                onDuplicate = { onEvent(EditorEvent.DuplicateSelectedClip) },
                onDelete = { onEvent(EditorEvent.DeleteSelectedClip) },
                onSpeed = { onEvent(EditorEvent.SetSpeedSheetVisible(true)) },
                onVolume = { onEvent(EditorEvent.SetVolumeSheetVisible(true)) },
                onAudio = { onNavigateMediaPicker(TrackType.AUDIO) },
                onText = { onEvent(EditorEvent.SetTextSheetVisible(true)) },
                onOverlay = { onNavigateMediaPicker(TrackType.OVERLAY) },
                onFilters = { onEvent(EditorEvent.SetFiltersSheetVisible(true)) },
                onTransform = { onEvent(EditorEvent.SetTransformSheetVisible(true)) },
                onCanvas = { onEvent(EditorEvent.SetCanvasSheetVisible(true)) },
                onKeyframe = { onEvent(EditorEvent.SetKeyframeSheetVisible(true)) },
                onBeats = { onTimelineAction(TimelineAction.ToggleBeatMarker(uiState.playheadPositionMs)) }
            )
        }
        if (uiState.isKeyframeSheetVisible) { KeyframeBottomSheet(onDismiss = { onEvent(EditorEvent.SetKeyframeSheetVisible(false)) }) }
        if (uiState.isBeatsSheetVisible) { BeatsBottomSheet(onDismiss = { onEvent(EditorEvent.SetBeatsSheetVisible(false)) }) }
        if (uiState.isSpeedSheetVisible) { SpeedBottomSheet(currentSpeed = 1.0f, onSpeedChanged = {}, onDismiss = { onEvent(EditorEvent.SetSpeedSheetVisible(false)) }) }
        if (uiState.isTransformSheetVisible) { TransformBottomSheet(currentTransform = com.example.core.model.Transform.DEFAULT, onTransformChanged = {}, onDismiss = { onEvent(EditorEvent.SetTransformSheetVisible(false)) }) }
        if (uiState.isVolumeSheetVisible) { VolumeBottomSheet(currentVolume = 1.0f, onVolumeChanged = {}, onDismiss = { onEvent(EditorEvent.SetVolumeSheetVisible(false)) }) }
        if (uiState.isCanvasSheetVisible) { CanvasBottomSheet(currentRatio = com.example.core.model.AspectRatio.RATIO_9_16, onRatioSelected = {}, onDismiss = { onEvent(EditorEvent.SetCanvasSheetVisible(false)) }) }
        if (uiState.isTextSheetVisible) {
            com.example.feature.editor.text.TextEditorBottomSheet(
                onDismiss = { onEvent(EditorEvent.SetTextSheetVisible(false)) },
                onApply = { text, size, color, font, align ->
                    onEvent(EditorEvent.ApplyTextClip(text, size, color, font, align))
                }
            )
        }
        if (uiState.isFiltersSheetVisible) {
            com.example.feature.editor.filter.FiltersBottomSheet(
                filterSettings = uiState.filterSettings,
                onSettingsChanged = { onEvent(EditorEvent.UpdateFilterSettings(it)) },
                onReset = { onEvent(EditorEvent.ResetFilterSettings) },
                onDismiss = { onEvent(EditorEvent.SetFiltersSheetVisible(false)) }
            )
        }
    }
}

@Composable
fun EditorToolButton(tool: EditorTool, icon: ImageVector, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp)
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
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}
