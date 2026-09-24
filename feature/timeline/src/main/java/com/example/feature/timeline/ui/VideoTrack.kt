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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.core.model.Asset
import com.example.core.model.Track
import com.example.core.model.TransitionType
import kotlin.math.roundToInt

/**
 * Dedicated track component for primary video clips (DEV-071, DEV-072).
 * Supports thumbnail rendering, trim handles, keyframe diamonds, clip dragging, multi-selection,
 * and transition cut boundary indicators.
 */
@Composable
fun VideoTrack(
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
    onEditTransition: (firstClipId: String, secondClipId: String) -> Unit = { _, _ -> },
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

        // Transition indicators at adjacent clip boundaries
        val sortedClips = remember(track.clips) { track.clips.sortedBy { it.startTimeMs } }
        if (sortedClips.size >= 2) {
            for (i in 0 until sortedClips.size - 1) {
                val clipA = sortedClips[i]
                val clipB = sortedClips[i + 1]
                if (clipB.startTimeMs - clipA.endTimeMs <= 100L) {
                    val cutTimeMs = clipA.endTimeMs
                    val transition = track.transitions.find {
                        it.firstClipId == clipA.id && it.secondClipId == clipB.id
                    }
                    val hasTransition = transition != null && transition.type != TransitionType.NONE

                    val indicatorSize = 22.dp
                    Box(
                        modifier = Modifier
                            .offset {
                                val centerX = (cutTimeMs * pixelsPerMs).roundToInt()
                                IntOffset(
                                    centerX - (indicatorSize.toPx() / 2f).roundToInt(),
                                    ((64.dp.toPx() - indicatorSize.toPx()) / 2f).roundToInt()
                                )
                            }
                            .size(indicatorSize)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (hasTransition) Color(0xFF6B4BFF) else Color(0xFF1E2230))
                            .border(1.dp, if (hasTransition) Color.White else Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .clickable { onEditTransition(clipA.id, clipB.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasTransition) {
                            Canvas(modifier = Modifier.size(10.dp)) {
                                val path = Path().apply {
                                    moveTo(size.width / 2f, 0f)
                                    lineTo(size.width, size.height / 2f)
                                    lineTo(size.width / 2f, size.height)
                                    lineTo(0f, size.height / 2f)
                                    close()
                                }
                                drawPath(path, Color.White)
                            }
                        } else {
                            Canvas(modifier = Modifier.size(8.dp)) {
                                val path = Path().apply {
                                    moveTo(size.width / 2f, 0f)
                                    lineTo(size.width, size.height / 2f)
                                    lineTo(size.width / 2f, size.height)
                                    lineTo(0f, size.height / 2f)
                                    close()
                                }
                                drawPath(path, Color.White.copy(alpha = 0.7f), style = Stroke(width = 1.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}
