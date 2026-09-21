---
title: Database Schema
version: 1.0
related: PRD.md, ARCHITECTURE.md
database: Room
platform: Android
status: Initial Schema
---

# Database Schema

## 1. Purpose

This document defines the Room database schema for the Android video editing app.

The database stores:

- Projects
- Tracks
- Clips
- Assets
- Effects
- Keyframes
- Text clip data
- Transitions later

The database must support:

- Project list
- Editor autosave
- Timeline restore
- Undo/redo persistence later
- Asset metadata
- Effects and keyframes for future advanced editing

---

## 2. Database Design Rules

1. Use Room as the persistence layer.
2. Keep domain models separate from Room entities.
3. Use mappers between entities and domain models.
4. Use stable unique IDs.
5. Store time values in milliseconds.
6. Use foreign keys where relationships are strong.
7. Use cascade delete carefully.
8. Use indexes for frequently queried columns.
9. Avoid storing large binary data directly in database.
10. Store thumbnails as file paths or cache keys, not blobs.

---

## 3. Entity Relationship Overview

```text
Project
  ├── Track
  │     └── Clip
  │           ├── Effect
  │           ├── Keyframe
  │           └── TextClipData
  └── Asset
```

Detailed relationship:

```text
ProjectEntity 1 -> Many TrackEntity
TrackEntity 1 -> Many ClipEntity
ClipEntity 1 -> Many EffectEntity
ClipEntity 1 -> Many KeyframeEntity
ClipEntity 1 -> Optional TextClipEntity
AssetEntity 1 -> Many ClipEntity
```

---

## 4. Tables Overview

```text
projects
tracks
clips
assets
effects
keyframes
text_clips
transitions
```

Priority:

```text
P0: projects, tracks, clips, assets
P1: effects, text_clips
P2: keyframes, transitions
```

---

## 5. ProjectEntity

Table name:

```text
projects
```

Purpose:

Stores top-level project metadata.

Fields:

| Field | Type | Description |
|---|---:|---|
| id | TEXT | Primary key, UUID |
| name | TEXT | Project name |
| width | INTEGER | Output width |
| height | INTEGER | Output height |
| fps | INTEGER | Frames per second |
| durationMs | INTEGER | Cached project duration |
| aspectRatio | TEXT | 9:16, 16:9, 1:1, 4:5 |
| createdAt | INTEGER | Creation timestamp |
| updatedAt | INTEGER | Last updated timestamp |
| thumbnailPath | TEXT | Cached project thumbnail |
| stateVersion | INTEGER | Schema/state version |

Example:

```kotlin
@Entity(
    tableName = "projects"
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
    val stateVersion: Int
)
```

Indexes:

```text
updatedAt DESC
```

---

## 6. TrackEntity

Table name:

```text
tracks
```

Purpose:

Stores tracks inside a project.

Track types:

```text
VIDEO
OVERLAY
TEXT
AUDIO
```

Fields:

| Field | Type | Description |
|---|---:|---|
| id | TEXT | Primary key |
| projectId | TEXT | Foreign key to projects |
| type | TEXT | Track type |
| trackOrder | INTEGER | Track order |
| isVisible | INTEGER | Track visibility |
| isLocked | INTEGER | Track lock state |

Example:

```kotlin
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
```

Rules:

- Tracks should be ordered by `trackOrder`.
- Deleting project should delete its tracks.

---

## 7. ClipEntity

Table name:

```text
clips
```

Purpose:

Stores clips placed on tracks.

Clip types:

```text
VIDEO
IMAGE
AUDIO
TEXT
COLOR
SHAPE
```

Fields:

| Field | Type | Description |
|---|---:|---|
| id | TEXT | Primary key |
| trackId | TEXT | Foreign key to tracks |
| assetId | TEXT | Foreign key to assets, nullable |
| type | TEXT | Clip type |
| startTimeMs | INTEGER | Position on timeline |
| durationMs | INTEGER | Duration on timeline |
| inPointMs | INTEGER | Source trim start |
| outPointMs | INTEGER | Source trim end |
| speed | REAL | Playback speed |
| volume | REAL | Clip volume, nullable |
| isVisible | INTEGER | Visibility |
| zIndex | INTEGER | Layer order inside track |
| createdAt | INTEGER | Creation timestamp |
| updatedAt | INTEGER | Updated timestamp |

Example:

```kotlin
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
    val createdAt: Long,
    val updatedAt: Long
)
```

Rules:

- `startTimeMs` is timeline position.
- `inPointMs` and `outPointMs` are source media trim points.
- `durationMs` should remain consistent with trim and speed.
- Audio clips may have volume.
- Text clips may have no assetId.

---

## 8. Transform Data

Transform can be stored in one of two ways.

### Option A: Separate transform table

Recommended if transform values become complex.

Table name:

```text
transforms
```

Fields:

| Field | Type | Description |
|---|---:|---|
| clipId | TEXT | Primary key, foreign key |
| x | REAL | Position X |
| y | REAL | Position Y |
| scaleX | REAL | Scale X |
| scaleY | REAL | Scale Y |
| rotation | REAL | Rotation degrees |
| opacity | REAL | Opacity 0 to 1 |
| anchorX | REAL | Anchor X |
| anchorY | REAL | Anchor Y |

Example:

```kotlin
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
```

### Option B: Store transform JSON in clips table

Simpler for MVP.

Fields:

```text
transformJson TEXT
```

Recommendation:

- Use separate table if keyframes and transforms become advanced.
- For MVP, either option is acceptable.
- If simplicity is priority, use JSON column.
- If scalability is priority, use separate table.

---

## 9. AssetEntity

Table name:

```text
assets
```

Purpose:

Stores imported media references.

Fields:

| Field | Type | Description |
|---|---:|---|
| id | TEXT | Primary key |
| uri | TEXT | Media URI |
| mimeType | TEXT | MIME type |
| mediaType | TEXT | VIDEO, IMAGE, AUDIO |
| durationMs | INTEGER | Duration if available |
| width | INTEGER | Width if available |
| height | INTEGER | Height if available |
| sizeBytes | INTEGER | File size |
| displayName | TEXT | File display name |
| thumbnailPath | TEXT | Cached thumbnail path |
| createdAt | INTEGER | Import timestamp |

Example:

```kotlin
@Entity(
    tableName = "assets"
)
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
```

Rules:

- Store URI string, not raw file path if using MediaStore/Photo Picker.
- Persist URI permission if needed.
- Thumbnail path points to app cache.
- Do not store media binary in database.

---

## 10. EffectEntity

Table name:

```text
effects
```

Purpose:

Stores effects applied to clips.

Fields:

| Field | Type | Description |
|---|---:|---|
| id | TEXT | Primary key |
| clipId | TEXT | Foreign key to clips |
| type | TEXT | Effect type |
| effectOrder | INTEGER | Effect stack order |
| isEnabled | INTEGER | Enabled/disabled |
| parametersJson | TEXT | Effect parameters JSON |

Example:

```kotlin
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
```

Example parameters JSON:

```json
{
  "brightness": 0.2,
  "contrast": 1.1,
  "saturation": 1.2
}
```

Rules:

- Effect order matters.
- Parameters should be JSON for flexibility.
- Keep parameter keys stable.

---

## 11. KeyframeEntity

Table name:

```text
keyframes
```

Purpose:

Stores keyframes for animated properties.

Fields:

| Field | Type | Description |
|---|---:|---|
| id | TEXT | Primary key |
| clipId | TEXT | Foreign key to clips |
| property | TEXT | Animated property |
| timeMs | INTEGER | Keyframe time |
| value | REAL | Keyframe value |
| interpolation | TEXT | Interpolation type |
| bezierX1 | REAL | Optional bezier X1 |
| bezierY1 | REAL | Optional bezier Y1 |
| bezierX2 | REAL | Optional bezier X2 |
| bezierY2 | REAL | Optional bezier Y2 |

Example:

```kotlin
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
```

Supported property examples:

```text
positionX
positionY
scaleX
scaleY
rotation
opacity
volume
effect:brightness
effect:blur
```

Supported interpolation:

```text
LINEAR
EASE_IN
EASE_OUT
EASE_IN_OUT
BEZIER
HOLD
```

---

## 12. TextClipEntity

Table name:

```text
text_clips
```

Purpose:

Stores text-specific data for text clips.

Fields:

| Field | Type | Description |
|---|---:|---|
| clipId | TEXT | Primary key, foreign key |
| text | TEXT | Text content |
| fontFamily | TEXT | Font family |
| fontSize | REAL | Font size |
| textColor | TEXT | Text color hex |
| backgroundColor | TEXT | Background color hex |
| alignment | TEXT | Alignment |
| letterSpacing | REAL | Letter spacing |
| lineHeight | REAL | Line height |

Example:

```kotlin
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
```

Rules:

- Text clips may exist without asset.
- Text animation can later use keyframes.
- Store colors as hex strings.



## 13. TransitionEntity

Table name:

```text
transitions
```

Purpose:

Stores transitions between clips.

Fields:

| Field | Type | Description |
|---|---:|---|
| id | TEXT | Primary key |
| projectId | TEXT | Foreign key to projects |
| trackId | TEXT | Foreign key to tracks |
| firstClipId | TEXT | First clip |
| secondClipId | TEXT | Second clip |
| type | TEXT | Transition type |
| durationMs | INTEGER | Transition duration |
| parametersJson | TEXT | Extra parameters |

Example:

```kotlin
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
```

Supported transition types:

```text
NONE
FADE
SLIDE
ZOOM
WIPE
BLUR
SPIN
GLITCH
```

Rules:

- Transition duration should not exceed clip overlap limits.
- Transition should be optional.
- Parameters JSON can store direction, blur amount, easing, etc.

---

## 14. Enums and Constants

Use enums in domain models and store as strings in database.

```kotlin
enum class TrackType {
    VIDEO,
    OVERLAY,
    TEXT,
    AUDIO
}

enum class ClipType {
    VIDEO,
    IMAGE,
    AUDIO,
    TEXT,
    COLOR,
    SHAPE
}

enum class MediaType {
    VIDEO,
    IMAGE,
    AUDIO
}

enum class AspectRatio {
    RATIO_9_16,
    RATIO_16_9,
    RATIO_1_1,
    RATIO_4_5
}

enum class EffectType {
    BRIGHTNESS,
    CONTRAST,
    SATURATION,
    EXPOSURE,
    BLUR,
    VIGNETTE,
    SHARPEN,
    GLITCH,
    RGB_SPLIT,
    PIXELATE
}

enum class InterpolationType {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    BEZIER,
    HOLD
}

enum class TransitionType {
    NONE,
    FADE,
    SLIDE,
    ZOOM,
    WIPE,
    BLUR,
    SPIN,
    GLITCH
}
```

Rules:

- Store enum names as TEXT.
- Avoid storing enum ordinal values.
- Keep enum names stable for backward compatibility.

---

## 15. Type Converters

Room type converters for enums.

```kotlin
class Converters {

    @TypeConverter
    fun fromTrackType(value: TrackType): String = value.name

    @TypeConverter
    fun toTrackType(value: String): TrackType = TrackType.valueOf(value)

    @TypeConverter
    fun fromClipType(value: ClipType): String = value.name

    @TypeConverter
    fun toClipType(value: String): ClipType = ClipType.valueOf(value)

    @TypeConverter
    fun fromMediaType(value: MediaType): String = value.name

    @TypeConverter
    fun toMediaType(value: String): MediaType = MediaType.valueOf(value)

    @TypeConverter
    fun fromAspectRatio(value: AspectRatio): String = value.name

    @TypeConverter
    fun toAspectRatio(value: String): AspectRatio = AspectRatio.valueOf(value)

    @TypeConverter
    fun fromEffectType(value: EffectType): String = value.name

    @TypeConverter
    fun toEffectType(value: String): EffectType = EffectType.valueOf(value)

    @TypeConverter
    fun fromInterpolationType(value: InterpolationType): String = value.name

    @TypeConverter
    fun toInterpolationType(value: String): InterpolationType = InterpolationType.valueOf(value)

    @TypeConverter
    fun fromTransitionType(value: TransitionType): String = value.name

    @TypeConverter
    fun toTransitionType(value: String): TransitionType = TransitionType.valueOf(value)
}
```

If entities store enums directly, Room can use these converters.

If entities store strings, converters may not be required for entity fields, but they are still useful for domain mapping.

---

## 16. Room Database

```kotlin
@Database(
    entities = [
        ProjectEntity::class,
        TrackEntity::class,
        ClipEntity::class,
        AssetEntity::class,
        TransformEntity::class,
        EffectEntity::class,
        KeyframeEntity::class,
        TextClipEntity::class,
        TransitionEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun trackDao(): TrackDao
    abstract fun clipDao(): ClipDao
    abstract fun assetDao(): AssetDao
    abstract fun transformDao(): TransformDao
    abstract fun effectDao(): EffectDao
    abstract fun keyframeDao(): KeyframeDao
    abstract fun textClipDao(): TextClipDao
    abstract fun transitionDao(): TransitionDao

    companion object {
        const val DATABASE_NAME = "video_editor.db"
    }
}
```

Hilt provider:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigrationOnDowngrade()
        .build()
    }
}
```

Rules:

- Enable `exportSchema = true`.
- Keep schema JSON in version control.
- Add migrations when schema changes.

---

## 17. ProjectDao

```kotlin
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
}
```

---

## 18. TrackDao

```kotlin
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

    @Query("DELETE FROM tracks WHERE projectId = :projectId")
    suspend fun deleteByProjectId(projectId: String)

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteById(trackId: String)
}
```

---

## 19. ClipDao

```kotlin
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

    @Query("DELETE FROM clips WHERE id = :clipId")
    suspend fun deleteById(clipId: String)

    @Query("DELETE FROM clips WHERE trackId = :trackId")
    suspend fun deleteByTrackId(trackId: String)
}
```

Useful future query:

```kotlin
@Query(
    """
    SELECT * FROM clips
    WHERE trackId = :trackId
    AND startTimeMs <= :timeMs
    AND startTimeMs + durationMs > :timeMs
    LIMIT 1
    """
)
suspend fun getActiveClipAtTime(trackId: String, timeMs: Long): ClipEntity?
```

---

## 20. AssetDao

```kotlin
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
```

Rules:

- Do not delete asset if clips still reference it unless handling cleanup carefully.
- Use asset reference count or background cleanup job later.

---

## 21. TransformDao

```kotlin
@Dao
interface TransformDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transform: TransformEntity)

    @Query("SELECT * FROM transforms WHERE clipId = :clipId")
    suspend fun getByClipId(clipId: String): TransformEntity?

    @Query("DELETE FROM transforms WHERE clipId = :clipId")
    suspend fun deleteByClipId(clipId: String)
}
```

---

## 22. EffectDao

```kotlin
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
```

---

## 23. KeyframeDao

```kotlin
@Dao
interface KeyframeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(keyframe: KeyframeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(keyframes: List<KeyframeEntity>)

    @Query(
        """
        SELECT * FROM keyframes
        WHERE clipId = :clipId
        ORDER BY property ASC, timeMs ASC
        """
    )
    suspend fun getByClipId(clipId: String): List<KeyframeEntity>

    @Query(
        """
        SELECT * FROM keyframes
        WHERE clipId = :clipId AND property = :property
        ORDER BY timeMs ASC
        """
    )
    suspend fun getByProperty(clipId: String, property: String): List<KeyframeEntity>

    @Query("DELETE FROM keyframes WHERE clipId = :clipId")
    suspend fun deleteByClipId(clipId: String)

    @Query("DELETE FROM keyframes WHERE id = :keyframeId")
    suspend fun deleteById(keyframeId: String)
}
```

---

## 24. TextClipDao

```kotlin
@Dao
interface TextClipDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(textClip: TextClipEntity)

    @Query("SELECT * FROM text_clips WHERE clipId = :clipId")
    suspend fun getByClipId(clipId: String): TextClipEntity?

    @Query("DELETE FROM text_clips WHERE clipId = :clipId")
    suspend fun deleteByClipId(clipId: String)
}
```

---

## 25. TransitionDao

```kotlin
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
```

---

## 26. Room Relations

Use relation classes to load complete project data.

### TrackWithClips

```kotlin
data class TrackWithClips(
    @Embedded
    val track: TrackEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "trackId"
    )
    val clips: List<ClipEntity>
)
```

### ProjectWithTracks

```kotlin
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
```

### ClipWithDetails

```kotlin
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
```

Rules:

- Use relations for loading full editor state.
- Avoid loading all keyframes for home screen.
- Load only what each screen needs.

---

## 27. Project Load Query

Add this to `ProjectDao`:

```kotlin
@Transaction
@Query("SELECT * FROM projects WHERE id = :projectId")
suspend fun getProjectWithTracks(projectId: String): ProjectWithTracks?
```

Add this to `ClipDao`:

```kotlin
@Transaction
@Query("SELECT * FROM clips WHERE id = :clipId")
suspend fun getClipWithDetails(clipId: String): ClipWithDetails?
```

Add this to `TrackDao`:

```kotlin
@Transaction
@Query("SELECT * FROM tracks WHERE projectId = :projectId ORDER BY trackOrder ASC")
suspend fun getTracksWithClips(projectId: String): List<TrackWithClips>
```

---

## 28. Domain Models vs Entities

Keep domain models separate from Room entities.

Example:

```text
ProjectEntity -> Project
TrackEntity -> Track
ClipEntity -> Clip
AssetEntity -> Asset
EffectEntity -> Effect
KeyframeEntity -> Keyframe
TextClipEntity -> TextClipData
TransformEntity -> Transform
```

Why:

- UI/domain logic stays clean.
- Database schema can evolve independently.
- Entities can include DB-only fields.
- Domain models can include computed fields.

---

## 29. Mapper Examples

```kotlin
fun ProjectEntity.toDomain(tracks: List<Track>): Project {
    return Project(
        id = id,
        name = name,
        width = width,
        height = height,
        fps = fps,
        durationMs = durationMs,
        createdAt = createdAt,
        updatedAt = updatedAt,
        tracks = tracks
    )
}

fun TrackEntity.toDomain(clips: List<Clip>): Track {
    return Track(
        id = id,
        type = TrackType.valueOf(type),
        order = trackOrder,
        clips = clips
    )
}

fun ClipEntity.toDomain(
    transform: Transform?,
    effects: List<Effect>,
    keyframes: List<Keyframe>,
    textData: TextClipData?
): Clip {
    return Clip(
        id = id,
        type = ClipType.valueOf(type),
        assetId = assetId,
        startTimeMs = startTimeMs,
        durationMs = durationMs,
        inPointMs = inPointMs,
        outPointMs = outPointMs,
        speed = speed,
        volume = volume,
        transform = transform ?: Transform.DEFAULT,
        effects = effects,
        keyframes = keyframes,
        textData = textData
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
```

Rules:

- Put mappers in `core:data`.
- Do not use entities directly in Compose UI.
- Keep mapper functions pure.

---

## 30. Repository Interface

```kotlin
interface ProjectRepository {
    suspend fun createProject(name: String, aspectRatio: AspectRatio): Project
    suspend fun getProjectById(projectId: String): Project?
    fun observeProjects(): Flow<List<Project>>
    suspend fun updateProject(project: Project)
    suspend fun deleteProject(projectId: String)
    suspend fun duplicateProject(projectId: String): Project
}
```

Asset repository:

```kotlin
interface AssetRepository {
    suspend fun importAsset(uri: Uri): Asset
    suspend fun getAssetById(assetId: String): Asset?
    fun observeAssets(): Flow<List<Asset>>
}
```

---

## 31. Save Project Strategy

For MVP, use full project save:

```text
Begin transaction
    Delete old tracks/clips/effects/keyframes
    Insert project
    Insert tracks
    Insert clips
    Insert transforms
    Insert effects
    Insert keyframes
    Insert text clips
End transaction
```

Why:

- Simple and reliable.
- Works well for small/medium projects.
- Easy to maintain.

Later optimization:

- Use diff-based upsert.
- Save only changed entities.
- Debounce saves.
- Save lightweight metadata frequently and heavy data less frequently.

Example:

```kotlin
suspend fun saveProject(project: Project) {
    database.withTransaction {
        trackDao.deleteByProjectId(project.id)
        projectDao.insert(project.toEntity())
        trackDao.insertAll(project.tracks.map { it.toEntity(project.id) })

        val clips = project.tracks.flatMap { it.clips }
        clipDao.insertAll(clips.map { it.toEntity() })
    }
}
```

Note:

Actual mapper functions may need track/project IDs passed explicitly.

---

## 32. Autosave Strategy

Autosave should not write too frequently.

Rules:

- Debounce saves by 1000ms to 2000ms.
- Save immediately on:
  - back navigation
  - app background
  - critical action
  - export start
- Do not save every frame during playhead scrubbing.
- Save playhead position optionally in project or preferences.

Example:

```kotlin
private var saveJob: Job? = null

fun scheduleAutosave(project: Project) {
    saveJob?.cancel()
    saveJob = viewModelScope.launch {
        delay(1500)
        repository.updateProject(project)
    }
}
```

---

## 33. Migration Strategy

Room migrations are required when schema changes.

Rules:

- Increase database version.
- Add Migration object.
- Test migration paths.
- Keep `exportSchema = true`.
- Keep schema JSON files in version control.

Example version change:

```text
Version 1:
Initial schema

Version 2:
Add transition table or new column
```

Example migration:

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS transitions (
                id TEXT NOT NULL PRIMARY KEY,
                projectId TEXT NOT NULL,
                trackId TEXT NOT NULL,
                firstClipId TEXT NOT NULL,
                secondClipId TEXT NOT NULL,
                type TEXT NOT NULL,
                durationMs INTEGER NOT NULL,
                parametersJson TEXT,
                FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE,
                FOREIGN KEY(trackId) REFERENCES tracks(id) ON DELETE CASCADE
            )
            """
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS index_transitions_projectId ON transitions(projectId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transitions_trackId ON transitions(trackId)")
    }
}
```

Provide migration to Room:

```kotlin
Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
    .addMigrations(MIGRATION_1_2)
    .build()
```

---

## 34. Project JSON Export Format

This is useful for backup, import/export, or AI understanding.

Example:

```json
{
  "project": {
    "id": "proj_123",
    "name": "My Edit",
    "width": 1080,
    "height": 1920,
    "fps": 30,
    "durationMs": 15000,
    "aspectRatio": "RATIO_9_16",
    "tracks": [
      {
        "id": "track_1",
        "type": "VIDEO",
        "order": 0,
        "clips": [
          {
            "id": "clip_1",
            "type": "VIDEO",
            "assetId": "asset_1",
            "startTimeMs": 0,
            "durationMs": 5000,
            "inPointMs": 0,
            "outPointMs": 5000,
            "speed": 1.0,
            "volume": 1.0,
            "transform": {
              "x": 0,
              "y": 0,
              "scaleX": 1,
              "scaleY": 1,
              "rotation": 0,
              "opacity": 1
            },
            "effects": [],
            "keyframes": []
          }
        ]
      }
    ]
  }
}
```

---

## 35. Testing Strategy

### DAO Tests

Use in-memory Room database.

Test:

```text
Insert project
Observe project list
Update project
Delete project
Insert track and clip
Cascade delete track
Load project with tracks
Load clip with details
```

Example:

```kotlin
@Before
fun setup() {
    database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java
    ).allowMainThreadQueries().build()
}

@After
fun teardown() {
    database.close()
}
```

Rules:

- Do not use main database for tests.
- Test foreign key behavior.
- Test cascade delete carefully.
- Test time values as Long milliseconds.

---

## 36. Performance Considerations

Use:

- Indexes on foreign keys.
- Batch insert with `insertAll`.
- Transactions for full project save.
- Flow only where observation is needed.
- Paging later if asset library becomes large.

Avoid:

- Loading all keyframes for project list.
- Storing bitmaps in database.
- Saving on every playhead movement.
- Running large saves on main thread.
- Querying full project repeatedly for small UI updates.

---

## 37. Data Integrity Rules

```text
Project must have at least one track before editing.
Track order must be unique per project.
Clip start time must not be negative.
Clip duration must be greater than zero.
InPoint must be less than or equal to outPoint.
Text clip must have text data.
Audio clip should have volume.
Missing asset should not crash editor.
```

Validation should happen in domain layer before saving.

---

## 38. AI Prompts

Use these after giving this database schema to AI.

### Generate Room Entities

```text
Use this DATABASE_SCHEMA.md as source of truth. Generate all Room entities for this Android video editor app in Kotlin. Include ProjectEntity, TrackEntity, ClipEntity, AssetEntity, TransformEntity, EffectEntity, KeyframeEntity, TextClipEntity, and TransitionEntity.
```

### Generate DAOs

```text
Use this DATABASE_SCHEMA.md as source of truth. Generate Room DAOs for projects, tracks, clips, assets, transforms, effects, keyframes, text clips, and transitions.
```

### Generate Database Class

```text
Use this DATABASE_SCHEMA.md as source of truth. Generate AppDatabase, type converters, and Hilt database module.
```

### Generate Mappers

```text
Use this DATABASE_SCHEMA.md as source of truth. Generate Kotlin mapper functions between Room entities and domain models for Project, Track, Clip, Asset, Transform, Effect, Keyframe, and TextClipData.
```

### Generate Repository

```text
Use this DATABASE_SCHEMA.md as source of truth. Generate ProjectRepository and AssetRepository implementations using Room DAOs, transactions, and domain mappers.
```

---

## 39. Final Schema Checklist

```text
[ ] projects table defined
[ ] tracks table defined
[ ] clips table defined
[ ] assets table defined
[ ] transforms table defined
[ ] effects table defined
[ ] keyframes table defined
[ ] text_clips table defined
[ ] transitions table defined
[ ] Foreign keys added
[ ] Indexes added
[ ] Type converters added
[ ] Room database created
[ ] DAOs created
[ ] Relation classes created
[ ] Mapper functions created
[ ] Repository created
[ ] Autosave strategy defined
[ ] Migration strategy defined
[ ] DAO tests planned
```

