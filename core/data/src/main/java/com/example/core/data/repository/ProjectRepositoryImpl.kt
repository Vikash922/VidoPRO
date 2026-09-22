package com.example.core.data.repository

import androidx.room.withTransaction
import com.example.core.common.DefaultDispatcherProvider
import com.example.core.common.DispatcherProvider
import com.example.core.data.mappers.toDomain
import com.example.core.data.mappers.toEntity
import com.example.core.database.AppDatabase
import com.example.core.model.AspectRatio
import com.example.core.model.Project
import com.example.core.model.Track
import com.example.core.model.TrackType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class ProjectRepositoryImpl(
    private val database: AppDatabase,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : ProjectRepository {

    private val projectDao = database.projectDao()
    private val trackDao = database.trackDao()
    private val clipDao = database.clipDao()
    private val transformDao = database.transformDao()
    private val effectDao = database.effectDao()
    private val keyframeDao = database.keyframeDao()
    private val textClipDao = database.textClipDao()

    override suspend fun createProject(name: String, aspectRatio: AspectRatio): Project = withContext(dispatchers.io) {
        val (width, height) = when (aspectRatio) {
            AspectRatio.RATIO_9_16 -> 1080 to 1920
            AspectRatio.RATIO_16_9 -> 1920 to 1080
            AspectRatio.RATIO_1_1 -> 1080 to 1080
            AspectRatio.RATIO_4_5 -> 1080 to 1350
        }

        val projectId = UUID.randomUUID().toString()
        val defaultTrack = Track(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            type = TrackType.VIDEO,
            order = 0,
            isVisible = true,
            isLocked = false,
            clips = emptyList()
        )

        val project = Project(
            id = projectId,
            name = name,
            width = width,
            height = height,
            fps = 30,
            durationMs = 0L,
            aspectRatio = aspectRatio,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            thumbnailPath = null,
            tracks = listOf(defaultTrack)
        )

        database.withTransaction {
            projectDao.insert(project.toEntity())
            trackDao.insert(defaultTrack.toEntity())
        }

        project
    }

    override suspend fun getProjectById(projectId: String): Project? = withContext(dispatchers.io) {
        projectDao.getProjectWithTracks(projectId)?.toDomain()
    }

    override fun observeProjects(): Flow<List<Project>> {
        return projectDao.observeProjects()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override fun observeProjectById(projectId: String): Flow<Project?> {
        return projectDao.observeProjectWithTracks(projectId)
            .map { it?.toDomain() }
            .flowOn(dispatchers.io)
    }

    override suspend fun updateProject(project: Project) = withContext(dispatchers.io) {
        val updatedProject = project.copy(updatedAt = System.currentTimeMillis())

        database.withTransaction {
            projectDao.insert(updatedProject.toEntity())

            // Clean existing hierarchy for project
            trackDao.deleteByProjectId(updatedProject.id)

            // Insert new hierarchy
            for (track in updatedProject.tracks) {
                trackDao.insert(track.toEntity())

                for (clip in track.clips) {
                    clipDao.insert(clip.toEntity())

                    transformDao.insert(clip.transform.toEntity(clip.id))

                    if (clip.effects.isNotEmpty()) {
                        effectDao.insertAll(clip.effects.map { it.toEntity() })
                    }

                    if (clip.keyframes.isNotEmpty()) {
                        keyframeDao.insertAll(clip.keyframes.map { it.toEntity() })
                    }

                    clip.textData?.let { textData ->
                        textClipDao.insert(textData.toEntity())
                    }
                }
            }
        }
    }

    override suspend fun deleteProject(projectId: String) = withContext(dispatchers.io) {
        projectDao.deleteById(projectId)
    }

    override suspend fun duplicateProject(projectId: String): Project? = withContext(dispatchers.io) {
        val original = getProjectById(projectId) ?: return@withContext null
        val newProjectId = UUID.randomUUID().toString()

        val duplicatedTracks = original.tracks.map { track ->
            val newTrackId = UUID.randomUUID().toString()
            val duplicatedClips = track.clips.map { clip ->
                val newClipId = UUID.randomUUID().toString()
                clip.copy(
                    id = newClipId,
                    trackId = newTrackId,
                    effects = clip.effects.map { it.copy(id = UUID.randomUUID().toString(), clipId = newClipId) },
                    keyframes = clip.keyframes.map { it.copy(id = UUID.randomUUID().toString(), clipId = newClipId) },
                    textData = clip.textData?.copy(clipId = newClipId)
                )
            }
            track.copy(
                id = newTrackId,
                projectId = newProjectId,
                clips = duplicatedClips
            )
        }

        val duplicatedProject = original.copy(
            id = newProjectId,
            name = "${original.name} Copy",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            tracks = duplicatedTracks
        )

        updateProject(duplicatedProject)
        duplicatedProject
    }
}
