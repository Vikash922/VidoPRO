package com.example.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.common.TimeUtils
import com.example.core.model.AspectRatio
import com.example.core.model.Project
import com.example.core.ui.components.EmptyStateView
import com.example.core.ui.components.LoadingView
import com.example.core.ui.components.ProjectCard
import com.example.core.ui.theme.AppSpacing
import com.example.feature.home.components.DeleteProjectDialog
import com.example.feature.home.components.NewProjectBottomSheet
import com.example.feature.home.components.RenameProjectDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onNewProjectClick: () -> Unit,
    onDismissNewProjectSheet: () -> Unit,
    onCreateProject: (name: String, aspectRatio: AspectRatio) -> Unit,
    onProjectClick: (projectId: String) -> Unit,
    onProjectRenameClick: (project: Project) -> Unit,
    onConfirmRename: (newName: String) -> Unit,
    onDismissRename: () -> Unit,
    onProjectDuplicateClick: (projectId: String) -> Unit,
    onProjectDeleteClick: (project: Project) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onErrorDismiss: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onErrorDismiss()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Projects",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewProjectClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("new_project_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create new project"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingView(message = "Loading projects...")
                }

                uiState.projects.isEmpty() -> {
                    EmptyStateView(
                        icon = Icons.Default.Movie,
                        title = "No projects yet",
                        subtitle = "Create your first video project to get started",
                        actionButtonText = "New Project",
                        onActionClick = onNewProjectClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(
                            start = AppSpacing.md,
                            top = AppSpacing.md,
                            end = AppSpacing.md,
                            bottom = AppSpacing.xxl + 56.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.projects,
                            key = { it.id }
                        ) { project ->
                            ProjectCard(
                                projectName = project.name,
                                durationText = TimeUtils.formatDuration(project.durationMs),
                                lastEditedText = TimeUtils.formatLastEdited(project.updatedAt),
                                thumbnailUri = project.thumbnailPath,
                                aspectRatio = project.aspectRatio.floatRatio,
                                onClick = { onProjectClick(project.id) },
                                onRenameClick = { onProjectRenameClick(project) },
                                onDuplicateClick = { onProjectDuplicateClick(project.id) },
                                onDeleteClick = { onProjectDeleteClick(project) }
                            )
                        }
                    }
                }
            }
        }
    }

    // New Project Dialog/Sheet
    if (uiState.isNewProjectSheetOpen) {
        NewProjectBottomSheet(
            onDismissRequest = onDismissNewProjectSheet,
            onCreateProject = onCreateProject
        )
    }

    // Rename Dialog
    if (uiState.projectToRename != null) {
        RenameProjectDialog(
            initialName = uiState.projectToRename.name,
            onConfirm = onConfirmRename,
            onDismiss = onDismissRename
        )
    }

    // Delete Confirmation Dialog
    if (uiState.projectToDelete != null) {
        DeleteProjectDialog(
            projectName = uiState.projectToDelete.name,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDelete
        )
    }
}
