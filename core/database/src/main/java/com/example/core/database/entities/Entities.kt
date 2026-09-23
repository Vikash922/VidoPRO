package com.example.core.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "projects",
    indices = [Index(value = ["updatedAt"])]
)
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val fps: Int,
    val durationMs: Long,
    val aspectRatio: String,
    val createdAt: Long,
    val updatedAt: Long,
    val thumbnailPath: String?,
    val stateVersion: Int = 1
)

@Entity(
    tableName = "tracks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["projectId", "trackOrder"])
    ]
)
data class TrackEntity(
    @PrimaryKey
    val id: String,
    val projectId: String,
    val type: String,
    val trackOrder: Int,
    val isVisible: Boolean,
    val isLocked: Boolean
)

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey
    val id: String,
    val uri: String,
    val mimeType: String?,
    val mediaType: String,
    val durationMs: Long?,
    val width: Int?,
    val height: Int?,
    val sizeBytes: Long?,
    val displayName: String?,
    val thumbnailPath: String?,
    val createdAt: Long
)

@Entity(
    tableName = "clips",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["assetId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["trackId"]),
        Index(value = ["assetId"]),
        Index(value = ["trackId", "startTimeMs"])
    ]
)
data class ClipEntity(
    @PrimaryKey
    val id: String,
    val trackId: String,
    val assetId: String?,
    val type: String,
    val startTimeMs: Long,
    val durationMs: Long,
    val inPointMs: Long,
    val outPointMs: Long,
    val speed: Float,
    val volume: Float?,
    val isVisible: Boolean,
    val zIndex: Int,
    val groupId: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)


@Entity(
    tableName = "transforms",
    foreignKeys = [
        ForeignKey(
            entity = ClipEntity::class,
            parentColumns = ["id"],
            childColumns = ["clipId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TransformEntity(
    @PrimaryKey
    val clipId: String,
    val x: Float,
    val y: Float,
    val scaleX: Float,
    val scaleY: Float,
    val rotation: Float,
    val opacity: Float,
    val anchorX: Float,
    val anchorY: Float
)

@Entity(
    tableName = "effects",
    foreignKeys = [
        ForeignKey(
            entity = ClipEntity::class,
            parentColumns = ["id"],
            childColumns = ["clipId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["clipId"]),
        Index(value = ["clipId", "effectOrder"])
    ]
)
data class EffectEntity(
    @PrimaryKey
    val id: String,
    val clipId: String,
    val type: String,
    val effectOrder: Int,
    val isEnabled: Boolean,
    val parametersJson: String
)

@Entity(
    tableName = "keyframes",
    foreignKeys = [
        ForeignKey(
            entity = ClipEntity::class,
            parentColumns = ["id"],
            childColumns = ["clipId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["clipId"]),
        Index(value = ["clipId", "property", "timeMs"], unique = true)
    ]
)
data class KeyframeEntity(
    @PrimaryKey
    val id: String,
    val clipId: String,
    val property: String,
    val timeMs: Long,
    val value: Float,
    val interpolation: String,
    val bezierX1: Float?,
    val bezierY1: Float?,
    val bezierX2: Float?,
    val bezierY2: Float?
)

@Entity(
    tableName = "text_clips",
    foreignKeys = [
        ForeignKey(
            entity = ClipEntity::class,
            parentColumns = ["id"],
            childColumns = ["clipId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TextClipEntity(
    @PrimaryKey
    val clipId: String,
    val text: String,
    val fontFamily: String,
    val fontSize: Float,
    val textColor: String,
    val backgroundColor: String?,
    val alignment: String,
    val letterSpacing: Float,
    val lineHeight: Float
)

@Entity(
    tableName = "transitions",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["trackId"]),
        Index(value = ["firstClipId"]),
        Index(value = ["secondClipId"])
    ]
)
data class TransitionEntity(
    @PrimaryKey
    val id: String,
    val projectId: String,
    val trackId: String,
    val firstClipId: String,
    val secondClipId: String,
    val type: String,
    val durationMs: Long,
    val parametersJson: String?
)
