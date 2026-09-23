package com.example.feature.timeline.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Timeline viewport component coordinating horizontal scrolling, zoom coordinates,
 * time ruler, vertical beat marker lines, track lane contents, and the synchronized playhead.
 */
@Composable
fun TimelineViewport(
    scrollState: ScrollState,
    timelineWidthDp: Dp,
    pixelsPerMs: Float,
    totalDurationMs: Long,
    playheadPositionMs: Long,
    beatMarkers: Set<Long>,
    onSeek: (Long) -> Unit,
    onToggleBeatMarker: (Long) -> Unit,
    modifier: Modifier = Modifier,
    trackContent: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val rulerColor = Color(0xFF13171F)
    val beatColor = Color(0xFFFF2D75)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .horizontalScroll(scrollState)
    ) {
        Box(
            modifier = Modifier
                .width(timelineWidthDp)
                .fillMaxHeight()
        ) {
            Column(Modifier.fillMaxSize()) {

                // ── Time Ruler (tap to add/remove beat marker and seek) ──────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .background(rulerColor)
                        .pointerInput(pixelsPerMs, totalDurationMs) {
                            detectTapGestures { offset ->
                                val tappedMs = (offset.x / pixelsPerMs).toLong()
                                    .coerceIn(0L, totalDurationMs)
                                onSeek(tappedMs)
                                onToggleBeatMarker(tappedMs)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                ) {
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
                    beatMarkers.forEach { beatMs ->
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

                // ── Track Lanes Area ─────────────────────────────────────────
                trackContent()
            }

            // ── Beat Marker vertical lines spanning all tracks ────────────────
            beatMarkers.forEach { beatMs ->
                val xDp = (beatMs * pixelsPerMs).dp
                Box(
                    Modifier
                        .offset(x = xDp)
                        .width(1.5.dp)
                        .fillMaxHeight()
                        .background(beatColor.copy(alpha = 0.55f))
                )
            }

            // ── Synchronized Playhead Indicator ───────────────────────────────
            Playhead(
                playheadPositionMs = playheadPositionMs,
                pixelsPerMs = pixelsPerMs,
                maxDurationMs = totalDurationMs,
                onSeek = onSeek,
                onTapPlayhead = { onToggleBeatMarker(playheadPositionMs) }
            )
        }
    }
}
