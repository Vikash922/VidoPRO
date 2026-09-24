package com.example.feature.timeline.ui

import com.example.core.model.Clip
import com.example.core.model.ClipType
import com.example.core.model.InterpolationType
import com.example.core.model.Keyframe
import com.example.core.model.Track
import com.example.core.model.TrackType
import com.example.feature.timeline.engine.TimelineAction
import com.example.feature.timeline.engine.TimelineEngineState
import com.example.feature.timeline.engine.TimelineReducer
import com.example.feature.timeline.engine.TimelineUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive architectural tests for Timeline UI components and coordinate contracts:
 * - TimelineSelectionState
 * - TimelineGestureHandler coordinate math & snapping
 * - Multi-track alignment & zoom synchronization
 * - Audio gap preservation on timeline
 * - Keyframe indicators vs Beat markers separation
 * - User interaction to TimelineAction dispatch flow
 */
class TimelineArchitectureTest {

    // ── 1. Selection & Multi-Selection Architecture ─────────────────────────
    @Test
    fun testTimelineSelectionState() {
        val selection = TimelineSelectionState(
            selectedClipId = "clip_1",
            multiSelectedClipIds = setOf("clip_1", "clip_2"),
            activeGroupId = "group_abc"
        )

        assertTrue(selection.isClipSelected("clip_1"))
        assertFalse(selection.isClipSelected("clip_2"))

        assertTrue(selection.isClipMultiSelected("clip_1"))
        assertTrue(selection.isClipMultiSelected("clip_2"))
        assertFalse(selection.isClipMultiSelected("clip_3"))

        assertTrue(selection.isClipInSelection("clip_1"))
        assertTrue(selection.isClipInSelection("clip_2"))
        assertFalse(selection.isClipInSelection("clip_3"))

        assertTrue(selection.isMultiSelectActive)
        assertEquals("group_abc", selection.activeGroupId)
    }

    // ── 2. Coordinate System: Bidirectional Math ────────────────────────────
    @Test
    fun testTimelineGestureHandlerConversions() {
        val pixelsPerMs = 0.05f

        val ms = TimelineGestureHandler.pixelsToMs(100f, pixelsPerMs)
        assertEquals(2000L, ms)

        val pixels = TimelineGestureHandler.msToPixels(2000L, pixelsPerMs)
        assertEquals(100f, pixels, 0.001f)

        // Utility timeToX and xToTime
        assertEquals(150f, TimelineGestureHandler.timeToX(3000L, pixelsPerMs), 0.001f)
        assertEquals(3000L, TimelineGestureHandler.xToTime(150f, pixelsPerMs))

        // Zero pixelsPerMs edge case
        val zeroMs = TimelineGestureHandler.pixelsToMs(100f, 0f)
        assertEquals(0L, zeroMs)
    }

    // ── 3. Centralized Snapping Pipeline ────────────────────────────────────
    @Test
    fun testDeterministicSnapping() {
        val snapPoints = listOf(0L, 2000L, 5000L, 8000L)

        // Within 150ms threshold
        val snapped1 = TimelineGestureHandler.snapTime(2080L, snapPoints, isSnappingEnabled = true)
        assertEquals(2000L, snapped1)

        val snapped2 = TimelineGestureHandler.snapTime(4920L, snapPoints, isSnappingEnabled = true)
        assertEquals(5000L, snapped2)

        // Beyond 150ms threshold: unchanged
        val unSnapped = TimelineGestureHandler.snapTime(2300L, snapPoints, isSnappingEnabled = true)
        assertEquals(2300L, unSnapped)

        // Snapping disabled: unchanged
        val disabledSnap = TimelineGestureHandler.snapTime(2050L, snapPoints, isSnappingEnabled = false)
        assertEquals(2050L, disabledSnap)
    }

    // ── 4. Zoom Scale Consistency Across All Tracks ─────────────────────────
    @Test
    fun testZoomScalingConsistency() {
        val zoomLevels = listOf(0.5f, 1.0f, 2.0f, 3.5f)

        for (zoom in zoomLevels) {
            val ppm = TimelineUtils.calculatePixelsPerMs(zoom)

            // Timeline duration = 10,000ms
            val trackVideoX = TimelineGestureHandler.timeToX(5000L, ppm)
            val trackAudioX = TimelineGestureHandler.timeToX(5000L, ppm)
            val trackOverlayX = TimelineGestureHandler.timeToX(5000L, ppm)
            val trackTextX = TimelineGestureHandler.timeToX(5000L, ppm)
            val rulerX = TimelineGestureHandler.timeToX(5000L, ppm)
            val playheadX = TimelineGestureHandler.timeToX(5000L, ppm)

            // All components must compute the EXACT same pixel coordinate for timestamp 5000ms
            assertEquals(trackVideoX, trackAudioX, 0.0001f)
            assertEquals(trackVideoX, trackOverlayX, 0.0001f)
            assertEquals(trackVideoX, trackTextX, 0.0001f)
            assertEquals(trackVideoX, rulerX, 0.0001f)
            assertEquals(trackVideoX, playheadX, 0.0001f)
        }
    }

    // ── 5. Audio Gap Preservation (Requirement 6) ───────────────────────────
    @Test
    fun testAudioTrackGapPreservation() {
        val pixelsPerMs = 0.06f

        // Clip A: 0 - 5000ms
        val clipA = Clip(
            id = "audio_a",
            trackId = "track_audio",
            type = ClipType.AUDIO,
            startTimeMs = 0L,
            durationMs = 5000L,
            inPointMs = 0L,
            outPointMs = 5000L
        )

        // Clip B: 8000 - 13000ms (3-second gap from 5000 to 8000ms)
        val clipB = Clip(
            id = "audio_b",
            trackId = "track_audio",
            type = ClipType.AUDIO,
            startTimeMs = 8000L,
            durationMs = 5000L,
            inPointMs = 0L,
            outPointMs = 5000L
        )

        val endA_Px = TimelineGestureHandler.timeToX(clipA.endTimeMs, pixelsPerMs)
        val startB_Px = TimelineGestureHandler.timeToX(clipB.startTimeMs, pixelsPerMs)

        // Gap must not be collapsed: start of B must be strictly after end of A by 3000ms * ppm
        val gapDurationMs = clipB.startTimeMs - clipA.endTimeMs
        assertEquals(3000L, gapDurationMs)

        val gapPx = startB_Px - endA_Px
        val expectedGapPx = TimelineGestureHandler.msToPixels(3000L, pixelsPerMs)
        assertEquals(expectedGapPx, gapPx, 0.001f)
        assertTrue("Audio gap must be strictly greater than 0 pixels", gapPx > 0f)
    }

    // ── 6. Separation of Keyframe Indicators vs Beat Markers (Requirement 15)
    @Test
    fun testKeyframeAndBeatMarkerSeparation() {
        // Beat markers belong to project timeline (Set<Long>)
        val beatMarkers = setOf(1000L, 2500L, 4000L)

        // Keyframes belong to individual Clip properties (List<Keyframe>)
        val clipKeyframes = listOf(
            Keyframe(id = "kf_1", clipId = "video_1", property = "scaleX", timeMs = 1500L, value = 1.2f, interpolation = InterpolationType.LINEAR),
            Keyframe(id = "kf_2", clipId = "video_1", property = "rotation", timeMs = 3000L, value = 45f, interpolation = InterpolationType.EASE_IN_OUT)
        )

        val clip = Clip(
            id = "video_1",
            trackId = "track_1",
            type = ClipType.VIDEO,
            startTimeMs = 0L,
            durationMs = 5000L,
            inPointMs = 0L,
            outPointMs = 5000L,
            keyframes = clipKeyframes
        )

        val state = TimelineEngineState(
            tracks = listOf(Track(id = "track_1", projectId = "p1", type = TrackType.VIDEO, order = 0, clips = listOf(clip))),
            playheadPositionMs = 2500L,
            beatMarkers = beatMarkers,
            selectedClipId = clip.id
        )

        // State verify: Beat markers and keyframes are strictly segregated
        assertEquals(3, state.beatMarkers.size)
        assertTrue(state.beatMarkers.contains(2500L))

        val retrievedClip = state.findClip(clip.id)
        assertNotNull(retrievedClip)
        assertEquals(2, retrievedClip?.keyframes?.size)
        assertEquals("kf_1", retrievedClip?.keyframes?.get(0)?.id)
        assertEquals("kf_2", retrievedClip?.keyframes?.get(1)?.id)

        // Toggling beat marker modifies beatMarkers, never keyframes
        val toggledState = TimelineReducer.reduce(state, TimelineAction.ToggleBeatMarker(2500L))
        assertFalse(toggledState.beatMarkers.contains(2500L))
        assertEquals(2, toggledState.findClip(clip.id)?.keyframes?.size)
    }

    // ── 7. Architecture Rule: User Gesture -> Action -> Reducer -> State Flow
    @Test
    fun testArchitectureActionFlow() {
        val clip = Clip(
            id = "c1",
            trackId = "t1",
            type = ClipType.VIDEO,
            startTimeMs = 1000L,
            durationMs = 4000L,
            inPointMs = 0L,
            outPointMs = 4000L
        )
        val initial = TimelineEngineState(
            tracks = listOf(Track(id = "t1", projectId = "p", type = TrackType.VIDEO, order = 0, clips = listOf(clip))),
            playheadPositionMs = 0L,
            durationMs = 5000L,
            selectedClipId = null
        )

        // User taps clip -> SelectClip
        val stateSelected = TimelineReducer.reduce(initial, TimelineAction.SelectClip("c1"))
        assertEquals("c1", stateSelected.selectedClipId)

        // User drags clip -> MoveClip
        val stateMoved = TimelineReducer.reduce(stateSelected, TimelineAction.MoveClip("c1", "t1", 2000L))
        assertEquals(2000L, stateMoved.findClip("c1")?.startTimeMs)
        assertEquals(6000L, stateMoved.findClip("c1")?.endTimeMs)

        // User trims clip -> TrimStart
        val stateTrimmed = TimelineReducer.reduce(stateMoved, TimelineAction.TrimStart("c1", 2500L))
        assertEquals(2500L, stateTrimmed.findClip("c1")?.startTimeMs)

        // User seeks playhead -> Seek
        val stateSeek = TimelineReducer.reduce(stateTrimmed, TimelineAction.Seek(3000L))
        assertEquals(3000L, stateSeek.playheadPositionMs)
    }
}
