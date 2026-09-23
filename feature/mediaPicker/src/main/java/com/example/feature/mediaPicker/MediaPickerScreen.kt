package com.example.feature.mediaPicker

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.common.TimeUtils
import com.example.core.model.MediaType
import com.example.core.ui.components.AppChip
import com.example.core.ui.components.AppPrimaryButton
import com.example.core.ui.components.AppSecondaryButton
import com.example.core.ui.components.EmptyStateView
import com.example.core.ui.components.LoadingView
import com.example.core.ui.theme.AppRadius
import com.example.core.ui.theme.AppSpacing
import com.example.feature.mediaPicker.model.MediaItem
import com.example.feature.mediaPicker.permission.PermissionHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPickerScreen(
    uiState: MediaPickerUiState,
    onFilterSelected: (MediaFilter) -> Unit,
    onMediaItemClick: (MediaItem) -> Unit,
    onConfirmSelection: () -> Unit,
    onNavigateBack: () -> Unit,
    onUrisPicked: (List<android.net.Uri>) -> Unit,
    onPermissionGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Zero-permission modern Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris ->
        if (uris.isNotEmpty()) {
            onUrisPicked(uris)
        }
    }

    // Traditional MediaStore permissions launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.any { it }
        if (granted) {
            onPermissionGranted()
        }
    }

    // Auto-request permission on first composition — no more silent loading
    LaunchedEffect(Unit) {
        if (PermissionHandler.hasMediaPermission(context)) {
            onPermissionGranted()
        } else {
            // Immediately launch permission dialog — don't make user hunt for button
            permissionLauncher.launch(PermissionHandler.getRequiredMediaPermissions())
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add Media",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("media_picker_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // System Photo Picker Action Button
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        modifier = Modifier.testTag("system_photo_picker_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Open System Gallery",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${uiState.selectedCount} selected",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (uiState.selectedCount > 0) {
                            val totalDuration = uiState.selectedItems.sumOf { it.durationMs }
                            Text(
                                text = "Total: ${TimeUtils.formatDuration(totalDuration)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AppPrimaryButton(
                        text = "Add to Timeline",
                        onClick = onConfirmSelection,
                        enabled = uiState.selectedCount > 0,
                        modifier = Modifier.testTag("confirm_media_selection_button")
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Filter chips row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MediaFilter.entries.forEach { filter ->
                    AppChip(
                        selected = uiState.currentFilter == filter,
                        onClick = { onFilterSelected(filter) },
                        label = filter.label
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        LoadingView(message = "Loading media...")
                    }

                    !uiState.hasPermission && uiState.mediaItems.isEmpty() -> {
                        // Permission rationale / System picker prompt
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(AppSpacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Collections,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.md))
                            Text(
                                text = "Media Access",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.xs))
                            Text(
                                text = "Grant permission to browse your device videos and images, or use the system picker.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.lg))
                            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                                AppSecondaryButton(
                                    text = "Grant Permission",
                                    onClick = {
                                        permissionLauncher.launch(PermissionHandler.getRequiredMediaPermissions())
                                    }
                                )
                                AppPrimaryButton(
                                    text = "Open Gallery",
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    uiState.mediaItems.isEmpty() -> {
                        EmptyStateView(
                            icon = Icons.Default.Movie,
                            title = "No media found",
                            subtitle = "Use the system gallery to pick videos and photos",
                            actionButtonText = "Open Gallery",
                            onActionClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 110.dp),
                            contentPadding = PaddingValues(AppSpacing.xs),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = uiState.mediaItems,
                                key = { it.id }
                            ) { item ->
                                val isSelected = uiState.selectedItems.any { it.id == item.id }
                                val selectionIndex = uiState.selectedItems.indexOfFirst { it.id == item.id }

                                MediaThumbnailItem(
                                    item = item,
                                    isSelected = isSelected,
                                    selectionNumber = if (isSelected) selectionIndex + 1 else null,
                                    onClick = { onMediaItemClick(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaThumbnailItem(
    item: MediaItem,
    isSelected: Boolean,
    selectionNumber: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(AppRadius.small))
            .background(Color(0xFF1C1C24))
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(AppRadius.small))
                else Modifier
            )
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dim overlay when selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )
        }

        // Selection badge in top right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(AppSpacing.xs)
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else Color.Black.copy(alpha = 0.5f)
                )
                .border(
                    width = 1.5.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected && selectionNumber != null) {
                Text(
                    text = selectionNumber.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // Duration or Type badge in bottom right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(AppSpacing.xs)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            if (item.mediaType == MediaType.VIDEO) {
                Text(
                    text = TimeUtils.formatDuration(item.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Photo",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
