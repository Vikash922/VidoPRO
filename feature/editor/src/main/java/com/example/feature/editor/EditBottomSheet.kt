package com.example.feature.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.common.TimeUtils
import com.example.core.model.Clip
import com.example.core.ui.theme.AppRadius
import com.example.core.ui.theme.AppSpacing

/**
 * Bottom Sheet containing Split, Delete, and Duplicate actions for selected clip.
 * Conforms to DEV-054 to DEV-057 and UI_DESIGN_SYSTEM.md.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBottomSheet(
    clip: Clip,
    playheadPositionMs: Long,
    onSplit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
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
        modifier = modifier.testTag("edit_bottom_sheet")
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
                Column {
                    Text(
                        text = "Edit Clip",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${clip.type.name} • ${TimeUtils.formatDuration(clip.durationMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("edit_sheet_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            // Clip Timing Details Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(AppRadius.medium)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.sm),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimingChip(
                        label = "Timeline Start",
                        value = TimeUtils.formatTimecode(clip.startTimeMs)
                    )
                    TimingChip(
                        label = "Timeline End",
                        value = TimeUtils.formatTimecode(clip.endTimeMs)
                    )
                    TimingChip(
                        label = "Playhead",
                        value = TimeUtils.formatTimecode(playheadPositionMs)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            // Action Buttons Row: Split, Duplicate, Delete
            val canSplit = playheadPositionMs > clip.startTimeMs && playheadPositionMs < clip.endTimeMs

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                // Split Action Button (DEV-054)
                EditActionButton(
                    icon = Icons.Default.ContentCut,
                    label = "Split",
                    subtitle = if (canSplit) "At playhead" else "Outside clip",
                    enabled = canSplit,
                    testTag = "action_split_button",
                    onClick = onSplit,
                    modifier = Modifier.weight(1f)
                )

                // Duplicate Action Button (DEV-056)
                EditActionButton(
                    icon = Icons.Default.ContentCopy,
                    label = "Duplicate",
                    subtitle = "Clone clip",
                    enabled = true,
                    testTag = "action_duplicate_button",
                    onClick = onDuplicate,
                    modifier = Modifier.weight(1f)
                )

                // Delete Action Button (DEV-055)
                EditActionButton(
                    icon = Icons.Default.Delete,
                    label = "Delete",
                    subtitle = "Remove",
                    enabled = true,
                    isDestructive = true,
                    testTag = "action_delete_button",
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.xl))
        }
    }
}

@Composable
private fun EditActionButton(
    icon: ImageVector,
    label: String,
    subtitle: String,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    val containerColor = if (isDestructive) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (!enabled) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    } else if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val iconTint = if (!enabled) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    } else if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 72.dp)
            .clip(RoundedCornerShape(AppRadius.medium))
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag),
        color = containerColor,
        shape = RoundedCornerShape(AppRadius.medium),
        border = if (isDestructive) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TimingChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
