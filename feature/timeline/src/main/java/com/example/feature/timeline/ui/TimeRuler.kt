package com.example.feature.timeline.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.core.common.TimeUtils
import com.example.core.ui.theme.EditorColors
import com.example.feature.timeline.engine.TimelineUtils
import kotlin.math.max

/**
 * Canvas-based Time Ruler for high-performance tick rendering and scrub gestures.
 * Optimized to prevent object allocations inside draw loops (DEV-073, DEV-074).
 */
@Composable
fun TimeRuler(
    totalDurationMs: Long,
    pixelsPerMs: Float,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val rulerHeight = 28.dp

    // Pre-allocate and remember text paint to avoid allocations in draw loop
    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(180, 160, 160, 175)
            textSize = 24f
            isAntiAlias = true
        }
    }

    val tickColorMajor = EditorColors.timelineRuler
    val tickColorMinor = remember { EditorColors.timelineRuler.copy(alpha = 0.4f) }
    val bgLineColor = remember { EditorColors.timelineRuler.copy(alpha = 0.3f) }

    // Interval between major ticks in ms (adaptive based on pixelsPerMs)
    val majorIntervalMs: Long = remember(pixelsPerMs) {
        when {
            pixelsPerMs >= 0.2f -> 1000L // every 1s
            pixelsPerMs >= 0.08f -> 2000L // every 2s
            else -> 5000L // every 5s
        }
    }
    val minorIntervalMs: Long = remember(majorIntervalMs) { majorIntervalMs / 5 }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(rulerHeight)
            .pointerInput(pixelsPerMs, totalDurationMs) {
                detectTapGestures { offset ->
                    val timeMs = TimelineUtils.xToTime(offset.x, pixelsPerMs)
                    onSeek(timeMs.coerceIn(0L, max(1000L, totalDurationMs)))
                }
            }
            .pointerInput(pixelsPerMs, totalDurationMs) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val timeMs = TimelineUtils.xToTime(change.position.x, pixelsPerMs)
                    onSeek(timeMs.coerceIn(0L, max(1000L, totalDurationMs)))
                }
            }
    ) {
        val width = size.width
        val height = size.height

        // Background line
        drawLine(
            color = bgLineColor,
            start = Offset(0f, height),
            end = Offset(width, height),
            strokeWidth = 1f
        )

        val durationToDraw = max(totalDurationMs + 10000L, 30000L)
        var currentMs = 0L

        while (currentMs <= durationToDraw) {
            val x = currentMs * pixelsPerMs
            if (x > width + 50f) break

            val isMajor = (currentMs % majorIntervalMs) == 0L
            val tickHeight = if (isMajor) height * 0.5f else height * 0.25f
            val tickColor = if (isMajor) tickColorMajor else tickColorMinor

            drawLine(
                color = tickColor,
                start = Offset(x, height - tickHeight),
                end = Offset(x, height),
                strokeWidth = if (isMajor) 1.5f else 1f
            )

            if (isMajor) {
                val timecode = TimeUtils.formatDuration(currentMs)
                drawContext.canvas.nativeCanvas.drawText(
                    timecode,
                    x + 4f,
                    height * 0.45f,
                    textPaint
                )
            }

            currentMs += minorIntervalMs
        }
    }
}

