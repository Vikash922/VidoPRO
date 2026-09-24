package com.example.feature.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.model.Transform
import com.example.core.ui.components.AppPrimaryButton
import com.example.core.ui.theme.AppRadius
import com.example.core.ui.theme.AppSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransformBottomSheet(
    currentTransform: Transform,
    onTransformChanged: (Transform) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var posX by remember(currentTransform) { mutableFloatStateOf(currentTransform.x) }
    var posY by remember(currentTransform) { mutableFloatStateOf(currentTransform.y) }
    var scale by remember(currentTransform) { mutableFloatStateOf(currentTransform.scaleX) }
    var rotationDegrees by remember(currentTransform) { mutableIntStateOf(currentTransform.rotation.toInt()) }

    fun emitChange(x: Float = posX, y: Float = posY, s: Float = scale, r: Int = rotationDegrees) {
        posX = x
        posY = y
        scale = s
        rotationDegrees = r
        onTransformChanged(
            currentTransform.copy(
                x = x,
                y = y,
                scaleX = s,
                scaleY = s,
                rotation = r.toFloat()
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier.testTag("transform_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Title + Reset + Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CropRotate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.padding(start = AppSpacing.xs))
                    Text(
                        text = "Transform & Scale",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        emitChange(x = 0f, y = 0f, s = 1.0f, r = 0)
                    }) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Reset Transform")
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // 1. SCALE CONTROLS
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(AppRadius.medium)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Scale / Zoom",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${String.format("%.2f", scale)}x",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    Slider(
                        value = scale,
                        onValueChange = { emitChange(s = it) },
                        valueRange = 0.5f..3.0f,
                        steps = 25,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { preset ->
                            FilterChip(
                                selected = (scale - preset).let { kotlin.math.abs(it) < 0.05f },
                                onClick = { emitChange(s = preset) },
                                label = { Text("${preset}x") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // 2. ROTATION CONTROLS
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(AppRadius.medium)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rotation",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$rotationDegrees°",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    Slider(
                        value = rotationDegrees.toFloat(),
                        onValueChange = { emitChange(r = it.toInt()) },
                        valueRange = 0f..360f,
                        steps = 35,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0, 90, 180, 270).forEach { deg ->
                            FilterChip(
                                selected = (rotationDegrees % 360) == deg,
                                onClick = { emitChange(r = deg) },
                                label = { Text("$deg°") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                        OutlinedButton(
                            onClick = { emitChange(r = (rotationDegrees + 90) % 360) },
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.RotateRight, contentDescription = null)
                            Spacer(Modifier.padding(start = 2.dp))
                            Text("+90°")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // 3. POSITION (X / Y) CONTROLS
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(AppRadius.medium)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Position (X: ${posX.toInt()}, Y: ${posY.toInt()})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { emitChange(x = 0f, y = 0f) }
                        ) {
                            Icon(imageVector = Icons.Default.CenterFocusStrong, contentDescription = null)
                            Spacer(Modifier.padding(start = 2.dp))
                            Text("Center")
                        }
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    Text(
                        text = "Horizontal (X)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = posX,
                        onValueChange = { emitChange(x = it) },
                        valueRange = -500f..500f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Vertical (Y)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = posY,
                        onValueChange = { emitChange(y = it) },
                        valueRange = -500f..500f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            AppPrimaryButton(
                text = "Done",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))
        }
    }
}
