package com.example.feature.timeline.ui

import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Playhead component: Single source of truth for timeline playhead indicator rendering.
 * Synchronized with timeline engine playhead position, with zoom/scroll tracking and scrubbing.
 */
@Composable
fun Playhead(
    playheadPositionMs: Long,
    pixelsPerMs: Float,
    maxDurationMs: Long,
    onSeek: (Long) -> Unit,
    onTapPlayhead: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    PlayheadView(
        playheadPositionMs = playheadPositionMs,
        pixelsPerMs = pixelsPerMs,
        onSeekDelta = { deltaMs ->
            val newTime = (playheadPositionMs + deltaMs).coerceIn(0L, maxDurationMs)
            onSeek(newTime)
        },
        onTapPlayhead = onTapPlayhead,
        modifier = modifier.offset {
            val px = (playheadPositionMs * pixelsPerMs).roundToInt()
            IntOffset(px - 22.dp.roundToPx(), 0)
        }
    )
}
