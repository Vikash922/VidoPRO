package com.example.feature.export

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.common.TimeUtils
import com.example.core.ui.components.AppPrimaryButton
import com.example.core.ui.components.AppSecondaryButton
import com.example.core.ui.components.LoadingView
import com.example.core.ui.theme.AppRadius
import com.example.core.ui.theme.AppSpacing

/**
 * Export screen enabling resolution, FPS, and quality configuration with real-time export progress (DEV-072).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExportScreen(
    uiState: ExportUiState,
    onResolutionSelected: (ResolutionPreset) -> Unit,
    onFpsSelected: (FpsPreset) -> Unit,
    onQualitySelected: (QualityPreset) -> Unit,
    onStartExport: () -> Unit,
    onCancelExport: () -> Unit,
    onResetState: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Export Project",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        enabled = !uiState.isExporting,
                        modifier = Modifier.testTag("export_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (uiState.isExporting) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (uiState.isLoadingProject) {
            LoadingView(
                message = "Loading export options...",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
            ) {
                // 1. Project Summary Card
                ExportSummaryCard(uiState = uiState)

                // 2. Settings Section (disabled while exporting)
                if (!uiState.isExporting && uiState.status != ExportStatus.SUCCESS) {
                    // Resolution selector
                    ExportOptionGroup(
                        title = "Resolution",
                        icon = Icons.Default.Videocam
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                        ) {
                            ResolutionPreset.entries.forEach { preset ->
                                val isSelected = uiState.selectedResolution == preset
                                val (w, h) = preset.getDimensions(uiState.aspectRatio)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onResolutionSelected(preset) },
                                    label = {
                                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Text(
                                                text = preset.label,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                            Text(
                                                text = "${w}x${h}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.testTag("export_resolution_${preset.shortLabel.lowercase()}")
                                )
                            }
                        }
                    }

                    // Frame Rate selector
                    ExportOptionGroup(
                        title = "Frame Rate (FPS)",
                        icon = Icons.Default.Speed
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                        ) {
                            FpsPreset.entries.forEach { preset ->
                                val isSelected = uiState.selectedFps == preset
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onFpsSelected(preset) },
                                    label = {
                                        Text(
                                            text = preset.shortLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("export_fps_${preset.fps}")
                                )
                            }
                        }
                    }

                    // Quality / Bitrate selector
                    ExportOptionGroup(
                        title = "Bitrate Quality",
                        icon = Icons.Default.HighQuality
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                        ) {
                            QualityPreset.entries.forEach { preset ->
                                val isSelected = uiState.selectedQuality == preset
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onQualitySelected(preset) },
                                    label = {
                                        Text(
                                            text = preset.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("export_quality_${preset.name.lowercase()}")
                                )
                            }
                        }
                    }
                }

                // 3. Progress / Action Area
                when (uiState.status) {
                    ExportStatus.IDLE, ExportStatus.CANCELLED -> {
                        if (uiState.status == ExportStatus.CANCELLED) {
                            Surface(
                                shape = RoundedCornerShape(AppRadius.medium),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(AppSpacing.md),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(AppSpacing.sm))
                                    Text(
                                        text = "Export was cancelled.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        AppPrimaryButton(
                            text = "Start Export",
                            onClick = onStartExport,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("export_start_button")
                        )
                    }

                    ExportStatus.PREPARING, ExportStatus.EXPORTING, ExportStatus.SAVING_TO_GALLERY -> {
                        ExportProgressCard(
                            uiState = uiState,
                            onCancel = onCancelExport
                        )
                    }

                    ExportStatus.SUCCESS -> {
                        ExportSuccessCard(
                            uiState = uiState,
                            onOpenVideo = {
                                uiState.outputUri?.let { uriStr ->
                                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse(uriStr), "video/mp4")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    try {
                                        context.startActivity(viewIntent)
                                    } catch (_: Exception) {}
                                }
                            },
                            onShareVideo = {
                                uiState.outputUri?.let { uriStr ->
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "video/mp4"
                                        putExtra(Intent.EXTRA_STREAM, Uri.parse(uriStr))
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    try {
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                                    } catch (_: Exception) {}
                                }
                            },
                            onDone = onNavigateBack
                        )
                    }

                    ExportStatus.ERROR -> {
                        ExportErrorCard(
                            errorMessage = uiState.errorMessage ?: "Export encountered an unexpected error.",
                            onRetry = onStartExport,
                            onReset = onResetState
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportSummaryCard(uiState: ExportUiState) {
    val project = uiState.project
    val (w, h) = uiState.dimensions

    Card(
        shape = RoundedCornerShape(AppRadius.medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(AppSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project?.name ?: "Untitled Project",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Format: MP4 (H.264 / AAC)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(AppRadius.small),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(start = AppSpacing.sm)
                ) {
                    Text(
                        text = uiState.aspectRatio.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(AppRadius.small)
                    )
                    .padding(AppSpacing.sm),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                SummaryItem(label = "Duration", value = TimeUtils.formatDuration(project?.durationMs ?: 0L))
                SummaryItem(label = "Resolution", value = "${w}x${h}")
                SummaryItem(label = "Est. Size", value = String.format("%.1f MB", uiState.estimatedSizeMb))
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ExportOptionGroup(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.xs))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        content()
    }
}

@Composable
private fun ExportProgressCard(
    uiState: ExportUiState,
    onCancel: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(AppRadius.medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("export_progress_card")
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.statusMessage.ifBlank { "Exporting video..." },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${uiState.progressPercent}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            LinearProgressIndicator(
                progress = { uiState.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(AppRadius.button))
                    .testTag("export_progress_indicator")
            )

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            AppSecondaryButton(
                text = "Cancel Export",
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_cancel_button")
            )
        }
    }
}

@Composable
private fun ExportSuccessCard(
    uiState: ExportUiState,
    onOpenVideo: () -> Unit,
    onShareVideo: () -> Unit,
    onDone: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(AppRadius.medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1B5E20).copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = "Export Complete!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Video successfully rendered and saved to your device's Gallery under Movies / VideoEditor.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                AppSecondaryButton(
                    text = "Share",
                    onClick = onShareVideo,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_share_button")
                )

                AppPrimaryButton(
                    text = "Done",
                    onClick = onDone,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_done_button")
                )
            }
        }
    }
}

@Composable
private fun ExportErrorCard(
    errorMessage: String,
    onRetry: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(AppRadius.medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )

            Text(
                text = "Export Failed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                AppSecondaryButton(
                    text = "Change Settings",
                    onClick = onReset,
                    modifier = Modifier.weight(1f)
                )

                AppPrimaryButton(
                    text = "Retry",
                    onClick = onRetry,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_retry_button")
                )
            }
        }
    }
}
