package com.example.feature.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
    var rotationDegrees by remember(currentTransform) { mutableIntStateOf(currentTransform.rotation.toInt()) }
    var scale by remember(currentTransform) { mutableFloatStateOf(currentTransform.scaleX) }

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
        ) {
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
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

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
                    Text(
                        text = "Scale / Zoom: ${String.format("%.2fx", scale)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 0.5f..3.0f,
                        steps = 25,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                OutlinedButton(
                    onClick = { rotationDegrees = (rotationDegrees + 90) % 360 },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.RotateRight, contentDescription = null)
                    Spacer(modifier = Modifier.padding(start = 4.dp))
                    Text("Rotate ($rotationDegrees°)")
                }

                OutlinedButton(
                    onClick = {
                        rotationDegrees = 0
                        scale = 1.0f
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset")
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            AppPrimaryButton(
                text = "Apply Transform",
                onClick = {
                    onTransformChanged(
                        currentTransform.copy(
                            rotation = rotationDegrees.toFloat(),
                            scaleX = scale,
                            scaleY = scale
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))
        }
    }
}
