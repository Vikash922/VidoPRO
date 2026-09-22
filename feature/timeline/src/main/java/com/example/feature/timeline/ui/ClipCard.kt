package com.example.feature.timeline.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.common.TimeUtils
import com.example.core.model.Clip
import com.example.core.model.ClipType
import com.example.core.ui.theme.AppRadius
import com.example.core.ui.theme.AppSpacing
import com.example.core.ui.theme.EditorColors
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.random.Random

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

    val (bgGradient, borderColor) = remember(clip.type, isSelected) {
        when (clip.type) {
            ClipType.TEXT -> 
                Pair(
                    listOf(Color(0xFFB8A7FF), Color(0xFF7B61FF)),
                    if (isSelected) Color.White else Color(0xFF9E84FF)
                )
            ClipType.AUDIO ->
                Pair(
                    listOf(Color(0xFF00D2FF), Color(0xFF0096FF)),
                    if (isSelected) Color.White else Color(0xFF33B5E5)
                )
            else ->
                Pair(
                    listOf(Color(0xFF2C3248), Color(0xFF161925)),
                    if (isSelected) Color(0xFF7B61FF) else Color(0xFF2C3248)
                )
        }
    }

    val cornerShape = remember { RoundedCornerShape(8.dp) }
    var accumulatedMovePx by remember { mutableFloatStateOf(0f) }
    var accumulatedTrimStartPx by remember { mutableFloatStateOf(0f) }
    var accumulatedTrimEndPx by remember { mutableFloatStateOf(0f) }
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else if (isSelected) 1.02f else 1f,
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
            .shadow(if (isSelected) 4.dp else 0.dp, cornerShape, spotColor = borderColor)
            .clip(cornerShape)
            .background(Brush.horizontalGradient(bgGradient))
            .border(if (isSelected) 2.dp else 1.dp, borderColor, cornerShape)
    ) {
        // Main body for tap/move
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isSelected) 16.dp else 0.dp)
                .pointerInput(clip.id, pixelsPerMs) {
                    val touchSlop = 8f
                    var touchStartX = 0f
                    var isDragging = false
                    
                    awaitPointerEventScope {
                        while(true) {
                            val event = awaitPointerEvent()
                            val changes = event.changes
                            val downChange = changes.find { it.pressed }
                            
                            if (downChange != null && !isPressed) {
                                touchStartX = downChange.position.x
                                isPressed = true
                            }
                            
                            if (isPressed && !isDragging) {
                                val totalDragX = changes.filter { it.pressed }.sumOf { (it.position.x - touchStartX).absoluteValue.toDouble() }.toFloat()
                                if (totalDragX > touchSlop) {
                                    isDragging = true
                                    accumulatedMovePx = 0f
                                }
                            }
                            
                            if (isDragging) {
                                val dragAmount = changes.filter { it.pressed }.sumOf { it.positionChange().x.toDouble() }.toFloat()
                                if (dragAmount != 0f) {
                                    changes.forEach { it.consume() }
                                    accumulatedMovePx += dragAmount
                                    val deltaMs = (accumulatedMovePx / pixelsPerMs).toLong()
                                    if (deltaMs != 0L) {
                                        currentOnMoveDelta(deltaMs)
                                        accumulatedMovePx -= deltaMs * pixelsPerMs
                                    }
                                }
                            }
                            
                            if (changes.all { !it.pressed }) {
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
            // Visual Content based on Clip Type
            when (clip.type) {
                ClipType.AUDIO -> {
                    // Audio Waveform Visuals
                    Canvas(modifier = Modifier.fillMaxSize().padding(vertical = 8.dp)) {
                        val numBars = (size.width / 12f).toInt()
                        val random = Random(clip.id.hashCode())
                        for (i in 0 until numBars) {
                            val heightRatio = 0.2f + random.nextFloat() * 0.8f
                            val barHeight = size.height * heightRatio
                            val x = i * 12f + 6f
                            val y = (size.height - barHeight) / 2f
                            drawLine(
                                color = Color(0xFF00FFD1),
                                start = Offset(x, y),
                                end = Offset(x, y + barHeight),
                                strokeWidth = 6f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
                ClipType.TEXT -> {
                    // Text Visuals
                    Text(
                        text = "Good Vibes",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
                else -> {
                    // Video/Image Thumbnails Simulation
                    Row(modifier = Modifier.fillMaxSize()) {
                        val numThumbs = (clipWidthDp.value / 40f).toInt().coerceAtLeast(1)
                        for (i in 0 until numThumbs) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Brush.linearGradient(listOf(Color(0xFF232533), Color(0xFF2C3248))))
                            ) {
                                if (i == 0) {
                                    Icon(Icons.Default.Movie, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.Center).size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Left Trim
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(16.dp)
                    .fillMaxHeight()
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
                Box(modifier = Modifier.width(2.dp).height(16.dp).background(Color.Black.copy(alpha = 0.5f)))
            }
        }

        // Right Trim
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(16.dp)
                    .fillMaxHeight()
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
                Box(modifier = Modifier.width(2.dp).height(16.dp).background(Color.Black.copy(alpha = 0.5f)))
            }
        }
    }
}
