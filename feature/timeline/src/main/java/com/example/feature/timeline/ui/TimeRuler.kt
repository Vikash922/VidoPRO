package com.example.feature.timeline.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.core.common.TimeUtils
import com.example.core.ui.theme.EditorColors
import com.example.feature.timeline.engine.TimelineUtils
import kotlin.math.max

/**
 * Canvas-based Time Ruler for high-performance tick rendering, beat dots, and scrub gestures.
 * Optimized to prevent object allocations inside draw loops (DEV-073, DEV-074).
 */
@Composable
fun TimeRuler(
    totalDurationMs: Long,
    pixelsPerMs: Float,
    onSeek: (Long) -> Unit,
    beatMarkers: Set<Long> = emptySet(),
    onToggleBeatMarker: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val rulerHeight = 30.dp
    val rulerBgColor = Color(0xFF13171F)
    val beatColor = Color(0xFFFF2D75)

    // Pre-allocate and remember text paint to avoid allocations in draw loop
    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(160, 255, 255, 255)
            textSize = 22f
            isAntiAlias = true
        }
    }

    val tickColorMajor = EditorColors.timelineRuler
    val tickColorMinor = remember { EditorColors.timelineRuler.copy(alpha = 0.35f) }
    val bgLineColor = remember { EditorColors.timelineRuler.copy(alpha = 0.25f) }

    // Interval between major ticks in ms (adaptive based on pixelsPerMs)
    val majorIntervalMs: Long = remember(pixelsPerMs) {
        when {
            pixelsPerMs >= 0.2f -> 1000L  // every 1s
            pixelsPerMs >= 0.08f -> 2000L // every 2s
            else -> 5000L                 // every 5s
        }
    }
    val minorIntervalMs: Long = remember(majorIntervalMs) { majorIntervalMs / 5 }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(rulerHeight)
            .background(rulerBgColor)
            .pointerInput(pixelsPerMs, totalDurationMs) {
                detectTapGestures(
                    onTap = { offset ->
                        val timeMs = TimelineGestureHandler.pixelsToMs(offset.x, pixelsPerMs)
                            .coerceIn(0L, totalDurationMs)
                        onSeek(timeMs)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleBeatMarker?.invoke(timeMs)
                    }
                )
            }
            .pointerInput(pixelsPerMs, totalDurationMs) {
                var accumulatedDx = 0f
                detectDragGestures(
                    onDragStart = {
                        accumulatedDx = 0f
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDx += dragAmount.x
                        val deltaMs = TimelineGestureHandler.pixelsToMs(accumulatedDx, pixelsPerMs)
                        if (deltaMs != 0L) {
                            val curMs = TimelineGestureHandler.pixelsToMs(change.position.x, pixelsPerMs)
                            onSeek(curMs.coerceIn(0L, totalDurationMs))
                            accumulatedDx = 0f
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Bottom border line
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
                val tickHeight = if (isMajor) height * 0.65f else height * 0.35f
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

        // Beat marker indicator dots on the ruler
        beatMarkers.forEach { beatMs ->
            val xDp = (beatMs * pixelsPerMs).dp
            Box(
                modifier = Modifier
                    .offset(x = xDp - 4.dp, y = 2.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(beatColor)
            )
        }
    }
}
