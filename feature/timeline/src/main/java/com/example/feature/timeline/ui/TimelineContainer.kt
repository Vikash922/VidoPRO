package com.example.feature.timeline.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.text.font.FontWeight
import com.example.core.model.Asset
import com.example.core.model.Track
import com.example.core.model.TrackType
import com.example.core.ui.theme.AppSpacing
import com.example.feature.timeline.engine.TimelineAction
import com.example.feature.timeline.engine.TimelineEngineState
import com.example.feature.timeline.engine.TimelineUtils
import kotlin.math.max
import kotlin.math.roundToInt

enum class TimelineMode {
    MAIN,
    OVERLAY,
    AUDIO,
    TEXT
}

/**
 * Main Timeline Container — Flat dark mode without gradients.
 * Features:
 * - Left side track indicators ("Cover", Text, Audio Add)
 * - CapCut-style Fold / Unfold dedicated sub-timeline modes (Overlay, Audio, Text)
 * - Time ruler
 * - Beat markers (Pink vertical line highlighting beat positions)
 * - Sleek 1dp playhead line and smooth dragging
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
    onPlayPause: () -> Unit,
    onAddMedia: () -> Unit,
    onLongPressClip: (clipId: String) -> Unit = {},
    onMoveKeyframe: (clipId: String, keyframeId: String, newTimeMs: Long) -> Unit = { _, _, _ -> },
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

    // Flat solid dark background (NO GRADIENT)
    val bgColor = Color(0xFF0A0D14)
    val pinkBeatColor = Color(0xFFFF2D75)

    val visibleTracks = remember(state.tracks, timelineMode) {
        when (timelineMode) {
            TimelineMode.MAIN -> state.tracks.filter { it.type == TrackType.VIDEO }
            TimelineMode.OVERLAY -> state.tracks.filter { it.type == TrackType.OVERLAY }
            TimelineMode.AUDIO -> state.tracks.filter { it.type == TrackType.AUDIO }
            TimelineMode.TEXT -> state.tracks.filter { it.type == TrackType.TEXT }
        }
    }

    Column(
        modifier = modifier.background(bgColor)
    ) {
        // CapCut-style Sub-Timeline Header when in dedicated Overlay / Audio / Text mode
        if (timelineMode != TimelineMode.MAIN) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(Color(0xFF161925))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onBackToMain() }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (timelineMode) {
                            TimelineMode.OVERLAY -> "Overlay Track"
                            TimelineMode.AUDIO -> "Audio Track"
                            TimelineMode.TEXT -> "Text Track"
                            else -> ""
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E2230))
                        .border(1.dp, Color(0xFF384055), RoundedCornerShape(4.dp))
                        .clickable { onAddSubTrackMedia() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (timelineMode) {
                            TimelineMode.OVERLAY -> "Add Overlay"
                            TimelineMode.AUDIO -> "Add Audio"
                            TimelineMode.TEXT -> "Add Text"
                            else -> "Add"
                        },
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Main timeline area: Left track labels + Right scrollable tracks
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {

            // LEFT SIDE: Track type labels
            Column(
                modifier = Modifier
                    .width(42.dp)
                    .fillMaxHeight()
                    .background(bgColor)
                    .padding(top = 28.dp) // Offset for time ruler height
            ) {
                Spacer(modifier = Modifier.height(AppSpacing.xs))

                if (visibleTracks.isEmpty()) {
                    Box(
                        modifier = Modifier.size(42.dp, 56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E2230))
                                .border(1.dp, Color(0xFF2C3448), CircleShape)
                                .clickable {
                                    if (timelineMode == TimelineMode.MAIN) onAddMedia() else onAddSubTrackMedia()
                                },
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
                    visibleTracks.forEach { track ->
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
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                                TrackType.TEXT, TrackType.OVERLAY -> {
                                    Icon(
                                        Icons.Default.TextFields,
                                        contentDescription = "Text",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                TrackType.AUDIO -> {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0F3658))
                                            .border(1.dp, Color(0xFF00D2FF), CircleShape)
                                            .clickable { onAddSubTrackMedia() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Add Audio",
                                            tint = Color(0xFF00D2FF),
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

            // Thin vertical separator line
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.08f))
            )

            // RIGHT SIDE: Scrollable timeline
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(scrollState)
            ) {
                Box(
                    modifier = Modifier
                        .width(timelineWidthDp)
                        .fillMaxHeight()
                        .pointerInput(pixelsPerMs, totalTimelineDurationMs) {
                            detectTapGestures { offset ->
                                onAction(TimelineAction.SelectClip(null))
                                val tappedTime = (offset.x / pixelsPerMs).toLong().coerceIn(0L, totalTimelineDurationMs)
                                onSeekAction(tappedTime)
                            }
                        }
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 1. Time Ruler
                        TimeRuler(
                            totalDurationMs = totalTimelineDurationMs,
                            pixelsPerMs = pixelsPerMs,
                            onSeek = onSeekAction
                        )

                        Spacer(modifier = Modifier.height(AppSpacing.xs))

                        // 2. Track Lanes
                        if (visibleTracks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(horizontal = 8.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E2230))
                                    .border(1.dp, Color(0xFF384055), RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (timelineMode == TimelineMode.MAIN) onAddMedia() else onAddSubTrackMedia()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when (timelineMode) {
                                            TimelineMode.OVERLAY -> "Tap to add overlay"
                                            TimelineMode.AUDIO -> "Tap to add audio"
                                            TimelineMode.TEXT -> "Tap to add text"
                                            else -> "Tap to add media"
                                        },
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            visibleTracks.forEach { track ->
                                TrackLane(
                                    track = track,
                                    selectedClipId = state.selectedClipId,
                                    multiSelectedClipIds = multiSelectedClipIds,
                                    pixelsPerMs = pixelsPerMs,
                                    assets = assets,
                                    onAction = onAction,
                                    onSeek = onSeekAction,
                                    onAddMedia = {
                                        if (timelineMode == TimelineMode.MAIN) onAddMedia() else onAddSubTrackMedia()
                                    },
                                    onLongPressClip = onLongPressClip,
                                    onMoveKeyframe = onMoveKeyframe
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                        }
                    }

                    // 3. Pink Beat Marker Lines (Highlighting exact beat time)
                    state.beatMarkers.forEach { beatTimeMs ->
                        val markerX = (beatTimeMs * pixelsPerMs).dp
                        Box(
                            modifier = Modifier
                                .offset(x = markerX)
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(pinkBeatColor)
                        )
                        // Top pink diamond icon
                        Box(
                            modifier = Modifier
                                .offset(x = markerX - 4.dp, y = 2.dp)
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(pinkBeatColor)
                        )
                    }

                    // 4. Pink Playhead Scrubber Line & Handle
                    PlayheadView(
                        playheadPositionMs = state.playheadPositionMs,
                        pixelsPerMs = pixelsPerMs,
                        onSeekDelta = { deltaMs ->
                            val newTime = (state.playheadPositionMs + deltaMs).coerceIn(0L, state.durationMs)
                            onAction(TimelineAction.Seek(newTime))
                        },
                        onTapPlayhead = {
                            onAction(TimelineAction.ToggleBeatMarker(state.playheadPositionMs))
                        },
                        modifier = Modifier.offset {
                            val currentPlayheadPx = (state.playheadPositionMs * pixelsPerMs).roundToInt()
                            IntOffset(currentPlayheadPx - 22.dp.roundToPx(), 0)
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
    multiSelectedClipIds: Set<String> = emptySet(),
    pixelsPerMs: Float,
    assets: Map<String, Asset>,
    onAction: (TimelineAction) -> Unit,
    onSeek: (Long) -> Unit,
    onAddMedia: () -> Unit,
    onLongPressClip: (clipId: String) -> Unit = {},
    onMoveKeyframe: (clipId: String, keyframeId: String, newTimeMs: Long) -> Unit = { _, _, _ -> },
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
            val isMultiSelected = multiSelectedClipIds.contains(clip.id)

            val onSelectClip = remember(clip.id, onAction) {
                { onAction(TimelineAction.SelectClip(clip.id)) }
            }
            val onLongPressClipCb = remember(clip.id, onLongPressClip) {
                { onLongPressClip(clip.id) }
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
            val onMoveKf = remember(clip.id, onMoveKeyframe) {
                { keyframeId: String, newTimeMs: Long -> onMoveKeyframe(clip.id, keyframeId, newTimeMs) }
            }

            ClipCard(
                clip = clip,
                isSelected = isSelected,
                isMultiSelected = isMultiSelected,
                pixelsPerMs = pixelsPerMs,
                assets = assets,
                onSelect = onSelectClip,
                onLongPress = onLongPressClipCb,
                onSeek = onSeek,
                onMoveDelta = onMoveClipDelta,
                onTrimStartDelta = onTrimStartClipDelta,
                onTrimEndDelta = onTrimEndClipDelta,
                onMoveKeyframe = onMoveKf,
                modifier = Modifier.offset {
                    val clipOffsetPx = (clip.startTimeMs * pixelsPerMs).roundToInt()
                    IntOffset(clipOffsetPx, 0)
                }
            )
        }
    }
}

