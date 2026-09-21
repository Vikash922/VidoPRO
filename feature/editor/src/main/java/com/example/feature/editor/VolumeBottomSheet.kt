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
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.ui.components.AppPrimaryButton
import com.example.core.ui.theme.AppRadius
import com.example.core.ui.theme.AppSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeBottomSheet(
    currentVolume: Float,
    onVolumeChanged: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedVolume by remember(currentVolume) { mutableFloatStateOf(currentVolume) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier.testTag("volume_bottom_sheet")
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
                        imageVector = if (selectedVolume == 0f) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.padding(start = AppSpacing.xs))
                    Text(
                        text = "Clip Volume",
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
                        .padding(AppSpacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${(selectedVolume * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    Slider(
                        value = selectedVolume,
                        onValueChange = { selectedVolume = it },
                        valueRange = 0.0f..2.0f,
                        steps = 20,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                FilterChip(
                    selected = selectedVolume == 0f,
                    onClick = { selectedVolume = 0f },
                    label = { Text("Mute (0%)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
                FilterChip(
                    selected = Math.abs(selectedVolume - 1.0f) < 0.05f,
                    onClick = { selectedVolume = 1.0f },
                    label = { Text("100% (Default)") }
                )
                FilterChip(
                    selected = Math.abs(selectedVolume - 2.0f) < 0.05f,
                    onClick = { selectedVolume = 2.0f },
                    label = { Text("Boost 200%") }
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                AppPrimaryButton(
                    text = "Apply Volume",
                    onClick = {
                        onVolumeChanged(selectedVolume)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(AppSpacing.md))
        }
    }
}
