package com.example.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.ui.theme.AppRadius
import com.example.core.ui.theme.AppSpacing

/**
 * Project card component for Home screen matching UI_DESIGN_SYSTEM.md Section 15.3
 */
@Composable
fun ProjectCard(
    projectName: String,
    durationText: String,
    lastEditedText: String,
    thumbnailUri: String?,
    onClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 16f / 9f
) {
    var menuExpanded by remember { mutableStateOf(false) }

    AppCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.sm)
        ) {
            // Thumbnail container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio.coerceIn(0.5f, 2.0f))
                    .clip(RoundedCornerShape(AppRadius.medium))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!thumbnailUri.isNullOrBlank()) {
                    AsyncImage(
                        model = thumbnailUri,
                        contentDescription = "Project thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Duration badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(AppSpacing.xs)
                        .clip(RoundedCornerShape(AppRadius.small))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = AppSpacing.xs, vertical = 2.dp)
                ) {
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            // Metadata row with overflow menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = projectName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = lastEditedText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Project options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = {
                                menuExpanded = false
                                onRenameClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            onClick = {
                                menuExpanded = false
                                onDuplicateClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }
        }
    }
}
