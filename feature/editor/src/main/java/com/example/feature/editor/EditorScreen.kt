package com.example.feature.editor

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import coil.compose.AsyncImage
import com.example.feature.timeline.ui.TimelineMode
import kotlin.math.roundToInt
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.example.core.common.TimeUtils
import com.example.core.media.CanvasCoordinateHelper
import com.example.core.media.KeyframeEvaluator
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
    var timelineMode by remember { mutableStateOf(TimelineMode.MAIN) }

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

                        // Export button — SOLID WHITE (NO GRADIENT)
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .clickable { uiState.project?.id?.let { onNavigateExport(it) } }
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Export",
                                color = Color(0xFF0A0D14),
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

        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val isCompactScreen = maxHeight < 640.dp
            val timelineHeight = if (isCompactScreen) 160.dp else if (maxHeight < 800.dp) 190.dp else 220.dp

            Column(modifier = Modifier.fillMaxSize()) {

                // VIDEO PREVIEW AREA — Real Live PlayerView (Media3 ExoPlayer) on Project Canvas
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0A0D14))
                        .border(1.dp, Color(0xFF1F2432), RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectTapGestures {
                                onEvent(EditorEvent.SelectClip(null))
                                onTimelineAction(TimelineAction.SelectClip(null))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val viewportWidth = maxWidth
                    val viewportHeight = maxHeight
                    val currentAspectRatio = uiState.project?.aspectRatio ?: com.example.core.model.AspectRatio.RATIO_9_16
                    val targetRatio = currentAspectRatio.floatRatio

                    val (canvasWidth, canvasHeight) = if (viewportWidth.value / viewportHeight.value > targetRatio) {
                        val h = viewportHeight
                        val w = h * targetRatio
                        w to h
                    } else {
                        val w = viewportWidth
                        val h = w / targetRatio
                        w to h
                    }

                    Box(
                        modifier = Modifier
                            .size(canvasWidth, canvasHeight)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        if (player != null && hasClips) {
                            val density = LocalDensity.current
                            val canvasWidthPx = with(density) { canvasWidth.toPx() }
                            val canvasHeightPx = with(density) { canvasHeight.toPx() }
                            val projectWidth = uiState.project?.width ?: 1080
                            val projectHeight = uiState.project?.height ?: 1920

                            val mainTrack = uiState.project?.tracks?.find { it.type == TrackType.VIDEO }
                            val mainClips = mainTrack?.clips ?: emptyList()
                            val activeMainClip = uiState.selectedClip?.takeIf { it.type == ClipType.VIDEO }
                                ?: mainClips.find { uiState.playheadPositionMs in it.startTimeMs..it.endTimeMs }
                                ?: mainClips.firstOrNull()

                            val currentTransform = remember(activeMainClip, uiState.playheadPositionMs) {
                                if (activeMainClip != null) {
                                    KeyframeEvaluator.evaluateTransform(activeMainClip, uiState.playheadPositionMs)
                                } else {
                                    com.example.core.model.Transform.DEFAULT
                                }
                            }

                            val previewTransform = remember(currentTransform, canvasWidthPx, canvasHeightPx, projectWidth, projectHeight) {
                                CanvasCoordinateHelper.toPreviewCoordinates(
                                    currentTransform,
                                    canvasWidthPx = canvasWidthPx,
                                    canvasHeightPx = canvasHeightPx,
                                    projectWidth = projectWidth,
                                    projectHeight = projectHeight
                                )
                            }

                            val isMainVideoSelected = uiState.selectedClipId != null && uiState.selectedClipId == activeMainClip?.id

                            val currentFilterSettings = remember(
                                uiState.filterSettings,
                                uiState.selectedClipId,
                                uiState.playheadPositionMs,
                                uiState.project,
                                uiState.isFiltersSheetVisible
                            ) {
                                if (uiState.isFiltersSheetVisible) {
                                    uiState.filterSettings
                                } else {
                                    val allClips = uiState.project?.tracks?.flatMap { it.clips } ?: emptyList()
                                    val activeClip = uiState.selectedClipId?.let { selId -> allClips.find { it.id == selId } }
                                        ?: allClips.find { uiState.playheadPositionMs in it.startTimeMs..it.endTimeMs }
                                    if (activeClip != null && activeClip.effects.isNotEmpty()) {
                                        com.example.feature.editor.filter.FilterSettingsMapper.fromEffects(activeClip.effects)
                                    } else if (uiState.selectedClipId == null && !uiState.filterSettings.isDefault) {
                                        uiState.filterSettings
                                    } else {
                                        com.example.feature.editor.filter.FilterSettings()
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        translationX = previewTransform.x
                                        translationY = previewTransform.y
                                        scaleX = previewTransform.scaleX
                                        scaleY = previewTransform.scaleY
                                        rotationZ = previewTransform.rotation
                                        alpha = previewTransform.opacity
                                    }
                                    .pointerInput(activeMainClip?.id, isMainVideoSelected) {
                                        if (activeMainClip != null && isMainVideoSelected) {
                                            detectTransformGestures { _, pan, zoom, rotation ->
                                                val currentT = activeMainClip.transform
                                                val (deltaX, deltaY) = CanvasCoordinateHelper.fromPreviewPan(
                                                    panX = pan.x,
                                                    panY = pan.y,
                                                    canvasWidthPx = canvasWidthPx,
                                                    canvasHeightPx = canvasHeightPx,
                                                    projectWidth = projectWidth,
                                                    projectHeight = projectHeight
                                                )
                                                val newX = currentT.x + deltaX
                                                val newY = currentT.y + deltaY
                                                val newScale = (currentT.scaleX * zoom).coerceIn(0.1f, 10.0f)
                                                val rawRot = (currentT.rotation + rotation) % 360f
                                                val newRot = if (rawRot < 0f) rawRot + 360f else rawRot

                                                onEvent(
                                                    EditorEvent.ChangeClipTransform(
                                                        currentT.copy(
                                                            x = newX,
                                                            y = newY,
                                                            scaleX = newScale,
                                                            scaleY = newScale,
                                                            rotation = newRot
                                                        ),
                                                        clipId = activeMainClip.id
                                                    )
                                                )
                                            }
                                        }
                                    }
                            ) {
                                AndroidView(
                                    factory = { ctx ->
                                        (android.view.LayoutInflater.from(ctx).inflate(R.layout.texture_player_view, null) as PlayerView).apply {
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
                                        if (currentFilterSettings.isDefault) {
                                            view.setLayerType(android.view.View.LAYER_TYPE_NONE, null)
                                            view.videoSurfaceView?.setLayerType(android.view.View.LAYER_TYPE_NONE, null)
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                                view.setRenderEffect(null)
                                                view.videoSurfaceView?.setRenderEffect(null)
                                            }
                                        } else {
                                            val colorArray = com.example.feature.editor.filter.ColorFilterHelper.createColorMatrixArray(currentFilterSettings)
                                            val paint = android.graphics.Paint().apply {
                                                colorFilter = android.graphics.ColorMatrixColorFilter(colorArray)
                                                alpha = (currentFilterSettings.opacity.coerceIn(0f, 100f) / 100f * 255).toInt()
                                            }
                                            view.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, paint)
                                            view.videoSurfaceView?.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, paint)
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                                val cm = android.graphics.ColorMatrix(colorArray)
                                                val cf = android.graphics.ColorMatrixColorFilter(cm)
                                                val effect = android.graphics.RenderEffect.createColorFilterEffect(cf)
                                                view.setRenderEffect(effect)
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(uiState.selectedClipId) {
                                            detectTapGestures {
                                                if (uiState.selectedClipId != null) {
                                                    onEvent(EditorEvent.SelectClip(null))
                                                    onTimelineAction(TimelineAction.SelectClip(null))
                                                } else {
                                                    onEvent(EditorEvent.PlayPauseClicked)
                                                }
                                            }
                                        }
                                )

                                if (isMainVideoSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .border(1.5.dp, Color(0xFF6B4BFF).copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                                    )
                                }
                            }

                            // Real-time Optical Overlays (Vignette, Fade, Bloom/Glow)
                            if (currentFilterSettings.vignette > 0f) {
                                val vignetteAlpha = (currentFilterSettings.vignette / 100f * 0.9f).coerceIn(0f, 0.95f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            androidx.compose.ui.graphics.Brush.radialGradient(
                                                listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = vignetteAlpha)
                                                )
                                            )
                                        )
                                )
                            }

                            if (currentFilterSettings.fade > 0f) {
                                val fadeAlpha = (currentFilterSettings.fade / 100f * 0.45f).coerceIn(0f, 0.8f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF0A0D14).copy(alpha = fadeAlpha))
                                )
                            }

                            if (currentFilterSettings.glow > 0f) {
                                val glowAlpha = (currentFilterSettings.glow / 100f * 0.25f).coerceIn(0f, 0.5f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.White.copy(alpha = glowAlpha))
                                )
                            }
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
                                    tint = Color.White.copy(alpha = 0.6f)
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

                        // Real-time Visual Overlays (PiP / Images / Videos) active at current playhead
                        val activeOverlayClips = remember(uiState.project?.tracks, uiState.playheadPositionMs) {
                            uiState.project?.tracks
                                ?.filter { it.type == TrackType.OVERLAY && it.isVisible }
                                ?.flatMap { it.clips }
                                ?.filter { clip ->
                                    clip.isVisible &&
                                    uiState.playheadPositionMs >= clip.startTimeMs &&
                                    uiState.playheadPositionMs <= clip.endTimeMs &&
                                    clip.assetId != null
                                } ?: emptyList()
                        }

                        activeOverlayClips.forEach { overlayClip ->
                            val asset = uiState.assets[overlayClip.assetId] ?: return@forEach
                            val isSelected = uiState.selectedClipId == overlayClip.id

                            val baseWidth = 160.dp
                            val baseHeight = 110.dp

                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            overlayClip.transform.x.roundToInt(),
                                            overlayClip.transform.y.roundToInt()
                                        )
                                    }
                                    .graphicsLayer {
                                        scaleX = overlayClip.transform.scaleX
                                        scaleY = overlayClip.transform.scaleY
                                        rotationZ = overlayClip.transform.rotation
                                        alpha = overlayClip.transform.opacity
                                    }
                                    .size(baseWidth, baseHeight)
                                    .pointerInput(overlayClip.id) {
                                        detectTapGestures(
                                            onTap = {
                                                onEvent(EditorEvent.SelectClip(overlayClip.id))
                                                onTimelineAction(TimelineAction.SelectClip(overlayClip.id))
                                                timelineMode = TimelineMode.OVERLAY
                                            }
                                        )
                                    }
                                    .pointerInput(overlayClip.id) {
                                        detectTransformGestures { _, pan, zoom, rotation ->
                                            if (uiState.selectedClipId != overlayClip.id) {
                                                onEvent(EditorEvent.SelectClip(overlayClip.id))
                                                onTimelineAction(TimelineAction.SelectClip(overlayClip.id))
                                                timelineMode = TimelineMode.OVERLAY
                                            }
                                            val currentT = overlayClip.transform
                                            val newScale = (currentT.scaleX * zoom).coerceIn(0.1f, 10.0f)
                                            val newRotation = (currentT.rotation + rotation) % 360f
                                            val newX = currentT.x + pan.x
                                            val newY = currentT.y + pan.y
                                            onEvent(
                                                EditorEvent.ChangeClipTransform(
                                                    currentT.copy(
                                                        x = newX,
                                                        y = newY,
                                                        scaleX = newScale,
                                                        scaleY = newScale,
                                                        rotation = newRotation
                                                    ),
                                                    clipId = overlayClip.id
                                                )
                                            )
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = asset.uri,
                                    contentDescription = "Overlay",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(4.dp))
                                )

                                if (isSelected) {
                                    // Bounding Box Outline
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .border(2.dp, Color.White, RoundedCornerShape(4.dp))
                                    )

                                    // Top-Right: Remove / Delete Button ("X")
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 10.dp, y = (-10).dp)
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE57373))
                                            .clickable { onEvent(EditorEvent.DeleteSelectedClip) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove Overlay",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }

                                    // Top-Left: Duplicate Button
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .offset(x = (-10).dp, y = (-10).dp)
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1E2230))
                                            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                                            .clickable { onEvent(EditorEvent.DuplicateSelectedClip) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Duplicate Overlay",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    // Bottom-Right: Resize / Scale Handle
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .offset(x = 10.dp, y = 10.dp)
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1E2230))
                                            .border(1.dp, Color.White, CircleShape)
                                            .pointerInput(overlayClip.id) {
                                                detectDragGestures { change, dragAmount ->
                                                    change.consume()
                                                    val currentT = overlayClip.transform
                                                    val delta = (dragAmount.x + dragAmount.y) / 80f
                                                    val newScale = (currentT.scaleX + delta).coerceIn(0.1f, 10.0f)
                                                    onEvent(
                                                        EditorEvent.ChangeClipTransform(
                                                            currentT.copy(scaleX = newScale, scaleY = newScale),
                                                            clipId = overlayClip.id
                                                        )
                                                    )
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.OpenInFull,
                                            contentDescription = "Scale Overlay",
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Real-time Text Overlays active at current playhead
                        val activeTextClips = remember(uiState.project?.tracks, uiState.playheadPositionMs) {
                            uiState.project?.tracks
                                ?.filter { it.type == TrackType.TEXT && it.isVisible }
                                ?.flatMap { it.clips }
                                ?.filter { clip ->
                                    clip.isVisible &&
                                    uiState.playheadPositionMs >= clip.startTimeMs &&
                                    uiState.playheadPositionMs <= clip.endTimeMs &&
                                    clip.textData != null
                                } ?: emptyList()
                        }

                        activeTextClips.forEach { textClip ->
                            val textData = textClip.textData ?: return@forEach
                            val isSelected = uiState.selectedClipId == textClip.id

                            val textColor = remember(textData.textColor) {
                                try {
                                    val hex = if (!textData.textColor.startsWith("#")) "#${textData.textColor}" else textData.textColor
                                    Color(android.graphics.Color.parseColor(hex))
                                } catch (e: Exception) {
                                    Color.White
                                }
                            }

                            val fontFamily = when (textData.fontFamily) {
                                "Serif" -> androidx.compose.ui.text.font.FontFamily.Serif
                                "SansSerif" -> androidx.compose.ui.text.font.FontFamily.SansSerif
                                "Monospace" -> androidx.compose.ui.text.font.FontFamily.Monospace
                                else -> androidx.compose.ui.text.font.FontFamily.Default
                            }

                            val textAlign = when (textData.alignment) {
                                "LEFT" -> androidx.compose.ui.text.style.TextAlign.Start
                                "RIGHT" -> androidx.compose.ui.text.style.TextAlign.End
                                else -> androidx.compose.ui.text.style.TextAlign.Center
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .clickable {
                                        onEvent(EditorEvent.SelectClip(textClip.id))
                                        onTimelineAction(TimelineAction.SelectClip(textClip.id))
                                        timelineMode = TimelineMode.TEXT
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = textData.text,
                                    color = textColor,
                                    fontSize = textData.fontSize.sp,
                                    fontFamily = fontFamily,
                                    textAlign = textAlign,
                                    fontWeight = FontWeight.Bold,
                                    modifier = if (isSelected) {
                                        Modifier
                                            .border(1.5.dp, Color.White, RoundedCornerShape(4.dp))
                                            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    } else {
                                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    }
                                )
                            }
                        }

                        // Canvas Aspect Ratio Badge (tappable quick shortcut)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1E2230).copy(alpha = 0.85f))
                                .clickable { onEvent(EditorEvent.SetCanvasSheetVisible(true)) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = currentAspectRatio.label,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
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

                    // Undo, Redo, Keyframe, Fullscreen icons
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
                        Spacer(Modifier.width(14.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (uiState.canRedo) Color.White else Color.White.copy(alpha = 0.2f),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(enabled = uiState.canRedo) { onEvent(EditorEvent.RedoClicked) }
                        )
                        Spacer(Modifier.width(14.dp))
                        // Keyframe Diamond Button
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (uiState.selectedClipId != null) Color(0xFF1E2230) else Color.Transparent)
                                .clickable {
                                    onEvent(EditorEvent.ToggleKeyframeAtPlayhead)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val activeClip = uiState.selectedClip
                            val hasKeyframeAtPlayhead = activeClip != null && activeClip.keyframes.any {
                                kotlin.math.abs(it.timeMs - uiState.playheadPositionMs) <= 50L
                            }
                            Canvas(modifier = Modifier.size(14.dp)) {
                                val path = Path().apply {
                                    moveTo(size.width / 2f, 0f)
                                    lineTo(size.width, size.height / 2f)
                                    lineTo(size.width / 2f, size.height)
                                    lineTo(0f, size.height / 2f)
                                    close()
                                }
                                val diamondColor = if (hasKeyframeAtPlayhead) {
                                    Color(0xFFFFD600)
                                } else if (uiState.selectedClipId != null) {
                                    Color(0xFF00D2FF)
                                } else {
                                    Color.White.copy(alpha = 0.7f)
                                }
                                drawPath(
                                    path = path,
                                    color = diamondColor
                                )
                            }
                        }

                        Spacer(Modifier.width(14.dp))
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

                // TIMELINE AREA
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(timelineHeight)
                        .background(bgColor)
                ) {
                    if (uiState.project != null) {
                        val timelineEngineState = remember(
                            uiState.project,
                            uiState.playheadPositionMs,
                            uiState.selectedClipId,
                            uiState.multiSelectedClipIds,
                            uiState.durationMs,
                            uiState.beatMarkers
                        ) {
                            com.example.feature.timeline.engine.TimelineEngineState(
                                tracks = uiState.project?.tracks ?: emptyList(),
                                playheadPositionMs = uiState.playheadPositionMs,
                                durationMs = uiState.durationMs,
                                selectedClipId = uiState.selectedClipId,
                                multiSelectedClipIds = uiState.multiSelectedClipIds,
                                beatMarkers = uiState.beatMarkers
                            )
                        }

                        com.example.feature.timeline.ui.TimelineContainer(
                            state = timelineEngineState,
                            isPlaying = uiState.isPlaying,
                            assets = uiState.assets,
                            timelineMode = timelineMode,
                            multiSelectedClipIds = uiState.multiSelectedClipIds,
                            onBackToMain = {
                                timelineMode = TimelineMode.MAIN
                                onEvent(EditorEvent.SelectClip(null))
                                onTimelineAction(TimelineAction.SelectClip(null))
                            },
                            onAddSubTrackMedia = {
                                when (timelineMode) {
                                    TimelineMode.OVERLAY -> onNavigateMediaPicker(TrackType.OVERLAY)
                                    TimelineMode.AUDIO -> onNavigateMediaPicker(TrackType.AUDIO)
                                    TimelineMode.TEXT -> onEvent(EditorEvent.SetTextSheetVisible(true))
                                    else -> onNavigateMediaPicker(TrackType.VIDEO)
                                }
                            },
                            onAction = onTimelineAction,
                            onPlayPause = { onEvent(EditorEvent.PlayPauseClicked) },
                            onAddMedia = { onNavigateMediaPicker(TrackType.VIDEO) },
                            onLongPressClip = { clipId -> onEvent(EditorEvent.LongPressClip(clipId)) },
                            onMoveKeyframe = { clipId, kfId, newMs -> onEvent(EditorEvent.MoveKeyframe(clipId, kfId, newMs)) },
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
                        if (timelineMode == TimelineMode.MAIN) {
                            if (uiState.selectedClipId == null) {
                                // Primary tools
                                EditorToolButton(EditorTool.EDIT, Icons.Default.ContentCut) { onEvent(EditorEvent.SetEditSheetVisible(true)) }
                                Spacer(Modifier.width(20.dp))
                                EditorToolButton(EditorTool.CANVAS, Icons.Default.AspectRatio) { onEvent(EditorEvent.SetCanvasSheetVisible(true)) }
                                Spacer(Modifier.width(20.dp))
                                EditorToolButton(EditorTool.AUDIO, Icons.Default.Audiotrack) { timelineMode = TimelineMode.AUDIO }
                                Spacer(Modifier.width(20.dp))
                                EditorToolButton(EditorTool.TEXT, Icons.Default.Title) { timelineMode = TimelineMode.TEXT }
                                Spacer(Modifier.width(20.dp))
                                EditorToolButton(EditorTool.OVERLAY, Icons.Default.Layers) { timelineMode = TimelineMode.OVERLAY }
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
                                if (clip?.type == ClipType.TEXT) {
                                    EditorToolButton(EditorTool.TEXT, Icons.Default.Title) {
                                        onEvent(EditorEvent.SetTextSheetVisible(true))
                                    }
                                    Spacer(Modifier.width(18.dp))
                                }
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
                                EditorToolButton(EditorTool.DUPLICATE, Icons.Default.ContentCopy) {
                                    onEvent(EditorEvent.DuplicateSelectedClip)
                                }
                                Spacer(Modifier.width(18.dp))
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
                                    onTimelineAction(TimelineAction.ToggleBeatMarker(uiState.playheadPositionMs))
                                }
                                // ── Multi-select group controls ──────────────
                                if (uiState.isMultiSelectMode) {
                                    Spacer(Modifier.width(18.dp))
                                    EditorToolButton("Group", Icons.Default.Folder) {
                                        onEvent(EditorEvent.GroupSelectedClips)
                                    }
                                }
                                if (uiState.selectedGroupId != null) {
                                    Spacer(Modifier.width(18.dp))
                                    EditorToolButton("Ungroup", Icons.Default.FolderOff) {
                                        onEvent(EditorEvent.UngroupSelectedClips)
                                    }
                                }

                            }
                        } else if (timelineMode == TimelineMode.OVERLAY) {

                            EditorToolButton("Back", Icons.AutoMirrored.Filled.ArrowBack) {
                                timelineMode = TimelineMode.MAIN
                                onEvent(EditorEvent.SelectClip(null))
                                onTimelineAction(TimelineAction.SelectClip(null))
                            }
                            Spacer(Modifier.width(18.dp))
                            EditorToolButton("Add", Icons.Default.Add) {
                                onNavigateMediaPicker(TrackType.OVERLAY)
                            }
                            Spacer(Modifier.width(18.dp))
                            EditorToolButton(EditorTool.SPLIT, Icons.Default.CallSplit) {
                                onEvent(EditorEvent.SplitSelectedClip)
                            }
                            Spacer(Modifier.width(18.dp))
                            if (uiState.selectedClipId != null) {
                                EditorToolButton(EditorTool.TRANSFORM, Icons.Default.CropRotate) {
                                    onEvent(EditorEvent.SetTransformSheetVisible(true))
                                }
                                Spacer(Modifier.width(18.dp))
                                EditorToolButton(EditorTool.SPEED, Icons.Default.Speed) {
                                    onEvent(EditorEvent.SetSpeedSheetVisible(true))
                                }
                                Spacer(Modifier.width(18.dp))
                                EditorToolButton(EditorTool.VOLUME, Icons.Default.VolumeUp) {
                                    onEvent(EditorEvent.SetVolumeSheetVisible(true))
                                }
                                Spacer(Modifier.width(18.dp))
                                EditorToolButton(EditorTool.DUPLICATE, Icons.Default.ContentCopy) {
                                    onEvent(EditorEvent.DuplicateSelectedClip)
                                }
                                Spacer(Modifier.width(18.dp))
                                EditorToolButton(EditorTool.DELETE, Icons.Default.Delete) {
                                    onEvent(EditorEvent.DeleteSelectedClip)
                                }
                                Spacer(Modifier.width(18.dp))
                                EditorToolButton(EditorTool.KEYFRAME, Icons.Default.Star) {
                                    onEvent(EditorEvent.SetKeyframeSheetVisible(true))
                                }
                            }
                        } else if (timelineMode == TimelineMode.AUDIO) {
                            EditorToolButton("Back", Icons.AutoMirrored.Filled.ArrowBack) {
                                timelineMode = TimelineMode.MAIN
                                onEvent(EditorEvent.SelectClip(null))
                                onTimelineAction(TimelineAction.SelectClip(null))
                            }
                            Spacer(Modifier.width(18.dp))
                            EditorToolButton("Add", Icons.Default.Add) {
                                onNavigateMediaPicker(TrackType.AUDIO)
                            }
                            Spacer(Modifier.width(18.dp))
                            EditorToolButton(EditorTool.SPLIT, Icons.Default.CallSplit) {
                                onEvent(EditorEvent.SplitSelectedClip)
                            }
                            Spacer(Modifier.width(18.dp))
                            if (uiState.selectedClipId != null) {
                                EditorToolButton(EditorTool.VOLUME, Icons.Default.VolumeUp) {
                                    onEvent(EditorEvent.SetVolumeSheetVisible(true))
                                }
                                Spacer(Modifier.width(18.dp))
                                EditorToolButton(EditorTool.SPEED, Icons.Default.Speed) {
                                    onEvent(EditorEvent.SetSpeedSheetVisible(true))
                                }
                                Spacer(Modifier.width(18.dp))
                                EditorToolButton(EditorTool.DUPLICATE, Icons.Default.ContentCopy) {
                                    onEvent(EditorEvent.DuplicateSelectedClip)
                                }
                                Spacer(Modifier.width(18.dp))
                                EditorToolButton(EditorTool.DELETE, Icons.Default.Delete) {
                                    onEvent(EditorEvent.DeleteSelectedClip)
                                }
                                Spacer(Modifier.width(18.dp))
                                EditorToolButton(EditorTool.BEATS, Icons.Default.GraphicEq) {
                                    onTimelineAction(TimelineAction.ToggleBeatMarker(uiState.playheadPositionMs))
                                }
                            }
                        } else if (timelineMode == TimelineMode.TEXT) {
                            EditorToolButton("Back", Icons.AutoMirrored.Filled.ArrowBack) {
                                timelineMode = TimelineMode.MAIN
                                onEvent(EditorEvent.SelectClip(null))
                                onTimelineAction(TimelineAction.SelectClip(null))
                            }
                            Spacer(Modifier.width(18.dp))
                            EditorToolButton("Add", Icons.Default.Add) {
                                onEvent(EditorEvent.SetTextSheetVisible(true))
                            }
                            Spacer(Modifier.width(18.dp))
                            EditorToolButton(EditorTool.SPLIT, Icons.Default.CallSplit) {
                                onEvent(EditorEvent.SplitSelectedClip)
                            }
                            Spacer(Modifier.width(18.dp))
                            if (uiState.selectedClipId != null) {
                                EditorToolButton(EditorTool.TEXT, Icons.Default.Title) {
                                    onEvent(EditorEvent.SetTextSheetVisible(true))
                                }
                                Spacer(Modifier.width(18.dp))
                                EditorToolButton(EditorTool.DUPLICATE, Icons.Default.ContentCopy) {
                                    onEvent(EditorEvent.DuplicateSelectedClip)
                                }
                                Spacer(Modifier.width(18.dp))
                                EditorToolButton(EditorTool.DELETE, Icons.Default.Delete) {
                                    onEvent(EditorEvent.DeleteSelectedClip)
                                }
                                Spacer(Modifier.width(18.dp))
                                EditorToolButton(EditorTool.KEYFRAME, Icons.Default.Star) {
                                    onEvent(EditorEvent.SetKeyframeSheetVisible(true))
                                }
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
        if (uiState.isKeyframeSheetVisible) {
            KeyframeBottomSheet(
                clip = uiState.selectedClip,
                playheadPositionMs = uiState.playheadPositionMs,
                activeProperty = uiState.activeKeyframeProperty,
                onSelectProperty = { onEvent(EditorEvent.SetActiveKeyframeProperty(it)) },
                onAddKeyframe = { clipId, prop, timeMs, value, interp ->
                    onEvent(EditorEvent.AddKeyframe(clipId, prop, timeMs, value, interp))
                },
                onUpdateKeyframe = { clipId, kfId, value, interp ->
                    onEvent(EditorEvent.UpdateKeyframe(clipId, kfId, value, interp))
                },
                onDeleteKeyframe = { clipId, kfId ->
                    onEvent(EditorEvent.DeleteKeyframe(clipId, kfId))
                },
                onSeek = { onEvent(EditorEvent.SeekTo(it)) },
                onDismiss = { onEvent(EditorEvent.SetKeyframeSheetVisible(false)) }
            )
        }
        if (uiState.isBeatsSheetVisible) { BeatsBottomSheet(onDismiss = { onEvent(EditorEvent.SetBeatsSheetVisible(false)) }) }
        if (uiState.isSpeedSheetVisible) {
            SpeedBottomSheet(
                currentSpeed = uiState.selectedClip?.speed ?: 1.0f,
                onSpeedChanged = { newSpeed ->
                    onEvent(EditorEvent.ChangeClipSpeed(newSpeed))
                },
                onDismiss = { onEvent(EditorEvent.SetSpeedSheetVisible(false)) }
            )
        }
        if (uiState.isTransformSheetVisible) {
            TransformBottomSheet(
                currentTransform = uiState.selectedClip?.transform ?: com.example.core.model.Transform.DEFAULT,
                onTransformChanged = { newTransform ->
                    onEvent(EditorEvent.ChangeClipTransform(newTransform))
                },
                onDismiss = { onEvent(EditorEvent.SetTransformSheetVisible(false)) }
            )
        }
        if (uiState.isVolumeSheetVisible) {
            VolumeBottomSheet(
                currentVolume = uiState.selectedClip?.volume ?: 1.0f,
                onVolumeChanged = { newVolume ->
                    onEvent(EditorEvent.ChangeClipVolume(newVolume))
                },
                onDismiss = { onEvent(EditorEvent.SetVolumeSheetVisible(false)) }
            )
        }
        if (uiState.isCanvasSheetVisible) {
            CanvasBottomSheet(
                currentRatio = uiState.project?.aspectRatio ?: com.example.core.model.AspectRatio.RATIO_9_16,
                onRatioSelected = { ratio ->
                    onEvent(EditorEvent.ChangeAspectRatio(ratio))
                    onEvent(EditorEvent.SetCanvasSheetVisible(false))
                },
                onDismiss = { onEvent(EditorEvent.SetCanvasSheetVisible(false)) }
            )
        }
        if (uiState.isTextSheetVisible) {
            val selectedTextData = uiState.selectedClip?.textData
            com.example.feature.editor.text.TextEditorBottomSheet(
                initialText = selectedTextData?.text ?: "Your Text Here",
                initialFontSize = selectedTextData?.fontSize ?: 28f,
                initialColor = selectedTextData?.textColor ?: "#FFFFFF",
                initialFontFamily = selectedTextData?.fontFamily ?: "Default",
                initialAlignment = selectedTextData?.alignment ?: "CENTER",
                onDismiss = { onEvent(EditorEvent.SetTextSheetVisible(false)) },
                onApply = { text, size, color, font, align ->
                    onEvent(EditorEvent.ApplyTextClip(text, size, color, font, align))
                }
            )
        }
        if (uiState.isFiltersSheetVisible) {
            com.example.feature.editor.filter.FiltersBottomSheet(
                filterSettings = uiState.filterSettings,
                onFilterChange = { onEvent(EditorEvent.UpdateFilterSettings(it)) },
                onReset = { onEvent(EditorEvent.ResetFilterSettings) },
                onDismiss = { onEvent(EditorEvent.SetFiltersSheetVisible(false)) }
            )
        }
    }
}

@Composable
fun EditorToolButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun EditorToolButton(tool: EditorTool, icon: ImageVector, onClick: () -> Unit) {
    EditorToolButton(label = tool.label, icon = icon, onClick = onClick)
}
