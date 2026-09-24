package com.example.feature.timeline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
            TimelineMode.MAIN -> state.tracks.filter { it.type == TrackType.VIDEO }
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

        // ── 2. Timeline Body (Sidebar + Viewport) ─────────────────────────────
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {

            // Left track type indicator sidebar
            TrackSidebar(
                visibleTracks = visibleTracks,
                timelineMode = timelineMode,
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
 * Sidebar displaying track type badges (Video, Overlay, Text, Audio).
 */
@Composable
private fun TrackSidebar(
    visibleTracks: List<Track>,
    timelineMode: TimelineMode,
    onAddMedia: () -> Unit,
    onAddSubTrackMedia: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(44.dp)
            .fillMaxHeight()
            .background(Color(0xFF0D1018))
            .padding(top = 32.dp) // align below time ruler
    ) {
        if (visibleTracks.isEmpty()) {
            Box(
                Modifier
                    .size(44.dp, 64.dp)
                    .clickable {
                        if (timelineMode == TimelineMode.MAIN) onAddMedia() else onAddSubTrackMedia()
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E2230))
                        .border(1.dp, Color(0xFF384055), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        } else {
            visibleTracks.forEach { track ->
                Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
                    when (track.type) {
                        TrackType.VIDEO -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Image, null, tint = Color.White.copy(.55f), modifier = Modifier.size(17.dp))
                            Text("Video", color = Color.White.copy(.4f), fontSize = 8.sp)
                        }
                        TrackType.OVERLAY -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Layers, null, tint = Color(0xFF9C27B0).copy(.9f), modifier = Modifier.size(17.dp))
                            Text("OVL", color = Color.White.copy(.4f), fontSize = 8.sp)
                        }
                        TrackType.TEXT -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.TextFields, null, tint = Color(0xFF4CAF50).copy(.9f), modifier = Modifier.size(17.dp))
                            Text("Text", color = Color.White.copy(.4f), fontSize = 8.sp)
                        }
                        TrackType.AUDIO -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F3658))
                                    .border(1.dp, Color(0xFF00D2FF), CircleShape)
                                    .clickable { onAddSubTrackMedia() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, null, tint = Color(0xFF00D2FF), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
