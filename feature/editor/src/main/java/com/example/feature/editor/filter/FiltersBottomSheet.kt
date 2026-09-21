package com.example.feature.editor.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.ui.components.AppPrimaryButton
import com.example.core.ui.theme.AppSpacing

/**
 * Filter adjustment values for basic preview effects (DEV-065).
 */
@Immutable
data class FilterSettings(
    val brightness: Float = 0f,   // Range: -0.5f to 0.5f (0 = default)
    val contrast: Float = 1f,     // Range: 0.5f to 2.0f (1 = default)
    val saturation: Float = 1f    // Range: 0.0f to 2.0f (1 = default)
) {
    val isDefault: Boolean
        get() = brightness == 0f && contrast == 1f && saturation == 1f
}

/**
 * Bottom sheet for controlling Brightness, Contrast, and Saturation filters (DEV-064, DEV-065).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersBottomSheet(
    filterSettings: FilterSettings,
    onFilterChange: (FilterSettings) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier.testTag("filters_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.xs)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Color Filters",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onReset,
                        modifier = Modifier.testTag("reset_filters_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset")
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("filters_sheet_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Brightness Slider (-0.5 to 0.5)
            FilterSliderItem(
                label = "Brightness",
                value = filterSettings.brightness,
                displayValue = String.format("%+.2f", filterSettings.brightness),
                valueRange = -0.5f..0.5f,
                onValueChange = { onFilterChange(filterSettings.copy(brightness = it)) }
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Contrast Slider (0.5 to 2.0)
            FilterSliderItem(
                label = "Contrast",
                value = filterSettings.contrast,
                displayValue = String.format("%.2fx", filterSettings.contrast),
                valueRange = 0.5f..2.0f,
                onValueChange = { onFilterChange(filterSettings.copy(contrast = it)) }
            )

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Saturation Slider (0.0 to 2.0)
            FilterSliderItem(
                label = "Saturation",
                value = filterSettings.saturation,
                displayValue = String.format("%.2fx", filterSettings.saturation),
                valueRange = 0.0f..2.0f,
                onValueChange = { onFilterChange(filterSettings.copy(saturation = it)) }
            )

            Spacer(modifier = Modifier.height(AppSpacing.xl))

            // Done Button
            AppPrimaryButton(
                text = "Apply Filters",
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("apply_filters_button")
            )

            Spacer(modifier = Modifier.height(AppSpacing.xl))
        }
    }
}

@Composable
private fun FilterSliderItem(
    label: String,
    value: Float,
    displayValue: String,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = displayValue,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
