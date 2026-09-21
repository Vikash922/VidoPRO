package com.example.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.core.ui.theme.AppRadius

/**
 * Base surface card matching UI_DESIGN_SYSTEM.md Section 15.3
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(AppRadius.card)
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = colors,
            border = border,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            border = border,
            content = content
        )
    }
}
