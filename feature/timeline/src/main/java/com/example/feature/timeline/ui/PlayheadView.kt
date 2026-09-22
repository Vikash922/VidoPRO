package com.example.feature.timeline.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.core.ui.theme.EditorColors

/**
 * Canvas-based Playhead indicator with scrubbing handle.
 * Tapping the white playhead toggles a beat marker (pink line) at the current timestamp.
 * Dragging scrubs the timeline.
 */
@Composable
fun PlayheadView(
    playheadPositionMs: Long,
    pixelsPerMs: Float,
    onSeekDelta: (Long) -> Unit,
    onTapPlayhead: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val handleWidth = 24.dp
    
    var isDragging by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.25f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "playhead_scale"
    )

    Canvas(
        modifier = modifier
            .width(handleWidth)
            .fillMaxHeight()
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTapPlayhead()
                }
            }
            .pointerInput(pixelsPerMs) {
                detectDragGestures(
                    onDragStart = { 
                        isDragging = true 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val deltaMs = (dragAmount.x / pixelsPerMs).toLong()
                        if (deltaMs != 0L) {
                            onSeekDelta(deltaMs)
                        }
                    }
                )
            }
            .drawWithCache {
                val centerX = size.width / 2f
                val height = size.height
                val handleHeight = 16.dp.toPx()
                val handleCapWidth = 10.dp.toPx()
                val lineWidth = 2.5.dp.toPx()

                val handlePath = Path().apply {
                    moveTo(centerX - handleCapWidth, 0f)
                    lineTo(centerX + handleCapWidth, 0f)
                    lineTo(centerX + handleCapWidth, handleHeight * 0.65f)
                    lineTo(centerX, handleHeight)
                    lineTo(centerX - handleCapWidth, handleHeight * 0.65f)
                    close()
                }

                onDrawBehind {
                    // Vertical playhead line
                    drawLine(
                        color = EditorColors.playhead,
                        start = Offset(centerX, handleHeight),
                        end = Offset(centerX, height),
                        strokeWidth = lineWidth
                    )
                    
                    // White handle head
                    drawPath(
                        path = handlePath,
                        color = EditorColors.playhead,
                    )
                }
            }
    ) {
        // Drawing handled by drawWithCache
    }
}
