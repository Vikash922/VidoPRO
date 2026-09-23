package com.example.feature.timeline.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Asset
import com.example.core.model.Track
import com.example.core.model.TrackType
import com.example.core.ui.theme.AppSpacing
import com.example.feature.timeline.engine.TimelineAction
import com.example.feature.timeline.engine.TimelineEngineState
import com.example.feature.timeline.engine.TimelineUtils
import kotlin.math.max
import kotlin.math.roundToInt

enum class TimelineMode { MAIN, OVERLAY, AUDIO, TEXT }

/**
 * CapCut/VN-style Timeline:
 * - Fixed center playhead — clips scroll UNDER the playhead (like CapCut)
 * - Tap on time ruler → add/remove beat marker at that position
 * - Beat markers survive split/trim
 * - Thick clips (64dp) with thumbnails
 * - Sub-timelines fold/unfold per type
 * - Haptic feedback on scrub
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
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
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

    // CapCut-style: playhead is at center of visible area → scroll so playhead is centered
    LaunchedEffect(state.playheadPositionMs, pixelsPerMs) {
        if (isPlaying) {
            val playheadPx = (state.playheadPositionMs * pixelsPerMs)
            val centerOffset = (playheadPx - scrollState.viewportSize / 2f).coerceAtLeast(0f)
            scrollState.scrollTo(centerOffset.toInt())
        }
    }

    val bgColor = Color(0xFF0A0D14)
    val rulerColor = Color(0xFF13171F)
    val playheadColor = Color(0xFFFF2D55) // CapCut-style red playhead
    val beatColor = Color(0xFFFF2D75)

    val visibleTracks = remember(state.tracks, timelineMode) {
        when (timelineMode) {
            TimelineMode.MAIN -> state.tracks.filter { it.type == TrackType.VIDEO }
            TimelineMode.OVERLAY -> state.tracks.filter { it.type == TrackType.OVERLAY }
            TimelineMode.AUDIO -> state.tracks.filter { it.type == TrackType.AUDIO }
            TimelineMode.TEXT -> state.tracks.filter { it.type == TrackType.TEXT }
        }
    }

    Column(modifier = modifier.background(bgColor)) {

        // ── Sub-mode header (Overlay / Audio / Text) ─────────────────────────
        if (timelineMode != TimelineMode.MAIN) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(Color(0xFF13171F))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onBackToMain() }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        when (timelineMode) {
                            TimelineMode.OVERLAY -> "Overlay"
                            TimelineMode.AUDIO -> "Audio"
                            TimelineMode.TEXT -> "Text"
                            else -> ""
                        },
                        color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E2230))
                        .border(1.dp, Color(0xFF384055), RoundedCornerShape(6.dp))
                        .clickable { onAddSubTrackMedia() }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        when (timelineMode) {
                            TimelineMode.OVERLAY -> "Add Overlay"
                            TimelineMode.AUDIO -> "Add Audio"
                            TimelineMode.TEXT -> "Add Text"
                            else -> "Add"
                        },
                        color = Color.White, fontSize = 11.sp
                    )
                }
            }
        }

        // ── Main timeline body ────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {

            // LEFT track labels (42dp wide)
            Column(
                modifier = Modifier
                    .width(44.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF0D1018))
                    .padding(top = 32.dp) // clear time ruler
            ) {
                if (visibleTracks.isEmpty()) {
                    Box(
                        Modifier.size(44.dp, 64.dp).clickable {
                            if (timelineMode == TimelineMode.MAIN) onAddMedia() else onAddSubTrackMedia()
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier.size(26.dp).clip(CircleShape)
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
                                        Modifier.size(26.dp).clip(CircleShape)
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

            // 1px separator
            Box(Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(.06f)))

            // RIGHT scrollable timeline
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(scrollState)
            ) {
                // Full-width scrollable content
                Box(
                    modifier = Modifier
                        .width(timelineWidthDp)
                        .fillMaxHeight()
                ) {
                    Column(Modifier.fillMaxSize()) {

                        // ── Time Ruler (tap to add/remove beat marker) ────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(30.dp)
                                .background(rulerColor)
                                .pointerInput(pixelsPerMs, totalDurationMs) {
                                    detectTapGestures { offset ->
                                        val tappedMs = (offset.x / pixelsPerMs).toLong()
                                            .coerceIn(0L, totalDurationMs)
                                        // Tap ruler → toggle beat marker AND seek
                                        onAction(TimelineAction.Seek(tappedMs))
                                        onAction(TimelineAction.ToggleBeatMarker(tappedMs))
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                        ) {
                            // Draw ruler ticks + timestamps
                            Canvas(Modifier.fillMaxSize()) {
                                val stepMs = when {
                                    pixelsPerMs > 0.3f -> 1000L
                                    pixelsPerMs > 0.1f -> 5000L
                                    else -> 10000L
                                }
                                var t = 0L
                                while (t <= totalDurationMs) {
                                    val x = t * pixelsPerMs
                                    val isMajor = t % (stepMs * 5) == 0L
                                    val tickH = if (isMajor) size.height * 0.7f else size.height * 0.35f
                                    drawLine(
                                        color = if (isMajor) Color.White.copy(.5f) else Color.White.copy(.2f),
                                        start = Offset(x, size.height - tickH),
                                        end = Offset(x, size.height),
                                        strokeWidth = if (isMajor) 1.5f else 1f
                                    )
                                    if (isMajor) {
                                        val secs = t / 1000L
                                        val label = if (secs >= 60) "${secs / 60}:${(secs % 60).toString().padStart(2, '0')}" else "${secs}s"
                                        drawContext.canvas.nativeCanvas.drawText(
                                            label, x + 3f, size.height * 0.45f,
                                            android.graphics.Paint().also { p ->
                                                p.color = android.graphics.Color.argb(160, 255, 255, 255)
                                                p.textSize = 22f
                                            }
                                        )
                                    }
                                    t += stepMs
                                }
                            }
                            // Beat marker dots on ruler
                            state.beatMarkers.forEach { beatMs ->
                                val xDp = (beatMs * pixelsPerMs).dp
                                Box(
                                    Modifier
                                        .offset(x = xDp - 4.dp, y = 2.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(beatColor)
                                )
                            }
                        }

                        Spacer(Modifier.height(2.dp))

                        // ── Track Lanes ───────────────────────────────────────
                        if (visibleTracks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .padding(horizontal = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1A1F2E))
                                    .border(1.5.dp, Color(0xFF2A3050), RoundedCornerShape(8.dp))
                                    .clickable { if (timelineMode == TimelineMode.MAIN) onAddMedia() else onAddSubTrackMedia() },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, null, tint = Color.White, Modifier.size(18.dp))
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
                                TrackLane(
                                    track = track,
                                    selectedClipId = state.selectedClipId,
                                    multiSelectedClipIds = multiSelectedClipIds,
                                    pixelsPerMs = pixelsPerMs,
                                    assets = assets,
                                    onAction = onAction,
                                    onSeek = { ms -> onAction(TimelineAction.Seek(ms)) },
                                    onAddMedia = {
                                        if (timelineMode == TimelineMode.MAIN) onAddMedia() else onAddSubTrackMedia()
                                    },
                                    onLongPressClip = onLongPressClip,
                                    onMoveKeyframe = onMoveKeyframe
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }

                    // ── Beat marker vertical lines (full height) ──────────────
                    state.beatMarkers.forEach { beatMs ->
                        val xDp = (beatMs * pixelsPerMs).dp
                        Box(
                            Modifier
                                .offset(x = xDp)
                                .width(1.5.dp)
                                .fillMaxHeight()
                                .background(beatColor.copy(alpha = 0.55f))
                        )
                    }

                    // ── CapCut-style center playhead ──────────────────────────
                    // Red vertical line with top triangle handle
                    PlayheadView(
                        playheadPositionMs = state.playheadPositionMs,
                        pixelsPerMs = pixelsPerMs,
                        onSeekDelta = { deltaMs ->
                            val newTime = (state.playheadPositionMs + deltaMs)
                                .coerceIn(0L, state.durationMs)
                            onAction(TimelineAction.Seek(newTime))
                        },
                        onTapPlayhead = {
                            onAction(TimelineAction.ToggleBeatMarker(state.playheadPositionMs))
                        },
                        modifier = Modifier.offset {
                            val px = (state.playheadPositionMs * pixelsPerMs).roundToInt()
                            IntOffset(px - 22.dp.roundToPx(), 0)
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)  // CapCut-style thick clips
            .background(Color(0xFF0A0D14))
    ) {
        track.clips.forEach { clip ->
            val isSelected = clip.id == selectedClipId
            val isMultiSelected = multiSelectedClipIds.contains(clip.id)

            val onSelectCb = remember(clip.id) { { onAction(TimelineAction.SelectClip(clip.id)) } }
            val onLongCb = remember(clip.id) { { onLongPressClip(clip.id) } }
            val onMoveCb = remember(clip.id, clip.trackId, clip.startTimeMs) {
                { deltaMs: Long ->
                    onAction(TimelineAction.MoveClip(clip.id, clip.trackId, (clip.startTimeMs + deltaMs).coerceAtLeast(0L)))
                }
            }
            val onTrimStartCb = remember(clip.id, clip.startTimeMs) {
                { deltaMs: Long -> onAction(TimelineAction.TrimStart(clip.id, clip.startTimeMs + deltaMs)) }
            }
            val onTrimEndCb = remember(clip.id, clip.endTimeMs) {
                { deltaMs: Long -> onAction(TimelineAction.TrimEnd(clip.id, clip.endTimeMs + deltaMs)) }
            }
            val onMoveKfCb = remember(clip.id) {
                { kfId: String, newMs: Long -> onMoveKeyframe(clip.id, kfId, newMs) }
            }

            ClipCard(
                clip = clip,
                isSelected = isSelected,
                isMultiSelected = isMultiSelected,
                pixelsPerMs = pixelsPerMs,
                assets = assets,
                onSelect = onSelectCb,
                onLongPress = onLongCb,
                onSeek = onSeek,
                onMoveDelta = onMoveCb,
                onTrimStartDelta = onTrimStartCb,
                onTrimEndDelta = onTrimEndCb,
                onMoveKeyframe = onMoveKfCb,
                modifier = Modifier.offset {
                    IntOffset((clip.startTimeMs * pixelsPerMs).roundToInt(), 0)
                }
            )
        }
    }
}
