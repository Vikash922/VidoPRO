package com.example.feature.timeline.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.core.ui.theme.EditorColors

/**
 * Canvas-based Playhead indicator with interactive scrubbing handle.
 * Uses drawWithCache to prevent Path reallocation per draw frame (DEV-073, DEV-074).
 */
@Composable
fun PlayheadView(
    playheadPositionMs: Long,
    pixelsPerMs: Float,
    onSeekDelta: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val handleWidth = 24.dp

    Canvas(
        modifier = modifier
            .width(handleWidth)
            .fillMaxHeight()
            .pointerInput(pixelsPerMs) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val deltaMs = (dragAmount.x / pixelsPerMs).toLong()
                    if (deltaMs != 0L) {
                        onSeekDelta(deltaMs)
                    }
                }
            }
            .drawWithCache {
                val centerX = size.width / 2f
                val height = size.height
                val handleHeight = 14.dp.toPx()
                val handleCapWidth = 8.dp.toPx()
                val lineWidth = 2.dp.toPx()

                // Cache handle path geometry so it is not re-instantiated every frame
                val handlePath = Path().apply {
                    moveTo(centerX - handleCapWidth, 0f)
                    lineTo(centerX + handleCapWidth, 0f)
                    lineTo(centerX + handleCapWidth, handleHeight * 0.6f)
                    lineTo(centerX, handleHeight)
                    lineTo(centerX - handleCapWidth, handleHeight * 0.6f)
                    close()
                }

                onDrawBehind {
                    // Draw handle head
                    drawPath(
                        path = handlePath,
                        color = EditorColors.playhead
                    )

                    // Draw vertical playhead line
                    drawLine(
                        color = EditorColors.playhead,
                        start = Offset(centerX, handleHeight),
                        end = Offset(centerX, height),
                        strokeWidth = lineWidth
                    )
                }
            }
    ) {
        // Drawing handled by drawWithCache
    }
}

