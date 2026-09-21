package com.example.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.data.repository.ProjectRepository
import com.example.core.model.AspectRatio
import com.example.core.model.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing Home screen operations and observing ProjectRepository.
 */
class HomeViewModel(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeProjects()
    }

    private fun observeProjects() {
        projectRepository.observeProjects()
            .onEach { projects ->
                _uiState.update { it.copy(isLoading = false, projects = projects, error = null) }
            }
            .catch { throwable ->
                _uiState.update { it.copy(isLoading = false, error = throwable.message ?: "Failed to load projects") }
            }
            .launchIn(viewModelScope)
    }

    fun openNewProjectSheet() {
        _uiState.update { it.copy(isNewProjectSheetOpen = true) }
    }

    fun closeNewProjectSheet() {
        _uiState.update { it.copy(isNewProjectSheetOpen = false) }
    }

    fun createProject(name: String, aspectRatio: AspectRatio, onCreated: (String) -> Unit) {
        val projectName = if (name.isBlank()) "Untitled Project" else name.trim()
        viewModelScope.launch {
            try {
                val newProject = projectRepository.createProject(projectName, aspectRatio)
                _uiState.update { it.copy(isNewProjectSheetOpen = false) }
                onCreated(newProject.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to create project") }
            }
        }
    }

    fun openRenameDialog(project: Project) {
        _uiState.update { it.copy(projectToRename = project) }
    }

    fun closeRenameDialog() {
        _uiState.update { it.copy(projectToRename = null) }
    }

    fun renameProject(newName: String) {
        val currentProject = _uiState.value.projectToRename ?: return
        if (newName.isBlank() || newName.trim() == currentProject.name) {
            closeRenameDialog()
            return
        }

        viewModelScope.launch {
            try {
                projectRepository.updateProject(currentProject.copy(name = newName.trim()))
                closeRenameDialog()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to rename project") }
            }
        }
    }

    fun duplicateProject(projectId: String) {
        viewModelScope.launch {
            try {
                projectRepository.duplicateProject(projectId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to duplicate project") }
            }
        }
    }

    fun openDeleteDialog(project: Project) {
        _uiState.update { it.copy(projectToDelete = project) }
    }

    fun closeDeleteDialog() {
        _uiState.update { it.copy(projectToDelete = null) }
    }

    fun confirmDelete() {
        val project = _uiState.value.projectToDelete ?: return
        viewModelScope.launch {
            try {
                projectRepository.deleteProject(project.id)
                closeDeleteDialog()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to delete project") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        fun provideFactory(projectRepository: ProjectRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(projectRepository) as T
                }
            }
    }
}
