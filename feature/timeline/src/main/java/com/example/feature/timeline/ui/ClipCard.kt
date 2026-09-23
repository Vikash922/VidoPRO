package com.example.feature.timeline.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.example.core.model.Asset
import com.example.core.model.Clip
import com.example.core.model.ClipType
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * ClipCard — Professional flat dark-mode timeline clip without gradients.
 * Supports:
 *  - Tap to select and seek
 *  - Long-press (Alight Motion-style) to toggle multi-selection
 *  - Drag to move left/right
 *  - Trim handles for selected clips
 *  - Keyframe diamond markers with drag-to-move
 *  - Group highlight (amber border) and multi-select indicator (blue border + check)
 */
@Composable
fun ClipCard(
    clip: Clip,
    isSelected: Boolean,
    isMultiSelected: Boolean = false,
    pixelsPerMs: Float,
    assets: Map<String, Asset> = emptyMap(),
    onSelect: () -> Unit,
    onLongPress: () -> Unit = {},
    onSeek: (Long) -> Unit = {},
    onMoveDelta: (Long) -> Unit,
    onTrimStartDelta: (Long) -> Unit,
    onTrimEndDelta: (Long) -> Unit,
    onMoveKeyframe: (keyframeId: String, newTimeMs: Long) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val clipWidthDp = remember(clip.durationMs, pixelsPerMs) {
        maxOf(48.dp, (clip.durationMs * pixelsPerMs).dp)
    }

    // Border color priority: multiSelected (blue) > selected (white) > grouped (amber) > type default
    val borderColor = remember(clip.type, isSelected, isMultiSelected, clip.groupId) {
        when {
            isMultiSelected -> Color(0xFF2196F3)       // Blue — multi-select mode
            isSelected -> Color(0xFFFFFFFF)             // White — single select
            clip.groupId != null -> Color(0xFFFFC107)  // Amber — belongs to group
            else -> when (clip.type) {
                ClipType.VIDEO, ClipType.IMAGE -> Color(0xFF384055)
                ClipType.TEXT -> Color(0xFF2E7D32)
                ClipType.AUDIO -> Color(0xFF1D5688)
                else -> Color(0xFF323A4D)
            }
        }
    }

    val bgColor = remember(clip.type) {
        when (clip.type) {
            ClipType.VIDEO, ClipType.IMAGE -> Color(0xFF1E2230)
            ClipType.TEXT -> Color(0xFF1B5E20)
            ClipType.AUDIO -> Color(0xFF0F3658)
            ClipType.COLOR, ClipType.SHAPE -> Color(0xFF242A38)
        }
    }

    val cornerShape = remember { RoundedCornerShape(6.dp) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    var accumulatedTrimStartPx by remember { mutableFloatStateOf(0f) }
    var accumulatedTrimEndPx by remember { mutableFloatStateOf(0f) }

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.04f else if (isPressed) 0.98f else if (isSelected || isMultiSelected) 1.01f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "clip_scale"
    )

    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnSeek by rememberUpdatedState(onSeek)
    val currentOnMoveDelta by rememberUpdatedState(onMoveDelta)
    val currentOnTrimStartDelta by rememberUpdatedState(onTrimStartDelta)
    val currentOnTrimEndDelta by rememberUpdatedState(onTrimEndDelta)
    val currentOnMoveKeyframe by rememberUpdatedState(onMoveKeyframe)

    Box(
        modifier = modifier
            .offset { IntOffset(dragOffsetX.roundToInt(), 0) }
            .width(clipWidthDp)
            .height(52.dp)
            .scale(scale)
            .shadow(if (isDragging || isSelected || isMultiSelected) 6.dp else 0.dp, cornerShape)
            .clip(cornerShape)
            .background(bgColor)
            .border(if (isSelected || isMultiSelected) 2.dp else 1.dp, borderColor, cornerShape)
    ) {
        // Main body: Tap to select and seek, Long-press for multi-select, Drag to move
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isSelected) 20.dp else 0.dp)
                .pointerInput(clip.id, pixelsPerMs, clip.startTimeMs) {
                    detectTapGestures(
                        onTap = { offset ->
                            currentOnSelect()
                            val tapOffsetMs = (offset.x / pixelsPerMs).toLong()
                            val seekTimeMs = (clip.startTimeMs + tapOffsetMs).coerceIn(clip.startTimeMs, clip.endTimeMs)
                            currentOnSeek(seekTimeMs)
                        },
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentOnLongPress()
                        }
                    )
                }
                .pointerInput(clip.id, pixelsPerMs) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                            isPressed = true
                            dragOffsetX = 0f
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetX += dragAmount.x
                        },
                        onDragEnd = {
                            isDragging = false
                            isPressed = false
                            val deltaMs = (dragOffsetX / pixelsPerMs).toLong()
                            if (deltaMs != 0L) {
                                currentOnMoveDelta(deltaMs)
                            }
                            dragOffsetX = 0f
                        },
                        onDragCancel = {
                            isDragging = false
                            isPressed = false
                            dragOffsetX = 0f
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // Visual content per clip type (NO GRADIENTS)
            when (clip.type) {
                ClipType.AUDIO -> {
                    // Audio waveform with clear rhythmic beat spikes
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFF00D2FF),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Canvas(modifier = Modifier.fillMaxSize().padding(vertical = 6.dp)) {
                            val barSpacing = 4f
                            val barWidth = 2.5f
                            val numBars = ((size.width - 4f) / (barWidth + barSpacing)).toInt().coerceAtLeast(1)
                            val random = Random(clip.id.hashCode())
                            for (i in 0 until numBars) {
                                val isBeat = (i % 4 == 0)
                                val heightRatio = if (isBeat) {
                                    0.75f + random.nextFloat() * 0.25f
                                } else {
                                    0.15f + random.nextFloat() * 0.45f
                                }
                                val barHeight = size.height * heightRatio
                                val x = i * (barWidth + barSpacing) + barWidth / 2f
                                val y = (size.height - barHeight) / 2f
                                drawLine(
                                    color = if (isBeat) Color(0xFF00D2FF) else Color(0xFF00D2FF).copy(alpha = 0.5f),
                                    start = Offset(x, y),
                                    end = Offset(x, y + barHeight),
                                    strokeWidth = if (isBeat) barWidth * 1.2f else barWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }

                ClipType.TEXT -> {
                    val textLabel = clip.textData?.text ?: "Good Vibes"
                    Text(
                        text = textLabel,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }

                else -> {
                    val asset = assets[clip.assetId]
                    val thumbModel = asset?.thumbnailPath ?: asset?.uri
                    val numThumbs = (clipWidthDp.value / 44f).toInt().coerceIn(1, 16)
                    val context = LocalContext.current

                    val imageRequest = remember(thumbModel) {
                        if (thumbModel != null) {
                            ImageRequest.Builder(context)
                                .data(thumbModel)
                                .decoderFactory(VideoFrameDecoder.Factory())
                                .crossfade(true)
                                .build()
                        } else null
                    }

                    Row(modifier = Modifier.fillMaxSize()) {
                        for (i in 0 until numThumbs) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .padding(horizontal = 0.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFF1E2230)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (imageRequest != null) {
                                    AsyncImage(
                                        model = imageRequest,
                                        contentDescription = "Video Thumbnail",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Movie,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.35f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Keyframe diamonds drawn on top of clip body ──────────────────────
        // Only shown for selected clip that has keyframes
        if (isSelected && clip.keyframes.isNotEmpty()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp) // avoid trim handles
                    .pointerInput(clip.id, clip.keyframes, pixelsPerMs) {
                        detectDragGestures(
                            onDragStart = {},
                            onDrag = { change, dragAmount ->
                                change.consume()
                                // Find closest keyframe to touch x
                                val touchX = change.position.x
                                val touchMs = clip.startTimeMs + (touchX / pixelsPerMs).toLong()
                                val nearest = clip.keyframes.minByOrNull { kf ->
                                    val kfX = ((kf.timeMs - clip.startTimeMs) * pixelsPerMs)
                                    (kfX - touchX).absoluteValue
                                }
                                if (nearest != null) {
                                    val deltaMs = (dragAmount.x / pixelsPerMs).toLong()
                                    val newTimeMs = (nearest.timeMs + deltaMs).coerceIn(clip.startTimeMs, clip.endTimeMs)
                                    currentOnMoveKeyframe(nearest.id, newTimeMs)
                                }
                            },
                            onDragEnd = {},
                            onDragCancel = {}
                        )
                    }
            ) {
                val diamondSize = 8.dp.toPx()
                val centerY = size.height / 2f
                clip.keyframes.forEach { kf ->
                    val relativeMs = kf.timeMs - clip.startTimeMs
                    val x = relativeMs * pixelsPerMs
                    if (x >= 0f && x <= size.width) {
                        val diamond = Path().apply {
                            moveTo(x, centerY - diamondSize)
                            lineTo(x + diamondSize * 0.6f, centerY)
                            lineTo(x, centerY + diamondSize)
                            lineTo(x - diamondSize * 0.6f, centerY)
                            close()
                        }
                        drawPath(diamond, color = Color(0xFFFFD600)) // Vivid yellow diamond
                        drawPath(diamond, color = Color.Black.copy(alpha = 0.35f)) // subtle stroke sim
                    }
                }
            }
        }

        // ── Multi-select checkmark badge (top-left corner) ────────────────────
        if (isMultiSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(3.dp)
                    .size(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2196F3)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        // ── Left Trim Handle ──────────────────────────────────────────────────
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(20.dp)
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
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Trim Left",
                    tint = Color(0xFF0A0D14),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // ── Right Trim Handle ─────────────────────────────────────────────────
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(20.dp)
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
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Trim Right",
                    tint = Color(0xFF0A0D14),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
