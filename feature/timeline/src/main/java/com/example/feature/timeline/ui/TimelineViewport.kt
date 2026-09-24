package com.example.feature.timeline.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Asset
import com.example.core.model.Track
import com.example.core.model.TrackType
import com.example.feature.timeline.ui.TimelineGestureHandler.timelinePinchZoomGesture

/**
 * TimelineViewport responsibility:
 * Owns the horizontally scrollable, zoomable timeline area.
 *
 * Composes:
 * ├── TimeRuler
 * ├── VideoTrack
 * ├── OverlayTrack
 * ├── AudioTrack
 * └── TextTrack
 * ├── Beat marker vertical lines
 * └── Playhead
 */
@Composable
fun TimelineViewport(
    scrollState: ScrollState,
    timelineWidthDp: Dp,
    pixelsPerMs: Float,
    totalDurationMs: Long,
    playheadPositionMs: Long,
    beatMarkers: Set<Long>,
    visibleTracks: List<Track>,
    selectionState: TimelineSelectionState,
    selectedKeyframeId: String? = null,
    assets: Map<String, Asset> = emptyMap(),
    timelineMode: TimelineMode = TimelineMode.MAIN,
    zoomLevel: Float = 1.0f,
    onSeek: (Long) -> Unit,
    onToggleBeatMarker: (Long) -> Unit,
    onSelectClip: (String) -> Unit = {},
    onLongPressClip: (String) -> Unit = {},
    onMoveClipDelta: (clipId: String, deltaMs: Long) -> Unit = { _, _ -> },
    onTrimStartDelta: (clipId: String, deltaMs: Long) -> Unit = { _, _ -> },
    onTrimEndDelta: (clipId: String, deltaMs: Long) -> Unit = { _, _ -> },
    onMoveKeyframe: (clipId: String, keyframeId: String, newTimeMs: Long) -> Unit = { _, _, _ -> },
    onSelectKeyframe: (keyframeId: String) -> Unit = {},
    onEditTransition: (firstClipId: String, secondClipId: String) -> Unit = { _, _ -> },
    onAddMedia: () -> Unit = {},
    onAddSubTrackMedia: () -> Unit = {},
    onZoomChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val beatColor = Color(0xFFFF2D75)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .horizontalScroll(scrollState)
            .timelinePinchZoomGesture(
                currentZoom = zoomLevel,
                onZoomChange = onZoomChange
            )
    ) {
        Box(
            modifier = Modifier
                .width(timelineWidthDp)
                .fillMaxHeight()
        ) {
            Column(Modifier.fillMaxSize()) {

                // ── 1. Time Ruler ─────────────────────────────────────────────
                TimeRuler(
                    totalDurationMs = totalDurationMs,
                    pixelsPerMs = pixelsPerMs,
                    onSeek = onSeek,
                    beatMarkers = beatMarkers,
                    onToggleBeatMarker = onToggleBeatMarker
                )

                Spacer(Modifier.height(2.dp))

                // ── 2. Track Lanes (VideoTrack, OverlayTrack, AudioTrack, TextTrack) ─
                if (visibleTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A1F2E))
                            .border(1.5.dp, Color(0xFF2A3050), RoundedCornerShape(8.dp))
                            .clickable {
                                if (timelineMode == TimelineMode.MAIN) onAddMedia() else onAddSubTrackMedia()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                when (timelineMode) {
                                    TimelineMode.OVERLAY -> "Tap to add overlay"
                                    TimelineMode.AUDIO -> "Tap to add audio"
                                    TimelineMode.TEXT -> "Tap to add text"
                                    else -> "Tap to add media"
                                },
                                color = Color.White.copy(.55f), fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    visibleTracks.forEach { track ->
                        when (track.type) {
                            TrackType.VIDEO -> VideoTrack(
                                track = track,
                                selectionState = selectionState,
                                selectedKeyframeId = selectedKeyframeId,
                                pixelsPerMs = pixelsPerMs,
                                assets = assets,
                                onSelectClip = onSelectClip,
                                onLongPressClip = onLongPressClip,
                                onSeek = onSeek,
                                onMoveClipDelta = onMoveClipDelta,
                                onTrimStartDelta = onTrimStartDelta,
                                onTrimEndDelta = onTrimEndDelta,
                                onMoveKeyframe = onMoveKeyframe,
                                onSelectKeyframe = onSelectKeyframe,
                                onEditTransition = onEditTransition
                            )
                            TrackType.OVERLAY -> OverlayTrack(
                                track = track,
                                selectionState = selectionState,
                                selectedKeyframeId = selectedKeyframeId,
                                pixelsPerMs = pixelsPerMs,
                                assets = assets,
                                onSelectClip = onSelectClip,
                                onLongPressClip = onLongPressClip,
                                onSeek = onSeek,
                                onMoveClipDelta = onMoveClipDelta,
                                onTrimStartDelta = onTrimStartDelta,
                                onTrimEndDelta = onTrimEndDelta,
                                onMoveKeyframe = onMoveKeyframe,
                                onSelectKeyframe = onSelectKeyframe
                            )
                            TrackType.AUDIO -> AudioTrack(
                                track = track,
                                selectionState = selectionState,
                                selectedKeyframeId = selectedKeyframeId,
                                pixelsPerMs = pixelsPerMs,
                                assets = assets,
                                onSelectClip = onSelectClip,
                                onLongPressClip = onLongPressClip,
                                onSeek = onSeek,
                                onMoveClipDelta = onMoveClipDelta,
                                onTrimStartDelta = onTrimStartDelta,
                                onTrimEndDelta = onTrimEndDelta,
                                onMoveKeyframe = onMoveKeyframe,
                                onSelectKeyframe = onSelectKeyframe
                            )
                            TrackType.TEXT -> TextTrack(
                                track = track,
                                selectionState = selectionState,
                                selectedKeyframeId = selectedKeyframeId,
                                pixelsPerMs = pixelsPerMs,
                                assets = assets,
                                onSelectClip = onSelectClip,
                                onLongPressClip = onLongPressClip,
                                onSeek = onSeek,
                                onMoveClipDelta = onMoveClipDelta,
                                onTrimStartDelta = onTrimStartDelta,
                                onTrimEndDelta = onTrimEndDelta,
                                onMoveKeyframe = onMoveKeyframe,
                                onSelectKeyframe = onSelectKeyframe
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }

            // ── 3. Beat Marker vertical lines spanning all tracks ────────────
            beatMarkers.forEach { beatMs ->
                val xDp = (beatMs * pixelsPerMs).dp
                Box(
                    Modifier
                        .offset(x = xDp)
                        .width(1.5.dp)
                        .fillMaxHeight()
                        .background(beatColor.copy(alpha = 0.55f))
                )
            }

            // ── 4. Playhead ──────────────────────────────────────────────────
            Playhead(
                playheadPositionMs = playheadPositionMs,
                pixelsPerMs = pixelsPerMs,
                maxDurationMs = totalDurationMs,
                onSeek = onSeek,
                onTapPlayhead = { onToggleBeatMarker(playheadPositionMs) }
            )
        }
    }
}
