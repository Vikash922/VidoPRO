---
title: Timeline Engine Specification
version: 1.0
related: PRD.md, ARCHITECTURE.md, DATABASE_SCHEMA.md
module: core/domain or feature/timeline engine
status: Ready for Implementation
---

# Timeline Engine Specification

## 1. Purpose

This document defines the complete timeline engine specification for the Android video editing app.

The timeline engine is responsible for:

- Representing clips on a time-based timeline
- Calculating clip positions and widths
- Handling selection
- Handling trim, split, move, delete, duplicate
- Handling playhead and seeking
- Handling zoom and visible time range
- Handling snapping
- Calculating project duration
- Providing deterministic logic for unit testing
- Supporting future transitions, keyframes, and multi-track editing

The timeline engine must be pure Kotlin where possible and independent from Compose UI.

---

## 2. Scope

### In scope

```text
Timeline state
Clip state
Time calculations
Pixel/time conversion
Add clip
Move clip
Trim start
Trim end
Split clip
Delete clip
Duplicate clip
Snap logic
Zoom logic
Playhead seek
Active clip resolution
Project duration calculation
Validation rules
Undo/redo integration
Unit test cases
```

### Out of scope for initial engine

```text
OpenGL rendering
Media decoding
Export encoding
Waveform generation
AI features
Keyframe graph editing UI
Complex transition rendering
```

The engine may define transition data structures, but rendering transitions belongs to preview/export pipeline.

---

## 3. Core Principles

1. Timeline logic must be deterministic.
2. Timeline logic must not depend on Compose.
3. Time values must be stored in milliseconds.
4. UI should only render state, not calculate editing logic.
5. Every editing action should produce a new immutable state.
6. Invalid actions should be ignored or safely corrected.
7. Actions should be undoable through command/reducer integration.
8. Engine should be easy to unit test.
9. Engine should avoid unnecessary allocations.
10. Engine should support future multi-track expansion.

---

## 4. Time Model

All timeline time values use milliseconds.

```text
1 second = 1000 ms
```

Important time values:

```text
currentTimeMs:
Current playhead position on timeline.

clip.startTimeMs:
Clip position on timeline.

clip.durationMs:
Visible duration of clip on timeline.

clip.inPointMs:
Trim start point inside source media.

clip.outPointMs:
Trim end point inside source media.

project.durationMs:
Total duration of project timeline.
```

---

## 5. Basic Timeline Formula

A clip occupies this timeline range:

```text
clipStart = clip.startTimeMs
clipEnd = clip.startTimeMs + clip.durationMs
```

A clip is active when:

```text
currentTimeMs >= clipStart
and
currentTimeMs < clipEnd
```

Source media time for a normal speed clip:

```text
sourceTimeMs = clip.inPointMs + (currentTimeMs - clip.startTimeMs)
```

With speed:

```text
sourceTimeMs = clip.inPointMs + ((currentTimeMs - clip.startTimeMs) * clip.speed)
```

If speed is negative for reverse playback:

```text
sourceTimeMs = clip.outPointMs - ((currentTimeMs - clip.startTimeMs) * abs(clip.speed))
```

For MVP, speed can remain positive. Reverse playback can be added later.

---

## 6. Pixel/Time Coordinate System

Timeline UI uses horizontal pixels.

Define:

```text
pixelsPerMs = basePixelsPerMs * zoom
```

Example:

```text
basePixelsPerMs = 0.1f
zoom = 1.0f
pixelsPerMs = 0.1f

1000ms width = 100px
```

Time to X:

```kotlin
fun timeToX(timeMs: Long, pixelsPerMs: Float): Float {
    return timeMs * pixelsPerMs
}
```

X to time:

```kotlin
fun xToTime(x: Float, pixelsPerMs: Float): Long {
    return (x / pixelsPerMs).toLong()
}
```

Clip X:

```kotlin
fun clipX(clip: Clip, pixelsPerMs: Float): Float {
    return timeToX(clip.startTimeMs, pixelsPerMs)
}
```

Clip width:

```kotlin
fun clipWidth(clip: Clip, pixelsPerMs: Float): Float {
    return clip.durationMs * pixelsPerMs
}
```

Clip end X:

```kotlin
fun clipEndX(clip: Clip, pixelsPerMs: Float): Float {
    return clipX(clip, pixelsPerMs) + clipWidth(clip, pixelsPerMs)
}
```

---

## 7. Zoom Rules

Zoom changes how much time is visible.

```text
Higher zoom = more detailed timeline
Lower zoom = more timeline visible
```

Recommended zoom limits:

```text
minZoom = 0.25f
maxZoom = 8.0f
```

Pixels per ms limits:

```text
minPixelsPerMs = 0.02f
maxPixelsPerMs = 1.0f
```

Zoom behavior:

```text
newPixelsPerMs = basePixelsPerMs * zoom
newPixelsPerMs = newPixelsPerMs.coerceIn(minPixelsPerMs, maxPixelsPerMs)
```

Zoom should preserve anchor point if possible.

Example:

```text
If user zooms while playhead is visible,
playhead should remain near same visual position.
```

---

## 8. Timeline State

Primary engine state:

```kotlin
data class TimelineEngineState(
    val projectId: String,
    val currentTimeMs: Long,
    val durationMs: Long,
    val zoom: Float,
    val selectedClipId: String?,
    val tracks: List<Track>,
    val snappingEnabled: Boolean = true
)
```

Derived UI state:

```kotlin
data class TimelineUiState(
    val currentTimeMs: Long,
    val durationMs: Long,
    val zoom: Float,
    val pixelsPerMs: Float,
    val selectedClipId: String?,
    val tracks: List<TrackUiModel>,
    val visibleStartTimeMs: Long,
    val visibleEndTimeMs: Long,
    val snappingEnabled: Boolean
)
```

Rules:

- Engine state should be source of truth.
- UI state can be derived from engine state.
- Do not mutate state directly.
- Always create new state on action.

---

## 9. Track Model

```kotlin
data class Track(
    val id: String,
    val type: TrackType,
    val order: Int,
    val clips: List<Clip>
)
```

Track types:

```kotlin
enum class TrackType {
    VIDEO,
    OVERLAY,
    TEXT,
    AUDIO
}
```

Track rules:

```text
Track order defines vertical order.
Lower order can render below or above based on design choice.
Each track can contain clips of compatible type.
MVP may start with one main track.
```

---

## 10. Clip Model

```kotlin
data class Clip(
    val id: String,
    val type: ClipType,
    val assetId: String?,
    val startTimeMs: Long,
    val durationMs: Long,
    val inPointMs: Long,
    val outPointMs: Long,
    val speed: Float = 1.0f,
    val volume: Float? = null,
    val transform: Transform = Transform.DEFAULT,
    val effects: List<Effect> = emptyList(),
    val keyframes: List<Keyframe> = emptyList(),
    val textData: TextClipData? = null
)
```

Clip types:

```kotlin
enum class ClipType {
    VIDEO,
    IMAGE,
    AUDIO,
    TEXT,
    COLOR,
    SHAPE
}
```

Important:

```text
VIDEO:
Requires assetId.

IMAGE:
Requires assetId.
Duration can be user-controlled.

AUDIO:
Requires assetId.

TEXT:
Does not require assetId.
Requires textData.

COLOR:
Does not require assetId.
Requires color data later.

SHAPE:
Does not require assetId.
Requires shape data later.
```

---

## 11. Clip End Time

Helper:

```kotlin
val Clip.endTimeMs: Long
    get() = startTimeMs + durationMs
```

Rules:

```text
endTimeMs must be greater than startTimeMs.
durationMs must be greater than zero.
startTimeMs must not be negative.
```

---

## 12. Minimum Clip Duration

Define minimum clip duration:

```kotlin
const val MIN_CLIP_DURATION_MS = 100L
```

Rules:

```text
Trim cannot create duration below MIN_CLIP_DURATION_MS.
Split cannot create clip below MIN_CLIP_DURATION_MS.
Move should not affect duration.
```

For better UX, use:

```text
MIN_CLIP_DURATION_MS = 200L
```

---

## 13. Timeline Actions

Use sealed actions:

```kotlin
sealed class TimelineAction {

    data class LoadProject(val tracks: List<Track>) : TimelineAction()

    data class Seek(val timeMs: Long) : TimelineAction()

    data class SelectClip(val clipId: String?) : TimelineAction()

    data class AddClip(
        val trackId: String,
        val clip: Clip
    ) : TimelineAction()

    data class MoveClip(
        val clipId: String,
        val newStartTimeMs: Long
    ) : TimelineAction()

    data class TrimStart(
        val clipId: String,
        val newInPointMs: Long
    ) : TimelineAction()

    data class TrimEnd(
        val clipId: String,
        val newOutPointMs: Long
    ) : TimelineAction()

    data class SplitAtPlayhead(
        val clipId: String
    ) : TimelineAction()

    data class DeleteClip(
        val clipId: String
    ) : TimelineAction()

    data class DuplicateClip(
        val clipId: String
    ) : TimelineAction()

    data class SetZoom(val zoom: Float) : TimelineAction()

    data class ToggleSnap(val enabled: Boolean) : TimelineAction()
}
```

---

## 14. Timeline Reducer

Reducer signature:

```kotlin
class TimelineReducer {
    fun reduce(
        state: TimelineEngineState,
        action: TimelineAction
    ): TimelineEngineState
}
```

Reducer rules:

```text
Each action returns new state.
Do not mutate old state.
Validate action before applying.
Recalculate duration after structural changes.
Keep selected clip if still exists.
Clamp playhead to project duration.
```

Example structure:

```kotlin
fun reduce(
    state: TimelineEngineState,
    action: TimelineAction
): TimelineEngineState {
    return when (action) {
        is TimelineAction.LoadProject -> loadProject(state, action)
        is TimelineAction.Seek -> seek(state, action)
        is TimelineAction.SelectClip -> selectClip(state, action)
        is TimelineAction.AddClip -> addClip(state, action)
        is TimelineAction.MoveClip -> moveClip(state, action)
        is TimelineAction.TrimStart -> trimStart(state, action)
        is TimelineAction.TrimEnd -> trimEnd(state, action)
        is TimelineAction.SplitAtPlayhead -> splitAtPlayhead(state, action)
        is TimelineAction.DeleteClip -> deleteClip(state, action)
        is TimelineAction.DuplicateClip -> duplicateClip(state, action)
        is TimelineAction.SetZoom -> setZoom(state, action)
        is TimelineAction.ToggleSnap -> toggleSnap(state, action)
    }
}
```

---

## 15. Load Project Action

Purpose:

Initialize timeline with project tracks.

Rules:

```text
Set tracks.
Set duration.
Set currentTimeMs to 0.
Clear selected clip.
Keep zoom default.
```

Default zoom:

```kotlin
const val DEFAULT_ZOOM = 1.0f
```

---

## 16. Seek Action

Purpose:

Move playhead.

Rules:

```text
timeMs must be >= 0.
timeMs must be <= project duration.
Seeking should not modify clips.
Seeking should not trigger autosave.
```

Implementation:

```kotlin
private fun seek(
    state: TimelineEngineState,
    action: TimelineAction.Seek
): TimelineEngineState {
    val safeTime = action.timeMs.coerceIn(0L, state.durationMs)
    return state.copy(currentTimeMs = safeTime)
}
```

---

## 17. Select Clip Action

Purpose:

Update selected clip.

Rules:

```text
If clipId is null, deselect.
If clipId does not exist, deselect.
Selection should not modify timeline structure.
```

Implementation:

```kotlin
private fun selectClip(
    state: TimelineEngineState,
    action: TimelineAction.SelectClip
): TimelineEngineState {
    val exists = action.clipId != null && state.findClip(action.clipId) != null
    return state.copy(
        selectedClipId = if (exists) action.clipId else null
    )
}
```

---

## 18. Add Clip Action

Purpose:

Add clip to a track.

Inputs:

```text
trackId
clip
```

Rules:

```text
Track must exist.
Clip start time must not be negative.
Clip duration must be >= minimum.
For MVP, place clip at provided startTimeMs.
If no start time provided, append after last clip.
Recalculate project duration.
Select newly added clip.
```

Append logic:

```kotlin
fun appendTime(track: Track): Long {
    return track.clips.maxOfOrNull { it.endTimeMs } ?: 0L
}
```

After add:

```text
Sort clips by startTimeMs.
Recalculate duration.
```

---

## 19. Move Clip Action

Purpose:

Change clip start time.

Rules:

```text
Clip must exist.
New start time must be >= 0.
Clip duration remains same.
Clip in/out points remain same.
Clip cannot move before zero.
Snap may adjust final start time.
Recalculate duration.
```

Implementation:

```kotlin
private fun moveClip(
    state: TimelineEngineState,
    action: TimelineAction.MoveClip
): TimelineEngineState {
    val clip = state.findClip(action.clipId) ?: return state

    val safeStart = action.newStartTimeMs.coerceAtLeast(0L)

    val updatedTrack = state.tracks.map { track ->
        if (track.clips.any { it.id == clip.id }) {
            track.copy(
                clips = track.clips.map {
                    if (it.id == clip.id) {
                        it.copy(startTimeMs = safeStart)
                    } else {
                        it
                    }
                }
            )
        } else {
            track
        }
    }

    return state
        .copy(tracks = updatedTrack)
        .recalculateDuration()
}
```

Optional overlap rule for MVP:

```text
Allow overlap initially if UI handles it simply.
Or prevent overlap on same track.
```

Recommended for CapCut-like MVP:

```text
Prevent overlapping clips on same main video track.
```

---

## 20. Trim Start Action

Purpose:

Adjust beginning of clip.

Important:

Trimming start changes both:

```text
clip.startTimeMs
clip.inPointMs
```

Rules:

```text
newInPointMs must be >= 0.
newInPointMs must be < clip.outPointMs.
Resulting duration must be >= MIN_CLIP_DURATION_MS.
Clip endTimeMs should remain same for visual trim-left behavior.
```

Trim start formula:

```text
delta = newInPointMs - oldInPointMs
newStartTimeMs = oldStartTimeMs + delta
newDurationMs = oldDurationMs - delta
```

Implementation:

```kotlin
private fun trimStart(
    state: TimelineEngineState,
    action: TimelineAction.TrimStart
): TimelineEngineState {
    val clip = state.findClip(action.clipId) ?: return state

    val maxInPoint = clip.outPointMs - MIN_CLIP_DURATION_MS
    val safeInPoint = action.newInPointMs.coerceIn(0L, maxInPoint)

    val delta = safeInPoint - clip.inPointMs
    val newStart = (clip.startTimeMs + delta).coerceAtLeast(0L)
    val newDuration = clip.durationMs - delta

    if (newDuration < MIN_CLIP_DURATION_MS) return state

    return state.updateClip(clip.id) {
        it.copy(
            inPointMs = safeInPoint,
            startTimeMs = newStart,
            durationMs = newDuration
        )
    }.recalculateDuration()
}
```

---

## 21. Trim End Action

Purpose:

Adjust ending of clip.

Important:

Trimming end changes:

```text
clip.outPointMs
clip.durationMs
```

Rules:

```text
newOutPointMs must be > clip.inPointMs.
newOutPointMs must be <= source duration if known.
Resulting duration must be >= MIN_CLIP_DURATION_MS.
clip.startTimeMs remains same.
```

Trim end formula:

```text
newDurationMs = newOutPointMs - clip.inPointMs
```

If speed is used:

```text
timelineDurationMs = (newOutPointMs - clip.inPointMs) / speed
```

For MVP with speed = 1:

```text
timelineDurationMs = newOutPointMs - clip.inPointMs
```

Implementation:

```kotlin
private fun trimEnd(
    state: TimelineEngineState,
    action: TimelineAction.TrimEnd
): TimelineEngineState {
    val clip = state.findClip(action.clipId) ?: return state

    val minOutPoint = clip.inPointMs + MIN_CLIP_DURATION_MS
    val safeOutPoint = action.newOutPointMs.coerceAtLeast(minOutPoint)

    val newDuration = safeOutPoint - clip.inPointMs

    if (newDuration < MIN_CLIP_DURATION_MS) return state

    return state.updateClip(clip.id) {
        it.copy(
            outPointMs = safeOutPoint,
            durationMs = newDuration
        )
    }.recalculateDuration()
}
```

---

## 22. Split Clip Action

Purpose:

Split one clip into two clips at current playhead.

Conditions:

```text
Clip must exist.
Playhead must be inside clip.
Playhead must not be exactly at clip start.
Playhead must not be exactly at clip end.
Both resulting clips must be >= MIN_CLIP_DURATION_MS.
```

Split calculation:

```text
splitTimelineTimeMs = state.currentTimeMs

splitOffsetMs = splitTimelineTimeMs - clip.startTimeMs

splitSourceTimeMs = clip.inPointMs + splitOffsetMs
```

First clip:

```text
id: new UUID
startTimeMs: clip.startTimeMs
durationMs: splitOffsetMs
inPointMs: clip.inPointMs
outPointMs: splitSourceTimeMs
```

Second clip:

```text
id: new UUID
startTimeMs: splitTimelineTimeMs
durationMs: clip.durationMs - splitOffsetMs
inPointMs: splitSourceTimeMs
outPointMs: clip.outPointMs
```

Rules:

```text
Keep effects/keyframes as needed.
For MVP, copy effects to both clips.
Reset selection to first or second clip depending UX.
Recalculate duration.
```

Implementation skeleton:

```kotlin
private fun splitAtPlayhead(
    state: TimelineEngineState,
    action: TimelineAction.SplitAtPlayhead
): TimelineEngineState {
    val clip = state.findClip(action.clipId) ?: return state

    val playhead = state.currentTimeMs

    if (playhead <= clip.startTimeMs) return state
    if (playhead >= clip.endTimeMs) return state

    val offset = playhead - clip.startTimeMs

    if (offset < MIN_CLIP_DURATION_MS) return state
    if (clip.durationMs - offset < MIN_CLIP_DURATION_MS) return state

    val splitSourceTime = clip.inPointMs + offset

    val first = clip.copy(
        id = generateClipId(),
        durationMs = offset,
        outPointMs = splitSourceTime
    )

    val second = clip.copy(
        id = generateClipId(),
        startTimeMs = playhead,
        durationMs = clip.durationMs - offset,
        inPointMs = splitSourceTime
    )

    return state.replaceClipWithTwoClips(clip.id, first, second)
        .recalculateDuration()
}
```

---

## 23. Delete Clip Action

Purpose:

Remove clip from timeline.

Rules:

```text
Clip must exist.
Remove clip from its track.
If deleted clip was selected, clear selection.
Recalculate duration.
```

Implementation:

```kotlin
private fun deleteClip(
    state: TimelineEngineState,
    action: TimelineAction.DeleteClip
): TimelineEngineState {
    val updatedTracks = state.tracks.map { track ->
        track.copy(
            clips = track.clips.filterNot { it.id == action.clipId }
        )
    }

    val newSelection = if (state.selectedClipId == action.clipId) {
        null
    } else {
        state.selectedClipId
    }

    return state.copy(
        tracks = updatedTracks,
        selectedClipId = newSelection
    ).recalculateDuration()
}
```

---

## 24. Duplicate Clip Action

Purpose:

Copy clip and place it after original.

Rules:

```text
Duplicate must get new ID.
Duplicate starts at original.endTimeMs.
Duplicate keeps trim, effects, transform.
Duplicate may be selected after creation.
Recalculate duration.
```

Implementation:

```kotlin
private fun duplicateClip(
    state: TimelineEngineState,
    action: TimelineAction.DuplicateClip
): TimelineEngineState {
    val clip = state.findClip(action.clipId) ?: return state

    val duplicate = clip.copy(
        id = generateClipId(),
        startTimeMs = clip.endTimeMs
    )

    return state.addClipToTrackContaining(clip.id, duplicate)
        .copy(selectedClipId = duplicate.id)
        .recalculateDuration()
}
```

---

## 25. Snap Engine

Purpose:

Help clips align to meaningful positions.

Snap targets:

```text
Clip start edges
Clip end edges
Playhead
Zero
Project end optional
```

Snap threshold:

```text
8dp to 12dp converted to time
```

Convert threshold to time:

```kotlin
fun snapThresholdMs(thresholdPx: Float, pixelsPerMs: Float): Long {
    return (thresholdPx / pixelsPerMs).toLong()
}
```

Snap logic:

```kotlin
data class SnapResult(
    val adjustedTimeMs: Long,
    val snapTargetMs: Long?,
    val snapped: Boolean
)

fun snapTime(
    rawTimeMs: Long,
    targets: List<Long>,
    thresholdMs: Long
): SnapResult {
    val nearest = targets.minByOrNull {
        abs(it - rawTimeMs)
    }

    return if (nearest != null && abs(nearest - rawTimeMs) <= thresholdMs) {
        SnapResult(
            adjustedTimeMs = nearest,
            snapTargetMs = nearest,
            snapped = true
        )
    } else {
        SnapResult(
            adjustedTimeMs = rawTimeMs,
            snapTargetMs = null,
            snapped = false
        )
    }
}
```

Snap usage:

```text
During move clip:
Snap clip.startTimeMs and clip.endTimeMs.

During trim:
Snap trimmed edge to nearby clip edges.

During split:
Snap playhead optional.
```

Rules:

```text
Snap only if snappingEnabled.
Snap UI line should appear when snapped.
Do not snap every micro-movement.
Do not snap if it creates invalid clip duration.
```

---

## 26. Snap Targets Builder

Build snap targets from timeline:

```kotlin
fun buildSnapTargets(state: TimelineEngineState): List<Long> {
    val targets = mutableListOf<Long>()

    targets.add(0L)
    targets.add(state.currentTimeMs)

    state.tracks.forEach { track ->
        track.clips.forEach { clip ->
            targets.add(clip.startTimeMs)
            targets.add(clip.endTimeMs)
        }
    }

    return targets.distinct().sorted()
}
```

Optional performance optimization:

```text
Only include targets near visible range.
Only include targets on same track for clip moves.
```

---

## 27. Overlap Rules

### Option A: Allow overlap

Pros:

```text
Simple engine
Useful for overlay/text/audio tracks
```

Cons:

```text
Main video track can become confusing
```

### Option B: Prevent overlap on main track

Recommended for MVP main video track.

Rules:

```text
Clip cannot overlap another clip on same track.
Move should clamp to previous clip end and next clip start.
Trim cannot cross neighboring clip.
```

Clamp move start:

```kotlin
fun clampMoveTime(
    track: Track,
    clip: Clip,
    desiredStart: Long
): Long {
    val others = track.clips
        .filterNot { it.id == clip.id }
        .sortedBy { it.startTimeMs }

    val previous = others.lastOrNull { it.endTimeMs <= desiredStart }
    val next = others.firstOrNull { it.startTimeMs >= desiredStart + clip.durationMs }

    val minStart = previous?.endTimeMs ?: 0L
    val maxStart = next?.startTimeMs?.minus(clip.durationMs) ?: Long.MAX_VALUE

    return desiredStart.coerceIn(minStart, maxStart)
}
```

Rules:

```text
Audio/text/overlay tracks may allow overlap later.
Main video track should usually prevent overlap.
```

---

## 28. Gap Handling

Timeline may have gaps between clips.

MVP options:

```text
Option A:
Allow gaps and show empty timeline space.

Option B:
Ripple mode automatically closes gaps.
```

Recommended MVP:

```text
Allow gaps visually.
Add ripple delete/ripple edit later.
```

If gap exists:

```text
Preview should show background/black during gap.
Export should render blank or background during gap.
```

---

## 29. Project Duration Calculation

Project duration is max clip end time across tracks.

```kotlin
fun calculateDuration(tracks: List<Track>): Long {
    return tracks.flatMap { it.clips }
        .maxOfOrNull { it.endTimeMs }
        ?: 0L
}
```

Recalculate after:

```text
Add clip
Move clip
Trim start
Trim end
Split
Delete
Duplicate
```

Helper:

```kotlin
fun TimelineEngineState.recalculateDuration(): TimelineEngineState {
    val newDuration = calculateDuration(tracks)
    val safeCurrent = currentTimeMs.coerceIn(0L, newDuration)
    return copy(durationMs = newDuration, currentTimeMs = safeCurrent)
}
```

---

## 30. Active Clip Resolution

Purpose:

Find clips active at current time.

Single track helper:

```kotlin
fun Track.activeClipAt(timeMs: Long): Clip? {
    return clips.firstOrNull { clip ->
        timeMs >= clip.startTimeMs && timeMs < clip.endTimeMs
    }
}
```

Multi-track helper:

```kotlin
fun TimelineEngineState.activeClipsAt(timeMs: Long): List<Clip> {
    return tracks.mapNotNull { it.activeClipAt(timeMs) }
}
```

Preview rules:

```text
Main video track active clip is primary frame.
Overlay clips render above.
Text clips render above overlays.
Audio clips play in audio mixer.
```

---

## 31. Playhead Behavior

Playhead rules:

```text
Playhead starts at 0.
Playhead cannot be negative.
Playhead cannot exceed duration.
When duration changes, clamp playhead.
Seeking should update preview.
Scrubbing should not trigger heavy autosave.
```

Playback state:

```kotlin
data class PlaybackState(
    val isPlaying: Boolean,
    val currentTimeMs: Long,
    val durationMs: Long
)
```

During playback:

```text
Player emits current time.
Timeline updates currentTimeMs.
If playback reaches end, pause or loop depending UX.
```

Recommended:

```text
Pause at end.
```

---

## 32. Speed Handling

For speed = 1:

```text
timelineDuration = sourceOut - sourceIn
```

For speed > 0:

```text
timelineDuration = (sourceOut - sourceIn) / speed
```

Example:

```text
Source segment = 10000ms
Speed = 2.0
Timeline duration = 5000ms
```

Source time during playback:

```text
sourceTime = inPoint + (currentTime - startTime) * speed
```

Rules:

```text
Speed change recalculates duration.
Speed change should not change in/out source range.
Speed cannot be zero.
Negative speed only for reverse feature later.
```

Safe speed:

```kotlin
fun safeSpeed(speed: Float): Float {
    return if (speed <= 0f) 1.0f else speed
}
```

---

## 33. Transition Awareness

For future transitions between clips.

Transition model:

```kotlin
data class Transition(
    val id: String,
    val firstClipId: String,
    val secondClipId: String,
    val durationMs: Long,
    val type: TransitionType
)
```

Rules:

```text
Transition duration must be <= half of first clip duration.
Transition duration must be <= half of second clip duration.
Transition should not create invalid trim points.
Transition should not exceed clip overlap area.
```

Transition overlap formula:

```text
transitionStart = firstClip.endTimeMs - transitionDurationMs
transitionEnd = firstClip.endTimeMs
```

For MVP:

```text
Store transition data.
Render later in preview/export.
```

---

## 34. Undo/Redo Integration

Each structural action should be undoable.

Use command wrapper:

```kotlin
interface TimelineCommand {
    fun execute(state: TimelineEngineState): TimelineEngineState
    fun undo(state: TimelineEngineState): TimelineEngineState
}
```

Example:

```kotlin
class MoveClipCommand(
    private val clipId: String,
    private val oldStart: Long,
    private val newStart: Long
) : TimelineCommand {

    override fun execute(state: TimelineEngineState): TimelineEngineState {
        return state.moveClip(clipId, newStart)
    }

    override fun undo(state: TimelineEngineState): TimelineEngineState {
        return state.moveClip(clipId, oldStart)
    }
}
```

Recommended commands:

```text
AddClipCommand
MoveClipCommand
TrimStartCommand
TrimEndCommand
SplitClipCommand
DeleteClipCommand
DuplicateClipCommand
```

Rules:

```text
Seeking is not undoable.
Selection is not undoable.
Zoom is not undoable.
Structural edits are undoable.
```

---

## 35. Validation Rules

### Clip validation

```text
id must not be blank
startTimeMs >= 0
durationMs > 0
durationMs >= MIN_CLIP_DURATION_MS
inPointMs >= 0
outPointMs > inPointMs
speed > 0 for MVP
```

### Track validation

```text
trackId must exist
track order should be unique
clip type should be compatible with track type
```

### Project validation

```text
durationMs >= 0
currentTimeMs between 0 and durationMs
```

Validation helper:

```kotlin
fun Clip.isValid(): Boolean {
    return id.isNotBlank() &&
        startTimeMs >= 0 &&
        durationMs >= MIN_CLIP_DURATION_MS &&
        inPointMs >= 0 &&
        outPointMs > inPointMs &&
        speed > 0f
}
```

---

## 36. State Helper Functions

```kotlin
fun TimelineEngineState.findClip(clipId: String): Clip? {
    tracks.forEach { track ->
        track.clips.firstOrNull { it.id == clipId }?.let {
            return it
        }
    }
    return null
}

fun TimelineEngineState.findTrackByClipId(clipId: String): Track? {
    return tracks.firstOrNull { track ->
        track.clips.any { it.id == clipId }
    }
}

fun TimelineEngineState.updateClip(
    clipId: String,
    update: (Clip) -> Clip
): TimelineEngineState {
    val newTracks = tracks.map { track ->
        track.copy(
            clips = track.clips.map { clip ->
                if (clip.id == clipId) update(clip) else clip
            }
        )
    }
    return copy(tracks = newTracks)
}
```

---

## 37. Replace Clip with Two Clips

Used for split:

```kotlin
fun TimelineEngineState.replaceClipWithTwoClips(
    oldClipId: String,
    first: Clip,
    second: Clip
): TimelineEngineState {
    val newTracks = tracks.map { track ->
        if (track.clips.any { it.id == oldClipId }) {
            val newClips = mutableListOf<Clip>()

            track.clips.forEach { clip ->
                if (clip.id == oldClipId) {
                    newClips.add(first)
                    newClips.add(second)
                } else {
                    newClips.add(clip)
                }
            }

            track.copy(clips = newClips.sortedBy { it.startTimeMs })
        } else {
            track
        }
    }

    return copy(tracks = newTracks)
}
```

---

## 38. Timeline Metrics for UI

```kotlin
data class TimelineMetrics(
    val pixelsPerMs: Float,
    val visibleStartTimeMs: Long,
    val visibleEndTimeMs: Long,
    val viewportWidthPx: Float
)
```

Calculate visible range:

```kotlin
fun calculateVisibleRange(
    scrollX: Float,
    viewportWidthPx: Float,
    pixelsPerMs: Float
): Pair<Long, Long> {
    val start = xToTime(scrollX, pixelsPerMs)
    val end = xToTime(scrollX + viewportWidthPx, pixelsPerMs)
    return start to end
}
```

Rules:

```text
Only render clips intersecting visible range.
Keep playhead layer separate.
Keep snap line layer separate.
```

---

## 39. Performance Rules

1. Do not recalculate all clips inside Compose draw.
2. Cache clip layout rectangles when state changes.
3. Use derivedStateOf for scroll/zoom derived values.
4. Draw only visible clips.
5. Avoid object allocation in draw loops.
6. Avoid sorting large clip lists repeatedly.
7. Use stable clip IDs.
8. Keep reducer pure and fast.
9. Debounce autosave.
10. Do not save during every drag frame.

---

## 40. Rendering Optimization

Clip layout cache:

```kotlin
data class ClipLayout(
    val clipId: String,
    val x: Float,
    val width: Float,
    val trackIndex: Int
)
```

Build cache when:

```text
Zoom changes
Timeline state changes
Track order changes
Clip start/duration changes
```

Do not rebuild cache on:

```text
Simple scroll
Playhead movement
Selection change unless visual needed
```

---

## 41. Unit Test Cases

### Seek tests

```text
Seek to negative -> clamp to 0
Seek beyond duration -> clamp to duration
Seek inside timeline -> exact time
```

### Add clip tests

```text
Add clip to empty track
Add clip at specific time
Add clip after last clip
Add invalid clip rejected
```

### Move clip tests

```text
Move clip forward
Move clip backward
Move before zero clamps to zero
Move with overlap prevented
Move updates duration
```

### Trim start tests

```text
Trim start valid
Trim start cannot exceed outPoint
Trim start maintains minimum duration
Trim start updates startTimeMs
```

### Trim end tests

```text
Trim end valid
Trim end cannot be below inPoint
Trim end maintains minimum duration
Trim end updates duration
```

### Split tests

```text
Split at start does nothing
Split at end does nothing
Split in middle creates two clips
Split preserves total duration
Split calculates source in/out correctly
Split respects minimum duration
```

### Delete tests

```text
Delete existing clip
Delete selected clip clears selection
Delete recalculates duration
Delete nonexistent clip does nothing
```

### Duplicate tests

```text
Duplicate creates new ID
Duplicate starts after original
Duplicate keeps trim values
Duplicate recalculates duration
```

### Snap tests

```text
Snap when within threshold
No snap outside threshold
Snap disabled when snappingEnabled false
Snap target list contains clip edges
```

---

## 42. Example Reducer Test

```kotlin
@Test
fun `split clip in middle creates two clips`() {
    val clip = Clip(
        id = "clip1",
        type = ClipType.VIDEO,
        assetId = "asset1",
        startTimeMs = 0,
        durationMs = 5000,
        inPointMs = 0,
        outPointMs = 5000
    )

    val track = Track(
        id = "track1",
        type = TrackType.VIDEO,
        order = 0,
        clips = listOf(clip)
    )

    val state = TimelineEngineState(
        projectId = "project1",
        currentTimeMs = 2500,
        durationMs = 5000,
        zoom = 1f,
        selectedClipId = "clip1",
        tracks = listOf(track)
    )

    val reducer = TimelineReducer()

    val newState = reducer.reduce(
        state,
        TimelineAction.SplitAtPlayhead("clip1")
    )

    val clips = newState.tracks.first().clips

    assert(clips.size == 2)
    assert(clips[0].durationMs == 2500L)
    assert(clips[1].durationMs == 2500L)
    assert(clips[1].startTimeMs == 2500L)
}
```

---

## 43. UI Integration

Timeline UI should receive:

```kotlin
data class TimelineRenderState(
    val currentTimeMs: Long,
    val durationMs: Long,
    val pixelsPerMs: Float,
    val selectedClipId: String?,
    val tracks: List<TrackUiModel>,
    val snapLineTimeMs: Long?,
    val visibleStartTimeMs: Long,
    val visibleEndTimeMs: Long
)
```

UI responsibilities:

```text
Draw clips
Draw playhead
Handle gestures
Convert gestures to actions
Send actions to ViewModel/engine
```

UI must not do:

```text
Trim math
Split math
Duration recalculation
Snap final decision
Overlap validation
```

---

## 44. Gesture Mapping

```text
Tap clip:
TimelineAction.SelectClip

Tap empty timeline:
TimelineAction.SelectClip(null)

Drag playhead:
TimelineAction.Seek

Drag clip body:
TimelineAction.MoveClip

Drag left handle:
TimelineAction.TrimStart

Drag right handle:
TimelineAction.TrimEnd

Double tap clip optional:
Open clip tools

Long press clip optional:
Multi-select mode later
```

---

## 45. Drag State

Temporary drag state should live in UI, not engine.

```kotlin
data class DragState(
    val clipId: String,
    val dragType: DragType,
    val originalStartTimeMs: Long,
    val originalInPointMs: Long,
    val originalOutPointMs: Long,
    val currentDragTimeMs: Long
)

enum class DragType {
    MOVE,
    TRIM_START,
    TRIM_END,
    PLAYHEAD
}
```

Rules:

```text
During drag, show preview position.
On drag end, commit final action.
If cancelled, restore original visual state.
```

---

## 46. Autosave Interaction

Timeline engine should not directly save.

Flow:

```text
User action
    â†“
ViewModel sends action
    â†“
Reducer updates state
    â†“
ViewModel updates database with debounce
```

Do not autosave:

```text
Seek
Zoom
Selection
Drag preview frames
```

Autosave after:

```text
Add
Move
Trim
Split
Delete
Duplicate
Text edit
Effect change
```

---

## 47. Edge Cases

### Split edge cases

```text
Split at clip start: ignore
Split at clip end: ignore
Split too close to start: ignore
Split too close to end: ignore
Split clip with speed: calculate source time carefully
```

### Trim edge cases

```text
Trim to zero: block
Trim beyond source duration: clamp
Trim start past end: block
Trim end before start: block
Trim while neighboring clip exists: clamp if overlap prevented
```

### Move edge cases

```text
Move before zero: clamp
Move beyond duration: allowed, duration extends
Move into overlap: clamp or allow based track rule
Move missing clip: ignore
```

### Empty timeline

```text
Duration = 0
Playhead = 0
No active clips
UI shows empty state
```

### Missing media

```text
Clip remains in timeline
Show warning state
Preview shows placeholder
Export blocks if media required and missing
```

---

## 48. Future Expansion

The engine should be ready for:

```text
Multi-track overlay
Audio mixing
Keyframe animation
Masks
Blend modes
Transitions
Ripple edit
Magnetic timeline
Grouped clips
Parent-child layers
```

Do not implement these in MVP unless necessary.

---

## 49. Implementation Checklist

```text
[ ] TimelineEngineState created
[ ] Track model created
[ ] Clip model created
[ ] Time utilities created
[ ] Pixel/time conversion created
[ ] Zoom calculation created
[ ] Reducer created
[ ] Load project action created
[ ] Seek action created
[ ] Select clip action created
[ ] Add clip action created
[ ] Move clip action created
[ ] Trim start action created
[ ] Trim end action created
[ ] Split action created
[ ] Delete action created
[ ] Duplicate action created
[ ] Snap engine created
[ ] Duration recalculation created
[ ] Active clip resolution created
[ ] Validation helpers created
[ ] Undo/redo commands connected
[ ] Unit tests written
[ ] UI gesture mapping defined
[ ] Autosave integration defined
```

---

## 50. AI Prompts

### Generate timeline state

```text
Use this TIMELINE_ENGINE_SPEC.md as source of truth. Generate pure Kotlin timeline engine state models for TimelineEngineState, Track, Clip, TimelineAction, and TimelineUiState.
```

### Generate reducer

```text
Use this TIMELINE_ENGINE_SPEC.md as source of truth. Generate TimelineReducer with load project, seek, select clip, add clip, move clip, trim start, trim end, split, delete, duplicate, zoom, and snap actions.
```

### Generate trim/split logic

```text
Use this TIMELINE_ENGINE_SPEC.md as source of truth. Generate trim start, trim end, and split clip logic with validation, minimum duration rules, and immutable state updates.
```

### Generate snap engine

```text
Use this TIMELINE_ENGINE_SPEC.md as source of truth. Generate snap engine with snap targets, threshold calculation, and SnapResult.
```

### Generate unit tests

```text
Use this TIMELINE_ENGINE_SPEC.md as source of truth. Generate unit tests for timeline engine seek, move, trim, split, delete, duplicate, snap, and duration calculation.
```

---

## 51. Final Rule

The timeline engine must always satisfy:

```text
Correct time math
Stable state updates
Safe edge-case handling
Undo-friendly actions
UI-independent logic
Testable behavior
```

If any feature conflicts with these rules, prioritize correctness and stability over visual complexity.

---

<!-- END TIMELINE_ENGINE_SPEC.md -->