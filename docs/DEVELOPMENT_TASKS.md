---
title: Development Tasks
version: 1.0
related: PRD.md, ARCHITECTURE.md, UI_DESIGN_SYSTEM.md, DATABASE_SCHEMA.md
status: Ready for Development
part: 1/3
---

# Development Tasks

## 1. Purpose

This document breaks the video editing app into practical development tasks.

Use this as your execution checklist.

Goal:

- Build MVP first.
- Keep each task small and testable.
- Avoid building advanced features too early.
- Maintain smooth UI and stable architecture.

---

## 2. How to Use This Document

Work in this order:

```text
Phase 0: Project Setup
Phase 1: Design System
Phase 2: Core Models and Database
Phase 3: Home and Project Management
Phase 4: Editor Shell
Phase 5: Media Picker and Assets
Phase 6: Timeline Engine
Phase 7: Timeline UI
Phase 8: Preview Playback
Phase 9: Basic Editing Actions
Phase 10: Autosave and Undo/Redo
Phase 11: Text and Filters
Phase 12: Export
Phase 13: Polish and Performance
Phase 14: Testing and Release Prep
```

For each task:

- Mark checkbox when done.
- Test before moving to next task.
- Do not jump to advanced features early.

---

## 3. Task Format

Each task includes:

```text
Task ID
Goal
Dependencies
Files/Modules
Acceptance criteria
AI prompt
```

Status values:

```text
[ ] Not started
[x] Done
```

---

## 4. Development Rules

1. Build MVP before advanced motion graphics.
2. Keep timeline logic separate from UI.
3. Keep media processing off main thread.
4. Use immutable UI state.
5. Use repository pattern for data.
6. Use Room for project persistence.
7. Test trim/split logic with unit tests.
8. Avoid premature OpenGL implementation.
9. Maintain dark theme consistency.
10. Check UI on small screen devices.

---

# Phase 0 — Project Setup

## DEV-001: Create Android Studio Project

Goal:

Create base Android project.

Dependencies:

None.

Modules:

```text
app
```

Acceptance criteria:

```text
[ ] Project builds successfully
[ ] Minimum SDK set to API 26
[ ] Kotlin enabled
[ ] Empty Compose MainActivity runs
```

AI prompt:

```text
Create a new Android project using Kotlin and Jetpack Compose with minimum SDK API 26.
```

---

## DEV-002: Setup Gradle Modules

Goal:

Create modular project structure.

Dependencies:

DEV-001

Modules:

```text
app
core/common
core/model
core/database
core/data
core/ui
core/media
feature/home
feature/editor
feature/timeline
feature/mediaPicker
feature/export
feature/settings
```

Acceptance criteria:

```text
[ ] All modules added
[ ] App compiles
[ ] app depends on feature modules
[ ] feature modules depend on required core modules
```

AI prompt:

```text
Generate Android Gradle module structure for a modular app with app, core, and feature modules as defined in ARCHITECTURE.md.
```

---

## DEV-003: Setup Version Catalog

Goal:

Use centralized dependency versions.

Dependencies:

DEV-002

Files:

```text
gradle/libs.versions.toml
```

Acceptance criteria:

```text
[ ] Version catalog created
[ ] Dependencies declared centrally
[ ] Modules use catalog references
```

AI prompt:

```text
Create libs.versions.toml for Kotlin, Compose, Hilt, Room, Media3, Coil, WorkManager, and Coroutines.
```

---

## DEV-004: Setup Hilt

Goal:

Add dependency injection.

Dependencies:

DEV-002

Acceptance criteria:

```text
[ ] Application class annotated with @HiltAndroidApp
[ ] MainActivity uses @AndroidEntryPoint
[ ] Hilt compiles successfully
```

AI prompt:

```text
Setup Hilt in this Android project with Application class and MainActivity.
```

---

## DEV-005: Setup Navigation

Goal:

Add Navigation Compose.

Dependencies:

DEV-001, DEV-004

Acceptance criteria:

```text
[ ] NavHost created
[ ] Routes defined for Home, Editor, Export, Settings
[ ] Navigation works without crashes
```

AI prompt:

```text
Setup Navigation Compose with routes for home, editor, export, and settings.
```

---

# Phase 1 — Design System

## DEV-006: Create Color Tokens

Goal:

Define dark theme colors.

Dependencies:

DEV-002

Module:

```text
core/ui
```

Acceptance criteria:

```text
[ ] Color tokens match UI_DESIGN_SYSTEM.md
[ ] Material 3 dark color scheme created
[ ] Editor-specific colors separated
```

AI prompt:

```text
Create Compose color tokens and Material 3 dark color scheme using UI_DESIGN_SYSTEM.md.
```

---

## DEV-007: Create Typography Tokens

Goal:

Define app typography.

Dependencies:

DEV-006

Acceptance criteria:

```text
[ ] Typography scale defined
[ ] Headline, title, body, caption styles created
[ ] App uses typography tokens
```

AI prompt:

```text
Create Compose Typography tokens using UI_DESIGN_SYSTEM.md.
```

---

## DEV-008: Create Spacing and Radius Tokens

Goal:

Define spacing/radius constants.

Dependencies:

DEV-006

Acceptance criteria:

```text
[ ] AppSpacing object created
[ ] AppRadius object created
[ ] Components use tokens instead of hardcoded dp
```

AI prompt:

```text
Create AppSpacing and AppRadius objects using UI_DESIGN_SYSTEM.md.
```

---

## DEV-009: Create Base Buttons

Goal:

Create reusable buttons.

Dependencies:

DEV-006, DEV-007, DEV-008

Components:

```text
AppPrimaryButton
AppSecondaryButton
AppDangerButton
AppIconButton
```

Acceptance criteria:

```text
[ ] Buttons follow design system
[ ] Disabled states work
[ ] Loading state supported for primary button
```

AI prompt:

```text
Create reusable Compose buttons using UI_DESIGN_SYSTEM.md.
```

---

## DEV-010: Create Cards and Sheets

Goal:

Create base surface components.

Dependencies:

DEV-009

Components:

```text
AppCard
ProjectCard
AppBottomSheet
```

Acceptance criteria:

```text
[ ] Card radius and colors match design
[ ] Bottom sheet has drag handle
[ ] Sheet animation smooth
```

AI prompt:

```text
Create reusable Compose card and modal bottom sheet components using UI_DESIGN_SYSTEM.md.
```

---

## DEV-011: Create Input Components

Goal:

Create text input and slider.

Dependencies:

DEV-009

Components:

```text
AppTextField
AppSlider
AppChip
```

Acceptance criteria:

```text
[ ] Text field has focus state
[ ] Slider shows value
[ ] Chip supports selected state
```

AI prompt:

```text
Create reusable Compose text field, slider, and chip components using UI_DESIGN_SYSTEM.md.
```

---

## DEV-012: Create Empty/Loading/Error Components

Goal:

Create state components.

Dependencies:

DEV-009

Components:

```text
EmptyStateView
LoadingView
ErrorView
```

Acceptance criteria:

```text
[ ] Empty state supports title/subtitle/button
[ ] Loading view supports shimmer/spinner
[ ] Error view supports retry action
```

AI prompt:

```text
Create reusable empty, loading, and error state Compose components using UI_DESIGN_SYSTEM.md.
```

---

# Phase 2 — Core Models and Database

## DEV-013: Create Domain Models

Goal:

Create pure Kotlin models.

Dependencies:

DEV-002

Module:

```text
core/model
```

Models:

```text
Project
Track
Clip
Asset
Transform
Effect
Keyframe
TextClipData
ExportSettings
```

Acceptance criteria:

```text
[ ] Models are immutable where possible
[ ] Time values use milliseconds
[ ] Enums defined
[ ] No Android dependency where avoidable
```

AI prompt:

```text
Generate core:model Kotlin data classes using PRD.md and DATABASE_SCHEMA.md.
```

---

## DEV-014: Create Room Entities

Goal:

Create database entities.

Dependencies:

DEV-013

Module:

```text
core/database
```

Entities:

```text
ProjectEntity
TrackEntity
ClipEntity
AssetEntity
TransformEntity
EffectEntity
KeyframeEntity
TextClipEntity
TransitionEntity
```

Acceptance criteria:

```text
[ ] Entities match DATABASE_SCHEMA.md
[ ] Primary keys defined
[ ] Foreign keys defined
[ ] Indexes defined
```

AI prompt:

```text
Generate Room entities using DATABASE_SCHEMA.md.
```

---

## DEV-015: Create DAOs

Goal:

Create database access objects.

Dependencies:

DEV-014

DAOs:

```text
ProjectDao
TrackDao
ClipDao
AssetDao
TransformDao
EffectDao
KeyframeDao
TextClipDao
TransitionDao
```

Acceptance criteria:

```text
[ ] Insert/update/delete/query methods added
[ ] Flow observation available where needed
[ ] Room compiles successfully
```

AI prompt:

```text
Generate Room DAOs using DATABASE_SCHEMA.md.
```

---

## DEV-016: Create AppDatabase

Goal:

Create Room database.

Dependencies:

DEV-014, DEV-015

Acceptance criteria:

```text
[ ] AppDatabase includes all entities
[ ] TypeConverters registered
[ ] DAOs exposed
[ ] exportSchema true
```

AI prompt:

```text
Generate AppDatabase with type converters and Hilt module using DATABASE_SCHEMA.md.
```

---

## DEV-017: Create Entity Mappers

Goal:

Map entities to domain models.

Dependencies:

DEV-013, DEV-014

Module:

```text
core/data
```

Acceptance criteria:

```text
[ ] ProjectEntity maps to Project
[ ] TrackEntity maps to Track
[ ] ClipEntity maps to Clip
[ ] Reverse mapping works
```

AI prompt:

```text
Generate mapper functions between Room entities and domain models using DATABASE_SCHEMA.md.
```

---

## DEV-018: Create Project Repository

Goal:

Provide project data access.

Dependencies:

DEV-016, DEV-017

Acceptance criteria:

```text
[ ] createProject works
[ ] getProjectById works
[ ] observeProjects works
[ ] deleteProject works
[ ] duplicateProject works
```

AI prompt:

```text
Generate ProjectRepository implementation using Room and mappers from DATABASE_SCHEMA.md.
```

---

# Phase 3 — Home and Project Management

## DEV-019: Create Home UI State

Goal:

Define Home screen state.

Dependencies:

DEV-018

Module:

```text
feature/home
```

State:

```kotlin
data class HomeUiState(
    val isLoading: Boolean = false,
    val projects: List<ProjectUiModel> = emptyList(),
    val errorMessage: String? = null
)
```

Acceptance criteria:

```text
[ ] State is immutable
[ ] Loading/list/error states supported
```

AI prompt:

```text
Create HomeUiState and ProjectUiModel for the home screen.
```

---

## DEV-020: Create HomeViewModel

Goal:

Observe projects and handle actions.

Dependencies:

DEV-019

Acceptance criteria:

```text
[ ] Projects observed from repository
[ ] Create project action works
[ ] Delete project action works
[ ] Duplicate project action works
```

AI prompt:

```text
Create HomeViewModel using ProjectRepository and StateFlow.
```

---

## DEV-021: Build Home Screen UI

Goal:

Create Home screen.

Dependencies:

DEV-010, DEV-012, DEV-020

Acceptance criteria:

```text
[ ] New Project button visible
[ ] Project list/grid visible
[ ] Empty state visible when no projects
[ ] Project card shows thumbnail, name, duration
[ ] Project overflow menu works
```

AI prompt:

```text
Create Home screen in Jetpack Compose using UI_DESIGN_SYSTEM.md.
```

---

## DEV-022: Create New Project Sheet

Goal:

Allow project creation with aspect ratio.

Dependencies:

DEV-021

Acceptance criteria:

```text
[ ] Bottom sheet opens
[ ] Aspect ratio selectable
[ ] Project name optional
[ ] Create button creates project and opens editor
```

AI prompt:

```text
Create New Project bottom sheet with aspect ratio selector.
```

---

## DEV-023: Add Project Rename/Delete Dialogs

Goal:

Support project actions safely.

Dependencies:

DEV-021

Acceptance criteria:

```text
[ ] Rename dialog works
[ ] Delete confirmation works
[ ] Delete updates list
```

AI prompt:

```text
Add rename and delete confirmation dialogs to Home screen.
```

---

<!-- END PART 1/3 -->

---
part: 2/3
---

# Phase 4 — Editor Shell

## DEV-024: Create Editor Route

Goal:

Open editor with project ID.

Dependencies:

DEV-005, DEV-018

Acceptance criteria:

```text
[ ] Editor route accepts projectId
[ ] Editor opens from Home
[ ] Invalid projectId shows error state
```

AI prompt:

```text
Create editor navigation route with projectId argument.
```

---

## DEV-025: Create EditorUiState

Goal:

Define editor state.

Dependencies:

DEV-013

State:

```kotlin
data class EditorUiState(
    val projectId: String? = null,
    val isLoading: Boolean = false,
    val project: Project? = null,
    val timeline: TimelineUiState = TimelineUiState(),
    val selectedClipId: String? = null,
    val isPlaying: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val errorMessage: String? = null
)
```

Acceptance criteria:

```text
[ ] State is immutable
[ ] Loading and error supported
[ ] Timeline state separated
```

AI prompt:

```text
Create EditorUiState and TimelineUiState models for video editor.
```

---

## DEV-026: Create EditorViewModel Skeleton

Goal:

Load project and expose events.

Dependencies:

DEV-018, DEV-025

Acceptance criteria:

```text
[ ] Loads project by ID
[ ] Exposes StateFlow
[ ] Handles basic events
[ ] Updates loading/error state
```

AI prompt:

```text
Create EditorViewModel skeleton using ProjectRepository and StateFlow.
```

---

## DEV-027: Build Editor Top Bar

Goal:

Create top app bar for editor.

Dependencies:

DEV-009, DEV-026

Acceptance criteria:

```text
[ ] Back button works
[ ] Project name visible
[ ] Undo/redo buttons visible
[ ] Export button visible
[ ] Undo/redo disabled state visible
```

AI prompt:

```text
Create editor top bar in Compose with back, project name, undo, redo, and export.
```

---

## DEV-028: Build Preview Placeholder

Goal:

Create preview area.

Dependencies:

DEV-026

Acceptance criteria:

```text
[ ] Preview area respects aspect ratio
[ ] Placeholder visible when no media
[ ] Play/pause overlay visible
[ ] Time labels visible
```

AI prompt:

```text
Create editor preview placeholder area with aspect ratio support.
```

---

## DEV-029: Build Bottom Tool Panel

Goal:

Create bottom tools.

Dependencies:

DEV-009, DEV-026

Tools:

```text
Edit
Audio
Text
Effects
Filters
Speed
Overlay
```

Acceptance criteria:

```text
[ ] Tool icons visible
[ ] Selected tool highlighted
[ ] Horizontal scroll if needed
[ ] Tool click opens placeholder sheet
```

AI prompt:

```text
Create bottom tool panel for video editor using UI_DESIGN_SYSTEM.md.
```

---

# Phase 5 — Media Picker and Assets

## DEV-030: Add Media Permissions

Goal:

Handle media access safely.

Dependencies:

DEV-001

Acceptance criteria:

```text
[ ] Android 13+ media permissions handled
[ ] Older storage permission handled
[ ] Permission denied state shown
[ ] No unnecessary permissions requested
```

AI prompt:

```text
Implement Android media permissions for video, image, and audio access.
```

---

## DEV-031: Create Media Repository

Goal:

Load device media.

Dependencies:

DEV-030

Acceptance criteria:

```text
[ ] Can load videos
[ ] Can load images
[ ] Can load audio later
[ ] Returns URI, duration, thumbnail info
```

AI prompt:

```text
Create media repository to load device videos and images using MediaStore.
```

---

## DEV-032: Build Media Picker UI

Goal:

Create media selection screen/sheet.

Dependencies:

DEV-011, DEV-031

Acceptance criteria:

```text
[ ] Grid thumbnails visible
[ ] Duration badge visible for videos
[ ] Selection state visible
[ ] Add button enabled after selection
```

AI prompt:

```text
Create media picker screen with grid thumbnails and selection state.
```

---

## DEV-033: Import Selected Media as Clips

Goal:

Add selected media to timeline.

Dependencies:

DEV-018, DEV-032

Acceptance criteria:

```text
[ ] Asset saved to database
[ ] Clip created for selected media
[ ] Clip duration based on media duration
[ ] Image clips get default duration
[ ] Timeline updated
```

AI prompt:

```text
Import selected media and create timeline clips from them.
```

---

# Phase 6 — Timeline Engine

## DEV-034: Create TimelineEngineState

Goal:

Define pure timeline state.

Dependencies:

DEV-013

State:

```kotlin
data class TimelineEngineState(
    val currentTimeMs: Long,
    val durationMs: Long,
    val zoom: Float,
    val selectedClipId: String?,
    val tracks: List<Track>
)
```

Acceptance criteria:

```text
[ ] State is pure Kotlin
[ ] No Android dependency
[ ] Easy to unit test
```

AI prompt:

```text
Create pure Kotlin timeline engine state using PRD.md and ARCHITECTURE.md.
```

---

## DEV-035: Create Timeline Time Utilities

Goal:

Handle time/pixel conversion.

Dependencies:

DEV-034

Functions:

```kotlin
fun timeToX(timeMs: Long, pixelsPerMs: Float): Float
fun xToTime(x: Float, pixelsPerMs: Float): Long
fun calculatePixelsPerMs(basePixelsPerMs: Float, zoom: Float): Float
```

Acceptance criteria:

```text
[ ] Conversions are accurate
[ ] Negative values handled
[ ] Unit tests added
```

AI prompt:

```text
Create timeline time-to-pixel and pixel-to-time utilities with unit tests.
```

---

## DEV-036: Implement Add Clip Action

Goal:

Add clip to timeline state.

Dependencies:

DEV-034

Acceptance criteria:

```text
[ ] Clip added to correct track
[ ] Timeline duration recalculated
[ ] Clip start time valid
```

AI prompt:

```text
Implement add clip action in pure Kotlin timeline engine.
```

---

## DEV-037: Implement Move Clip Action

Goal:

Move clip on same track.

Dependencies:

DEV-036

Acceptance criteria:

```text
[ ] Clip start time updates
[ ] Clip cannot move before zero
[ ] Invalid overlaps handled based on MVP rule
[ ] Timeline duration recalculated
```

AI prompt:

```text
Implement move clip action in timeline engine.
```

---

## DEV-038: Implement Trim Clip Actions

Goal:

Trim start/end.

Dependencies:

DEV-036

Acceptance criteria:

```text
[ ] Trim start updates inPointMs and startTimeMs
[ ] Trim end updates outPointMs and durationMs
[ ] Duration never negative
[ ] Minimum clip duration respected
```

AI prompt:

```text
Implement trim start and trim end actions with validation and unit tests.
```

---

## DEV-039: Implement Split Clip Action

Goal:

Split clip at playhead.

Dependencies:

DEV-036

Acceptance criteria:

```text
[ ] Split at playhead creates two clips
[ ] Split at clip start does nothing
[ ] Split at clip end does nothing
[ ] New clip in/out points correct
```

AI prompt:

```text
Implement split clip at playhead action with unit tests.
```

---

## DEV-040: Implement Delete/Duplicate Actions

Goal:

Basic clip management.

Dependencies:

DEV-036

Acceptance criteria:

```text
[ ] Delete removes clip
[ ] Duplicate copies clip with new ID
[ ] Timeline duration recalculated
```

AI prompt:

```text
Implement delete and duplicate clip actions in timeline engine.
```

---

## DEV-041: Add Timeline Engine Unit Tests

Goal:

Ensure logic stability.

Dependencies:

DEV-035 to DEV-040

Test cases:

```text
Add clip
Move clip
Trim start
Trim end
Split at start
Split in middle
Split at end
Delete clip
Duplicate clip
Project duration recalculation
```

Acceptance criteria:

```text
[ ] All tests pass
[ ] Edge cases covered
[ ] No UI dependency in tests
```

AI prompt:

```text
Write unit tests for timeline engine trim, split, move, delete, and duration logic.
```

---

# Phase 7 — Timeline UI

## DEV-042: Create Timeline Metrics

Goal:

Manage zoom and visible range.

Dependencies:

DEV-035

State:

```kotlin
data class TimelineMetrics(
    val pixelsPerMs: Float,
    val visibleStartTimeMs: Long,
    val visibleEndTimeMs: Long
)
```

Acceptance criteria:

```text
[ ] Zoom changes pixelsPerMs
[ ] Visible range calculated
[ ] No heavy recomputation during scroll
```

AI prompt:

```text
Create timeline metrics and visible range calculation for Compose timeline.
```

---

## DEV-043: Draw Time Ruler and Playhead

Goal:

Basic timeline visual.

Dependencies:

DEV-042

Acceptance criteria:

```text
[ ] Time ruler visible
[ ] Playhead visible
[ ] Playhead draggable
[ ] Time labels update with zoom
```

AI prompt:

```text
Create timeline ruler and draggable playhead in Jetpack Compose Canvas.
```

---

## DEV-044: Render Clip Cards

Goal:

Show clips on timeline.

Dependencies:

DEV-043

Acceptance criteria:

```text
[ ] Clips positioned by startTimeMs
[ ] Clip width based on durationMs
[ ] Clip type colors shown
[ ] Thumbnails shown for video/image clips
```

AI prompt:

```text
Render timeline clip cards based on timeline state and metrics.
```

---

## DEV-045: Add Clip Selection

Goal:

Select clip on tap.

Dependencies:

DEV-044

Acceptance criteria:

```text
[ ] Tap clip selects it
[ ] Selected clip border visible
[ ] Tap empty timeline deselects
[ ] Selection state in ViewModel
```

AI prompt:

```text
Add clip selection state and selected clip visual style to timeline UI.
```

---

## DEV-046: Add Clip Drag Movement

Goal:

Move selected clip by drag.

Dependencies:

DEV-045, DEV-037

Acceptance criteria:

```text
[ ] Drag updates clip position visually
[ ] Drop updates timeline state
[ ] Clip cannot move before zero
[ ] Drag feels smooth
```

AI prompt:

```text
Implement clip drag movement in Compose timeline connected to timeline engine.
```

---

## DEV-047: Add Trim Handles

Goal:

Trim selected clip.

Dependencies:

DEV-045, DEV-038

Acceptance criteria:

```text
[ ] Handles visible only for selected clip
[ ] Drag left handle trims start
[ ] Drag right handle trims end
[ ] Minimum clip duration respected
```

AI prompt:

```text
Add trim handles to selected timeline clip and connect them to trim actions.
```

---

## DEV-048: Add Timeline Zoom and Scroll

Goal:

Smooth timeline navigation.

Dependencies:

DEV-043

Acceptance criteria:

```text
[ ] Horizontal scroll works
[ ] Pinch zoom works
[ ] Zoom has min/max limit
[ ] Playhead remains visible
[ ] Scroll performance smooth
```

AI prompt:

```text
Implement smooth horizontal scroll and pinch zoom for timeline UI.
```

---

## DEV-049: Add Snap Feedback

Goal:

Show snapping near clip edges.

Dependencies:

DEV-046

Acceptance criteria:

```text
[ ] Snap line appears near edge
[ ] Clip snaps within threshold
[ ] Snap feedback subtle
[ ] No lag during drag
```

AI prompt:

```text
Implement snap lines and snap logic for timeline clip dragging.
```

---

# Phase 8 — Preview Playback

## DEV-050: Create Preview Controller Interface

Goal:

Abstract preview playback.

Dependencies:

DEV-013

Interface:

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

Acceptance criteria:

```text
[ ] Interface does not expose Media3 directly
[ ] Easy to replace later
```

AI prompt:

```text
Create PreviewPlayerController interface for Android video editor.
```

---

## DEV-051: Implement Basic Media3 Preview

Goal:

Play selected media.

Dependencies:

DEV-050

Acceptance criteria:

```text
[ ] Video plays in preview area
[ ] Pause works
[ ] Seek works
[ ] Release handled properly
```

AI prompt:

```text
Implement basic preview playback using Media3 and PreviewPlayerController.
```

---

## DEV-052: Sync Playhead with Player

Goal:

Timeline and preview stay together.

Dependencies:

DEV-043, DEV-051

Acceptance criteria:

```text
[ ] Dragging playhead seeks player
[ ] Playback updates playhead
[ ] Pause keeps playhead position
```

AI prompt:

```text
Sync timeline playhead with Media3 player position using Flow.
```

---

## DEV-053: Handle Multi-Clip Preview Basic

Goal:

Preview active clip based on time.

Dependencies:

DEV-052

Acceptance criteria:

```text
[ ] Active clip determined by currentTimeMs
[ ] Preview switches clip at boundaries
[ ] Basic gaps handled gracefully
```

AI prompt:

```text
Implement basic active clip preview based on timeline playhead time.
```

---

# Phase 9 — Basic Editing Actions UI

## DEV-054: Add Split Button

Goal:

Split selected clip at playhead.

Dependencies:

DEV-039, DEV-045

Acceptance criteria:

```text
[ ] Split button visible when clip selected
[ ] Split updates timeline
[ ] Undo supports split
```

AI prompt:

```text
Add split selected clip button to editor tools.
```

---

## DEV-055: Add Delete Button

Goal:

Delete selected clip.

Dependencies:

DEV-040, DEV-045

Acceptance criteria:

```text
[ ] Delete button visible for selected clip
[ ] Clip removed from timeline
[ ] Snackbar with undo shown
```

AI prompt:

```text
Add delete clip action with undo snackbar.
```

---

## DEV-056: Add Duplicate Button

Goal:

Duplicate selected clip.

Dependencies:

DEV-040

Acceptance criteria:

```text
[ ] Duplicate creates new clip after original
[ ] New clip selected
[ ] Undo supports duplicate
```

AI prompt:

```text
Add duplicate clip action to editor.
```

---

## DEV-057: Add Edit Sheet

Goal:

Central clip actions sheet.

Dependencies:

DEV-010, DEV-054, DEV-055, DEV-056

Actions:

```text
Split
Delete
Duplicate
Speed later
Volume later
```

Acceptance criteria:

```text
[ ] Sheet opens from Edit tool
[ ] Actions enabled/disabled correctly
[ ] UI follows design system
```

AI prompt:

```text
Create Edit bottom sheet with split, delete, and duplicate actions.
```

---

<!-- END PART 2/3 -->

---
part: 3/3
---

# Phase 10 — Autosave and Undo/Redo

## DEV-058: Create UndoRedoManager

Goal:

Support undo/redo commands.

Dependencies:

DEV-026

Acceptance criteria:

```text
[ ] Command execute works
[ ] Undo works
[ ] Redo works
[ ] New action clears redo stack
[ ] Stack limit handled
```

AI prompt:

```text
Create UndoRedoManager using command pattern in Kotlin.
```

---

## DEV-059: Connect Timeline Actions to Undo/Redo

Goal:

Make editing actions reversible.

Dependencies:

DEV-041, DEV-058

Supported actions:

```text
Add clip
Delete clip
Move clip
Trim start
Trim end
Split clip
Duplicate clip
```

Acceptance criteria:

```text
[ ] Each action creates command
[ ] Undo restores previous state
[ ] Redo reapplies action
[ ] UI buttons update correctly
```

AI prompt:

```text
Connect timeline actions with UndoRedoManager and editor state.
```

---

## DEV-060: Implement Autosave

Goal:

Save project automatically.

Dependencies:

DEV-018, DEV-026

Acceptance criteria:

```text
[ ] Project saved after edit with debounce
[ ] Save happens on background
[ ] Save happens on back navigation
[ ] updatedAt timestamp updates
[ ] No save spam during drag
```

AI prompt:

```text
Implement debounced autosave for editor using ProjectRepository.
```

---

## DEV-061: Restore Project State

Goal:

Open project exactly where user left off.

Dependencies:

DEV-060

Acceptance criteria:

```text
[ ] Tracks restored
[ ] Clips restored
[ ] Selected clip state optional restore
[ ] Playhead position optional restore
[ ] Missing media handled gracefully
```

AI prompt:

```text
Restore full project state from Room database when editor opens.
```

---

# Phase 11 — Text and Filters

## DEV-062: Create Text Layer Model

Goal:

Support text clips.

Dependencies:

DEV-013

Acceptance criteria:

```text
[ ] TextClipData model created
[ ] Text clip can be added to timeline
[ ] Text duration adjustable
```

AI prompt:

```text
Create text layer model and text clip creation logic.
```

---

## DEV-063: Build Text Editor Sheet

Goal:

Allow user to add/edit text.

Dependencies:

DEV-011, DEV-062

Controls:

```text
Text input
Font size
Text color
Alignment
Add button
```

Acceptance criteria:

```text
[ ] Text input works
[ ] Style controls work
[ ] Text clip added to timeline
```

AI prompt:

```text
Create text editor bottom sheet using UI_DESIGN_SYSTEM.md.
```

---

## DEV-064: Render Text in Preview

Goal:

Show text over video preview.

Dependencies:

DEV-028, DEV-062

Acceptance criteria:

```text
[ ] Text visible during its timeline duration
[ ] Text style matches editor settings
[ ] Text hidden outside duration
```

AI prompt:

```text
Render active text clips over preview based on playhead time.
```

---

## DEV-065: Create Filter/Effect Model

Goal:

Store basic filter values.

Dependencies:

DEV-013

Fields:

```text
brightness
contrast
saturation
exposure
```

Acceptance criteria:

```text
[ ] Effect model stored per clip
[ ] Values range-safe
[ ] Effect can be reset
```

AI prompt:

```text
Create basic filter/effect model for video editor.
```

---

## DEV-066: Build Filters Sheet

Goal:

UI for basic filters.

Dependencies:

DEV-011, DEV-065

Controls:

```text
Brightness slider
Contrast slider
Saturation slider
Exposure slider
Reset button
```

Acceptance criteria:

```text
[ ] Sliders update state
[ ] Reset restores defaults
[ ] Sheet follows design system
```

AI prompt:

```text
Create filters bottom sheet with sliders and reset button.
```

---

## DEV-067: Apply Basic Filters to Preview

Goal:

Show filter changes live.

Dependencies:

DEV-051, DEV-066

Acceptance criteria:

```text
[ ] Brightness preview updates
[ ] Contrast preview updates
[ ] Saturation preview updates
[ ] Performance remains smooth
```

AI prompt:

```text
Apply basic color filters to preview using Compose color matrix or simple rendering.
```

---

# Phase 12 — Export

## DEV-068: Build Export Settings UI

Goal:

Let user choose export options.

Dependencies:

DEV-011

Options:

```text
720p
1080p
30fps
Quality low/medium/high
```

Acceptance criteria:

```text
[ ] Resolution selectable
[ ] FPS selectable
[ ] Estimated size optional
[ ] Export button visible
```

AI prompt:

```text
Create export settings screen using UI_DESIGN_SYSTEM.md.
```

---

## DEV-069: Create ExportWorker

Goal:

Run export in background.

Dependencies:

DEV-068

Acceptance criteria:

```text
[ ] WorkManager worker created
[ ] Export does not block UI
[ ] Progress emitted
[ ] Cancellation supported
```

AI prompt:

```text
Create ExportWorker using WorkManager for Android video export.
```

---

## DEV-070: Implement Basic Exporter

Goal:

Export project to MP4.

Dependencies:

DEV-069

Initial approach:

```text
Media3 Transformer or simple clip export pipeline
```

Acceptance criteria:

```text
[ ] MP4 file generated
[ ] Trimmed clips exported correctly
[ ] Export respects project duration
[ ] Errors handled
```

AI prompt:

```text
Implement basic MP4 export for trimmed timeline using Media3 Transformer.
```

---

## DEV-071: Add Export Progress and Cancel

Goal:

Show export state.

Dependencies:

DEV-069

Acceptance criteria:

```text
[ ] Progress visible
[ ] Cancel works
[ ] UI remains responsive
[ ] Failure shows friendly error
```

AI prompt:

```text
Add export progress UI with cancel action.
```

---

## DEV-072: Save Exported Video to Gallery

Goal:

Make output visible to user.

Dependencies:

DEV-070

Acceptance criteria:

```text
[ ] File saved using MediaStore
[ ] User sees success state
[ ] Share/open option available
```

AI prompt:

```text
Save exported MP4 to MediaStore and show success state.
```

---

# Phase 13 — Polish and Performance

## DEV-073: Optimize Compose Recomposition

Goal:

Reduce UI lag.

Dependencies:

DEV-021, DEV-027, DEV-044

Acceptance criteria:

```text
[ ] No unnecessary recomposition
[ ] Stable state classes used
[ ] Expensive calculations remembered
[ ] Lists lazy-loaded
```

AI prompt:

```text
Review Compose code for unnecessary recomposition and optimize using remember, derivedStateOf, and stable state.
```

---

## DEV-074: Optimize Timeline Rendering

Goal:

Smooth timeline scroll/zoom.

Dependencies:

DEV-048

Acceptance criteria:

```text
[ ] Only visible clips drawn
[ ] Clip layout cached
[ ] Scroll remains smooth
[ ] No heavy object allocation in draw loop
```

AI prompt:

```text
Optimize timeline rendering to draw only visible clips and cache layout calculations.
```

---

## DEV-075: Add Thumbnail Caching

Goal:

Avoid repeated thumbnail generation.

Dependencies:

DEV-033

Acceptance criteria:

```text
[ ] Thumbnails cached in memory
[ ] Thumbnails cached on disk
[ ] Timeline scroll does not regenerate thumbnails
[ ] Cache can be cleared
```

AI prompt:

```text
Implement thumbnail caching for video editor timeline.
```

---

## DEV-076: Polish Animations and Haptics

Goal:

Make app feel premium.

Dependencies:

DEV-010, DEV-043, DEV-066

Acceptance criteria:

```text
[ ] Bottom sheet animation smooth
[ ] Clip selection feedback fast
[ ] Snap haptic subtle
[ ] No animation blocks user action
```

AI prompt:

```text
Polish animations and haptic feedback using UI_DESIGN_SYSTEM.md.
```

---

## DEV-077: Test on Low-End Devices

Goal:

Ensure reasonable performance.

Dependencies:

Most features complete.

Acceptance criteria:

```text
[ ] App opens without crash
[ ] Timeline scroll acceptable
[ ] Preview not freezing
[ ] Export completes for small project
```

AI prompt:

```text
Create a low-end device performance testing checklist for Android video editor.
```

---

# Phase 14 — Testing and Release Prep

## DEV-078: Unit Tests

Goal:

Protect core logic.

Dependencies:

DEV-041, DEV-058

Tests:

```text
Timeline trim
Timeline split
Move clip
Delete clip
Duplicate clip
Undo/redo
Project duration
Time conversion
```

Acceptance criteria:

```text
[ ] Core logic covered
[ ] Tests pass
[ ] Edge cases tested
```

AI prompt:

```text
Generate unit tests for timeline engine and undo/redo manager.
```

---

## DEV-079: UI Tests

Goal:

Protect main flows.

Dependencies:

DEV-021, DEV-027

Tests:

```text
Home opens
Create project
Editor opens
Select clip
Open bottom sheet
```

Acceptance criteria:

```text
[ ] Critical navigation tested
[ ] Basic editor actions tested
```

AI prompt:

```text
Generate Compose UI tests for home, editor, and basic timeline actions.
```

---

## DEV-080: Device Compatibility Tests

Goal:

Check real-world behavior.

Dependencies:

DEV-070

Test devices:

```text
Low RAM device
Mid-range device
High-end device
Different Android versions
```

Acceptance criteria:

```text
[ ] Playback works
[ ] Export works
[ ] Permissions work
[ ] No critical crashes
```

AI prompt:

```text
Create device compatibility test checklist for Android video editor.
```

---

## DEV-081: Error Handling Pass

Goal:

Make app stable and friendly.

Dependencies:

Most features.

Check:

```text
Missing media
Unsupported file
Permission denied
Export failure
Low storage
Corrupt file
```

Acceptance criteria:

```text
[ ] Friendly error messages
[ ] No crashes for common failures
[ ] Retry action where useful
```

AI prompt:

```text
Review video editor app for error handling and add user-friendly recovery states.
```

---

## DEV-082: Release Build Setup

Goal:

Prepare APK/AAB.

Dependencies:

All MVP tasks.

Acceptance criteria:

```text
[ ] Release signing configured
[ ] R8 enabled
[ ] App icon added
[ ] App name final
[ ] Privacy policy placeholder added
[ ] Release build installs and runs
```

AI prompt:

```text
Prepare Android release build configuration for Play Store testing.
```

---

## 43. MVP Completion Checklist

```text
[ ] Project setup complete
[ ] Modules created
[ ] Design system created
[ ] Room database working
[ ] Home screen working
[ ] New project working
[ ] Media picker working
[ ] Editor shell working
[ ] Timeline visible
[ ] Clip selection working
[ ] Trim working
[ ] Split working
[ ] Delete working
[ ] Duplicate working
[ ] Undo/redo working
[ ] Autosave working
[ ] Preview playback working
[ ] Text basic working
[ ] Filters basic working
[ ] Export MP4 working
[ ] Error states added
[ ] Loading states added
[ ] Empty states added
[ ] Performance pass done
[ ] Basic tests done
[ ] Release build ready
```

---

## 44. Recommended 30-Day Plan

### Day 1-3

```text
Project setup
Modules
Gradle
Hilt
Navigation
```

### Day 4-6

```text
Design system
Buttons
Cards
Sheets
Inputs
```

### Day 7-9

```text
Core models
Room database
Repositories
```

### Day 10-12

```text
Home screen
Project create/open/delete
```

### Day 13-16

```text
Editor shell
Media picker
Add clips
```

### Day 17-20

```text
Timeline engine
Trim/split/delete
Unit tests
```

### Day 21-23

```text
Timeline UI
Playhead
Selection
Drag/trim
```

### Day 24-25

```text
Preview playback
Seek sync
```

### Day 26-27

```text
Text
Filters
Undo/redo
Autosave
```

### Day 28-30

```text
Export
Polish
Testing
Release build
```

---

## 45. Final Development Rule

Follow this order strictly:

```text
First make it work.
Then make it smooth.
Then make it powerful.
```

Do not start with:

```text
OpenGL
Masks
Blend modes
Graph editor
AI features
Templates
```

First complete:

```text
Stable editor
Smooth timeline
Reliable export
```

Then move to advanced features.

---

<!-- END PART 3/3 -->
