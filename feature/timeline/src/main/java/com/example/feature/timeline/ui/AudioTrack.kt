package com.example.feature.timeline.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.core.model.Asset
import com.example.core.model.Track
import kotlin.math.roundToInt

/**
 * Dedicated track component for audio clips.
 * Supports waveform rendering, volume cues, gap-aware placement, trimming, and multi-selection.
 *
 * Gaps between clips (silence) are preserved visually along the timeline coordinate space
 * and never collapsed.
 */
@Composable
fun AudioTrack(
    track: Track,
    selectionState: TimelineSelectionState,
    pixelsPerMs: Float,
    assets: Map<String, Asset>,
    onSelectClip: (String) -> Unit,
    onLongPressClip: (String) -> Unit,
    onSeek: (Long) -> Unit,
    onMoveClipDelta: (clipId: String, deltaMs: Long) -> Unit,
    onTrimStartDelta: (clipId: String, deltaMs: Long) -> Unit,
    onTrimEndDelta: (clipId: String, deltaMs: Long) -> Unit,
    onMoveKeyframe: (clipId: String, keyframeId: String, newTimeMs: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color(0xFF0A0D14))
    ) {
        // Visual indicator for audio track lane & silence gaps
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            drawLine(
                color = Color(0xFF1D5688).copy(alpha = 0.35f),
                start = Offset(0f, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect
            )
        }

        track.clips.forEach { clip ->
            val isSelected = selectionState.isClipSelected(clip.id)
            val isMultiSelected = selectionState.isClipMultiSelected(clip.id)

            val onSelectCb = remember(clip.id) { { onSelectClip(clip.id) } }
            val onLongCb = remember(clip.id) { { onLongPressClip(clip.id) } }
            val onMoveCb = remember(clip.id) { { deltaMs: Long -> onMoveClipDelta(clip.id, deltaMs) } }
            val onTrimStartCb = remember(clip.id) { { deltaMs: Long -> onTrimStartDelta(clip.id, deltaMs) } }
            val onTrimEndCb = remember(clip.id) { { deltaMs: Long -> onTrimEndDelta(clip.id, deltaMs) } }
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
