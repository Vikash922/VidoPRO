package com.example.feature.timeline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.core.model.Asset
import com.example.core.model.Track
import kotlin.math.roundToInt

import androidx.compose.ui.graphics.graphicsLayer

/**
 * Dedicated track component for overlay (PiP / visual layers) clips.
 * Supports positioning, selection, trimming, dragging, and keyframe indicators.
 */
@Composable
fun OverlayTrack(
    track: Track,
    selectionState: TimelineSelectionState,
    selectedKeyframeId: String? = null,
    pixelsPerMs: Float,
    assets: Map<String, Asset>,
    onSelectClip: (String) -> Unit,
    onLongPressClip: (String) -> Unit,
    onSeek: (Long) -> Unit,
    onMoveClipDelta: (clipId: String, deltaMs: Long) -> Unit,
    onTrimStartDelta: (clipId: String, deltaMs: Long) -> Unit,
    onTrimEndDelta: (clipId: String, deltaMs: Long) -> Unit,
    onMoveKeyframe: (clipId: String, keyframeId: String, newTimeMs: Long) -> Unit,
    onSelectKeyframe: (keyframeId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color(0xFF0A0D14))
    ) {
        track.clips.forEach { clip ->
            val isSelected = selectionState.isClipSelected(clip.id)
            val isMultiSelected = selectionState.isClipMultiSelected(clip.id)

            val onSelectCb = remember(clip.id) { { onSelectClip(clip.id) } }
            val onLongCb = remember(clip.id) { { onLongPressClip(clip.id) } }
            val onMoveCb = remember(clip.id, track.isLocked) {
                if (track.isLocked) { { _: Long -> } }
                else { deltaMs: Long -> onMoveClipDelta(clip.id, deltaMs) }
            }
            val onTrimStartCb = remember(clip.id, track.isLocked) {
                if (track.isLocked) { { _: Long -> } }
                else { deltaMs: Long -> onTrimStartDelta(clip.id, deltaMs) }
            }
            val onTrimEndCb = remember(clip.id, track.isLocked) {
                if (track.isLocked) { { _: Long -> } }
                else { deltaMs: Long -> onTrimEndDelta(clip.id, deltaMs) }
            }
            val onMoveKfCb = remember(clip.id, track.isLocked) {
                if (track.isLocked) { { _: String, _: Long -> } }
                else { kfId: String, newMs: Long -> onMoveKeyframe(clip.id, kfId, newMs) }
            }

            ClipCard(
                clip = clip,
                isSelected = isSelected,
                isMultiSelected = isMultiSelected,
                selectedKeyframeId = selectedKeyframeId,
                pixelsPerMs = pixelsPerMs,
                assets = assets,
                onSelect = onSelectCb,
                onLongPress = onLongCb,
                onSeek = onSeek,
                onMoveDelta = onMoveCb,
                onTrimStartDelta = onTrimStartCb,
                onTrimEndDelta = onTrimEndCb,
                onMoveKeyframe = onMoveKfCb,
                onSelectKeyframe = onSelectKeyframe,
                modifier = Modifier
                    .offset {
                        IntOffset((clip.startTimeMs * pixelsPerMs).roundToInt(), 0)
                    }
                    .graphicsLayer {
                        alpha = if (track.isVisible) 1f else 0.45f
                    }
            )
        }
    }
}
