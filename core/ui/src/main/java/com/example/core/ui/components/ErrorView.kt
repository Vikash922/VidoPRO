package com.example.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.ui.theme.AppIconSize
import com.example.core.ui.theme.AppSpacing

/**
 * Standard error state presentation matching UI_DESIGN_SYSTEM.md Section 20
 */
@Composable
fun ErrorView(
    message: String,
    modifier: Modifier = Modifier,
    title: String = "Something went wrong",
    retryButtonText: String? = "Retry",
    onRetryClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(AppIconSize.emptyState)
        )

        Spacer(modifier = Modifier.height(AppSpacing.lg))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(AppSpacing.xs))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (retryButtonText != null && onRetryClick != null) {
            Spacer(modifier = Modifier.height(AppSpacing.xl))
            AppSecondaryButton(
                text = retryButtonText,
                onClick = onRetryClick
            )
        }
    }
}
