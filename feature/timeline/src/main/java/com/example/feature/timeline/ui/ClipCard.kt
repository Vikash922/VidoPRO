package com.example.feature.timeline.ui

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
 * Interactive Clip Card with gesture detectors for Selection, Move dragging, and Edge trimming.
 * Optimized with remember and derived values to minimize recomposition overhead (DEV-073, DEV-074).
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
        baseColor.copy(alpha = if (isSelected) 0.85f else 0.45f)
    }

    val borderColor = remember(baseColor, isSelected) {
        if (isSelected) EditorColors.playhead else baseColor.copy(alpha = 0.8f)
    }

    val cornerShape = remember { RoundedCornerShape(AppRadius.clip) }
    val formattedDuration = remember(clip.durationMs) { TimeUtils.formatDuration(clip.durationMs) }

    var accumulatedMovePx by remember { mutableFloatStateOf(0f) }
    var accumulatedTrimStartPx by remember { mutableFloatStateOf(0f) }
    var accumulatedTrimEndPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .width(clipWidthDp)
            .height(52.dp)
            .clip(cornerShape)
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = cornerShape
            )
            .testTag("clip_card_${clip.id}")
    ) {
        // Main clip body with Tap and Move gestures
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isSelected) 16.dp else 4.dp)
                .pointerInput(clip.id) {
                    detectTapGestures {
                        onSelect()
                    }
                }
                .pointerInput(clip.id, pixelsPerMs) {
                    detectDragGestures(
                        onDragStart = { accumulatedMovePx = 0f },
                        onDragEnd = { accumulatedMovePx = 0f },
                        onDragCancel = { accumulatedMovePx = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            accumulatedMovePx += dragAmount.x
                            val deltaMs = (accumulatedMovePx / pixelsPerMs).toLong()
                            if (deltaMs != 0L) {
                                onMoveDelta(deltaMs)
                                accumulatedMovePx -= deltaMs * pixelsPerMs
                            }
                        }
                    )
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
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = formattedDuration,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
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
                    .width(16.dp)
                    .fillMaxHeight()
                    .background(EditorColors.playhead.copy(alpha = 0.9f))
                    .pointerInput(clip.id, pixelsPerMs) {
                        detectDragGestures(
                            onDragStart = { accumulatedTrimStartPx = 0f },
                            onDragEnd = { accumulatedTrimStartPx = 0f },
                            onDragCancel = { accumulatedTrimStartPx = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                accumulatedTrimStartPx += dragAmount.x
                                val deltaMs = (accumulatedTrimStartPx / pixelsPerMs).toLong()
                                if (deltaMs != 0L) {
                                    onTrimStartDelta(deltaMs)
                                    accumulatedTrimStartPx -= deltaMs * pixelsPerMs
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Handle bar notch
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(18.dp)
                        .background(Color.Black.copy(alpha = 0.6f))
                )
            }
        }

        // Right Trim Handle (visible when clip is selected)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(16.dp)
                    .fillMaxHeight()
                    .background(EditorColors.playhead.copy(alpha = 0.9f))
                    .pointerInput(clip.id, pixelsPerMs) {
                        detectDragGestures(
                            onDragStart = { accumulatedTrimEndPx = 0f },
                            onDragEnd = { accumulatedTrimEndPx = 0f },
                            onDragCancel = { accumulatedTrimEndPx = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                accumulatedTrimEndPx += dragAmount.x
                                val deltaMs = (accumulatedTrimEndPx / pixelsPerMs).toLong()
                                if (deltaMs != 0L) {
                                    onTrimEndDelta(deltaMs)
                                    accumulatedTrimEndPx -= deltaMs * pixelsPerMs
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Handle bar notch
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(18.dp)
                        .background(Color.Black.copy(alpha = 0.6f))
                )
            }
        }
    }
}

