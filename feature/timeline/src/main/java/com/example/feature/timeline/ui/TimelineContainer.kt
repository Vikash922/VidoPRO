package com.example.feature.timeline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Track
import com.example.core.model.TrackType
import com.example.core.ui.theme.AppSpacing
import com.example.feature.timeline.engine.TimelineAction
import com.example.feature.timeline.engine.TimelineEngineState
import com.example.feature.timeline.engine.TimelineUtils
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Main Timeline Container — matches the reference UI design exactly.
 * Left side: track type labels (Cover, text icon, + for audio).
 * Right side: scrollable timeline with TimeRuler, ClipCards, Playhead.
 */
@Composable
fun TimelineContainer(
    state: TimelineEngineState,
    isPlaying: Boolean,
    onAction: (TimelineAction) -> Unit,
    onPlayPause: () -> Unit,
    onAddMedia: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val pixelsPerMs = remember(state.zoomLevel) {
        TimelineUtils.calculatePixelsPerMs(state.zoomLevel)
    }

    val totalTimelineDurationMs by remember(state.durationMs) {
        derivedStateOf { max(state.durationMs + 5000L, 20000L) }
    }

    val timelineWidthDp by remember(totalTimelineDurationMs, pixelsPerMs) {
        derivedStateOf { (totalTimelineDurationMs * pixelsPerMs).dp }
    }

    val onSeekAction = remember(onAction) {
        { timeMs: Long -> onAction(TimelineAction.Seek(timeMs)) }
    }

    val bgColor = Color(0xFF0A0D14)

    Column(
        modifier = modifier.background(bgColor)
    ) {
        // Main timeline area: Left track labels + Right scrollable tracks
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {

            // LEFT SIDE: Track type labels
            Column(
                modifier = Modifier
                    .width(40.dp)
                    .fillMaxHeight()
                    .background(bgColor)
                    .padding(top = 28.dp) // Offset for time ruler height
            ) {
                Spacer(modifier = Modifier.height(AppSpacing.xs))

                if (state.tracks.isEmpty()) {
                    // Empty state — just show add button
                    Box(
                        modifier = Modifier
                            .size(40.dp, 56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF7B61FF))
                                .clickable { onAddMedia() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else {
                    state.tracks.forEach { track ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when (track.type) {
                                TrackType.VIDEO -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Image,
                                            contentDescription = "Video",
                                            tint = Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            "Cover",
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 8.sp
                                        )
                                    }
                                }
                                TrackType.TEXT, TrackType.OVERLAY -> {
                                    Icon(
                                        Icons.Default.TextFields,
                                        contentDescription = "Text",
                                        tint = Color(0xFF4CAF50).copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                TrackType.AUDIO -> {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00B4D8).copy(alpha = 0.2f))
                                            .clickable { onAddMedia() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Add Audio",
                                            tint = Color(0xFF00B4D8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            // Thin separator line
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.06f))
            )

            // RIGHT SIDE: Scrollable timeline
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(scrollState)
            ) {
                Box(modifier = Modifier.width(timelineWidthDp).fillMaxHeight()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 1. Time Ruler
                        TimeRuler(
                            totalDurationMs = totalTimelineDurationMs,
                            pixelsPerMs = pixelsPerMs,
                            onSeek = onSeekAction
                        )

                        Spacer(modifier = Modifier.height(AppSpacing.xs))

                        // 2. Track Lanes
                        if (state.tracks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(horizontal = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF161925))
                                    .clickable { onAddMedia() },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF7B61FF), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tap to add media", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                }
                            }
                        } else {
                            state.tracks.forEach { track ->
                                TrackLane(
                                    track = track,
                                    selectedClipId = state.selectedClipId,
                                    pixelsPerMs = pixelsPerMs,
                                    onAction = onAction,
                                    onAddMedia = onAddMedia
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }

                    // 3. Playhead Overlay
                    PlayheadView(
                        playheadPositionMs = state.playheadPositionMs,
                        pixelsPerMs = pixelsPerMs,
                        onSeekDelta = { deltaMs ->
                            val newTime = (state.playheadPositionMs + deltaMs).coerceIn(0L, state.durationMs)
                            onAction(TimelineAction.Seek(newTime))
                        },
                        modifier = Modifier.offset {
                            val currentPlayheadPx = (state.playheadPositionMs * pixelsPerMs).roundToInt()
                            IntOffset(currentPlayheadPx - 12.dp.roundToPx(), 0)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackLane(
    track: Track,
    selectedClipId: String?,
    pixelsPerMs: Float,
    onAction: (TimelineAction) -> Unit,
    onAddMedia: () -> Unit,
    modifier: Modifier = Modifier
) {
    val laneHeight = 56.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(laneHeight)
            .background(Color(0xFF0A0D14))
            .padding(vertical = 2.dp)
    ) {
        track.clips.forEach { clip ->
            val isSelected = clip.id == selectedClipId

            val onSelectClip = remember(clip.id, onAction) {
                { onAction(TimelineAction.SelectClip(clip.id)) }
            }
            val onMoveClipDelta = remember(clip.id, clip.trackId, clip.startTimeMs, onAction) {
                { deltaMs: Long ->
                    val newStartTime = (clip.startTimeMs + deltaMs).coerceAtLeast(0L)
                    onAction(TimelineAction.MoveClip(clip.id, clip.trackId, newStartTime))
                }
            }
            val onTrimStartClipDelta = remember(clip.id, clip.startTimeMs, onAction) {
                { deltaMs: Long ->
                    val newStartTime = clip.startTimeMs + deltaMs
                    onAction(TimelineAction.TrimStart(clip.id, newStartTime))
                }
            }
            val onTrimEndClipDelta = remember(clip.id, clip.endTimeMs, onAction) {
                { deltaMs: Long ->
                    val newEndTime = clip.endTimeMs + deltaMs
                    onAction(TimelineAction.TrimEnd(clip.id, newEndTime))
                }
            }

            ClipCard(
                clip = clip,
                isSelected = isSelected,
                pixelsPerMs = pixelsPerMs,
                onSelect = onSelectClip,
                onMoveDelta = onMoveClipDelta,
                onTrimStartDelta = onTrimStartClipDelta,
                onTrimEndDelta = onTrimEndClipDelta,
                modifier = Modifier.offset {
                    val clipOffsetPx = (clip.startTimeMs * pixelsPerMs).roundToInt()
                    IntOffset(clipOffsetPx, 0)
                }
            )
        }
    }
}
