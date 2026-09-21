package com.example.feature.home

import com.example.core.model.Project

/**
 * UI State for the Home screen.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val projects: List<Project> = emptyList(),
    val error: String? = null,
    val isNewProjectSheetOpen: Boolean = false,
    val projectToDelete: Project? = null,
    val projectToRename: Project? = null
)
