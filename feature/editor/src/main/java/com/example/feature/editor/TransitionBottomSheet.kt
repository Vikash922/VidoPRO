package com.example.feature.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.media.transition.TransitionValidator
import com.example.core.model.Transition
import com.example.core.model.TransitionType
import com.example.core.ui.theme.AppRadius
import com.example.core.ui.theme.AppSpacing
import java.util.UUID
import kotlin.math.roundToLong

data class TransitionOption(
    val type: TransitionType,
    val label: String,
    val icon: ImageVector
)

private val SUPPORTED_TRANSITIONS = listOf(
    TransitionOption(TransitionType.NONE, "None", Icons.Default.Block),
    TransitionOption(TransitionType.FADE, "Fade", Icons.Default.Opacity),
    TransitionOption(TransitionType.SLIDE, "Slide", Icons.Default.LinearScale),
    TransitionOption(TransitionType.ZOOM, "Zoom", Icons.Default.ZoomIn),
    TransitionOption(TransitionType.WIPE, "Wipe", Icons.Default.Transform)
)

private val DIRECTIONS = listOf(
    "LEFT" to Icons.AutoMirrored.Filled.ArrowBack,
    "RIGHT" to Icons.AutoMirrored.Filled.ArrowForward,
    "UP" to Icons.Default.ArrowUpward,
    "DOWN" to Icons.Default.ArrowDownward
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransitionBottomSheet(
    firstClipId: String,
    secondClipId: String,
    firstClipDurationMs: Long,
    secondClipDurationMs: Long,
    existingTransition: Transition?,
    onApply: (Transition) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val maxDuration = remember(firstClipDurationMs, secondClipDurationMs) {
        TransitionValidator.maxTransitionDuration(firstClipDurationMs, secondClipDurationMs)
    }

    var selectedType by remember(existingTransition) {
        mutableStateOf(existingTransition?.type ?: TransitionType.FADE)
    }

    var durationMs by remember(existingTransition, maxDuration) {
        val initial = existingTransition?.durationMs ?: 500L
        mutableFloatStateOf(initial.coerceIn(TransitionValidator.MIN_TRANSITION_DURATION_MS, maxDuration).toFloat())
    }

    var selectedDirection by remember(existingTransition) {
        val dir = (existingTransition?.properties?.get("direction") as? String) ?: "LEFT"
        mutableStateOf(dir)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier.testTag("transition_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Transform,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.padding(start = AppSpacing.xs))
                    Text(
                        text = "Video Transition",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Transition Type Selection (Horizontal Chips/Cards)
            Text(
                text = "Transition Type",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(AppSpacing.xs))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(SUPPORTED_TRANSITIONS) { option ->
                    val isSelected = selectedType == option.type
                    Card(
                        shape = RoundedCornerShape(AppRadius.md),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .clickable { selectedType = option.type }
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = option.label,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = option.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (selectedType != TransitionType.NONE) {
                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Duration Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("%.2fs", durationMs / 1000f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Slider(
                    value = durationMs,
                    onValueChange = { durationMs = it },
                    valueRange = TransitionValidator.MIN_TRANSITION_DURATION_MS.toFloat()..maxDuration.toFloat(),
                    steps = if (maxDuration > 200L) ((maxDuration - 100L) / 100L).toInt().coerceAtMost(20) else 0,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Direction Options for SLIDE and WIPE
                if (selectedType == TransitionType.SLIDE || selectedType == TransitionType.WIPE) {
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    Text(
                        text = "Direction",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.xs))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DIRECTIONS.forEach { (dirName, dirIcon) ->
                            val isSelected = selectedDirection == dirName
                            Surface(
                                shape = RoundedCornerShape(AppRadius.sm),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedDirection = dirName }
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = dirIcon,
                                        contentDescription = dirName,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = dirName.lowercase().replaceFirstChar { it.uppercase() },
                                        fontSize = 11.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (existingTransition != null) {
                    OutlinedButton(
                        onClick = {
                            onRemove(existingTransition.id)
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Remove")
                    }
                }

                Button(
                    onClick = {
                        if (selectedType == TransitionType.NONE) {
                            if (existingTransition != null) {
                                onRemove(existingTransition.id)
                            }
                        } else {
                            val props = mutableMapOf<String, Any>()
                            if (selectedType == TransitionType.SLIDE || selectedType == TransitionType.WIPE) {
                                props["direction"] = selectedDirection
                            }
                            val transition = Transition(
                                id = existingTransition?.id ?: UUID.randomUUID().toString(),
                                type = selectedType,
                                durationMs = durationMs.roundToLong(),
                                firstClipId = firstClipId,
                                secondClipId = secondClipId,
                                properties = props
                            )
                            onApply(transition)
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(if (existingTransition != null) 1.5f else 1f)
                ) {
                    Text(if (selectedType == TransitionType.NONE) "Clear Transition" else "Apply Transition")
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))
        }
    }
}
