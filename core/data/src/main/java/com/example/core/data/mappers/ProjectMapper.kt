package com.example.core.data.mappers

import com.example.core.database.entities.ClipEntity
import com.example.core.database.entities.EffectEntity
import com.example.core.database.entities.KeyframeEntity
import com.example.core.database.entities.ProjectEntity
import com.example.core.database.entities.TextClipEntity
import com.example.core.database.entities.TrackEntity
import com.example.core.database.entities.TransformEntity
import com.example.core.database.entities.TransitionEntity
import com.example.core.database.relations.ClipWithDetails
import com.example.core.database.relations.ProjectWithTracks
import com.example.core.database.relations.TrackWithClips
import com.example.core.model.AspectRatio
import com.example.core.model.Clip
import com.example.core.model.ClipType
import com.example.core.model.Effect
import com.example.core.model.EffectType
import com.example.core.model.InterpolationType
import com.example.core.model.Keyframe
import com.example.core.model.Project
import com.example.core.model.TextClipData
import com.example.core.model.Track
import com.example.core.model.TrackType
import com.example.core.model.Transform
import com.example.core.model.Transition
import com.example.core.model.TransitionType
import org.json.JSONObject

// MARK: - Project Mappers

fun ProjectEntity.toDomain(tracks: List<Track> = emptyList()): Project {
    return Project(
        id = id,
        name = name,
        width = width,
        height = height,
        fps = fps,
        durationMs = durationMs,
        aspectRatio = runCatching { AspectRatio.valueOf(aspectRatio) }.getOrDefault(AspectRatio.RATIO_9_16),
        createdAt = createdAt,
        updatedAt = updatedAt,
        thumbnailPath = thumbnailPath,
        tracks = tracks
    )
}

fun ProjectWithTracks.toDomain(): Project {
    return project.toDomain(
        tracks = tracks.map { it.toDomain() }
    )
}

fun Project.toEntity(): ProjectEntity {
    return ProjectEntity(
        id = id,
        name = name,
        width = width,
        height = height,
        fps = fps,
        durationMs = durationMs,
        aspectRatio = aspectRatio.name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        thumbnailPath = thumbnailPath,
        stateVersion = 1
    )
}

// MARK: - Track Mappers

fun TrackEntity.toDomain(clips: List<Clip> = emptyList(), transitions: List<Transition> = emptyList()): Track {
    return Track(
        id = id,
        projectId = projectId,
        type = runCatching { TrackType.valueOf(type) }.getOrDefault(TrackType.VIDEO),
        order = trackOrder,
        isVisible = isVisible,
        isLocked = isLocked,
        clips = clips,
        transitions = transitions
    )
}

fun TrackWithClips.toDomain(): Track {
    return track.toDomain(
        clips = clips.map { it.toDomain() }
    )
}

fun Track.toEntity(): TrackEntity {
    return TrackEntity(
        id = id,
        projectId = projectId,
        type = type.name,
        trackOrder = order,
        isVisible = isVisible,
        isLocked = isLocked
    )
}
