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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.common.TimeUtils
import com.example.core.model.Track
import com.example.core.model.TrackType
import com.example.core.ui.theme.AppRadius
import com.example.core.ui.theme.AppSpacing
import com.example.core.ui.theme.EditorColors
import com.example.feature.timeline.engine.TimelineAction
import com.example.feature.timeline.engine.TimelineEngineState
import com.example.feature.timeline.engine.TimelineUtils
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Main Timeline Container containing TimeRuler, Track lanes, ClipCards, Playhead,
 * and quick editing controls for Split, Delete, Duplicate, and Add Media.
 * Optimized for 60fps scrolling and playhead movement (DEV-073, DEV-074).
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

    // Minimum timeline width calculated and cached with derivedStateOf
    val totalTimelineDurationMs by remember(state.durationMs) {
        derivedStateOf { max(state.durationMs + 5000L, 20000L) }
    }

    val timelineWidthDp by remember(totalTimelineDurationMs, pixelsPerMs) {
        derivedStateOf { (totalTimelineDurationMs * pixelsPerMs).dp }
    }

    // Stable action callbacks
    val onSplitClick = remember(state.selectedClipId, onAction) {
        { onAction(TimelineAction.SplitAtPlayhead(state.selectedClipId)) }
    }

    val onDeleteClick = remember(state.selectedClipId, onAction) {
        {
            state.selectedClipId?.let { clipId ->
                onAction(TimelineAction.DeleteClip(clipId))
            }
            Unit
        }
    }

    val onDuplicateClick = remember(state.selectedClipId, onAction) {
        {
            state.selectedClipId?.let { clipId ->
                onAction(TimelineAction.DuplicateClip(clipId))
            }
            Unit
        }
    }

    val onZoomOutClick = remember(state.zoomLevel, onAction) {
        { onAction(TimelineAction.SetZoom(state.zoomLevel * 0.8f)) }
    }

    val onZoomInClick = remember(state.zoomLevel, onAction) {
        { onAction(TimelineAction.SetZoom(state.zoomLevel * 1.25f)) }
    }

    val onSeekAction = remember(onAction) {
        { timeMs: Long -> onAction(TimelineAction.Seek(timeMs)) }
    }

    Column(
        modifier = modifier
            .background(EditorColors.timelineBackground)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline)
    ) {
        // Controls Row: Timecode, Quick Actions (Split, Delete, Duplicate), Play/Pause
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = TimeUtils.formatTimecode(state.playheadPositionMs),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = AppSpacing.xs)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Split at Playhead button
                IconButton(
                    onClick = onSplitClick,
                    modifier = Modifier.size(32.dp).testTag("timeline_split_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallSplit,
                        contentDescription = "Split",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete selected clip button
                if (state.selectedClipId != null) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp).testTag("timeline_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Duplicate selected clip button
                    IconButton(
                        onClick = onDuplicateClick,
                        modifier = Modifier.size(32.dp).testTag("timeline_duplicate_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Duplicate",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Zoom Out
                IconButton(
                    onClick = onZoomOutClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Zoom Out",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Zoom In
                IconButton(
                    onClick = onZoomInClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom In",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Add Media button
                IconButton(
                    onClick = onAddMedia,
                    modifier = Modifier.size(32.dp).testTag("timeline_add_media_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Media",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Play / Pause toggle
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(32.dp).testTag("timeline_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Scrollable Timeline Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .horizontalScroll(scrollState)
        ) {
            Box(modifier = Modifier.width(timelineWidthDp).fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. Time Ruler Canvas
                    TimeRuler(
                        totalDurationMs = totalTimelineDurationMs,
                        pixelsPerMs = pixelsPerMs,
                        onSeek = onSeekAction
                    )

                    Spacer(modifier = Modifier.height(AppSpacing.xs))

                    // 2. Track Lanes
                    if (state.tracks.isEmpty()) {
                        // Empty timeline hint
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .padding(horizontal = AppSpacing.md)
                                .clip(RoundedCornerShape(AppRadius.clip))
                                .background(EditorColors.clipVideo.copy(alpha = 0.15f))
                                .clickable { onAddMedia() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tap + or here to add media",
                                style = MaterialTheme.typography.bodyMedium,
                                color = EditorColors.clipVideo
                            )
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

                // 3. Playhead Overlay Line with fast lambda-based offset
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

                // 4. Selected clip highlight overlay (visual feedback)
                if (state.selectedClipId != null) {
                    val selectedClip = state.tracks.flatMap { it.clips }.find { it.id == state.selectedClipId }
                    if (selectedClip != null) {
                        Box(
                            modifier = Modifier
                                .offset {
                                    val clipStartPx = (selectedClip.startTimeMs * pixelsPerMs).roundToInt()
                                    IntOffset(clipStartPx, 0)
                                }
                                .width((selectedClip.durationMs * pixelsPerMs).dp)
                                .height(110.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        )
                    }
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
    val trackBg = remember(track.type) {
        when (track.type) {
            TrackType.VIDEO -> Color(0xFF14141E)
            TrackType.AUDIO -> Color(0xFF14191E)
            else -> Color(0xFF1A161E)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(laneHeight)
            .background(trackBg)
            .padding(vertical = 2.dp)
    ) {
        // Clips inside track
        track.clips.forEach { clip ->
            val isSelected = clip.id == selectedClipId

            // Memoize callbacks per clip to avoid extra lambda allocations per frame
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

