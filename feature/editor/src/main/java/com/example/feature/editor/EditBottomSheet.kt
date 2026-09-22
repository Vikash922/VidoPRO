package com.example.feature.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Build
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.common.TimeUtils
import com.example.core.model.Clip
import com.example.core.ui.theme.AppRadius
import com.example.core.ui.theme.AppSpacing

/**
 * Bottom Sheet containing all clip editing actions when a clip is tapped.
 * Includes: Split, Speed, Volume, Audio, Text, Overlay, Effects/Filters, Transform, Canvas, Keyframe, Beats, Duplicate, Delete
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
    onSpeed: () -> Unit,
    onVolume: () -> Unit,
    onAudio: () -> Unit,
    onText: () -> Unit,
    onOverlay: () -> Unit,
    onFilters: () -> Unit,
    onTransform: () -> Unit,
    onCanvas: () -> Unit,
    onKeyframe: () -> Unit,
    onBeats: () -> Unit,
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

            // Main Action Buttons Row 1: Split, Duplicate, Delete
            val canSplit = playheadPositionMs > clip.startTimeMs && playheadPositionMs < clip.endTimeMs

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                // Split Action Button
                EditActionButton(
                    icon = Icons.Default.ContentCut,
                    label = "Split",
                    subtitle = if (canSplit) "At playhead" else "Outside clip",
                    enabled = canSplit,
                    testTag = "action_split_button",
                    onClick = onSplit,
                    modifier = Modifier.weight(1f)
                )

                // Duplicate Action Button
                EditActionButton(
                    icon = Icons.Default.ContentCopy,
                    label = "Duplicate",
                    subtitle = "Clone clip",
                    enabled = true,
                    testTag = "action_duplicate_button",
                    onClick = onDuplicate,
                    modifier = Modifier.weight(1f)
                )

                // Delete Action Button
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

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            // Advanced Editing Tools Grid
            Text(
                text = "Editing Tools",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            val editingTools = remember {
                listOf(
                    EditorToolItem("Speed", Icons.Default.Speed, "Adjust playback speed", onSpeed),
                    EditorToolItem("Volume", Icons.Default.VolumeUp, "Adjust clip volume", onVolume),
                    EditorToolItem("Audio", Icons.Default.MusicNote, "Add audio track", onAudio),
                    EditorToolItem("Text", Icons.Default.TextFields, "Add text overlay", onText),
                    EditorToolItem("Overlay", Icons.Default.Layers, "Add image/video overlay", onOverlay),
                    EditorToolItem("Filters", Icons.Default.Filter, "Color filters & effects", onFilters),
                    EditorToolItem("Transform", Icons.Default.CropRotate, "Position, scale, rotate", onTransform),
                    EditorToolItem("Canvas", Icons.Default.AspectRatio, "Change aspect ratio", onCanvas),
                    EditorToolItem("Keyframes", Icons.Default.Star, "Animate properties", onKeyframe),
                    EditorToolItem("Beats", Icons.Default.Build, "Auto-cut to beats", onBeats),
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(editingTools) { tool ->
                    EditorToolButton(
                        tool = tool,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.xl))
        }
    }
}

@Composable
private fun EditorToolButton(
    tool: EditorToolItem,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 64.dp)
            .clip(RoundedCornerShape(AppRadius.medium))
            .clickable(onClick = tool.onClick)
            .testTag("tool_${tool.label.lowercase().replace(' ', '_')}_button"),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(AppRadius.medium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.md))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = tool.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class EditorToolItem(
    val label: String,
    val icon: ImageVector,
    val description: String,
    val onClick: () -> Unit
)

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
