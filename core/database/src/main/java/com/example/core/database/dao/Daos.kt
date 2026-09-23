package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.core.database.entities.AssetEntity
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
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity)

    @Update
    suspend fun update(project: ProjectEntity)

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getById(projectId: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun observeById(projectId: String): Flow<ProjectEntity?>

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteById(projectId: String)

    @Query("UPDATE projects SET updatedAt = :updatedAt WHERE id = :projectId")
    suspend fun touch(projectId: String, updatedAt: Long)

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectWithTracks(projectId: String): ProjectWithTracks?

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun observeProjectWithTracks(projectId: String): Flow<ProjectWithTracks?>
}

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(track: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<TrackEntity>)

    @Query("SELECT * FROM tracks WHERE projectId = :projectId ORDER BY trackOrder ASC")
    suspend fun getByProjectId(projectId: String): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE projectId = :projectId ORDER BY trackOrder ASC")
    fun observeByProjectId(projectId: String): Flow<List<TrackEntity>>

    @Transaction
    @Query("SELECT * FROM tracks WHERE projectId = :projectId ORDER BY trackOrder ASC")
    suspend fun getTracksWithClips(projectId: String): List<TrackWithClips>

    @Query("DELETE FROM tracks WHERE projectId = :projectId")
    suspend fun deleteByProjectId(projectId: String)

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteById(trackId: String)
}

@Dao
interface ClipDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(clip: ClipEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(clips: List<ClipEntity>)

    @Update
    suspend fun update(clip: ClipEntity)

    @Query("SELECT * FROM clips WHERE trackId = :trackId ORDER BY startTimeMs ASC")
    suspend fun getByTrackId(trackId: String): List<ClipEntity>

    @Query("SELECT * FROM clips WHERE id = :clipId")
    suspend fun getById(clipId: String): ClipEntity?

    @Transaction
    @Query("SELECT * FROM clips WHERE id = :clipId")
    suspend fun getClipWithDetails(clipId: String): ClipWithDetails?

    @Query("SELECT * FROM clips WHERE groupId = :groupId ORDER BY startTimeMs ASC")
    suspend fun getByGroupId(groupId: String): List<ClipEntity>


    @Query("DELETE FROM clips WHERE id = :clipId")
    suspend fun deleteById(clipId: String)

    @Query("DELETE FROM clips WHERE trackId = :trackId")
    suspend fun deleteByTrackId(trackId: String)
}

@Dao
interface AssetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(asset: AssetEntity)

    @Query("SELECT * FROM assets WHERE id = :assetId")
    suspend fun getById(assetId: String): AssetEntity?

    @Query("SELECT * FROM assets ORDER BY createdAt DESC")
    fun observeAssets(): Flow<List<AssetEntity>>

    @Query("DELETE FROM assets WHERE id = :assetId")
    suspend fun deleteById(assetId: String)
}

@Dao
interface TransformDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transform: TransformEntity)

    @Query("SELECT * FROM transforms WHERE clipId = :clipId")
    suspend fun getByClipId(clipId: String): TransformEntity?

    @Query("DELETE FROM transforms WHERE clipId = :clipId")
    suspend fun deleteByClipId(clipId: String)
}

@Dao
interface EffectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(effect: EffectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(effects: List<EffectEntity>)

    @Query("SELECT * FROM effects WHERE clipId = :clipId ORDER BY effectOrder ASC")
    suspend fun getByClipId(clipId: String): List<EffectEntity>

    @Query("DELETE FROM effects WHERE clipId = :clipId")
    suspend fun deleteByClipId(clipId: String)

    @Query("DELETE FROM effects WHERE id = :effectId")
    suspend fun deleteById(effectId: String)
}

@Dao
interface KeyframeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(keyframe: KeyframeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(keyframes: List<KeyframeEntity>)

    @Query("SELECT * FROM keyframes WHERE clipId = :clipId ORDER BY property ASC, timeMs ASC")
    suspend fun getByClipId(clipId: String): List<KeyframeEntity>

    @Query("SELECT * FROM keyframes WHERE clipId = :clipId AND property = :property ORDER BY timeMs ASC")
    suspend fun getByProperty(clipId: String, property: String): List<KeyframeEntity>

    @Query("DELETE FROM keyframes WHERE clipId = :clipId")
    suspend fun deleteByClipId(clipId: String)

    @Query("DELETE FROM keyframes WHERE id = :keyframeId")
    suspend fun deleteById(keyframeId: String)
}

@Dao
interface TextClipDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(textClip: TextClipEntity)

    @Query("SELECT * FROM text_clips WHERE clipId = :clipId")
    suspend fun getByClipId(clipId: String): TextClipEntity?

    @Query("DELETE FROM text_clips WHERE clipId = :clipId")
    suspend fun deleteByClipId(clipId: String)
}

@Dao
interface TransitionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transition: TransitionEntity)

    @Query("SELECT * FROM transitions WHERE projectId = :projectId")
    suspend fun getByProjectId(projectId: String): List<TransitionEntity>

    @Query("SELECT * FROM transitions WHERE trackId = :trackId")
    suspend fun getByTrackId(trackId: String): List<TransitionEntity>

    @Query("DELETE FROM transitions WHERE projectId = :projectId")
    suspend fun deleteByProjectId(projectId: String)
}
