---
title: Export Pipeline Specification
version: 1.0
related: PRD.md, ARCHITECTURE.md, DATABASE_SCHEMA.md, TIMELINE_ENGINE_SPEC.md
status: Ready for Implementation
platform: Android
---

# Export Pipeline Specification

## 1. Purpose

This document defines the complete export pipeline for the Android video editing app.

The export pipeline is responsible for converting the edited project into a final MP4 video file.

It handles:

- Project validation
- Export settings
- Frame rendering
- Video encoding
- Audio encoding
- Audio/video muxing
- Progress reporting
- Cancellation
- Error handling
- Saving output to gallery
- Background export execution

---

## 2. Export Goals

The export system must:

1. Produce a valid MP4 file.
2. Respect timeline trim, split, move, and duration.
3. Keep audio/video synchronized.
4. Run in background without freezing UI.
5. Show accurate progress.
6. Support cancellation.
7. Handle errors gracefully.
8. Save output using MediaStore.
9. Work on a wide range of Android devices.
10. Be upgradeable from Media3 export to OpenGL advanced export.

---

## 3. Non-Goals for MVP

Initial export does not need to support:

- Complex OpenGL shader effects
- Masks
- Blend modes
- Keyframe rendering
- 4K export
- HDR export
- Multi-pass render farm
- Cloud rendering
- Custom codec selection beyond basic options

These can be added later.

---

## 4. Export Modes

### Mode A: MVP Export

Recommended for initial version.

```text
Media3 Transformer based export
```

Best for:

- Basic trim/split/concatenation
- Simple speed changes
- Basic audio handling
- Faster implementation

### Mode B: Advanced Export

Recommended later.

```text
OpenGL renderer + MediaCodec encoder + MediaMuxer
```

Best for:

- Filters/effects
- Text overlays
- Stickers
- Transform animation
- Keyframes
- Masks
- Blend modes

### Mode C: FFmpeg Export

Optional.

```text
FFmpeg filter graph based export
```

Best for:

- Powerful filter graphs
- Complex conversions

Downsides:

- APK size
- Licensing complexity
- Native build maintenance

Recommendation:

```text
Start with Media3 Transformer.
Move to OpenGL + MediaCodec when effects/keyframes become required.
Use FFmpeg only if necessary.
```

---

## 5. High-Level Architecture

```text
Editor UI
    â†“
ExportViewModel
    â†“
ExportWorker
    â†“
ProjectExporter
    â†“
ProjectRenderer
    â†“
VideoEncoder + AudioEncoder
    â†“
MediaMuxer / Transformer output
    â†“
MediaStore save
    â†“
Success / Failure UI
```

Main components:

```text
ExportSettings
ExportValidator
ProjectExporter
ProjectRenderer
VideoEncoder
AudioEncoder
Muxer
ProgressReporter
CancelHandler
OutputWriter
```

---

## 6. Export Flow

```text
1. User taps Export
2. Export screen opens
3. User selects resolution/fps/quality
4. User taps Start Export
5. Project is validated
6. Output file is prepared
7. ExportWorker starts
8. Timeline is rendered frame-by-frame or segment-by-segment
9. Video frames are encoded
10. Audio is decoded/encoded or passed through
11. Video/audio are muxed
12. File is finalized
13. File is saved to MediaStore
14. Success state is shown
```

---

## 7. Export Settings

### ExportSettings Model

```kotlin
data class ExportSettings(
    val width: Int,
    val height: Int,
    val fps: Int,
    val videoBitrate: Int,
    val audioBitrate: Int,
    val format: OutputFormat,
    val videoCodec: VideoCodec,
    val audioCodec: AudioCodec
)

enum class OutputFormat {
    MP4
}

enum class VideoCodec {
    H264,
    H265
}

enum class AudioCodec {
    AAC
}
```

### Recommended MVP Options

```text
Resolution:
720p
1080p

FPS:
30fps default
60fps optional

Video codec:
H.264 default
H.265 optional later

Audio codec:
AAC

Container:
MP4
```

### Recommended Bitrates

| Resolution | FPS | Suggested Video Bitrate |
|---|---:|---:|
| 720p | 30 | 5 Mbps |
| 720p | 60 | 8 Mbps |
| 1080p | 30 | 10 Mbps |
| 1080p | 60 | 16 Mbps |

These are starting values. Adjust based on quality testing.

---

## 8. Aspect Ratio Rules

Common project ratios:

```text
9:16 = 1080 x 1920
16:9 = 1920 x 1080
1:1 = 1080 x 1080
4:5 = 1080 x 1350
```

Rules:

```text
Export resolution should match project aspect ratio.
If user selects mismatched resolution, either block or auto-adjust.
Prefer letterbox/pillarbox only if explicitly supported.
```

Recommended MVP:

```text
Use project aspect ratio and preset resolution together.
Do not allow arbitrary mismatched resolution.
```

---

## 9. Project Validation Before Export

Before export starts, validate:

```text
Project exists
Project has at least one clip
All required assets exist
No missing media for video/image clips
Duration is greater than zero
Export settings are valid
Enough storage available
Required permissions granted
```

Validation result:

```kotlin
sealed class ExportValidationResult {
    object Valid : ExportValidationResult()
    data class Invalid(val reason: ExportValidationError) : ExportValidationResult()
}

enum class ExportValidationError {
    EMPTY_PROJECT,
    MISSING_MEDIA,
    UNSUPPORTED_MEDIA,
    LOW_STORAGE,
    INVALID_RESOLUTION,
    PERMISSION_DENIED
}
```

UI rules:

```text
Disable export button for empty project.
Show clear error for missing media.
Show storage warning before export.
```

---

## 10. Export State

```kotlin
sealed class ExportState {
    object Idle : ExportState()
    object Validating : ExportState()
    object Preparing : ExportState()
    data class Exporting(val progress: Float) : ExportState()
    data class Success(val outputUri: Uri) : ExportState()
    data class Error(val message: String, val cause: Throwable?) : ExportState()
    object Cancelled : ExportState()
}
```

Progress rules:

```text
Progress range: 0.0 to 1.0
Show percentage: progress * 100
Update progress at least every 500ms if possible
Do not update progress on main thread directly
```

---

## 11. Output File Strategy

### App-private temp file

Use app cache for temporary export:

```text
cacheDir/exports/export_<projectId>_<timestamp>.mp4
```

Why:

```text
No permission needed while writing temp file
Safer during export
Can move to MediaStore after success
```

### Final output

After export completes:

```text
Insert file into MediaStore.Video.Media
Copy temp file to MediaStore output URI
Delete temp file after successful copy
```

Rules:

```text
Do not leave large temp files after success/failure if possible.
Handle cleanup on failure.
```

---

## 12. MediaStore Saving

Use MediaStore on Android 10+.

Example fields:

```kotlin
val values = ContentValues().apply {
    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/VideoEditor")
    put(MediaStore.Video.Media.IS_PENDING, 1)
}
```

Flow:

```text
1. Insert MediaStore item
2. Open output stream
3. Copy exported file
4. Set IS_PENDING = 0
```

Rules:

```text
Use IS_PENDING to avoid showing incomplete file.
Handle SecurityException gracefully.
Use background thread for file copy.
```

---

## 13. Background Execution

Export must not run on main thread.

Use:

```text
WorkManager CoroutineWorker
```

Why WorkManager:

```text
Survives process recreation
Supports constraints
Supports progress
Supports cancellation
Good for long user-initiated task
```

Worker input:

```text
projectId
width
height
fps
videoBitrate
audioBitrate
```

Worker output:

```text
Success
Failure
Cancelled
Progress
```

---

## 14. ExportWorker Design

```kotlin
class ExportWorker(
    private val context: Context,
    private val params: WorkerParameters,
    private val exporter: ProjectExporter
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val projectId = inputData.getString(KEY_PROJECT_ID) ?: return Result.failure()

        setProgress(workDataOf(KEY_PROGRESS to 0f))

        return try {
            val result = exporter.export(
                projectId = projectId,
                settings = readSettings(inputData),
                onProgress = { progress ->
                    setProgress(workDataOf(KEY_PROGRESS to progress))
                }
            )

            when (result) {
                is ExportResult.Success -> Result.success(
                    workDataOf(KEY_OUTPUT_URI to result.uri.toString())
                )
                is ExportResult.Failure -> Result.failure(
                    workDataOf(KEY_ERROR to result.message)
                )
                ExportResult.Cancelled -> Result.failure(
                    workDataOf(KEY_ERROR to "Cancelled")
                )
            }
        } catch (e: Exception) {
            Result.failure(workDataOf(KEY_ERROR to e.message))
        }
    }
}
```

Constants:

```text
KEY_PROJECT_ID
KEY_WIDTH
KEY_HEIGHT
KEY_FPS
KEY_VIDEO_BITRATE
KEY_AUDIO_BITRATE
KEY_PROGRESS
KEY_OUTPUT_URI
KEY_ERROR
```

---

## 15. Export Cancellation

Cancellation sources:

```text
User taps cancel
App clears export task
System stops worker
```

Implementation rules:

```text
Check cancellation regularly during frame loop.
Stop encoder cleanly if possible.
Delete incomplete temp file.
Mark state as Cancelled.
Do not show success.
```

Cancellation check:

```kotlin
if (isStopped) {
    return ExportResult.Cancelled
}
```

For custom frame loop:

```kotlin
for (frameIndex in frames) {
    if (coroutineContext.isActive.not()) {
        cleanup()
        return ExportResult.Cancelled
    }

    renderFrame()
    encodeFrame()
    updateProgress()
}
```

---

## 16. Progress Calculation

### Segment-based progress

If exporting clip-by-clip:

```text
progress = completedSegmentDurationMs / totalProjectDurationMs
```

### Frame-based progress

If rendering frame-by-frame:

```text
progress = renderedFrameIndex / totalFrames
```

Total frames:

```text
totalFrames = ceil(durationMs / 1000 * fps)
```

Example:

```text
Duration = 10 seconds
FPS = 30
Total frames = 300
```

Rules:

```text
Do not jump from 0 to 100 instantly.
Show preparing state before real progress.
Keep progress monotonic.
```

---

## 17. MVP Export Using Media3 Transformer

Media3 Transformer can be used for basic export.

Suitable MVP operations:

```text
Trim
Concatenate clips
Speed changes
Basic audio handling
Simple output configuration
```

High-level flow:

```text
Create Transformer
Create EditedMediaItem sequence for timeline clips
Set resolution/bitrate if supported
Start export
Listen for completion/error
```

Important:

```text
Media3 APIs evolve.
Check current Media3 Transformer documentation.
Keep exporter behind interface so implementation can change.
```

Exporter interface:

```kotlin
interface ProjectExporter {
    suspend fun export(
        projectId: String,
        settings: ExportSettings,
        onProgress: (Float) -> Unit
    ): ExportResult
}
```

Result:

```kotlin
sealed class ExportResult {
    data class Success(val uri: Uri) : ExportResult()
    data class Failure(val message: String, val cause: Throwable? = null) : ExportResult()
    object Cancelled : ExportResult()
}
```

---

## 18. Timeline to Export Segments

Convert project timeline into export segments.

```kotlin
data class ExportSegment(
    val assetUri: Uri,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val speed: Float,
    val volume: Float
)
```

Rules:

```text
Only include active clips in timeline order.
Handle gaps by inserting blank segment or background if needed.
For image clips, create duration-based media segment.
For text-only MVP, may need overlay renderer later.
```

Example:

```text
Project timeline:
Clip A: 0 to 5000
Clip B: 5000 to 8000

Export segments:
Segment A: source in/out 0-5000
Segment B: source in/out based on trim
```

---

## 19. Gap Handling

If timeline has gaps:

Option A:

```text
Render black/background frame for gap duration.
```

Option B:

```text
Ripple export ignoring gaps.
```

Recommended MVP:

```text
Use Option A if visual timeline accuracy matters.
Use Option B if building simple concat exporter.
```

If using Media3:

```text
Gap handling may require generated silent/video segment or custom composition.
```

---

## 20. Audio Export

Audio requirements:

```text
Audio clips should play at correct timeline position.
Video clips with audio should include audio unless muted.
Volume should be respected.
Fade in/out later.
Audio sample rate and channel config should be stable.
```

Audio options:

```text
1. Decode and re-encode to AAC
2. Passthrough if format compatible
3. Mix multiple audio tracks
```

MVP recommendation:

```text
Re-encode to AAC for reliability.
```

Audio sync rule:

```text
Audio presentation timestamp must match video timeline time.
```

---

## 21. Audio/Video Sync

Sync issues happen when:

```text
Timestamps are wrong
Frame duration is wrong
Audio sample count mismatch
Encoder start time not aligned
Variable frame rate input
```

Rules:

```text
Use milliseconds or microseconds consistently.
Generate video frames at fixed FPS.
Use proper presentation timestamps.
Do not drop audio randomly.
Normalize variable frame rate during export if needed.
```

Video frame timestamp:

```text
frameTimeUs = frameIndex * 1_000_000 / fps
```

Example for 30fps:

```text
Frame duration = 33.33ms
Frame duration = 33333 microseconds
```

---

## 22. Advanced Export Using OpenGL + MediaCodec

For effects, text, transforms, and keyframes.

Pipeline:

```text
For each output frame time:
    1. Find active clips
    2. Decode source frame if video/image
    3. Upload frame to texture
    4. Apply transform
    5. Apply effects
    6. Draw layers bottom to top
    7. Render to encoder input Surface
    8. Submit frame with timestamp
```

Components:

```text
EglCore
SurfaceTexture
TextureRenderer
LayerComposer
ShaderEffectChain
FrameScheduler
MediaCodecVideoEncoder
AudioEncoder
MediaMuxer
```

---

## 23. OpenGL Export Frame Loop

Pseudo-code:

```kotlin
val totalFrames = calculateTotalFrames(durationMs, fps)

for (frameIndex in 0 until totalFrames) {
    if (isCancelled) break

    val timeMs = frameIndex * 1000L / fps

    renderer.beginFrame()
    renderer.clear()

    val activeLayers = project.getActiveLayersAt(timeMs)

    for (layer in activeLayers) {
        renderer.drawLayer(layer, timeMs)
    }

    renderer.endFrame()
    encoder.queueFrame(frameIndex, timeMs)

    onProgress(frameIndex.toFloat() / totalFrames)
}

encoder.finish()
muxer.stop()
```

Rules:

```text
Rendering thread separate from UI thread.
Use EGL surface connected to MediaCodec.
Release EGL resources properly.
Handle decoder end-of-stream.
```

---

## 24. MediaCodec Encoder Setup

Video encoder:

```text
MIME: video/avc for H.264
Width: export width
Height: export height
Bitrate: export bitrate
FPS: export fps
Color format: Surface
```

Example:

```kotlin
val format = MediaFormat.createVideoFormat("video/avc", width, height).apply {
    setInteger(
        MediaFormat.KEY_COLOR_FORMAT,
        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
    )
    setInteger(MediaFormat.KEY_BIT_RATE, videoBitrate)
    setInteger(MediaFormat.KEY_FRAME_RATE, fps)
    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
}
```

Rules:

```text
Use hardware encoder when available.
Fallback to software encoder if needed.
Test on multiple devices.
```

---

## 25. MediaMuxer

Muxer combines encoded video/audio.

```text
Output container: MP4
Video track added after encoder format determined
Audio track added after audio encoder format determined
Write sample buffers with timestamps
Stop muxer after all tracks finish
```

Rules:

```text
Start muxer only after all tracks added.
Do not write samples before muxer start.
Handle stop/release safely.
```

---

## 26. Text and Sticker Export

For MVP, text may be preview-only unless export supports overlays.

To export text:

```text
Render text to bitmap/texture
Draw text layer during export frame loop
Use same position/scale as preview
```

Rules:

```text
Preview and export should use same layout math.
Text resolution should match export resolution.
Avoid blurry text by rendering at output size.
```

---

## 27. Filters/Effects Export

For basic filters:

```text
Apply during OpenGL render pass.
```

For Media3 MVP:

```text
Use supported Media3 effects if available.
Otherwise defer advanced filters to OpenGL phase.
```

Effect consistency rule:

```text
Effect parameters must be stored in project and consumed by both preview and export.
```

---

## 28. Speed Export

Speed changes affect duration and timestamps.

Example:

```text
Source segment = 10s
Speed = 2x
Output timeline segment = 5s
```

Rules:

```text
Audio pitch handling depends product choice.
For MVP, use Media3 speed handling if possible.
For custom export, resample audio or adjust timestamps.
```

---

## 29. Export Notifications

Use notification for long export.

Notification content:

```text
Title: Exporting video
Text: percentage or progress bar
Cancel action
Success notification after completion
Failure notification after error
```

Rules:

```text
Use POST_NOTIFICATIONS permission on Android 13+.
Do not spam notifications.
Update progress at reasonable interval.
```

---

## 30. Permissions

For Android 13+ media read:

```text
READ_MEDIA_VIDEO
READ_MEDIA_AUDIO
READ_MEDIA_IMAGES
```

For notifications:

```text
POST_NOTIFICATIONS
```

For writing output:

```text
Use MediaStore, no WRITE_EXTERNAL_STORAGE on modern Android.
```

Rules:

```text
Request permissions before export if needed.
Handle denied state with clear UI.
```

---

## 31. Storage Estimation

Optional but useful.

Estimate output size:

```text
estimatedBytes = (videoBitrate + audioBitrate) / 8 * durationSeconds
```

Example:

```text
10 Mbps video + 192 kbps audio
Duration = 60s

Estimated size â‰ˆ (10,000,000 + 192,000) / 8 * 60
â‰ˆ 76 MB
```

Rules:

```text
Show approximate size.
Check available storage before export.
Add safety margin.
```

---

## 32. Error Handling

Common errors:

```text
Missing media file
Unsupported codec
Decoder failure
Encoder failure
Low storage
Permission denied
Export cancelled
Output write failure
Muxer failure
Project invalid
```

Error model:

```kotlin
data class ExportErrorInfo(
    val userMessage: String,
    val technicalMessage: String?,
    val retryAllowed: Boolean
)
```

UI rules:

```text
Show friendly message.
Do not show raw stack trace.
Provide retry where useful.
Do not lose project data.
```

---

## 33. Retry Strategy

Allow retry for:

```text
Temporary encoder failure
Low storage after user frees space
Output write failure
```

Do not auto-retry endlessly for:

```text
Unsupported media
Invalid project
Permission denied
```

---

## 34. Device Compatibility

Test export on:

```text
Low-end device
Mid-range device
High-end device
Different Android versions
Different chipsets
```

Test cases:

```text
720p export
1080p export
30fps export
Long project export
Short project export
Project with image clips
Project with video clips
Project with audio clips
Project with gaps
App minimized during export
Screen off during export
Low storage
Cancel during export
```

---

## 35. Performance Rules

1. Do not export on main thread.
2. Use hardware encoder where possible.
3. Avoid decoding full-resolution frames if downscaling.
4. Reuse buffers where possible.
5. Avoid excessive progress updates.
6. Use temp file streaming, not in-memory output.
7. Release codecs promptly.
8. Handle memory pressure.
9. Use coroutine cancellation.
10. Benchmark on real devices.

---

## 36. Export Quality Rules

```text
Default H.264 for compatibility.
Use AAC audio.
Use MP4 container.
Do not over-compress below acceptable quality.
Keep I-frame interval around 1 second.
Use constant bitrate or constrained VBR.
```

---

## 37. Export Lifecycle

```text
Idle
    â†“
Validating
    â†“
Preparing
    â†“
Exporting
    â†“
Finalizing
    â†“
Success / Error / Cancelled
```

State transitions:

```text
Idle -> Validating:
User taps export.

Validating -> Preparing:
Validation success.

Validating -> Error:
Validation fails.

Preparing -> Exporting:
Encoder/output ready.

Exporting -> Success:
All frames/audio processed.

Exporting -> Error:
Exception occurs.

Exporting -> Cancelled:
User cancels.
```

---

## 38. ExportViewModel Design

```kotlin
data class ExportUiState(
    val exportState: ExportState = ExportState.Idle,
    val settings: ExportSettings = ExportSettings.default(),
    val estimatedSizeBytes: Long? = null,
    val canExport: Boolean = false
)
```

ViewModel responsibilities:

```text
Load project
Validate project
Update export settings
Start export worker
Observe worker progress
Handle cancel
Show result
```

Rules:

```text
ViewModel should not run export directly.
ViewModel should observe WorkManager.
Export logic stays in exporter/worker.
```

---

## 39. Export UI Requirements

Export screen should show:

```text
Project thumbnail
Project duration
Resolution selector
FPS selector
Quality selector
Estimated file size
Export button
Progress bar
Cancel button
Success actions
Error message
```

Success actions:

```text
Open video
Share video
Go to gallery
Back to editor
```

Error actions:

```text
Retry
Back
```

---

## 40. Testing Plan

### Unit Tests

Test:

```text
Export validation
Estimated size calculation
Progress calculation
Total frames calculation
Segment building
Gap handling
```

### Integration Tests

Test:

```text
Export empty project blocked
Export missing media blocked
Export worker starts
Export worker emits progress
Export worker cancellation
Output file created
MediaStore insertion
```

### Device Tests

Test:

```text
720p 30fps
1080p 30fps
1080p 60fps
Image-only project
Video-only project
Audio-only project
Mixed project
Trimmed project
Split project
Moved clips project
Speed clip project
App minimized during export
Screen off during export
Low storage
Unsupported codec
```

---

## 41. Example Test Cases

### Empty project

```text
Given project has no clips
When export starts
Then export is blocked with EMPTY_PROJECT error
```

### Missing media

```text
Given clip references missing asset
When export starts
Then export is blocked with MISSING_MEDIA error
```

### Progress

```text
Given 10 second project at 30fps
When 150 frames exported
Then progress is approximately 0.5
```

### Cancellation

```text
Given export is running
When user cancels
Then worker stops
And temp file cleaned
And UI shows Cancelled
```

### Success

```text
Given valid project
When export completes
Then MP4 exists
And file playable
And MediaStore item visible
```

---

## 42. Implementation Checklist

```text
[ ] ExportSettings model created
[ ] ExportState model created
[ ] ExportValidationResult created
[ ] Project validation implemented
[ ] Temp file strategy implemented
[ ] MediaStore save implemented
[ ] ExportWorker created
[ ] ExportViewModel created
[ ] Export UI created
[ ] Progress observation working
[ ] Cancellation working
[ ] Error handling working
[ ] Success state working
[ ] Media3 exporter implemented
[ ] Segment builder implemented
[ ] Audio handling implemented
[ ] Notifications optional added
[ ] Device testing completed
[ ] Cleanup temp files completed
[ ] Export analytics optional added
```

---

## 43. Migration Path

### Phase 1

```text
Media3 Transformer export
Basic clips
Trim/split/concatenate
Basic audio
```

### Phase 2

```text
Text overlay export
Basic filters export
Sticker export
```

### Phase 3

```text
OpenGL renderer
MediaCodec encoder
MediaMuxer
Effects stack
```

### Phase 4

```text
Keyframes
Masks
Blend modes
Advanced transitions
High-quality export presets
```

---

## 44. AI Prompts

### Generate export models

```text
Use this EXPORT_PIPELINE_SPEC.md as source of truth. Generate ExportSettings, ExportState, ExportResult, ExportValidationResult, and ExportSegment models in Kotlin.
```

### Generate ExportWorker

```text
Use this EXPORT_PIPELINE_SPEC.md as source of truth. Generate ExportWorker using WorkManager with progress, cancellation, error handling, and input/output data.
```

### Generate ExportViewModel

```text
Use this EXPORT_PIPELINE_SPEC.md as source of truth. Generate ExportViewModel with settings state, validation, worker observation, progress, cancel, and result handling.
```

### Generate Media3 exporter

```text
Use this EXPORT_PIPELINE_SPEC.md as source of truth. Generate a basic Media3 Transformer exporter that exports trimmed and concatenated clips to MP4.
```

### Generate OpenGL exporter architecture

```text
Use this EXPORT_PIPELINE_SPEC.md as source of truth. Generate advanced export architecture using OpenGL ES, MediaCodec, and MediaMuxer for frame-by-frame project rendering.
```

### Generate export tests

```text
Use this EXPORT_PIPELINE_SPEC.md as source of truth. Generate unit and integration test cases for export validation, progress, cancellation, and success flow.
```

---

## 45. Final Export Rules

Follow these strictly:

```text
1. Never export on main thread.
2. Never show incomplete file in gallery.
3. Never corrupt user project if export fails.
4. Always validate before export.
5. Always show progress.
6. Always allow cancellation.
7. Always handle missing media.
8. Always cleanup temp files.
9. Always test on real devices.
10. Keep exporter behind interface for future upgrades.
```

---

<!-- END EXPORT_PIPELINE_SPEC.md -->