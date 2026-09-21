package com.example.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.core.ui.theme.AppRadius

/**
 * Filter and selection chip matching UI_DESIGN_SYSTEM.md Section 15.5
 */
@Composable
fun AppChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall
            )
        },
        modifier = modifier.height(36.dp),
        enabled = enabled,
        leadingIcon = leadingIcon,
        shape = RoundedCornerShape(AppRadius.button),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary
        )
    )
}
