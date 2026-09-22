package com.example.core.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.example.core.database.entities.ClipEntity
import com.example.core.database.entities.EffectEntity
import com.example.core.database.entities.KeyframeEntity
import com.example.core.database.entities.ProjectEntity
import com.example.core.database.entities.TextClipEntity
import com.example.core.database.entities.TrackEntity
import com.example.core.database.entities.TransformEntity

data class TrackWithClips(
    @Embedded
    val track: TrackEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "trackId"
    )
    val clips: List<ClipEntity>
)

data class ProjectWithTracks(
    @Embedded
    val project: ProjectEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "projectId",
        entity = TrackEntity::class
    )
    val tracks: List<TrackWithClips>
)

data class ClipWithDetails(
    @Embedded
    val clip: ClipEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "clipId"
    )
    val transform: TransformEntity?,

    @Relation(
        parentColumn = "id",
        entityColumn = "clipId"
    )
    val effects: List<EffectEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "clipId"
    )
    val keyframes: List<KeyframeEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "clipId"
    )
    val textClip: TextClipEntity?
)
