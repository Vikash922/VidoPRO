package com.example.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.core.model.Clip
import com.example.core.model.EffectType
import com.example.core.model.InterpolationType
import com.example.core.model.Keyframe
import com.example.core.model.KeyframeProperty
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyframeBottomSheet(
    clip: Clip? = null,
    playheadPositionMs: Long = 0L,
    activeProperty: String = KeyframeProperty.POSITION_X,
    onSelectProperty: (String) -> Unit = {},
    onAddKeyframe: (clipId: String, property: String, timeMs: Long, value: Float, interpolation: InterpolationType) -> Unit = { _, _, _, _, _ -> },
    onUpdateKeyframe: (clipId: String, keyframeId: String, value: Float, interpolation: InterpolationType) -> Unit = { _, _, _, _ -> },
    onDeleteKeyframe: (clipId: String, keyframeId: String) -> Unit = { _, _ -> },
    onSeek: (Long) -> Unit = {},
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F111A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(16.dp)) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width / 2f, 0f)
                            lineTo(size.width, size.height / 2f)
                            lineTo(size.width / 2f, size.height)
                            lineTo(0f, size.height / 2f)
                            close()
                        }
                        drawPath(path, color = Color(0xFFFFD600))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Keyframe Editor", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                }
            }

            if (clip == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No clip selected.\nSelect a clip on the timeline to edit keyframes.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))

                // Property Selector Chips
                Text("Animation Property", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                val properties = listOf(
                    KeyframeProperty.POSITION_X to "Pos X",
                    KeyframeProperty.POSITION_Y to "Pos Y",
                    KeyframeProperty.SCALE_X to "Scale X",
                    KeyframeProperty.SCALE_Y to "Scale Y",
                    KeyframeProperty.ROTATION to "Rotation",
                    KeyframeProperty.OPACITY to "Opacity",
                    KeyframeProperty.VOLUME to "Volume",
                    KeyframeProperty.BRIGHTNESS to "Brightness",
                    KeyframeProperty.CONTRAST to "Contrast",
                    KeyframeProperty.SATURATION to "Saturation",
                    KeyframeProperty.EXPOSURE to "Exposure",
                    KeyframeProperty.TEMPERATURE to "Temp",
                    KeyframeProperty.TINT to "Tint",
                    KeyframeProperty.HIGHLIGHTS to "Highlights",
                    KeyframeProperty.SHADOWS to "Shadows",
                    KeyframeProperty.MASK_X to "Mask X",
                    KeyframeProperty.MASK_Y to "Mask Y",
                    KeyframeProperty.MASK_FEATHER to "Mask Feather"
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(properties) { (propKey, label) ->
                        val isSelected = propKey == activeProperty
                        val kfCountForProp = clip.keyframes.count { it.property == propKey }
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectProperty(propKey) },
                            label = {
                                Text(
                                    if (kfCountForProp > 0) "$label ($kfCountForProp)" else label,
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00D2FF),
                                selectedLabelColor = Color(0xFF0A0D14),
                                containerColor = Color(0xFF1E2230),
                                labelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Active Property Controls at current Playhead
                val existingKf = clip.keyframes.find {
                    it.property == activeProperty && kotlin.math.abs(it.timeMs - playheadPositionMs) <= 50L
                }

                val clipDefaultValue = when (activeProperty) {
                    KeyframeProperty.POSITION_X -> clip.transform.x
                    KeyframeProperty.POSITION_Y -> clip.transform.y
                    KeyframeProperty.SCALE_X -> clip.transform.scaleX
                    KeyframeProperty.SCALE_Y -> clip.transform.scaleY
                    KeyframeProperty.ROTATION -> clip.transform.rotation
                    KeyframeProperty.OPACITY -> clip.transform.opacity
                    KeyframeProperty.VOLUME -> clip.volume ?: 1.0f
                    KeyframeProperty.BRIGHTNESS -> clip.effects.find { it.type == EffectType.BRIGHTNESS }?.parameters?.get("brightness") ?: 0f
                    KeyframeProperty.CONTRAST -> clip.effects.find { it.type == EffectType.CONTRAST }?.parameters?.get("contrast") ?: 1f
                    KeyframeProperty.SATURATION -> clip.effects.find { it.type == EffectType.SATURATION }?.parameters?.get("saturation") ?: 1f
                    KeyframeProperty.EXPOSURE -> clip.effects.find { it.type == EffectType.EXPOSURE }?.parameters?.get("exposure") ?: 0f
                    KeyframeProperty.TEMPERATURE -> clip.effects.find { it.type == EffectType.TEMPERATURE }?.parameters?.get("temperature") ?: 0f
                    KeyframeProperty.TINT -> clip.effects.find { it.type == EffectType.TINT }?.parameters?.get("tint") ?: 0f
                    KeyframeProperty.HIGHLIGHTS -> clip.effects.find { it.type == EffectType.HIGHLIGHTS }?.parameters?.get("highlights") ?: 0f
                    KeyframeProperty.SHADOWS -> clip.effects.find { it.type == EffectType.SHADOWS }?.parameters?.get("shadows") ?: 0f
                    KeyframeProperty.MASK_X -> clip.mask?.x ?: 0.5f
                    KeyframeProperty.MASK_Y -> clip.mask?.y ?: 0.5f
                    KeyframeProperty.MASK_FEATHER -> clip.mask?.feather ?: 0f
                    else -> 0f
                }

                val liveEvaluatedValue = remember(clip, activeProperty, playheadPositionMs) {
                    com.example.core.media.KeyframeEvaluator.evaluateProperty(
                        clip.keyframes,
                        activeProperty,
                        playheadPositionMs,
                        clipDefaultValue
                    )
                }

                val valueRange: ClosedFloatingPointRange<Float> = when (activeProperty) {
                    KeyframeProperty.POSITION_X, KeyframeProperty.POSITION_Y -> -1000f..1000f
                    KeyframeProperty.SCALE_X, KeyframeProperty.SCALE_Y -> 0.1f..5f
                    KeyframeProperty.ROTATION -> -360f..360f
                    KeyframeProperty.OPACITY -> 0f..1f
                    KeyframeProperty.VOLUME -> 0f..2f
                    KeyframeProperty.BRIGHTNESS -> -1f..1f
                    KeyframeProperty.CONTRAST -> 0.2f..3f
                    KeyframeProperty.SATURATION -> 0f..3f
                    KeyframeProperty.EXPOSURE -> -2f..2f
                    KeyframeProperty.TEMPERATURE, KeyframeProperty.TINT -> -1f..1f
                    KeyframeProperty.HIGHLIGHTS, KeyframeProperty.SHADOWS -> -1f..1f
                    KeyframeProperty.MASK_X, KeyframeProperty.MASK_Y, KeyframeProperty.MASK_FEATHER -> 0f..1f
                    else -> -100f..100f
                }

                val currentValue = existingKf?.value ?: liveEvaluatedValue
                var sliderValue by remember(activeProperty, existingKf?.id, existingKf?.value, liveEvaluatedValue) {
                    mutableFloatStateOf(currentValue)
                }

                // Value Display & Slider
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161925)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Value: ${String.format(Locale.US, "%.2f", sliderValue)}",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            val relTimeSec = (playheadPositionMs - clip.startTimeMs).coerceAtLeast(0L) / 1000f
                            Text(
                                text = "At: ${String.format(Locale.US, "%.2f", relTimeSec)}s",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }

                        Slider(
                            value = sliderValue.coerceIn(valueRange),
                            onValueChange = { newValue ->
                                sliderValue = newValue
                                if (existingKf != null) {
                                    onUpdateKeyframe(clip.id, existingKf.id, newValue, existingKf.interpolation)
                                }
                            },
                            valueRange = valueRange,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFFD600),
                                activeTrackColor = Color(0xFF00D2FF),
                                inactiveTrackColor = Color(0xFF2C3248)
                            )
                        )

                        // Keyframe action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (existingKf != null) {
                                Text(
                                    "◆ Keyframe Active",
                                    color = Color(0xFFFFD600),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedButton(
                                    onClick = { onDeleteKeyframe(clip.id, existingKf.id) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Delete Keyframe", fontSize = 12.sp)
                                }
                            } else {
                                Text(
                                    "No keyframe here",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                                Button(
                                    onClick = {
                                        val clampedTime = playheadPositionMs.coerceIn(clip.startTimeMs, clip.endTimeMs)
                                        onAddKeyframe(clip.id, activeProperty, clampedTime, sliderValue, InterpolationType.LINEAR)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D2FF), contentColor = Color(0xFF0A0D14))
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add Keyframe", fontSize = 12.sp)
                                }
                            }
                        }

                        // Interpolation picker (when keyframe exists)
                        if (existingKf != null) {
                            Spacer(Modifier.height(8.dp))
                            Text("Interpolation", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                            Spacer(Modifier.height(4.dp))
                            val interpolations = listOf(
                                InterpolationType.LINEAR to "Linear",
                                InterpolationType.HOLD to "Hold",
                                InterpolationType.EASE_IN to "Ease In",
                                InterpolationType.EASE_OUT to "Ease Out",
                                InterpolationType.EASE_IN_OUT to "Ease In-Out",
                                InterpolationType.CUBIC_EASE_IN to "Cubic In",
                                InterpolationType.CUBIC_EASE_OUT to "Cubic Out",
                                InterpolationType.CUBIC_EASE_IN_OUT to "Cubic In-Out",
                                InterpolationType.SMOOTH to "Smooth"
                            )
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(interpolations) { (interpType, interpLabel) ->
                                    val isCurrent = existingKf.interpolation == interpType
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isCurrent) Color(0xFF00D2FF) else Color(0xFF1E2230))
                                            .clickable {
                                                onUpdateKeyframe(clip.id, existingKf.id, existingKf.value, interpType)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            interpLabel,
                                            color = if (isCurrent) Color(0xFF0A0D14) else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Keyframe List for this clip
                val propKeyframes = clip.keyframes.sortedBy { it.timeMs }
                Text(
                    "All Clip Keyframes (${propKeyframes.size})",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (propKeyframes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No keyframes created yet. Move playhead and tap Add Keyframe.",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(propKeyframes, key = { it.id }) { kf ->
                            val isAtPlayhead = kotlin.math.abs(kf.timeMs - playheadPositionMs) <= 50L
                            val relTimeSec = (kf.timeMs - clip.startTimeMs) / 1000f
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isAtPlayhead) Color(0xFF2C3248) else Color(0xFF161925))
                                    .clickable { onSeek(kf.timeMs) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Canvas(modifier = Modifier.size(10.dp)) {
                                        val path = androidx.compose.ui.graphics.Path().apply {
                                            moveTo(size.width / 2f, 0f)
                                            lineTo(size.width, size.height / 2f)
                                            lineTo(size.width / 2f, size.height)
                                            lineTo(0f, size.height / 2f)
                                            close()
                                        }
                                        drawPath(path, color = Color(0xFFFFD600))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "${kf.property} @ ${String.format(Locale.US, "%.2f", relTimeSec)}s",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "= ${String.format(Locale.US, "%.2f", kf.value)}",
                                        color = Color(0xFF00D2FF),
                                        fontSize = 12.sp
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteKeyframe(clip.id, kf.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Delete",
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeatsBottomSheet(
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F111A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFF00D2FF))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Audio Ducking & Beat Sync",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text("Automatically detect beats and sync cuts, while ducking background audio.", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            var isDuckingEnabled by remember { mutableStateOf(false) }
            var isBeatSyncEnabled by remember { mutableStateOf(false) }
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Audio Ducking", color = Color.White, fontSize = 16.sp)
                Switch(checked = isDuckingEnabled, onCheckedChange = { isDuckingEnabled = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00D2FF), checkedTrackColor = Color(0xFF00D2FF).copy(alpha = 0.5f)))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Auto Beat Sync (Auto Cut)", color = Color.White, fontSize = 16.sp)
                Switch(checked = isBeatSyncEnabled, onCheckedChange = { isBeatSyncEnabled = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00D2FF), checkedTrackColor = Color(0xFF00D2FF).copy(alpha = 0.5f)))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF0A0D14))) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF0A0D14))
                Spacer(Modifier.width(8.dp))
                Text("Apply & Sync")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
