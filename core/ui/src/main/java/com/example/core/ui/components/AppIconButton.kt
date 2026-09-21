package com.example.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.core.ui.theme.AppIconSize
import com.example.core.ui.theme.AppRadius

/**
 * Standard icon button matching UI_DESIGN_SYSTEM.md Section 15.2
 * Guarantees minimum 48dp touch target with standard 24dp icon size.
 */
@Composable
fun AppIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    tint: Color? = null
) {
    val effectiveTint = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        tint != null -> tint
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(AppRadius.medium))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = effectiveTint,
            modifier = Modifier.size(AppIconSize.standard)
        )
    }
}
