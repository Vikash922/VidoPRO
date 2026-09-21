---
title: Video Editing App Architecture Document
version: 1.0
related: PRD.md
platform: Android
tech: Kotlin, Jetpack Compose, Hilt, Room, Coroutines, Flow, Media3
status: Initial Architecture
---

# Video Editing App — Architecture Document

## 1. Purpose

This document defines the technical architecture for an Android video editing app inspired by CapCut and Alight Motion.

The app must support:

- Simple social video editing
- Timeline-based clip editing
- Media import and preview
- Text, filters, and effects
- Future keyframe animation and motion graphics
- Reliable MP4 export

This architecture is designed to be:

- Modular
- Testable
- Scalable
- UI-independent where possible
- Performance-focused
- Ready for future OpenGL rendering

---

## 2. Architecture Goals

1. Keep UI smooth and responsive.
2. Keep editing logic independent from Compose UI.
3. Keep media processing away from the main thread.
4. Support undo/redo safely.
5. Support autosave and project restoration.
6. Allow future migration from basic Media3 preview to advanced OpenGL rendering.
7. Keep timeline math deterministic and unit-testable.
8. Separate project data from media files.

---

## 3. High-Level Architecture

The app follows a modular clean architecture with unidirectional data flow.

```text
┌─────────────────────────────────────────────┐
│                  UI Layer                    │
│  Compose Screens, ViewModels, UI State       │
└────────────────────┬────────────────────────┘
                     │
┌────────────────────▼────────────────────────┐
│                Domain Layer                  │
│  Use Cases, Timeline Engine, Undo/Redo       │
└────────────────────┬────────────────────────┘
                     │
┌────────────────────▼────────────────────────┐
│                 Data Layer                   │
│  Repositories, Room DB, File Storage         │
└────────────────────┬────────────────────────┘
                     │
┌────────────────────▼────────────────────────┐
│              Media Engine Layer              │
│  Player, Decoder, Renderer, Exporter         │
└─────────────────────────────────────────────┘
```

---

## 4. Architecture Style

Recommended style:

- MVVM for UI state
- MVI-like event handling for editor actions
- Repository pattern for data access
- Use cases for domain logic
- Pure Kotlin engine for timeline operations
- Separate media engine for playback/export/rendering

Why this style:

- Editor apps have complex state.
- Timeline logic must be testable without UI.
- Media operations must be isolated.
- Undo/redo needs command-based state changes.
- Future rendering pipeline needs clear boundaries.

---

## 5. Module Structure

```text
video-editor/
├── app/
├── core/
│   ├── common/
│   ├── model/
│   ├── database/
│   ├── data/
│   ├── ui/
│   └── media/
└── feature/
    ├── home/
    ├── editor/
    ├── timeline/
    ├── mediaPicker/
    ├── export/
    └── settings/
```

---

## 6. Module Responsibilities

### app

Main application module.

Responsibilities:

- Application class
- MainActivity
- Navigation host
- App-level dependency injection
- Feature wiring

Dependencies:

- feature:*
- core:ui
- core:common

---

### core:common

Shared utilities.

Responsibilities:

- Constants
- Extensions
- Result wrapper
- Dispatchers provider
- Time utilities
- File utilities

Dependencies:

- Kotlin only

---

### core:model

Pure data models.

Responsibilities:

- Project
- Track
- Clip
- Asset
- Effect
- Keyframe
- Transform
- Export settings

Dependencies:

- Kotlin only

Important:

- This module should not depend on Android SDK if possible.
- Models should be immutable where practical.

---

### core:database

Local persistence.

Responsibilities:

- Room database
- Entities
- DAOs
- Type converters
- Migrations

Dependencies:

- core:model
- Room

---

### core:data

Data access layer.

Responsibilities:

- Project repository implementation
- Asset repository implementation
- Media metadata reader
- File storage access
- MediaStore helpers
- Thumbnail cache helpers

Dependencies:

- core:model
- core:database
- core:common

---

### core:ui

Design system.

Responsibilities:

- App theme
- Colors
- Typography
- Shapes
- Buttons
- Cards
- Bottom sheets
- Dialogs
- Loading components
- Empty state components

Dependencies:

- Jetpack Compose
- Material 3

---

### core:media

Media engine.

Responsibilities:

- Playback controller
- Media metadata extraction
- Thumbnail generation
- Preview rendering abstraction
- Export engine abstraction
- Future OpenGL renderer
- Future effects rendering

Dependencies:

- core:model
- core:common
- Media3
- Coil optional
- OpenGL later

---

## 7. Feature Modules

### feature:home

Home screen feature.

Responsibilities:

- Project list
- Create project
- Rename project
- Delete project
- Duplicate project
- Open editor

Dependencies:

- core:model
- core:data
- core:ui

---

### feature:editor

Main editor feature.

Responsibilities:

- Editor screen
- Editor ViewModel
- Editor state
- Preview container
- Tool panel
- Undo/redo UI actions
- Autosave triggers

Dependencies:

- core:model
- core:data
- core:media
- core:ui
- feature:timeline

---

### feature:timeline

Timeline feature.

Responsibilities:

- Timeline UI
- Timeline gestures
- Timeline drawing
- Timeline state display
- Clip cards
- Playhead
- Time ruler
- Snap visual indicators

Dependencies:

- core:model
- core:ui

Important:

- Pure timeline operations should not live inside UI code.
- Timeline UI should consume timeline state from ViewModel/engine.

---

### feature:mediaPicker

Media selection feature.

Responsibilities:

- Permission handling
- Video/image/audio browsing
- Multi-select
- Return selected media to editor

Dependencies:

- core:model
- core:data
- core:ui

---

### feature:export

Export feature.

Responsibilities:

- Export settings UI
- Export progress UI
- Export worker triggering
- Export state observation

Dependencies:

- core:model
- core:media
- core:ui
- WorkManager

---

### feature:settings

Settings feature.

Responsibilities:

- Theme preferences
- Default export settings
- Clear cache
- About screen

Dependencies:

- core:data
- core:ui

---

## 8. Module Dependency Graph

```text
app
 ├── feature:home
 ├── feature:editor
 ├── feature:timeline
 ├── feature:mediaPicker
 ├── feature:export
 ├── feature:settings
 ├── core:ui
 └── core:common

feature:editor
 ├── core:model
 ├── core:data
 ├── core:media
 ├── core:ui
 └── feature:timeline

feature:timeline
 ├── core:model
 └── core:ui

feature:home
 ├── core:model
 ├── core:data
 └── core:ui

feature:mediaPicker
 ├── core:model
 ├── core:data
 └── core:ui

feature:export
 ├── core:model
 ├── core:media
 ├── core:ui
 └── androidx.work

core:data
 ├── core:model
 ├── core:database
 └── core:common

core:database
 └── core:model

core:media
 ├── core:model
 └── core:common
```

Rules:

- Feature modules should not depend on each other except where necessary.
- core:model should remain independent.
- core:media should not depend on UI modules.
- Database should not know about media rendering.

---

## 9. Recommended Gradle Setup

Use version catalog:

```text
gradle/libs.versions.toml
```

Suggested libraries:

```toml
[versions]
kotlin = "2.0.0"
agp = "8.4.0"
composeBom = "2024.09.02"
hilt = "2.51.1"
room = "2.6.1"
media3 = "1.4.1"
coil = "2.7.0"
work = "2.9.1"
coroutines = "1.8.1"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version = "1.13.1" }
androidx-lifecycle-runtime = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version = "2.8.4" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version = "1.9.2" }

compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-material3 = { module = "androidx.compose.material3:material3" }

hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }

room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }

media3-exoplayer = { module = "androidx.media3:media3-exoplayer", version.ref = "media3" }
media3-transformer = { module = "androidx.media3:media3-transformer", version.ref = "media3" }
media3-effect = { module = "androidx.media3:media3-effect", version.ref = "media3" }
media3-ui = { module = "androidx.media3:media3-ui", version.ref = "media3" }

coil-compose = { module = "io.coil-kt:coil-compose", version.ref = "coil" }
coil-video = { module = "io.coil-kt:coil-video", version.ref = "coil" }

work-runtime = { module = "androidx.work:work-runtime-ktx", version.ref = "work" }

kotlinx-coroutines = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
```

Versions may change. Use latest stable compatible versions.

---

## 10. Layer Details

### UI Layer

Responsibilities:

- Render screens
- Observe state
- Send user events
- Show loading/error/success states
- Avoid business logic

Rules:

- Composables should be mostly stateless.
- ViewModels should hold UI state.
- Heavy operations should not run in composables.
- Use `collectAsStateWithLifecycle()` for lifecycle-aware collection.

Example:

```kotlin
data class EditorUiState(
    val projectId: String? = null,
    val isLoading: Boolean = false,
    val timeline: TimelineUiState = TimelineUiState(),
    val selectedClipId: String? = null,
    val isPlaying: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val errorMessage: String? = null
)
```

---

### Domain Layer

Responsibilities:

- Editor rules
- Timeline operations
- Undo/redo command handling
- Project duration calculation
- Clip validation
- Export validation

Rules:

- Prefer pure Kotlin.
- Avoid Android dependencies where possible.
- Make functions deterministic and unit-testable.

Example use cases:

```kotlin
class SplitClipUseCase {
    operator fun invoke(clip: Clip, playheadTimeMs: Long): SplitResult
}

class TrimClipUseCase {
    operator fun invoke(clip: Clip, trimRequest: TrimRequest): Clip
}

class CalculateProjectDurationUseCase {
    operator fun invoke(tracks: List<Track>): Long
}
```

---

### Data Layer

Responsibilities:

- Save/load projects
- Save/load assets
- Cache thumbnails
- Access media files
- Handle MediaStore operations

Repositories:

```kotlin
interface ProjectRepository {
    suspend fun createProject(name: String, aspectRatio: AspectRatio): Project
    suspend fun getProjectById(id: String): Project?
    fun observeProjects(): Flow<List<Project>>
    suspend fun updateProject(project: Project)
    suspend fun deleteProject(id: String)
}

interface AssetRepository {
    suspend fun importAsset(uri: Uri): Asset
    suspend fun getAssetById(id: String): Asset?
}
```

---

### Media Engine Layer

Responsibilities:

- Play preview
- Generate thumbnails
- Read media metadata
- Export project
- Later render OpenGL frames

Interfaces:

```kotlin
interface PreviewPlayerController {
    fun play()
    fun pause()
    fun seekTo(timeMs: Long)
    fun release()
    fun observePlaybackState(): Flow<PlaybackState>
}

interface ProjectExporter {
    suspend fun export(
        project: Project,
        settings: ExportSettings,
        progressListener: (Float) -> Unit
    ): ExportResult
}

interface FrameRenderer {
    fun renderFrame(project: Project, timeMs: Long): RenderedFrame
}
```

---

## 11. State Management

Use unidirectional data flow.

```text
User Action
    ↓
ViewModel Event
    ↓
Use Case / Engine
    ↓
Repository / Media Engine
    ↓
State Update
    ↓
Compose Recomposition
```

Editor event example:

```kotlin
sealed class EditorEvent {
    data class SeekTo(val timeMs: Long) : EditorEvent()
    data class SelectClip(val clipId: String?) : EditorEvent()
    data class SplitClipAtPlayhead(val clipId: String) : EditorEvent()
    data class TrimClipStart(val clipId: String, val newInPointMs: Long) : EditorEvent()
    data class TrimClipEnd(val clipId: String, val newOutPointMs: Long) : EditorEvent()
    data class MoveClip(val clipId: String, val newStartTimeMs: Long) : EditorEvent()
    object Undo : EditorEvent()
    object Redo : EditorEvent()
    object Play : EditorEvent()
    object Pause : EditorEvent()
    object ExportClicked : EditorEvent()
}
```

ViewModel should expose:

```kotlin
val uiState: StateFlow<EditorUiState>
fun onEvent(event: EditorEvent)
```

---

## 12. Timeline Architecture

Timeline is split into:

1. Pure timeline state/logic
2. Timeline UI rendering
3. Timeline gestures

### Pure Timeline State

```kotlin
data class TimelineEngineState(
    val currentTimeMs: Long,
    val durationMs: Long,
    val zoom: Float,
    val selectedClipId: String?,
    val tracks: List<Track>
)
```

### Timeline Actions

```kotlin
sealed class TimelineAction {
    data class Seek(val timeMs: Long) : TimelineAction()
    data class SelectClip(val clipId: String?) : TimelineAction()
    data class AddClip(val clip: Clip) : TimelineAction()
    data class DeleteClip(val clipId: String) : TimelineAction()
    data class MoveClip(val clipId: String, val newStartTimeMs: Long) : TimelineAction()
    data class TrimStart(val clipId: String, val newInPointMs: Long) : TimelineAction()
    data class TrimEnd(val clipId: String, val newOutPointMs: Long) : TimelineAction()
    data class SplitAtPlayhead(val clipId: String) : TimelineAction()
}
```

### Timeline Reducer

```kotlin
class TimelineReducer {
    fun reduce(
        state: TimelineEngineState,
        action: TimelineAction
    ): TimelineEngineState
}
```

Benefits:

- Easy to unit test.
- Undo/redo can wrap actions.
- UI remains simple.
- Logic does not depend on Compose.

---

## 13. Timeline UI Architecture

Timeline UI should be separated from logic.

Components:

```text
TimelineContainer
├── TimelineScrollState
├── TimelineCanvas
│   ├── TimeRuler
│   ├── TrackRows
│   ├── ClipCards
│   └── Playhead
└── TimelineGestureHandler
```

Rendering options:

### Option A: Jetpack Compose Canvas

Best for initial version.

Pros:

- Simple
- Modern
- Good enough for MVP

Cons:

- Needs performance optimization for long timelines

### Option B: Custom View

Use later if timeline becomes very heavy.

Pros:

- More control
- Better drawing optimization possible

Cons:

- More complex
- Less Compose-native

Recommended:

- Start with Compose Canvas.
- Migrate only if performance requires it.

---

## 14. Timeline Performance Strategy

Rules:

- Do not recompute all clips on every scroll frame.
- Use `derivedStateOf` for computed scroll values.
- Cache clip layout positions.
- Draw only visible clips.
- Use stable data classes.
- Avoid object allocation inside draw loops.
- Use thumbnails instead of full frames.

Timeline coordinate system:

```text
x = timeMs * pixelsPerMs
timeMs = x / pixelsPerMs
```

Where:

```text
pixelsPerMs = basePixelsPerMs * zoom
```

Example:

```kotlin
data class TimelineMetrics(
    val pixelsPerMs: Float,
    val visibleStartTimeMs: Long,
    val visibleEndTimeMs: Long
)
```

Use visible time range to render only clips inside viewport.

---

## 15. Preview Architecture

Initial approach:

- Use Media3/ExoPlayer for preview playback.
- Keep preview controller behind an interface.
- Later replace with custom OpenGL renderer.

```text
EditorViewModel
    ↓
PreviewController
    ↓
Media3Player
    ↓
SurfaceView / TextureView
```

Preview interface:

```kotlin
interface PreviewPlayerController {
    fun initialize(surface: Surface)
    fun setProject(project: Project)
    fun play()
    fun pause()
    fun seekTo(timeMs: Long)
    fun release()
    fun observePlaybackState(): Flow<PlaybackState>
}
```

Playback state:

```kotlin
data class PlaybackState(
    val isPlaying: Boolean,
    val currentTimeMs: Long,
    val durationMs: Long
)
```

Future OpenGL architecture:

```text
Project time
    ↓
FrameComposer
    ↓
LayerRenderer
    ↓
EffectChain
    ↓
OpenGL framebuffer
    ↓
Preview Surface / Encoder Surface
```

Important:

- Keep preview implementation replaceable.
- Do not hardcode Media3 across editor logic.
- Editor should only talk to `PreviewPlayerController`.

---

## 16. Media Import Architecture

Flow:

```text
User selects media URI
    ↓
Permission check
    ↓
Metadata reader extracts duration/size/resolution
    ↓
Asset entity saved
    ↓
Thumbnail generated
    ↓
Clip created
    ↓
Timeline updated
```

Metadata reader:

```kotlin
interface MediaMetadataReader {
    suspend fun read(uri: Uri): MediaMetadata
}

data class MediaMetadata(
    val uri: Uri,
    val mimeType: String?,
    val durationMs: Long,
    val width: Int?,
    val height: Int?,
    val sizeBytes: Long?
)
```

Rules:

- Never read media metadata on main thread.
- Handle unsupported files gracefully.
- Store URI permission persistently if needed.
- Do not assume file path access on newer Android versions.

---

## 17. Thumbnail Architecture

Thumbnails are needed for:

- Home project cards
- Timeline video clips
- Media picker

Strategy:

- Generate thumbnails in background.
- Store in app cache.
- Use LRU memory cache.
- Use disk cache for timeline thumbnails.
- Use Coil for display where possible.

Example:

```kotlin
interface ThumbnailGenerator {
    suspend fun generateVideoThumbnail(uri: Uri, timeMs: Long): File
}
```

Cache key example:

```text
assetId + timeMs + width
```

Rules:

- Avoid generating full-resolution frames.
- Generate timeline thumbnails at fixed intervals.
- Reuse cached thumbnails on timeline scroll.
- Clear cache from settings if needed.

---

## 18. Export Architecture

Initial recommendation:

- Use Media3 Transformer for MVP export if it satisfies project needs.
- Use WorkManager to run export in background.
- Keep exporter behind an interface for future OpenGL migration.

Export flow:

```text
User taps Export
    ↓
ExportViewModel validates project
    ↓
ExportWorker starts
    ↓
ProjectExporter renders/encodes
    ↓
Progress emitted
    ↓
Output saved to MediaStore
    ↓
Success notification/UI state
```

Export settings:

```kotlin
data class ExportSettings(
    val width: Int,
    val height: Int,
    val fps: Int,
    val videoBitrate: Int,
    val audioBitrate: Int,
    val format: OutputFormat
)

enum class OutputFormat {
    MP4
}
```

Exporter interface:

```kotlin
interface ProjectExporter {
    suspend fun export(
        project: Project,
        settings: ExportSettings,
        outputFileDescriptor: ParcelFileDescriptor,
        onProgress: (Float) -> Unit
    ): ExportResult
}

sealed class ExportResult {
    data class Success(val uri: Uri) : ExportResult()
    data class Failure(val message: String, val cause: Throwable?) : ExportResult()
    object Cancelled : ExportResult()
}
```

---

## 19. WorkManager Export Design

Use WorkManager for reliable background export.

```kotlin
class ExportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return Result.success()
    }
}
```

Input data:

```text
projectId
exportWidth
exportHeight
exportFps
exportBitrate
```

Output/progress:

- Use WorkManager `setProgress()`
- Observe work info in ExportViewModel

Rules:

- Do not run export on main thread.
- Handle cancellation.
- Handle low storage.
- Handle codec failures.
- Do not corrupt project data if export fails.

---

## 20. Undo/Redo Architecture

Use command pattern.

```kotlin
interface EditorCommand {
    fun execute()
    fun undo()
}
```

Manager:

```kotlin
class UndoRedoManager {
    private val undoStack = ArrayDeque<EditorCommand>()
    private val redoStack = ArrayDeque<EditorCommand>()

    fun execute(command: EditorCommand) {
        command.execute()
        undoStack.addLast(command)
        redoStack.clear()
    }

    fun undo() {
        val command = undoStack.removeLastOrNull() ?: return
        command.undo()
        redoStack.addLast(command)
    }

    fun redo() {
        val command = redoStack.removeLastOrNull() ?: return
        command.execute()
        undoStack.addLast(command)
    }
}
```

Recommended command types:

```text
AddClipCommand
DeleteClipCommand
MoveClipCommand
TrimClipCommand
SplitClipCommand
ChangeTextCommand
ChangeEffectCommand
ChangeVolumeCommand
```

Rules:

- Commands should be small and reversible.
- Command execution should update state immutably where possible.
- Autosave should debounce after command execution.
- Undo stack should have a limit, e.g. 50 commands.

---

## 21. Effects Architecture

Effects should be data-driven and shader-friendly.

```kotlin
data class Effect(
    val id: String,
    val type: EffectType,
    val parameters: Map<String, Float>
)
```

Effect engine interface:

```kotlin
interface EffectProcessor {
    fun apply(inputFrame: Frame, effects: List<Effect>): Frame
}
```

Initial preview effects:

- Brightness
- Contrast
- Saturation
- Exposure

Future GPU effects:

- Blur
- Glow
- RGB split
- Glitch
- Vignette
- Pixelate

Effect rendering future:

```text
Texture input
    ↓
Shader chain
    ↓
Effect 1
    ↓
Effect 2
    ↓
Effect N
    ↓
Output texture
```

Important:

- Effect data should be stored in project.
- Effect preview and export should use same parameter model.
- UI should only edit effect parameters, not rendering internals.

---

## 22. Keyframe Architecture

Keyframes will be needed for Alight Motion-style animation.

Data model:

```kotlin
data class Keyframe(
    val id: String,
    val clipId: String,
    val property: String,
    val timeMs: Long,
    val value: Float,
    val interpolation: InterpolationType
)
```

Keyframe engine:

```kotlin
interface KeyframeEvaluator {
    fun evaluate(
        keyframes: List<Keyframe>,
        timeMs: Long,
        defaultValue: Float
    ): Float
}
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

Animation properties:

```text
positionX
positionY
scaleX
scaleY
rotation
opacity
effectParameter:*
```

Architecture rule:

- Keyframe evaluation should be pure Kotlin.
- Rendering should consume evaluated values.
- UI graph editor should only edit keyframe data.
---

## 23. Database Architecture

Use Room.

Entities:

```text
ProjectEntity
TrackEntity
ClipEntity
AssetEntity
EffectEntity
KeyframeEntity
TextClipEntity
```

DAOs:

```kotlin
@Dao
interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity)

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: String): ProjectEntity?

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: String)
}
```

Rules:

- Use entity mappers to convert between DB entities and domain models.
- Keep domain models clean.
- Use foreign keys where practical.
- Use cascade delete carefully.

---

## 24. Data Mapping Strategy

Separate models:

```text
UI Model
Domain Model
Database Entity
Media Engine Model
```

Example:

```text
ProjectEntity -> Project
ClipEntity -> Clip
AssetEntity -> Asset
Project -> ExportProject
```

Why:

- Room entities may need DB-specific fields.
- UI may need derived fields.
- Media engine may need optimized structures.

---

## 25. Dependency Injection

Use Hilt.

Modules:

```text
AppModule
DatabaseModule
RepositoryModule
MediaModule
DispatcherModule
```

Example:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideProjectRepository(
        db: AppDatabase,
        dispatchers: DispatcherProvider
    ): ProjectRepository {
        return ProjectRepositoryImpl(db, dispatchers)
    }
}
```

Dispatcher provider:

```kotlin
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}
```

---

## 26. Navigation Architecture

Use Navigation Compose.

Screens:

```text
Splash
Home
Editor
MediaPicker
Export
Settings
```

Example routes:

```kotlin
object Routes {
    const val HOME = "home"
    const val EDITOR = "editor/{projectId}"
    const val MEDIA_PICKER = "media_picker/{projectId}"
    const val EXPORT = "export/{projectId}"
    const val SETTINGS = "settings"
}
```

Rules:

- Editor should receive `projectId`.
- Media picker should return result to editor.
- Export can be a separate screen or bottom sheet depending UX.
- Back navigation from editor should trigger autosave.

---

## 27. Threading Model

Main thread:

- UI rendering
- State collection
- Light state updates

IO dispatcher:

- Database reads/writes
- File operations
- MediaStore queries
- Thumbnail disk cache

Default dispatcher:

- Timeline calculations if heavy
- Metadata parsing
- Frame composition logic
- Export validation

Media thread:

- Player internals
- Encoder internals
- OpenGL rendering later

Rules:

- Never decode video on main thread.
- Never export on main thread.
- Never generate many thumbnails synchronously in UI.
- Debounce autosave.

---

## 28. Error Handling

Use result wrapper or sealed states.

```kotlin
sealed class UiResult<out T> {
    data class Success<T>(val data: T) : UiResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : UiResult<Nothing>()
    object Loading : UiResult<Nothing>()
}
```

Rules:

- Repository should throw domain exceptions or return Result.
- ViewModel should convert errors into UI-friendly messages.
- UI should not show raw stack traces.
- Media errors should be recoverable where possible.

---

## 29. Autosave Architecture

Autosave flow:

```text
State change
    ↓
Debounce 1-2 seconds
    ↓
Save project to Room
    ↓
Update updatedAt
```

Use:

```kotlin
saveJob?.cancel()
saveJob = viewModelScope.launch {
    delay(1000)
    repository.updateProject(project)
}
```

Rules:

- Do not save on every frame during drag.
- Save when editor goes background.
- Save on important actions.
- Restore last playhead position.

---

## 30. Performance Architecture

### UI Performance

- Stable state classes
- Avoid unnecessary recomposition
- Use `remember`
- Use `derivedStateOf`
- Avoid heavy lambda creation in Compose
- Use lazy layouts for lists

### Timeline Performance

- Draw only visible clips
- Cache clip rectangles
- Cache thumbnails
- Limit zoom levels
- Use snap calculations efficiently

### Media Performance

- Use proxy media for large videos later
- Use hardware codecs where possible
- Avoid full-resolution decoding for preview
- Reuse textures later in OpenGL

### Export Performance

- Use background worker
- Use hardware encoder if available
- Show progress
- Handle cancellation

---

## 31. Testing Strategy

### Unit Tests

Test:

- Timeline reducer
- Trim logic
- Split logic
- Move logic
- Undo/redo manager
- Keyframe evaluator
- Project duration calculator

### Repository Tests

Test:

- Create project
- Load project
- Update project
- Delete project
- Asset import mapping

### UI Tests

Test:

- Home screen navigation
- Editor top bar actions
- Clip selection state
- Export button state

### Integration Tests

Test:

- Project creation to editor open
- Media import to timeline add
- Simple project export

### Media Tests

Test on real devices:

- MP4 playback
- Trim export
- Different resolutions
- Different codecs
- Low storage behavior

---

## 32. Future OpenGL Migration Path

Do not start with full OpenGL unless necessary.

Phase 1:

- Media3 preview
- Media3 Transformer export
- Basic filters maybe limited

Phase 2:

- Introduce FrameRenderer interface
- Render simple frames to preview surface

Phase 3:

- OpenGL texture pipeline
- Shader effects
- Layer composition

Phase 4:

- Render to MediaCodec input surface for export
- Add masks, blend modes, advanced effects

Important abstraction:

```kotlin
interface FrameRenderer {
    fun renderFrame(project: Project, timeMs: Long): RenderedFrame
}
```

This allows changing rendering implementation without rewriting editor logic.

---

## 33. Security and Privacy Architecture

Rules:

- Use scoped storage.
- Persist URI permissions where needed.
- Store only necessary metadata.
- Do not upload user media.
- Cache files should be app-private.
- Provide cache clearing option.

---

## 34. Build Configuration

Recommended variants:

```text
debug
release
```

Optional later:

```text
dev
staging
production
```

Use:

- R8/ProGuard for release
- Minify enabled
- Resource shrinking
- Crash reporting optional

---

## 35. Implementation Order

Recommended architecture implementation order:

1. Create Gradle modules.
2. Setup Hilt and navigation.
3. Create core models.
4. Create Room database.
5. Create repositories.
6. Build Home screen.
7. Build Editor shell.
8. Build timeline state engine.
9. Build timeline UI.
10. Build media picker.
11. Integrate preview player.
12. Implement trim/split/delete.
13. Implement undo/redo.
14. Implement autosave.
15. Implement basic export.
16. Add text/filters.
17. Optimize performance.
18. Prepare for OpenGL later.

---

## 36. Architecture Rules

Follow these rules strictly:

1. Do not put timeline math inside Compose draw functions.
2. Do not perform media decoding on the main thread.
3. Do not let feature modules depend on each other unnecessarily.
4. Do not let Room entities leak directly into UI if mapping is needed.
5. Do not tightly couple export to one library.
6. Do not tightly couple preview to one player implementation.
7. Keep undo/redo command logic separate from UI.
8. Keep core:model pure where possible.
9. Use interfaces for media engine components.
10. Optimize only after measuring performance.

---

## 37. AI Implementation Prompts

Use these after giving this architecture document to AI.

### Module Setup Prompt

```text
Use this ARCHITECTURE.md as source of truth. Generate the Android Gradle module structure for this video editing app. Include settings.gradle.kts, module folders, and basic build.gradle.kts files.
```

### Core Model Prompt

```text
Use this ARCHITECTURE.md as source of truth. Generate the core:model module Kotlin data classes for Project, Track, Clip, Asset, Transform, Effect, Keyframe, ExportSettings, and related enums.
```

### Database Prompt

```text
Use this ARCHITECTURE.md as source of truth. Generate the core:database module with Room entities, DAOs, type converters, database class, and Hilt module.
```

### Repository Prompt

```text
Use this ARCHITECTURE.md as source of truth. Generate core:data repositories for ProjectRepository and AssetRepository, including mappers between Room entities and domain models.
```

### Timeline Engine Prompt

```text
Use this ARCHITECTURE.md as source of truth. Generate a pure Kotlin timeline engine with TimelineEngineState, TimelineAction, TimelineReducer, trim, split, move, delete, and unit tests.
```

### Editor ViewModel Prompt

```text
Use this ARCHITECTURE.md as source of truth. Generate EditorViewModel, EditorUiState, EditorEvent, and a Compose editor screen skeleton.
```

### Export Prompt

```text
Use this ARCHITECTURE.md as source of truth. Generate export architecture using WorkManager, ExportSettings, ProjectExporter interface, and ExportWorker skeleton.
```

---

## 38. Final Notes

This architecture is designed to start simple and scale later.

For MVP:

- Keep rendering simple.
- Use Media3 where possible.
- Focus on stable timeline and export.
- Avoid premature OpenGL complexity.

For future:

- Move to GPU-based rendering when effects/keyframes become advanced.
- Keep interfaces clean so migration does not rewrite the whole app.

---


