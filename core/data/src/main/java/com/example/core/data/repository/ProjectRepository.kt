package com.example.core.data.repository

import com.example.core.model.AspectRatio
import com.example.core.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    suspend fun createProject(name: String, aspectRatio: AspectRatio = AspectRatio.RATIO_9_16): Project
    suspend fun getProjectById(projectId: String): Project?
    fun observeProjects(): Flow<List<Project>>
    fun observeProjectById(projectId: String): Flow<Project?>
    suspend fun updateProject(project: Project)
    suspend fun deleteProject(projectId: String)
    suspend fun duplicateProject(projectId: String): Project?
}
