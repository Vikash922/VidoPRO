package com.example.feature.timeline.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.absoluteValue

import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
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

/**
 * Interactive Clip Card with playful, fun gestures! 🚀
 * Includes bouncy scale animations, gradient accents, and haptic feedback 
 * for Selection, Move dragging, and Edge trimming.
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

    val baseColor = remember(clip.type) {
        when (clip.type) {
            ClipType.VIDEO -> EditorColors.clipVideo
            ClipType.IMAGE -> EditorColors.clipImage
            ClipType.AUDIO -> EditorColors.clipAudio
            ClipType.TEXT -> EditorColors.clipText
            ClipType.COLOR -> EditorColors.clipOverlay
            ClipType.SHAPE -> EditorColors.clipOverlay
        }
    }

    val backgroundColor = remember(baseColor, isSelected) {
        baseColor.copy(alpha = if (isSelected) 0.9f else 0.5f)
    }

    val borderColor = remember(baseColor, isSelected) {
        if (isSelected) EditorColors.playhead else baseColor.copy(alpha = 0.8f)
    }

    val cornerShape = remember { RoundedCornerShape(AppRadius.clip) }
    val formattedDuration = remember(clip.durationMs) { TimeUtils.formatDuration(clip.durationMs) }

    var accumulatedMovePx by remember { mutableFloatStateOf(0f) }
    var accumulatedTrimStartPx by remember { mutableFloatStateOf(0f) }
    var accumulatedTrimEndPx by remember { mutableFloatStateOf(0f) }
    
    // Fun state for bouncy animations
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else if (isSelected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
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
            .shadow(
                elevation = if (isSelected) 8.dp else 2.dp,
                shape = cornerShape,
                spotColor = baseColor
            )
            .clip(cornerShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = backgroundColor.alpha + 0.1f),
                        backgroundColor
                    )
                )
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = cornerShape
            )
            .testTag("clip_card_${clip.id}")
    ) {
        // Main clip body with Tap and Move gestures - using combined pointerInput for proper gesture handling
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isSelected) 16.dp else 4.dp)
                .pointerInput(clip.id, pixelsPerMs) {
                    // Use combined gesture detection for better tap/drag separation
                    val touchSlop = 8f // pixels before considering it a drag
                    var touchStartX = 0f
                    var isDragging = false
                    
                    awaitPointerEventScope {
                        awaitFirstDown()
                        
                        do {
                            val event = awaitPointerEvent()
                            val changes = event.changes
                            val downChange = changes.find { it.pressed && it.positionChange().x != 0f }
                            
                            if (downChange != null) {
                                touchStartX = downChange.position.x
                                isPressed = true
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            
                            // Check for drag
                            if (!isDragging) {
                                val totalDragX = changes
                                    .filter { it.pressed }
                                    .map { (it.position.x - touchStartX).absoluteValue }.sum()
                                
                                if (totalDragX > touchSlop) {
                                    isDragging = true
                                    accumulatedMovePx = 0f
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                            
                            if (isDragging) {
                                val dragAmount = changes
                                    .filter { it.pressed }
                                    .map { it.positionChange().x }.sum()
                                
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
                        } while (event.changes.any { it.pressed })
                        
                        // If not dragging, it was a tap
                        if (!isDragging) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentOnSelect()
                        }
                        
                        isPressed = false
                        accumulatedMovePx = 0f
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                val icon = when (clip.type) {
                    ClipType.VIDEO, ClipType.IMAGE -> Icons.Default.Movie
                    ClipType.AUDIO -> Icons.Default.Audiotrack
                    ClipType.TEXT -> Icons.Default.TextFields
                    ClipType.COLOR, ClipType.SHAPE -> Icons.Default.Movie
                }

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = formattedDuration,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Left Trim Handle (visible when clip is selected)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(18.dp)
                    .fillMaxHeight()
                    .background(EditorColors.playhead.copy(alpha = 0.95f))
                    .pointerInput(clip.id, pixelsPerMs) {
                        detectDragGestures(
                            onDragStart = { 
                                accumulatedTrimStartPx = 0f 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragEnd = { accumulatedTrimStartPx = 0f },
                            onDragCancel = { accumulatedTrimStartPx = 0f },
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
                // Playful Handle bar notch
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White)
                )
            }
        }

        // Right Trim Handle (visible when clip is selected)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(18.dp)
                    .fillMaxHeight()
                    .background(EditorColors.playhead.copy(alpha = 0.95f))
                    .pointerInput(clip.id, pixelsPerMs) {
                        detectDragGestures(
                            onDragStart = { 
                                accumulatedTrimEndPx = 0f 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragEnd = { accumulatedTrimEndPx = 0f },
                            onDragCancel = { accumulatedTrimEndPx = 0f },
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
                // Playful Handle bar notch
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White)
                )
            }
        }
    }
}


