package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.data.di.RepositoryModule
import com.example.core.media.Media3PreviewPlayer
import com.example.core.model.TrackType
import com.example.core.ui.theme.VideoEditorTheme
import com.example.feature.editor.EditorScreen
import com.example.feature.editor.EditorViewModel
import com.example.feature.export.ExportScreen
import com.example.feature.export.ExportViewModel
import com.example.feature.home.HomeScreen
import com.example.feature.home.HomeViewModel
import com.example.feature.mediaPicker.MediaPickerScreen
import com.example.feature.mediaPicker.MediaPickerViewModel
import com.example.feature.mediaPicker.data.MediaRepositoryImpl
import com.example.feature.mediaPicker.util.MediaConverter
import com.example.feature.settings.SettingsScreen
import com.example.feature.settings.SettingsViewModel
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Auto-updater check
        AppUpdater.checkForUpdates(this)
        
        val projectRepository = RepositoryModule.provideProjectRepository(applicationContext)
        val assetRepository = RepositoryModule.provideAssetRepository(applicationContext)

        setContent {
            VideoEditorTheme {
                var activeProjectId by remember { mutableStateOf<String?>(null) }
                var showMediaPicker by remember { mutableStateOf(false) }
                var showExportScreen by remember { mutableStateOf(false) }
                var showSettingsScreen by remember { mutableStateOf(false) }

                if (showSettingsScreen) {
                    BackHandler {
                        showSettingsScreen = false
                    }
                    val settingsViewModel: SettingsViewModel = viewModel(
                        factory = SettingsViewModel.provideFactory(applicationContext)
                    )
                    val settingsUiState by settingsViewModel.uiState.collectAsState()

                    SettingsScreen(
                        uiState = settingsUiState,
                        onResolutionChange = settingsViewModel::setDefaultResolution,
                        onFpsChange = settingsViewModel::setDefaultFps,
                        onQualityChange = settingsViewModel::setDefaultQuality,
                        onSnappingToggle = settingsViewModel::toggleSnapping,
                        onAutosaveToggle = settingsViewModel::toggleAutosave,
                        onClearCache = settingsViewModel::clearCache,
                        onClearMessage = settingsViewModel::clearMessage,
                        onNavigateBack = { showSettingsScreen = false }
                    )
                } else if (activeProjectId != null) {
                    val currentProjectId = activeProjectId!!

                    val previewPlayer = remember(currentProjectId) {
                        Media3PreviewPlayer(applicationContext)
                    }

                    val editorViewModel: EditorViewModel = viewModel(
                        key = "editor_$currentProjectId",
                        factory = EditorViewModel.provideFactory(
                            SavedStateHandle(mapOf("projectId" to currentProjectId)),
                            projectRepository,
                            assetRepository,
                            previewPlayer
                        )
                    )
                    val editorUiState by editorViewModel.uiState.collectAsState()

                    if (showExportScreen) {
                        BackHandler {
                            showExportScreen = false
                        }

                        val exportViewModel: ExportViewModel = viewModel(
                            key = "export_$currentProjectId",
                            factory = ExportViewModel.provideFactory(
                                currentProjectId,
                                projectRepository,
                                assetRepository,
                                applicationContext
                            )
                        )
                        val exportUiState by exportViewModel.uiState.collectAsState()

                        ExportScreen(
                            uiState = exportUiState,
                            onResolutionSelected = exportViewModel::setResolution,
                            onFpsSelected = exportViewModel::setFps,
                            onQualitySelected = exportViewModel::setQuality,
                            onStartExport = exportViewModel::startExport,
                            onCancelExport = exportViewModel::cancelExport,
                            onResetState = exportViewModel::resetState,
                            onNavigateBack = { showExportScreen = false }
                        )
                    } else if (showMediaPicker) {
                        BackHandler {
                            showMediaPicker = false
                        }

                        val mediaRepository = remember { MediaRepositoryImpl(applicationContext) }
                        val mediaPickerViewModel: MediaPickerViewModel = viewModel(
                            key = "media_picker",
                            factory = MediaPickerViewModel.provideFactory(mediaRepository)
                        )
                        val mediaPickerUiState by mediaPickerViewModel.uiState.collectAsState()

                        MediaPickerScreen(
                            uiState = mediaPickerUiState,
                            onFilterSelected = mediaPickerViewModel::setFilter,
                            onMediaItemClick = mediaPickerViewModel::toggleSelection,
                            onConfirmSelection = {
                                val selectedItems = mediaPickerUiState.selectedItems
                                val targetTrack = editorUiState.project?.tracks?.firstOrNull { it.type == TrackType.VIDEO }
                                val trackId = targetTrack?.id ?: UUID.randomUUID().toString()
                                var currentStart = targetTrack?.clips?.maxOfOrNull { it.endTimeMs } ?: 0L

                                val assetsAndClips = selectedItems.map { mediaItem ->
                                    val pair = MediaConverter.toAssetAndClip(mediaItem, trackId, currentStart)
                                    currentStart += pair.second.durationMs
                                    pair
                                }

                                editorViewModel.addMedia(assetsAndClips)
                                mediaPickerViewModel.clearSelection()
                                showMediaPicker = false
                            },
                            onNavigateBack = { showMediaPicker = false },
                            onUrisPicked = { uris ->
                                mediaPickerViewModel.handlePickedUris(uris) {
                                    // Uris parsed into selectedItems
                                }
                            },
                            onPermissionGranted = {
                                mediaPickerViewModel.updatePermissionStatus(true)
                            }
                        )
                    } else {
                        BackHandler {
                            editorViewModel.onEvent(com.example.feature.editor.EditorEvent.SaveImmediately)
                            activeProjectId = null
                        }

                        EditorScreen(
                            uiState = editorUiState,
                            player = previewPlayer.player,
                            onEvent = editorViewModel::onEvent,
                            onTimelineAction = editorViewModel::onTimelineAction,
                            onNavigateBack = {
                                editorViewModel.onEvent(com.example.feature.editor.EditorEvent.SaveImmediately)
                                activeProjectId = null
                            },
                            onNavigateExport = { showExportScreen = true },
                            onNavigateMediaPicker = { showMediaPicker = true }
                        )
                    }
                } else {
                    val homeViewModel: HomeViewModel = viewModel(
                        factory = HomeViewModel.provideFactory(projectRepository)
                    )
                    val uiState by homeViewModel.uiState.collectAsState()

                    HomeScreen(
                        uiState = uiState,
                        onNewProjectClick = homeViewModel::openNewProjectSheet,
                        onDismissNewProjectSheet = homeViewModel::closeNewProjectSheet,
                        onCreateProject = { name, ratio ->
                            homeViewModel.createProject(name, ratio) { newProjectId ->
                                activeProjectId = newProjectId
                            }
                        },
                        onProjectClick = { projectId ->
                            activeProjectId = projectId
                        },
                        onProjectRenameClick = homeViewModel::openRenameDialog,
                        onConfirmRename = homeViewModel::renameProject,
                        onDismissRename = homeViewModel::closeRenameDialog,
                        onProjectDuplicateClick = homeViewModel::duplicateProject,
                        onProjectDeleteClick = homeViewModel::openDeleteDialog,
                        onConfirmDelete = homeViewModel::confirmDelete,
                        onDismissDelete = homeViewModel::closeDeleteDialog,
                        onSettingsClick = {
                            showSettingsScreen = true
                        },
                        onErrorDismiss = homeViewModel::clearError
                    )
                }
            }
        }
    }
}
