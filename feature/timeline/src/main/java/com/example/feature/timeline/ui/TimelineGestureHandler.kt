package com.example.feature.timeline.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import com.example.feature.timeline.engine.TimelineEngineState
import com.example.feature.timeline.engine.TimelineUtils
import kotlin.math.abs

/**
 * Modular gesture handling and coordinate system utilities for timeline interactions:
 * - Centralized Time <-> Pixel conversions
 * - Pinch-to-zoom gesture
 * - Drag-to-move clip gesture
 * - Drag-to-trim clip gesture
 * - Tap-to-seek and drag-to-scrub gestures
 * - Deterministic snapping
 */
object TimelineGestureHandler {

    /**
     * Converts a pixel delta/offset to milliseconds based on pixelsPerMs.
     */
    fun pixelsToMs(pixels: Float, pixelsPerMs: Float): Long {
        if (pixelsPerMs <= 0f) return 0L
        return (pixels / pixelsPerMs).toLong()
    }

    /**
     * Converts milliseconds to pixel offset based on pixelsPerMs.
     */
    fun msToPixels(ms: Long, pixelsPerMs: Float): Float {
        return ms * pixelsPerMs
    }

    /**
     * Converts a timeline millisecond timestamp to horizontal pixel coordinate X.
     */
    fun timeToX(timeMs: Long, pixelsPerMs: Float): Float {
        return TimelineUtils.timeToX(timeMs, pixelsPerMs)
    }

    /**
     * Converts a horizontal pixel coordinate X to timeline millisecond timestamp.
     */
    fun xToTime(x: Float, pixelsPerMs: Float): Long {
        return TimelineUtils.xToTime(x, pixelsPerMs)
    }

    /**
     * Snaps [targetTimeMs] to nearest snap point if [isSnappingEnabled] is true and within threshold.
     */
    fun snapTime(
        targetTimeMs: Long,
        snapPoints: List<Long>,
        isSnappingEnabled: Boolean = true,
        thresholdMs: Long = TimelineEngineState.SNAP_THRESHOLD_MS
    ): Long {
        if (!isSnappingEnabled || snapPoints.isEmpty()) return targetTimeMs
        return TimelineUtils.snapTime(targetTimeMs, snapPoints, thresholdMs)
    }

    /**
     * Modifier for tapping to seek and dragging to scrub along the timeline.
     */
    fun Modifier.timelineScrubGesture(
        pixelsPerMs: Float,
        maxDurationMs: Long,
        haptic: HapticFeedback? = null,
        onSeek: (Long) -> Unit
    ): Modifier = this.pointerInput(pixelsPerMs, maxDurationMs) {
        detectTapGestures { offset ->
            val timeMs = pixelsToMs(offset.x, pixelsPerMs).coerceIn(0L, maxDurationMs)
            haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
            onSeek(timeMs)
        }
    }.pointerInput(pixelsPerMs, maxDurationMs) {
        var accumulatedDx = 0f
        detectDragGestures(
            onDragStart = {
                accumulatedDx = 0f
                haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            onDrag = { change, dragAmount ->
                change.consume()
                accumulatedDx += dragAmount.x
                val deltaMs = pixelsToMs(accumulatedDx, pixelsPerMs)
                if (deltaMs != 0L) {
                    haptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSeek(deltaMs)
                    accumulatedDx -= deltaMs * pixelsPerMs
                }
            }
        )
    }

    /**
     * Modular pointerInput modifier for dragging a clip on the timeline.
     */
    fun Modifier.clipDragGesture(
        clipId: String,
        pixelsPerMs: Float,
        haptic: HapticFeedback? = null,
        onDragStateChanged: (isDragging: Boolean) -> Unit = {},
        onMoveDelta: (deltaMs: Long) -> Unit
    ): Modifier = this.pointerInput(clipId, pixelsPerMs) {
        var dragOffsetX = 0f
        detectDragGestures(
            onDragStart = {
                dragOffsetX = 0f
                onDragStateChanged(true)
                haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            onDrag = { change, dragAmount ->
                change.consume()
                dragOffsetX += dragAmount.x
            },
            onDragEnd = {
                onDragStateChanged(false)
                val deltaMs = pixelsToMs(dragOffsetX, pixelsPerMs)
                if (deltaMs != 0L) {
                    onMoveDelta(deltaMs)
                }
                dragOffsetX = 0f
            },
            onDragCancel = {
                onDragStateChanged(false)
                dragOffsetX = 0f
            }
        )
    }

    /**
     * Modular pointerInput modifier for dragging trim handles.
     */
    fun Modifier.clipTrimGesture(
        handleTag: String,
        pixelsPerMs: Float,
        haptic: HapticFeedback? = null,
        onTrimDelta: (deltaMs: Long) -> Unit
    ): Modifier = this.pointerInput(handleTag, pixelsPerMs) {
        var accumulatedTrimDx = 0f
        detectDragGestures(
            onDragStart = {
                accumulatedTrimDx = 0f
                haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            onDrag = { change, dragAmount ->
                change.consume()
                accumulatedTrimDx += dragAmount.x
            },
            onDragEnd = {
                val deltaMs = pixelsToMs(accumulatedTrimDx, pixelsPerMs)
                if (deltaMs != 0L) {
                    haptic?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTrimDelta(deltaMs)
                }
                accumulatedTrimDx = 0f
            },
            onDragCancel = {
                accumulatedTrimDx = 0f
            }
        )
    }

    /**
     * Modular pointerInput modifier for pinch-to-zoom scaling of timeline.
     */
    fun Modifier.timelinePinchZoomGesture(
        currentZoom: Float,
        onZoomChange: (Float) -> Unit
    ): Modifier = this.pointerInput(Unit) {
        detectTransformGestures { _, _, zoom, _ ->
            if (zoom != 1.0f) {
                val newZoom = (currentZoom * zoom).coerceIn(
                    TimelineEngineState.MIN_ZOOM,
                    TimelineEngineState.MAX_ZOOM
                )
                onZoomChange(newZoom)
            }
        }
    }
}
