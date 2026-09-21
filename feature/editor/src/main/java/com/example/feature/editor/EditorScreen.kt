package com.example.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.core.common.TimeUtils
import com.example.core.model.AspectRatio
import com.example.core.model.Transform
import com.example.core.model.TrackType
import com.example.core.ui.components.AppPrimaryButton
import com.example.core.ui.components.LoadingView
import com.example.core.ui.theme.AppRadius
import com.example.core.ui.theme.AppSpacing
import com.example.core.ui.theme.EditorColors
import com.example.feature.editor.filter.FiltersBottomSheet
import com.example.feature.editor.text.TextEditorBottomSheet
import com.example.feature.timeline.engine.TimelineAction
import com.example.feature.timeline.engine.TimelineEngineState
import com.example.feature.timeline.ui.TimelineContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    uiState: EditorUiState,
    player: Player? = null,
    onEvent: (EditorEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateExport: (projectId: String) -> Unit,
    onNavigateMediaPicker: () -> Unit = {},
    onTimelineAction: (TimelineAction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.error) {
        uiState.error?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Immediately flush autosave when the app goes into the background (DEV-060, DEV-061)
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        onEvent(EditorEvent.SaveImmediately)
    }

    // Stable remembered action callbacks
    val onBackClick = remember(onEvent, onNavigateBack) {
        {
            onEvent(EditorEvent.SaveImmediately)
            onNavigateBack()
        }
    }
    val onUndoClick = remember(onEvent) { { onEvent(EditorEvent.UndoClicked) } }
    val onRedoClick = remember(onEvent) { { onEvent(EditorEvent.RedoClicked) } }
    val onPlayPauseClick = remember(onEvent) { { onEvent(EditorEvent.PlayPauseClicked) } }
    val onExportClick = remember(uiState.project, onNavigateExport) {
        {
            uiState.project?.let { onNavigateExport(it.id) }
            Unit
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.project?.name ?: "Editor",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onUndoClick,
                        enabled = uiState.canUndo,
                        modifier = Modifier.testTag("undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (uiState.canUndo) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    IconButton(
                        onClick = onRedoClick,
                        enabled = uiState.canRedo,
                        modifier = Modifier.testTag("redo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (uiState.canRedo) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    Spacer(modifier = Modifier.width(AppSpacing.xs))

                    AppPrimaryButton(
                        text = "Export",
                        onClick = onExportClick,
                        modifier = Modifier
                            .padding(end = AppSpacing.sm)
                            .testTag("export_button")
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingView(
                message = "Loading project...",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // 1. Preview Area (adaptive weight)
                EditorPreviewArea(
                    uiState = uiState,
                    player = player,
                    onPlayPause = onPlayPauseClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(AppSpacing.md)
                )

                // 2. Timeline Area
                val timelineEngineState = remember(uiState.project, uiState.playheadPositionMs, uiState.selectedClipId, uiState.durationMs) {
                    TimelineEngineState(
                        tracks = uiState.project?.tracks ?: emptyList(),
                        playheadPositionMs = uiState.playheadPositionMs,
                        durationMs = uiState.durationMs,
                        selectedClipId = uiState.selectedClipId
                    )
                }

                TimelineContainer(
                    state = timelineEngineState,
                    isPlaying = uiState.isPlaying,
                    onAction = onTimelineAction,
                    onPlayPause = onPlayPauseClick,
                    onAddMedia = onNavigateMediaPicker,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                )

                // 3. Bottom Tool Panel
                EditorBottomToolPanel(
                    activeTool = uiState.activeTool,
                    onToolClick = { tool ->
                        when (tool) {
                            EditorTool.SPLIT -> {
                                if (uiState.selectedClip != null) {
                                    onEvent(EditorEvent.SetEditSheetVisible(true))
                                } else {
                                    onEvent(EditorEvent.ToolClicked(tool))
                                }
                            }
                            EditorTool.DELETE -> {
                                if (uiState.selectedClip != null) {
                                    onEvent(EditorEvent.DeleteSelectedClip)
                                    coroutineScope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Clip deleted",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            onEvent(EditorEvent.UndoClicked)
                                        }
                                    }
                                } else {
                                    onEvent(EditorEvent.ToolClicked(tool))
                                }
                            }
                            EditorTool.AUDIO, EditorTool.OVERLAY -> {
                                onNavigateMediaPicker()
                            }
                            else -> onEvent(EditorEvent.ToolClicked(tool))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
        }

        // Edit Actions Bottom Sheet (DEV-054 to DEV-057)
        if (uiState.isEditSheetVisible && uiState.selectedClip != null) {
            EditBottomSheet(
                clip = uiState.selectedClip!!,
                playheadPositionMs = uiState.playheadPositionMs,
                onSplit = {
                    onEvent(EditorEvent.SplitSelectedClip)
                    onEvent(EditorEvent.SetEditSheetVisible(false))
                },
                onDuplicate = {
                    onEvent(EditorEvent.DuplicateSelectedClip)
                    onEvent(EditorEvent.SetEditSheetVisible(false))
                },
                onDelete = {
                    onEvent(EditorEvent.DeleteSelectedClip)
                    onEvent(EditorEvent.SetEditSheetVisible(false))
                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "Clip deleted",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            onEvent(EditorEvent.UndoClicked)
                        }
                    }
                },
                onDismiss = {
                    onEvent(EditorEvent.SetEditSheetVisible(false))
                }
            )
        }

        // Text Overlay Editor Bottom Sheet (DEV-062, DEV-063)
        if (uiState.isTextSheetVisible) {
            val selectedTextData = uiState.selectedClip?.textData
            TextEditorBottomSheet(
                initialText = selectedTextData?.text ?: "Your Text Here",
                initialFontSize = selectedTextData?.fontSize ?: 28f,
                initialColor = selectedTextData?.textColor ?: "#FFFFFF",
                initialFontFamily = selectedTextData?.fontFamily ?: "Default",
                initialAlignment = selectedTextData?.alignment ?: "CENTER",
                onApply = { text, fontSize, color, fontFamily, alignment ->
                    onEvent(
                        EditorEvent.ApplyTextClip(
                            text = text,
                            fontSize = fontSize,
                            color = color,
                            fontFamily = fontFamily,
                            alignment = alignment
                        )
                    )
                },
                onDismiss = {
                    onEvent(EditorEvent.SetTextSheetVisible(false))
                }
            )
        }

        // Filters Adjustment Bottom Sheet (DEV-064, DEV-065)
        if (uiState.isFiltersSheetVisible) {
            FiltersBottomSheet(
                filterSettings = uiState.filterSettings,
                onFilterChange = { settings ->
                    onEvent(EditorEvent.UpdateFilterSettings(settings))
                },
                onReset = {
                    onEvent(EditorEvent.ResetFilterSettings)
                },
                onDismiss = {
                    onEvent(EditorEvent.SetFiltersSheetVisible(false))
                }
            )
        }

        // Speed Adjustment Bottom Sheet
        if (uiState.isSpeedSheetVisible) {
            val currentSpeed = uiState.selectedClip?.speed ?: 1.0f
            SpeedBottomSheet(
                currentSpeed = currentSpeed,
                onSpeedChanged = { speed ->
                    onEvent(EditorEvent.ChangeClipSpeed(speed))
                },
                onDismiss = {
                    onEvent(EditorEvent.SetSpeedSheetVisible(false))
                }
            )
        }

        // Volume Adjustment Bottom Sheet
        if (uiState.isVolumeSheetVisible) {
            val currentVolume = uiState.selectedClip?.volume ?: 1.0f
            VolumeBottomSheet(
                currentVolume = currentVolume,
                onVolumeChanged = { volume ->
                    onEvent(EditorEvent.ChangeClipVolume(volume))
                },
                onDismiss = {
                    onEvent(EditorEvent.SetVolumeSheetVisible(false))
                }
            )
        }

        // Canvas Aspect Ratio Bottom Sheet
        if (uiState.isCanvasSheetVisible) {
            val currentRatio = uiState.project?.aspectRatio ?: AspectRatio.RATIO_9_16
            CanvasBottomSheet(
                currentRatio = currentRatio,
                onRatioSelected = { ratio ->
                    onEvent(EditorEvent.ChangeAspectRatio(ratio))
                },
                onDismiss = {
                    onEvent(EditorEvent.SetCanvasSheetVisible(false))
                }
            )
        }

        // Transform Bottom Sheet
        if (uiState.isTransformSheetVisible) {
            val currentTransform = uiState.selectedClip?.transform ?: Transform.DEFAULT
            TransformBottomSheet(
                currentTransform = currentTransform,
                onTransformChanged = { transform ->
                    onEvent(EditorEvent.ChangeClipTransform(transform))
                },
                onDismiss = {
                    onEvent(EditorEvent.SetTransformSheetVisible(false))
                }
            )
        }
    }
}

/**
 * Preview Area displaying the video canvas with correct aspect ratio and timecode.
 */
@Composable
private fun EditorPreviewArea(
    uiState: EditorUiState,
    player: Player? = null,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aspectRatio = uiState.project?.aspectRatio?.floatRatio ?: (9f / 16f)
    val hasClips = (uiState.project?.tracks?.any { it.clips.isNotEmpty() } == true)

    // Calculate active text clips at current playhead position (DEV-062, DEV-063)
    val currentPlayhead = uiState.playheadPositionMs
    val activeTextClips = remember(uiState.project, currentPlayhead) {
        uiState.project?.tracks
            ?.filter { it.type == TrackType.TEXT && it.isVisible }
            ?.flatMap { it.clips }
            ?.filter { clip ->
                clip.isVisible &&
                currentPlayhead >= clip.startTimeMs &&
                currentPlayhead <= clip.endTimeMs &&
                clip.textData != null
            } ?: emptyList()
    }

    // Build ColorMatrix and cached Paint for Brightness, Contrast, Saturation filters (DEV-065)
    val filterSettings = uiState.filterSettings
    val cachedFilterPaint = remember(filterSettings) {
        if (filterSettings.isDefault) null
        else {
            val matrix = ColorMatrix()
            matrix.setToSaturation(filterSettings.saturation)

            val c = filterSettings.contrast
            val b = filterSettings.brightness * 255f
            val translate = (1f - c) * 128f + b

            val contrastBrightnessMatrix = ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, translate,
                    0f, c, 0f, 0f, translate,
                    0f, 0f, c, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            contrastBrightnessMatrix.timesAssign(matrix)

            Paint().apply {
                colorFilter = ColorFilter.colorMatrix(contrastBrightnessMatrix)
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AppRadius.medium))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Aspect ratio bounded preview box
        Box(
            modifier = Modifier
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(AppRadius.small))
                .background(Color(0xFF141419))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(AppRadius.small)),
            contentAlignment = Alignment.Center
        ) {
            // Media surface with optional ColorMatrix filter (DEV-065)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (cachedFilterPaint != null) {
                            Modifier.drawWithContent {
                                drawIntoCanvas { canvas ->
                                    canvas.saveLayer(size.toRect(), cachedFilterPaint)
                                    drawContent()
                                    canvas.restore()
                                }
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (player != null && hasClips) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                this.player = player
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        },
                        update = { playerView ->
                            playerView.player = player
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.sm))
                        Text(
                            text = uiState.project?.aspectRatio?.label ?: "9:16",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Text Overlays rendered on top of the preview surface (DEV-062, DEV-063)
            activeTextClips.forEach { clip ->
                val textData = clip.textData ?: return@forEach
                val textColor = remember(textData.textColor) { parseColorHex(textData.textColor) }
                val fontFamily = remember(textData.fontFamily) {
                    when (textData.fontFamily) {
                        "Serif" -> FontFamily.Serif
                        "SansSerif" -> FontFamily.SansSerif
                        "Monospace" -> FontFamily.Monospace
                        else -> FontFamily.Default
                    }
                }
                val textAlign = remember(textData.alignment) {
                    when (textData.alignment) {
                        "LEFT" -> TextAlign.Left
                        "RIGHT" -> TextAlign.Right
                        else -> TextAlign.Center
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppSpacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = textData.text,
                        color = textColor,
                        fontSize = textData.fontSize.sp,
                        fontFamily = fontFamily,
                        textAlign = textAlign,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(AppSpacing.xs)
                    )
                }
            }

            // Center Play/Pause button
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .testTag("preview_play_pause_button")
            ) {
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Timecode badge in bottom corner
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(AppSpacing.sm),
            shape = RoundedCornerShape(AppRadius.small),
            color = Color.Black.copy(alpha = 0.75f)
        ) {
            Text(
                text = "${TimeUtils.formatDuration(uiState.playheadPositionMs)} / ${TimeUtils.formatDuration(uiState.durationMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = 4.dp)
            )
        }
    }
}

/**
 * Timeline Area placeholder showing timecode, ruler, playhead, and multi-track lanes.
 */
@Composable

                IconButton(
                    onClick = { onEvent(EditorEvent.PlayPauseClicked) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Ruler bar placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(Color(0xFF161620))
                .border(width = 0.5.dp, color = EditorColors.timelineRuler.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("00:00", style = MaterialTheme.typography.labelSmall, color = EditorColors.timelineRuler)
                Text("00:05", style = MaterialTheme.typography.labelSmall, color = EditorColors.timelineRuler)
                Text("00:10", style = MaterialTheme.typography.labelSmall, color = EditorColors.timelineRuler)
                Text("00:15", style = MaterialTheme.typography.labelSmall, color = EditorColors.timelineRuler)
            }
        }

        // Track Lanes Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(AppSpacing.xs)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                // Video Track Lane
                val videoTrack = uiState.project?.tracks?.firstOrNull { it.type == TrackType.VIDEO }
                val videoClipsCount = videoTrack?.clips?.size ?: 0
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(AppRadius.clip))
                        .background(EditorColors.clipVideo.copy(alpha = 0.2f))
                        .border(1.dp, EditorColors.clipVideo.copy(alpha = 0.5f), RoundedCornerShape(AppRadius.clip))
                        .clickable { onAddMediaClick() }
                        .padding(horizontal = AppSpacing.sm),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = if (videoClipsCount > 0) "Main Video Track ($videoClipsCount clips)" else "Main Video Track (Tap + to add media)",
                        style = MaterialTheme.typography.labelSmall,
                        color = EditorColors.clipVideo
                    )
                }

                // Audio Track Lane
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(AppRadius.clip))
                        .background(EditorColors.clipAudio.copy(alpha = 0.2f))
                        .border(1.dp, EditorColors.clipAudio.copy(alpha = 0.5f), RoundedCornerShape(AppRadius.clip))
                        .padding(horizontal = AppSpacing.sm),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "Audio Track",
                        style = MaterialTheme.typography.labelSmall,
                        color = EditorColors.clipAudio
                    )
                }
            }

            // Playhead indicator vertical line
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(2.dp)
                    .fillMaxSize()
                    .background(EditorColors.playhead)
            )
        }
    }
}

/**
 * Bottom Tool Panel providing a horizontally scrolling row of editing actions.
 */
@Composable
private fun EditorBottomToolPanel(
    activeTool: EditorTool?,
    onToolClick: (EditorTool) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EditorTool.entries.forEach { tool ->
            val isSelected = activeTool == tool
            val icon = getToolIcon(tool)

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(AppRadius.medium))
                    .clickable { onToolClick(tool) }
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = tool.label,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tool.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getToolIcon(tool: EditorTool): ImageVector {
    return when (tool) {
        EditorTool.SPLIT -> Icons.Default.CropRotate
        EditorTool.SPEED -> Icons.Default.Speed
        EditorTool.VOLUME -> Icons.AutoMirrored.Filled.VolumeUp
        EditorTool.AUDIO -> Icons.Default.MusicNote
        EditorTool.TEXT -> Icons.Default.TextFields
        EditorTool.OVERLAY -> Icons.Default.Layers
        EditorTool.EFFECTS -> Icons.Default.AutoFixHigh
        EditorTool.FILTERS -> Icons.Default.Filter
        EditorTool.TRANSFORM -> Icons.Default.CropRotate
        EditorTool.CANVAS -> Icons.Default.AspectRatio
        EditorTool.DELETE -> Icons.Default.Delete
    }
}

private fun parseColorHex(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorLong = clean.toLong(16)
        if (clean.length == 6) {
            Color(0xFF000000 or colorLong)
        } else {
            Color(colorLong)
        }
    } catch (_: Exception) {
        Color.White
    }
}
