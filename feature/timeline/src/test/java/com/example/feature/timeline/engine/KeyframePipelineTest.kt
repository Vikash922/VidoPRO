package com.example.feature.timeline.engine

import com.example.core.model.Clip
import com.example.core.model.ClipType
import com.example.core.model.InterpolationType
import com.example.core.model.Keyframe
import com.example.core.model.KeyframeProperty
import com.example.core.model.Track
import com.example.core.model.TrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyframePipelineTest {

    private fun createTestClip(
        id: String = "clip_1",
        trackId: String = "track_1",
        startTimeMs: Long = 1000L,
        durationMs: Long = 5000L,
        keyframes: List<Keyframe> = emptyList()
    ): Clip {
        return Clip(
            id = id,
            trackId = trackId,
            type = ClipType.VIDEO,
            startTimeMs = startTimeMs,
            durationMs = durationMs,
            inPointMs = 0L,
            outPointMs = durationMs,
            keyframes = keyframes
        )
    }

    private fun createInitialState(
        clip: Clip,
        beatMarkers: Set<Long> = emptySet(),
        playheadPositionMs: Long = 2000L
    ): TimelineEngineState {
        val track = Track(
            id = clip.trackId,
            projectId = "test_project",
            type = TrackType.VIDEO,
            order = 0,
            clips = listOf(clip)
        )
        return TimelineEngineState(
            tracks = listOf(track),
            playheadPositionMs = playheadPositionMs,
            durationMs = 10000L,
            selectedClipId = clip.id,
            beatMarkers = beatMarkers
        )
    }

    @Test
    fun testAddKeyframeWithinClipBoundaries() {
        val clip = createTestClip(startTimeMs = 1000L, durationMs = 4000L) // range: 1000..5000
        val state = createInitialState(clip)

        val newState = TimelineReducer.reduce(
            state,
            TimelineAction.AddKeyframe(
                clipId = clip.id,
                property = KeyframeProperty.SCALE_X,
                timeMs = 2500L,
                value = 1.5f,
                interpolation = InterpolationType.EASE_IN_OUT
            )
        )

        val updatedClip = newState.findClip(clip.id)
        assertNotNull(updatedClip)
        assertEquals(1, updatedClip!!.keyframes.size)

        val kf = updatedClip.keyframes[0]
        assertEquals(clip.id, kf.clipId)
        assertEquals(KeyframeProperty.SCALE_X, kf.property)
        assertEquals(2500L, kf.timeMs)
        assertEquals(1.5f, kf.value, 0.001f)
        assertEquals(InterpolationType.EASE_IN_OUT, kf.interpolation)
    }

    @Test
    fun testAddKeyframeClampsToClipBoundaries() {
        val clip = createTestClip(startTimeMs = 1000L, durationMs = 4000L) // range: 1000..5000
        val state = createInitialState(clip)

        // Attempt to add keyframe before clip start (500ms < 1000ms)
        val stateBefore = TimelineReducer.reduce(
            state,
            TimelineAction.AddKeyframe(clip.id, KeyframeProperty.POSITION_X, 500L, 10f)
        )
        assertEquals(1000L, stateBefore.findClip(clip.id)!!.keyframes[0].timeMs)

        // Attempt to add keyframe after clip end (6000ms > 5000ms)
        val stateAfter = TimelineReducer.reduce(
            state,
            TimelineAction.AddKeyframe(clip.id, KeyframeProperty.POSITION_Y, 6000L, 20f)
        )
        assertEquals(5000L, stateAfter.findClip(clip.id)!!.keyframes[0].timeMs)
    }

    @Test
    fun testDuplicateTimestampBehaviorUpdatesExistingKeyframe() {
        val clip = createTestClip(startTimeMs = 1000L, durationMs = 4000L)
        val state = createInitialState(clip)

        // Add first keyframe for rotation at 2000ms
        val state1 = TimelineReducer.reduce(
            state,
            TimelineAction.AddKeyframe(clip.id, KeyframeProperty.ROTATION, 2000L, 45f)
        )
        val kfId = state1.findClip(clip.id)!!.keyframes[0].id
        assertEquals(1, state1.findClip(clip.id)!!.keyframes.size)

        // Add second keyframe for rotation at same timestamp 2000ms with new value 90f
        val state2 = TimelineReducer.reduce(
            state1,
            TimelineAction.AddKeyframe(clip.id, KeyframeProperty.ROTATION, 2000L, 90f, InterpolationType.BEZIER)
        )

        val keyframes = state2.findClip(clip.id)!!.keyframes
        assertEquals("Duplicate timestamp must update existing keyframe, not append", 1, keyframes.size)
        assertEquals(kfId, keyframes[0].id)
        assertEquals(90f, keyframes[0].value, 0.001f)
        assertEquals(InterpolationType.BEZIER, keyframes[0].interpolation)
    }

    @Test
    fun testUpdateKeyframeDirectly() {
        val initialKf = Keyframe("kf_1", "clip_1", KeyframeProperty.OPACITY, 2000L, 1.0f)
        val clip = createTestClip(id = "clip_1", keyframes = listOf(initialKf))
        val state = createInitialState(clip)

        val updatedState = TimelineReducer.reduce(
            state,
            TimelineAction.UpdateKeyframe("clip_1", "kf_1", 0.5f, InterpolationType.EASE_OUT)
        )

        val kf = updatedState.findClip("clip_1")!!.keyframes.first()
        assertEquals(0.5f, kf.value, 0.001f)
        assertEquals(InterpolationType.EASE_OUT, kf.interpolation)
    }

    @Test
    fun testDeleteKeyframe() {
        val kf1 = Keyframe("kf_1", "clip_1", KeyframeProperty.POSITION_X, 2000L, 10f)
        val kf2 = Keyframe("kf_2", "clip_1", KeyframeProperty.POSITION_Y, 3000L, 20f)
        val clip = createTestClip(id = "clip_1", keyframes = listOf(kf1, kf2))
        val state = createInitialState(clip)

        val updatedState = TimelineReducer.reduce(
            state,
            TimelineAction.DeleteKeyframe("clip_1", "kf_1")
        )

        val remaining = updatedState.findClip("clip_1")!!.keyframes
        assertEquals(1, remaining.size)
        assertEquals("kf_2", remaining[0].id)
    }

    @Test
    fun testMoveKeyframeRespectsClipBoundaries() {
        val kf = Keyframe("kf_1", "clip_1", KeyframeProperty.VOLUME, 2000L, 0.8f)
        val clip = createTestClip(id = "clip_1", startTimeMs = 1000L, durationMs = 3000L, keyframes = listOf(kf)) // 1000..4000
        val state = createInitialState(clip)

        // Move to valid time
        val stateValid = TimelineReducer.reduce(
            state,
            TimelineAction.MoveKeyframe("clip_1", "kf_1", 3500L)
        )
        assertEquals(3500L, stateValid.findClip("clip_1")!!.keyframes[0].timeMs)

        // Move past clip end (clamped to 4000L)
        val statePastEnd = TimelineReducer.reduce(
            state,
            TimelineAction.MoveKeyframe("clip_1", "kf_1", 9999L)
        )
        assertEquals(4000L, statePastEnd.findClip("clip_1")!!.keyframes[0].timeMs)
    }

    @Test
    fun testKeyframeOperationsDoNotTouchBeatMarkers() {
        val clip = createTestClip(id = "clip_1", startTimeMs = 1000L, durationMs = 4000L)
        val initialMarkers = setOf(1200L, 2400L, 3600L)
        val state = createInitialState(clip, beatMarkers = initialMarkers)

        // Add Keyframe
        val stateWithKf = TimelineReducer.reduce(
            state,
            TimelineAction.AddKeyframe("clip_1", KeyframeProperty.ROTATION, 2000L, 45f)
        )
        assertEquals("Adding keyframe must not alter beat markers", initialMarkers, stateWithKf.beatMarkers)

        // Delete Keyframe
        val kfId = stateWithKf.findClip("clip_1")!!.keyframes[0].id
        val stateDeleted = TimelineReducer.reduce(
            stateWithKf,
            TimelineAction.DeleteKeyframe("clip_1", kfId)
        )
        assertEquals("Deleting keyframe must not alter beat markers", initialMarkers, stateDeleted.beatMarkers)
    }

    @Test
    fun testToggleBeatMarkerDoesNotTouchKeyframes() {
        val kf = Keyframe("kf_1", "clip_1", KeyframeProperty.ROTATION, 2000L, 45f)
        val clip = createTestClip(id = "clip_1", keyframes = listOf(kf))
        val state = createInitialState(clip, beatMarkers = setOf(1000L))

        // Toggle Beat Marker at 2000ms
        val stateToggled = TimelineReducer.reduce(
            state,
            TimelineAction.ToggleBeatMarker(2000L)
        )

        assertTrue(stateToggled.beatMarkers.contains(2000L))
        val clipKeyframes = stateToggled.findClip("clip_1")!!.keyframes
        assertEquals("ToggleBeatMarker must not modify clip keyframes", listOf(kf), clipKeyframes)
    }

    @Test
    fun testClipMoveShiftsKeyframes() {
        val kf1 = Keyframe("kf_1", "clip_1", KeyframeProperty.POSITION_X, 1500L, 10f)
        val kf2 = Keyframe("kf_2", "clip_1", KeyframeProperty.POSITION_X, 3000L, 20f)
        val clip = createTestClip(id = "clip_1", startTimeMs = 1000L, durationMs = 4000L, keyframes = listOf(kf1, kf2))
        val state = createInitialState(clip)

        // Move clip from startTimeMs 1000 to 2000 (+1000ms delta)
        val movedState = TimelineReducer.reduce(
            state,
            TimelineAction.MoveClip("clip_1", "track_1", 2000L)
        )

        val keyframes = movedState.findClip("clip_1")!!.keyframes
        assertEquals(2, keyframes.size)
        assertEquals(2500L, keyframes[0].timeMs) // 1500 + 1000
        assertEquals(4000L, keyframes[1].timeMs) // 3000 + 1000
    }

    @Test
    fun testClipTrimFiltersKeyframes() {
        val kf1 = Keyframe("kf_1", "clip_1", KeyframeProperty.POSITION_X, 1500L, 10f)
        val kf2 = Keyframe("kf_2", "clip_1", KeyframeProperty.POSITION_X, 3000L, 20f)
        val clip = createTestClip(id = "clip_1", startTimeMs = 1000L, durationMs = 4000L, keyframes = listOf(kf1, kf2)) // 1000..5000
        val state = createInitialState(clip)

        // Trim start from 1000 to 2000 (kf1 at 1500 should be excluded)
        val trimmedStartState = TimelineReducer.reduce(
            state,
            TimelineAction.TrimClipStart("clip_1", 2000L)
        )
        val remainingStart = trimmedStartState.findClip("clip_1")!!.keyframes
        assertEquals(1, remainingStart.size)
        assertEquals("kf_2", remainingStart[0].id)

        // Trim end from 5000 to 2500 on state (kf2 at 3000 should be excluded)
        val trimmedEndState = TimelineReducer.reduce(
            state,
            TimelineAction.TrimClipEnd("clip_1", 2500L)
        )
        val remainingEnd = trimmedEndState.findClip("clip_1")!!.keyframes
        assertEquals(1, remainingEnd.size)
        assertEquals("kf_1", remainingEnd[0].id)
    }

    @Test
    fun testClipSplitDistributesKeyframes() {
        val kf1 = Keyframe("kf_1", "clip_1", KeyframeProperty.POSITION_X, 1500L, 10f)
        val kf2 = Keyframe("kf_2", "clip_1", KeyframeProperty.POSITION_X, 3500L, 20f)
        val clip = createTestClip(id = "clip_1", startTimeMs = 1000L, durationMs = 4000L, keyframes = listOf(kf1, kf2)) // 1000..5000
        val state = createInitialState(clip)

        // Split clip at 2500ms
        val splitState = TimelineReducer.reduce(
            state,
            TimelineAction.SplitClip("clip_1", 2500L)
        )

        val trackClips = splitState.tracks[0].clips
        assertEquals(2, trackClips.size)
        val firstClip = trackClips[0]
        val secondClip = trackClips[1]

        assertEquals(1, firstClip.keyframes.size)
        assertEquals("kf_1", firstClip.keyframes[0].id)
        assertEquals(1500L, firstClip.keyframes[0].timeMs)

        assertEquals(1, secondClip.keyframes.size)
        assertEquals(3500L, secondClip.keyframes[0].timeMs)
        assertEquals(secondClip.id, secondClip.keyframes[0].clipId)
    }
}
