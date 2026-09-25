package com.example.feature.timeline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Asset
import com.example.core.model.Track
import com.example.core.model.TrackType
import com.example.feature.timeline.engine.TimelineAction
import com.example.feature.timeline.engine.TimelineEngineState
import com.example.feature.timeline.engine.TimelineUtils
import kotlin.math.max

enum class TimelineMode { MAIN, OVERLAY, AUDIO, TEXT }

/**
 * High-level Timeline UI composition layer.
 *
 * Target Conceptual Architecture:
 * TimelineContainer
 * ├── TimelineHeader
 * ├── TimelineViewport
 * │   ├── TimeRuler
 * │   ├── VideoTrack
 * │   ├── OverlayTrack
 * │   ├── AudioTrack
 * │   └── TextTrack
 * ├── TimelineGestureHandler
 * ├── Playhead
 * └── TimelineSelection
 *
 * Connects timeline UI to TimelineEngineState, coordinates UI-only state (scroll, zoom),
 * and dispatches all user interactions through TimelineAction.
 */
@Composable
fun TimelineContainer(
    state: TimelineEngineState,
    isPlaying: Boolean,
    assets: Map<String, Asset> = emptyMap(),
    timelineMode: TimelineMode = TimelineMode.MAIN,
    multiSelectedClipIds: Set<String> = emptySet(),
    onBackToMain: () -> Unit = {},
    onAddSubTrackMedia: () -> Unit = {},
    onAction: (TimelineAction) -> Unit,
    onPlayPause: () -> Unit = {},
    onAddMedia: () -> Unit = {},
    onLongPressClip: (clipId: String) -> Unit = {},
    onMoveKeyframe: (clipId: String, keyframeId: String, newTimeMs: Long) -> Unit = { _, _, _ -> },
    onSelectKeyframe: (keyframeId: String) -> Unit = {},
    onEditTransition: (firstClipId: String, secondClipId: String) -> Unit = { _, _ -> },
    onSwitchMode: (TimelineMode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val pixelsPerMs = remember(state.zoomLevel) {
        TimelineUtils.calculatePixelsPerMs(state.zoomLevel)
    }

    val totalDurationMs by remember(state.durationMs) {
        derivedStateOf { max(state.durationMs + 8000L, 20000L) }
    }

    val timelineWidthDp by remember(totalDurationMs, pixelsPerMs) {
        derivedStateOf { (totalDurationMs * pixelsPerMs).dp }
    }

    // Auto-scroll when playing so playhead stays centered in the viewport
    LaunchedEffect(state.playheadPositionMs, pixelsPerMs, isPlaying) {
        if (isPlaying) {
            val playheadPx = (state.playheadPositionMs * pixelsPerMs)
            val centerOffset = (playheadPx - scrollState.viewportSize / 2f).coerceAtLeast(0f)
            scrollState.scrollTo(centerOffset.toInt())
        }
    }

    val selectionState = rememberTimelineSelection(
        selectedClipId = state.selectedClipId,
        multiSelectedClipIds = multiSelectedClipIds
    )

    val visibleTracks = remember(state.tracks, timelineMode) {
        when (timelineMode) {
            TimelineMode.MAIN -> {
                val orderMap = mapOf(
                    TrackType.VIDEO to 0,
                    TrackType.OVERLAY to 1,
                    TrackType.TEXT to 2,
                    TrackType.AUDIO to 3
                )
                state.tracks.sortedWith(
                    compareBy<Track> { orderMap[it.type] ?: 4 }.thenBy { it.order }
                )
            }
            TimelineMode.OVERLAY -> state.tracks.filter { it.type == TrackType.OVERLAY }
            TimelineMode.AUDIO -> state.tracks.filter { it.type == TrackType.AUDIO }
            TimelineMode.TEXT -> state.tracks.filter { it.type == TrackType.TEXT }
        }
    }

    val bgColor = Color(0xFF0A0D14)

    Column(modifier = modifier.background(bgColor)) {

        // ── 1. Timeline Header (Zoom, Snapping, Beat markers, Sub-modes) ──────
        TimelineHeader(
            timelineMode = timelineMode,
            zoomLevel = state.zoomLevel,
            isSnappingEnabled = state.isSnappingEnabled,
            beatMarkerCount = state.beatMarkers.size,
            playheadPositionMs = state.playheadPositionMs,
            onBackToMain = onBackToMain,
            onAddSubTrackMedia = onAddSubTrackMedia,
            onToggleSnapping = { onAction(TimelineAction.SetSnapping(!state.isSnappingEnabled)) },
            onZoomChange = { newZoom -> onAction(TimelineAction.SetZoom(newZoom)) },
            onToggleBeatMarker = { ms -> onAction(TimelineAction.ToggleBeatMarker(ms)) },
            onSwitchMode = onSwitchMode
        )

        // ── 2. Timeline Body (Layer Sidebar + Viewport) ────────────────────────
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {

            // Left layer control sidebar
            LayerSidebar(
                visibleTracks = visibleTracks,
                timelineMode = timelineMode,
                onAction = onAction,
                onAddMedia = onAddMedia,
                onAddSubTrackMedia = onAddSubTrackMedia
            )

            // 1px vertical separator
            Box(Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(.06f)))

            // Horizontal scrolling viewport coordinating TimeRuler, tracks, and Playhead
            TimelineViewport(
                scrollState = scrollState,
                timelineWidthDp = timelineWidthDp,
                pixelsPerMs = pixelsPerMs,
                totalDurationMs = totalDurationMs,
                playheadPositionMs = state.playheadPositionMs,
                beatMarkers = state.beatMarkers,
                visibleTracks = visibleTracks,
                selectionState = selectionState,
                selectedKeyframeId = state.selectedKeyframeId,
                assets = assets,
                timelineMode = timelineMode,
                zoomLevel = state.zoomLevel,
                onSeek = { ms -> onAction(TimelineAction.Seek(ms)) },
                onToggleBeatMarker = { ms -> onAction(TimelineAction.ToggleBeatMarker(ms)) },
                onSelectClip = { id -> onAction(TimelineAction.SelectClip(id)) },
                onLongPressClip = onLongPressClip,
                onMoveClipDelta = { id, deltaMs ->
                    val track = visibleTracks.find { t -> t.clips.any { it.id == id } }
                    val clip = track?.clips?.find { it.id == id }
                    if (track != null && clip != null) {
                        onAction(TimelineAction.MoveClip(id, track.id, (clip.startTimeMs + deltaMs).coerceAtLeast(0L)))
                    }
                },
                onTrimStartDelta = { id, deltaMs ->
                    val track = visibleTracks.find { t -> t.clips.any { it.id == id } }
                    val clip = track?.clips?.find { it.id == id }
                    if (clip != null) {
                        onAction(TimelineAction.TrimStart(id, clip.startTimeMs + deltaMs))
                    }
                },
                onTrimEndDelta = { id, deltaMs ->
                    val track = visibleTracks.find { t -> t.clips.any { it.id == id } }
                    val clip = track?.clips?.find { it.id == id }
                    if (clip != null) {
                        onAction(TimelineAction.TrimEnd(id, clip.endTimeMs + deltaMs))
                    }
                },
                onMoveKeyframe = onMoveKeyframe,
                onSelectKeyframe = { kfId ->
                    onAction(TimelineAction.SelectKeyframe(kfId))
                    onSelectKeyframe(kfId)
                },
                onEditTransition = onEditTransition,
                onAddMedia = onAddMedia,
                onAddSubTrackMedia = onAddSubTrackMedia,
                onZoomChange = { newZoom -> onAction(TimelineAction.SetZoom(newZoom)) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * LayerSidebar: Compact layer-control area on the left side (Alight Motion & CapCut inspired).
 * Displays:
 * - Header aligned with TimeRuler (30.dp height)
 * - Row for each visible track (64.dp height matching track lane)
 * - Layer Type Badge with icon and name (Video, Overlay, Text, Audio)
 * - Visibility toggle (Eye icon: Visibility / VisibilityOff)
 * - Lock toggle (Lock icon: Lock / LockOpen)
 * - Mute toggle (Speaker icon: VolumeUp / VolumeOff) for Audio / Video
 */
@Composable
private fun LayerSidebar(
    visibleTracks: List<Track>,
    timelineMode: TimelineMode,
    onAction: (TimelineAction) -> Unit,
    onAddMedia: () -> Unit,
    onAddSubTrackMedia: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(66.dp)
            .fillMaxHeight()
            .background(Color(0xFF0F121A))
    ) {
        // Aligned with TimeRuler (30.dp height)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(Color(0xFF13171F))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "LAYERS",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2838))
                    .clickable {
                        if (timelineMode == TimelineMode.MAIN) onAddMedia() else onAddSubTrackMedia()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Track",
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        }

        Spacer(Modifier.height(2.dp))

        if (visibleTracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clickable {
                        if (timelineMode == TimelineMode.MAIN) onAddMedia() else onAddSubTrackMedia()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2230))
                            .border(1.dp, Color(0xFF384055), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Add Layer", color = Color.White.copy(alpha = 0.45f), fontSize = 9.sp)
                }
            }
        } else {
            visibleTracks.forEach { track ->
                val isMuted = track.clips.isNotEmpty() && track.clips.all { (it.volume ?: 1f) == 0f }
                val trackAccent = when (track.type) {
                    TrackType.VIDEO -> Color(0xFF6B4BFF)
                    TrackType.OVERLAY -> Color(0xFF9C27B0)
                    TrackType.TEXT -> Color(0xFF4CAF50)
                    TrackType.AUDIO -> Color(0xFF00D2FF)
                }
                val trackLabel = when (track.type) {
                    TrackType.VIDEO -> "Video"
                    TrackType.OVERLAY -> "Overlay"
                    TrackType.TEXT -> "Text"
                    TrackType.AUDIO -> "Audio"
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(Color(0xFF0D1018))
                        .border(
                            width = 0.5.dp,
                            color = if (track.isLocked) Color(0xFFFFB74D).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.05f)
                        )
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Track title indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(trackAccent)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = trackLabel,
                                color = Color.White.copy(alpha = if (track.isVisible) 0.85f else 0.4f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }

                        // Layer control buttons: Visibility, Lock, Mute
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Visibility Toggle (Eye)
                            Icon(
                                imageVector = if (track.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (track.isVisible) "Hide layer" else "Show layer",
                                tint = if (track.isVisible) Color.White.copy(alpha = 0.85f) else Color(0xFFEF5350),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onAction(TimelineAction.ToggleTrackVisibility(track.id)) }
                            )

                            // Lock Toggle (Lock)
                            Icon(
                                imageVector = if (track.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = if (track.isLocked) "Unlock layer" else "Lock layer",
                                tint = if (track.isLocked) Color(0xFFFFB74D) else Color.White.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onAction(TimelineAction.ToggleTrackLock(track.id)) }
                            )

                            // Mute Toggle (Speaker) - for Audio and Video tracks
                            if (track.type == TrackType.AUDIO || track.type == TrackType.VIDEO) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = if (isMuted) "Unmute layer" else "Mute layer",
                                    tint = if (isMuted) Color(0xFFEF5350) else Color.White.copy(alpha = 0.55f),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onAction(TimelineAction.MuteTrack(track.id, !isMuted)) }
                                )
                            } else {
                                Spacer(Modifier.width(16.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
