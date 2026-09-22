package com.example.feature.timeline.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.Clip
import com.example.core.model.ClipType
import kotlin.math.absoluteValue
import kotlin.random.Random

/**
 * ClipCard — visual representation of a clip on the timeline.
 * Matches reference UI: Video=purple-blue thumbnails, Text=GREEN, Audio=cyan waveform.
 */
@Composable
fun ClipCard(
    clip: Clip,
    isSelected: Boolean,
    pixelsPerMs: Float,
    onSelect: () -> Unit,
    onMoveDelta: (Long) -> Unit,
    onTrimStartDelta: (Long) -> Unit,
    onTrimEndDelta: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val clipWidthDp = remember(clip.durationMs, pixelsPerMs) {
        maxOf(44.dp, (clip.durationMs * pixelsPerMs).dp)
    }

    // Colors matching reference image EXACTLY:
    // Video: dark purple/blue gradient (thumbnail-like)
    // Text: GREEN gradient
    // Audio: dark blue/cyan gradient
    val bgGradient = remember(clip.type) {
        when (clip.type) {
            ClipType.VIDEO, ClipType.IMAGE -> listOf(Color(0xFF3D2B6B), Color(0xFF2A1F4E), Color(0xFF4A2D7A))
            ClipType.TEXT -> listOf(Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFF388E3C))
            ClipType.AUDIO -> listOf(Color(0xFF0D47A1), Color(0xFF1565C0), Color(0xFF1976D2))
            ClipType.COLOR, ClipType.SHAPE -> listOf(Color(0xFF2C3248), Color(0xFF161925))
        }
    }

    val borderColor = remember(clip.type, isSelected) {
        if (isSelected) Color.White
        else when (clip.type) {
            ClipType.VIDEO, ClipType.IMAGE -> Color(0xFF5E35B1).copy(alpha = 0.5f)
            ClipType.TEXT -> Color(0xFF4CAF50).copy(alpha = 0.5f)
            ClipType.AUDIO -> Color(0xFF42A5F5).copy(alpha = 0.5f)
            else -> Color(0xFF2C3248)
        }
    }

    val cornerShape = remember { RoundedCornerShape(6.dp) }
    var accumulatedMovePx by remember { mutableFloatStateOf(0f) }
    var accumulatedTrimStartPx by remember { mutableFloatStateOf(0f) }
    var accumulatedTrimEndPx by remember { mutableFloatStateOf(0f) }
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else if (isSelected) 1.01f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "clip_scale"
    )

    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnMoveDelta by rememberUpdatedState(onMoveDelta)
    val currentOnTrimStartDelta by rememberUpdatedState(onTrimStartDelta)
    val currentOnTrimEndDelta by rememberUpdatedState(onTrimEndDelta)

    Box(
        modifier = modifier
            .width(clipWidthDp)
            .height(52.dp)
            .scale(scale)
            .shadow(if (isSelected) 4.dp else 0.dp, cornerShape)
            .clip(cornerShape)
            .background(Brush.horizontalGradient(bgGradient))
            .border(if (isSelected) 2.dp else 0.dp, borderColor, cornerShape)
    ) {
        // Main body — tap and move
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isSelected) 14.dp else 0.dp)
                .pointerInput(clip.id, pixelsPerMs) {
                    val touchSlop = 8f
                    var touchStartX = 0f
                    var isDragging = false

                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val changes = event.changes
                            val downChange = changes.find { change -> change.pressed }

                            if (downChange != null && !isPressed) {
                                touchStartX = downChange.position.x
                                isPressed = true
                            }

                            if (isPressed && !isDragging) {
                                val totalDragX = changes
                                    .filter { change -> change.pressed }
                                    .sumOf { change -> (change.position.x - touchStartX).absoluteValue.toDouble() }
                                    .toFloat()
                                if (totalDragX > touchSlop) {
                                    isDragging = true
                                    accumulatedMovePx = 0f
                                }
                            }

                            if (isDragging) {
                                val dragAmount = changes
                                    .filter { change -> change.pressed }
                                    .sumOf { change -> change.positionChange().x.toDouble() }
                                    .toFloat()
                                if (dragAmount != 0f) {
                                    changes.forEach { change -> change.consume() }
                                    accumulatedMovePx += dragAmount
                                    val deltaMs = (accumulatedMovePx / pixelsPerMs).toLong()
                                    if (deltaMs != 0L) {
                                        currentOnMoveDelta(deltaMs)
                                        accumulatedMovePx -= deltaMs * pixelsPerMs
                                    }
                                }
                            }

                            if (changes.all { change -> !change.pressed }) {
                                if (!isDragging) {
                                    currentOnSelect()
                                }
                                isPressed = false
                                isDragging = false
                                accumulatedMovePx = 0f
                            }
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // Visual content per clip type
            when (clip.type) {
                ClipType.AUDIO -> {
                    // Audio waveform bars — cyan
                    Canvas(modifier = Modifier.fillMaxSize().padding(vertical = 6.dp, horizontal = 4.dp)) {
                        val barSpacing = 4f
                        val barWidth = 2.5f
                        val numBars = ((size.width - 4f) / (barWidth + barSpacing)).toInt()
                        val random = Random(clip.id.hashCode())
                        for (i in 0 until numBars) {
                            val heightRatio = 0.15f + random.nextFloat() * 0.85f
                            val barHeight = size.height * heightRatio
                            val x = i * (barWidth + barSpacing) + barWidth / 2f + 2f
                            val y = (size.height - barHeight) / 2f
                            drawLine(
                                color = Color(0xFF4FC3F7),
                                start = Offset(x, y),
                                end = Offset(x, y + barHeight),
                                strokeWidth = barWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                ClipType.TEXT -> {
                    // Green text clip — show text label
                    val textLabel = clip.textData?.text ?: "Text"
                    Text(
                        text = textLabel,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }

                else -> {
                    // Video/Image — simulated thumbnail segments
                    Row(modifier = Modifier.fillMaxSize()) {
                        val numThumbs = (clipWidthDp.value / 36f).toInt().coerceIn(1, 20)
                        val random = Random(clip.id.hashCode())
                        for (i in 0 until numThumbs) {
                            val shade = random.nextFloat() * 0.15f
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .padding(horizontal = 0.5.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0xFF5C3FA0).copy(alpha = 0.6f + shade),
                                                Color(0xFF2A1F4E).copy(alpha = 0.8f + shade)
                                            )
                                        )
                                    )
                            ) {
                                if (i == 0) {
                                    Icon(
                                        Icons.Default.Movie,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.35f),
                                        modifier = Modifier.align(Alignment.Center).size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Left Trim Handle
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(14.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                    .background(Color.White)
                    .pointerInput(clip.id, pixelsPerMs) {
                        detectDragGestures(
                            onDragStart = { accumulatedTrimStartPx = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                accumulatedTrimStartPx += dragAmount.x
                                val deltaMs = (accumulatedTrimStartPx / pixelsPerMs).toLong()
                                if (deltaMs != 0L) {
                                    currentOnTrimStartDelta(deltaMs)
                                    accumulatedTrimStartPx -= deltaMs * pixelsPerMs
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                )
            }
        }

        // Right Trim Handle
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(14.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                    .background(Color.White)
                    .pointerInput(clip.id, pixelsPerMs) {
                        detectDragGestures(
                            onDragStart = { accumulatedTrimEndPx = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                accumulatedTrimEndPx += dragAmount.x
                                val deltaMs = (accumulatedTrimEndPx / pixelsPerMs).toLong()
                                if (deltaMs != 0L) {
                                    currentOnTrimEndDelta(deltaMs)
                                    accumulatedTrimEndPx -= deltaMs * pixelsPerMs
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                )
            }
        }
    }
}
