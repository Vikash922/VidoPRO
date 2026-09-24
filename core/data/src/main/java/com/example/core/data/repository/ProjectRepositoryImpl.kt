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
    private val transitionDao = database.transitionDao()

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
        val projectWithTracks = projectDao.getProjectWithTracks(projectId) ?: return@withContext null
        val transitions = transitionDao.getByProjectId(projectId).map { it.toDomain() }
        val tracks = projectWithTracks.tracks.map { trackWithClips ->
            val detailedClips = trackWithClips.clips.map { clipEntity ->
                val clipWithDetails = clipDao.getClipWithDetails(clipEntity.id)
                clipWithDetails?.toDomain() ?: clipEntity.toDomain()
            }
            val trackTransitions = transitions.filter { it.trackId == trackWithClips.track.id }
            trackWithClips.track.toDomain(clips = detailedClips, transitions = trackTransitions)
        }
        projectWithTracks.project.toDomain(tracks = tracks)
    }

    override fun observeProjects(): Flow<List<Project>> {
        return projectDao.observeProjects()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override fun observeProjectById(projectId: String): Flow<Project?> {
        return projectDao.observeProjectWithTracks(projectId)
            .map { projectWithTracks ->
                if (projectWithTracks == null) null
                else {
                    val transitions = transitionDao.getByProjectId(projectId).map { it.toDomain() }
                    val tracks = projectWithTracks.tracks.map { trackWithClips ->
                        val detailedClips = trackWithClips.clips.map { clipEntity ->
                            val clipWithDetails = clipDao.getClipWithDetails(clipEntity.id)
                            clipWithDetails?.toDomain() ?: clipEntity.toDomain()
                        }
                        val trackTransitions = transitions.filter { it.trackId == trackWithClips.track.id }
                        trackWithClips.track.toDomain(clips = detailedClips, transitions = trackTransitions)
                    }
                    projectWithTracks.project.toDomain(tracks = tracks)
                }
            }
            .flowOn(dispatchers.io)
    }

    override suspend fun updateProject(project: Project) = withContext(dispatchers.io) {
        val updatedProject = project.copy(updatedAt = System.currentTimeMillis())

        database.withTransaction {
            projectDao.insert(updatedProject.toEntity())

            // Clean existing hierarchy for project
            trackDao.deleteByProjectId(updatedProject.id)
            transitionDao.deleteByProjectId(updatedProject.id)

            // Insert new hierarchy
            for (track in updatedProject.tracks) {
                trackDao.insert(track.toEntity())

                for (transition in track.transitions) {
                    transitionDao.insert(transition.toEntity())
                }

                for (clip in track.clips) {
                    clipDao.insert(clip.toEntity())

                    transformDao.insert(clip.transform.toEntity(clip.id))

                    val allClipEffects = clip.effects.toMutableList()
                    clip.mask?.let { m ->
                        if (allClipEffects.none { it.type == com.example.core.model.EffectType.MASK }) {
                            allClipEffects.add(m.toEffect(clip.id))
                        }
                    }
                    if (clip.blendMode != com.example.core.model.BlendMode.NORMAL) {
                        if (allClipEffects.none { it.type == com.example.core.model.EffectType.BLEND_MODE }) {
                            allClipEffects.add(
                                com.example.core.model.Effect(
                                    id = java.util.UUID.randomUUID().toString(),
                                    clipId = clip.id,
                                    type = com.example.core.model.EffectType.BLEND_MODE,
                                    order = 999,
                                    isEnabled = true,
                                    parameters = mapOf("mode" to clip.blendMode.ordinal.toFloat())
                                )
                            )
                        }
                    }
                    if (allClipEffects.isNotEmpty()) {
                        effectDao.insertAll(allClipEffects.map { it.toEntity() })
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

        val clipIdMap = mutableMapOf<String, String>()
        val trackIdMap = mutableMapOf<String, String>()

        val duplicatedTracks = original.tracks.map { track ->
            val newTrackId = UUID.randomUUID().toString()
            trackIdMap[track.id] = newTrackId
            val duplicatedClips = track.clips.map { clip ->
                val newClipId = UUID.randomUUID().toString()
                clipIdMap[clip.id] = newClipId
                clip.copy(
                    id = newClipId,
                    trackId = newTrackId,
                    effects = clip.effects.map { it.copy(id = UUID.randomUUID().toString(), clipId = newClipId) },
                    keyframes = clip.keyframes.map { it.copy(id = UUID.randomUUID().toString(), clipId = newClipId) },
                    textData = clip.textData?.copy(clipId = newClipId)
                )
            }
            val duplicatedTransitions = track.transitions.map { transition ->
                transition.copy(
                    id = UUID.randomUUID().toString(),
                    projectId = newProjectId,
                    trackId = newTrackId,
                    firstClipId = clipIdMap[transition.firstClipId] ?: transition.firstClipId,
                    secondClipId = clipIdMap[transition.secondClipId] ?: transition.secondClipId
                )
            }
            track.copy(
                id = newTrackId,
                projectId = newProjectId,
                clips = duplicatedClips,
                transitions = duplicatedTransitions
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
