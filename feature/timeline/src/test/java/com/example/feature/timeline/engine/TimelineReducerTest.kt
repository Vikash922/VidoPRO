package com.example.feature.timeline.engine

import com.example.core.model.Clip
import com.example.core.model.ClipType
import com.example.core.model.Track
import com.example.core.model.TrackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [TimelineReducer] covering core editing actions:
 * 1. Split clip in middle
 * 2. Split clip at edges (safe no-op)
 * 3. Trim start respecting minimum clip duration
 * 4. Move clip with overlap prevention
 * 5. Project duration recalculation after delete
 */
class TimelineReducerTest {

    private fun createTestClip(
        id: String,
        trackId: String,
        startTimeMs: Long,
        durationMs: Long,
        inPointMs: Long = 0L,
        outPointMs: Long = durationMs
    ): Clip {
        return Clip(
            id = id,
            trackId = trackId,
            type = ClipType.VIDEO,
            startTimeMs = startTimeMs,
            durationMs = durationMs,
            inPointMs = inPointMs,
            outPointMs = outPointMs
        )
    }

    private fun createInitialState(tracks: List<Track>): TimelineEngineState {
        val totalDuration = TimelineUtils.recalculateProjectDuration(tracks)
        return TimelineEngineState(
            tracks = tracks,
            playheadPositionMs = 0L,
            durationMs = totalDuration,
            selectedClipId = null
        )
    }

    @Test
    fun testSplitClipInMiddle() {
        val trackId = "track_1"
        val clip = createTestClip(
            id = "clip_1",
            trackId = trackId,
            startTimeMs = 0L,
            durationMs = 4000L,
            inPointMs = 0L,
            outPointMs = 4000L
        )
        val track = Track(id = trackId, projectId = "proj_1", type = TrackType.VIDEO, order = 0, clips = listOf(clip))
        val initialState = createInitialState(listOf(track))

        // Split at 2000ms (in the middle)
        val splitState = TimelineReducer.reduce(
            initialState,
            TimelineAction.SplitClip(clipId = "clip_1", splitPointMs = 2000L)
        )

        val updatedTrack = splitState.tracks.first { it.id == trackId }
        assertEquals("Track should have 2 clips after split", 2, updatedTrack.clips.size)

        val firstClip = updatedTrack.clips[0]
        val secondClip = updatedTrack.clips[1]

        // First clip assertions
        assertEquals("First clip id should be preserved", "clip_1", firstClip.id)
        assertEquals("First clip start time", 0L, firstClip.startTimeMs)
        assertEquals("First clip duration", 2000L, firstClip.durationMs)
        assertEquals("First clip inPoint", 0L, firstClip.inPointMs)
        assertEquals("First clip outPoint", 2000L, firstClip.outPointMs)

        // Second clip assertions
        assertNotNull("Second clip should have a generated id", secondClip.id)
        assertTrue("Second clip id must differ from first clip", secondClip.id != firstClip.id)
        assertEquals("Second clip start time", 2000L, secondClip.startTimeMs)
        assertEquals("Second clip duration", 2000L, secondClip.durationMs)
        assertEquals("Second clip inPoint", 2000L, secondClip.inPointMs)
        assertEquals("Second clip outPoint", 4000L, secondClip.outPointMs)

        // Selected clip should become the second clip
        assertEquals(secondClip.id, splitState.selectedClipId)
    }

    @Test
    fun testSplitClipAtEdgesShouldBeIgnored() {
        val trackId = "track_1"
        val clip = createTestClip(
            id = "clip_1",
            trackId = trackId,
            startTimeMs = 0L,
            durationMs = 4000L
        )
        val track = Track(id = trackId, projectId = "proj_1", type = TrackType.VIDEO, order = 0, clips = listOf(clip))
        val initialState = createInitialState(listOf(track))

        // 1. Attempt split at exact start (0ms) -> should be no-op
        val splitAtStart = TimelineReducer.reduce(
            initialState,
            TimelineAction.SplitClip(clipId = "clip_1", splitPointMs = 0L)
        )
        assertEquals("Split at start should not modify clips count", 1, splitAtStart.tracks[0].clips.size)
        assertEquals("clip_1", splitAtStart.tracks[0].clips[0].id)

        // 2. Attempt split too close to start (50ms < MIN_CLIP_DURATION_MS) -> should be no-op
        val splitTooCloseStart = TimelineReducer.reduce(
            initialState,
            TimelineAction.SplitClip(clipId = "clip_1", splitPointMs = 50L)
        )
        assertEquals("Split too close to start should be ignored", 1, splitTooCloseStart.tracks[0].clips.size)

        // 3. Attempt split at exact end (4000ms) -> should be no-op
        val splitAtEnd = TimelineReducer.reduce(
            initialState,
            TimelineAction.SplitClip(clipId = "clip_1", splitPointMs = 4000L)
        )
        assertEquals("Split at end should not modify clips count", 1, splitAtEnd.tracks[0].clips.size)

        // 4. Attempt split too close to end (3950ms > 4000 - 100) -> should be no-op
        val splitTooCloseEnd = TimelineReducer.reduce(
            initialState,
            TimelineAction.SplitClip(clipId = "clip_1", splitPointMs = 3950L)
        )
        assertEquals("Split too close to end should be ignored", 1, splitTooCloseEnd.tracks[0].clips.size)
    }

    @Test
    fun testTrimStartRespectsMinimumDuration() {
        val trackId = "track_1"
        val clip = createTestClip(
            id = "clip_1",
            trackId = trackId,
            startTimeMs = 0L,
            durationMs = 3000L,
            inPointMs = 0L,
            outPointMs = 3000L
        )
        val track = Track(id = trackId, projectId = "proj_1", type = TrackType.VIDEO, order = 0, clips = listOf(clip))
        val initialState = createInitialState(listOf(track))

        // Attempt to trim start to 2980ms (which would leave duration at 20ms < MIN_CLIP_DURATION_MS of 100ms)
        val trimmedState = TimelineReducer.reduce(
            initialState,
            TimelineAction.TrimStart(clipId = "clip_1", newStartTimeMs = 2980L)
        )

        val updatedClip = trimmedState.tracks[0].clips[0]
        val minDuration = TimelineEngineState.MIN_CLIP_DURATION_MS // 100ms

        assertTrue("Duration must be at least MIN_CLIP_DURATION_MS (100ms)", updatedClip.durationMs >= minDuration)
        assertEquals("Clamped start time should not exceed originalEnd - MIN_CLIP_DURATION_MS", 3000L - minDuration, updatedClip.startTimeMs)
        assertEquals("New duration should equal clamped remaining duration", minDuration, updatedClip.durationMs)
    }

    @Test
    fun testMoveClipWithOverlapPrevention() {
        val trackId = "track_1"
        val clipA = createTestClip(
            id = "clip_a",
            trackId = trackId,
            startTimeMs = 0L,
            durationMs = 3000L
        )
        val clipB = createTestClip(
            id = "clip_b",
            trackId = trackId,
            startTimeMs = 3000L,
            durationMs = 2000L
        )
        val track = Track(id = trackId, projectId = "proj_1", type = TrackType.VIDEO, order = 0, clips = listOf(clipA, clipB))
        val initialState = createInitialState(listOf(track))

        // Attempt to move Clip B into Clip A's time range (e.g. at 1000ms)
        val movedState = TimelineReducer.reduce(
            initialState,
            TimelineAction.MoveClip(clipId = "clip_b", targetTrackId = trackId, newStartTimeMs = 1000L)
        )

        val updatedTrack = movedState.tracks.first { it.id == trackId }
        val finalClipA = updatedTrack.clips.first { it.id == "clip_a" }
        val finalClipB = updatedTrack.clips.first { it.id == "clip_b" }

        // Overlap prevention on main video track ensures Clip B starts at or after Clip A's end time (3000ms)
        assertTrue(
            "Clip B should start at or after Clip A ends (${finalClipA.endTimeMs}ms)",
            finalClipB.startTimeMs >= finalClipA.endTimeMs
        )
        assertEquals("Clip B should be rippled to 3000ms without overlapping Clip A", 3000L, finalClipB.startTimeMs)
    }

    @Test
    fun testProjectDurationRecalculationAfterDelete() {
        val trackId = "track_1"
        val clipA = createTestClip(
            id = "clip_a",
            trackId = trackId,
            startTimeMs = 0L,
            durationMs = 2000L
        )
        val clipB = createTestClip(
            id = "clip_b",
            trackId = trackId,
            startTimeMs = 2000L,
            durationMs = 3000L
        )
        val track = Track(id = trackId, projectId = "proj_1", type = TrackType.VIDEO, order = 0, clips = listOf(clipA, clipB))
        val initialState = createInitialState(listOf(track))

        // Initial total duration should be 5000ms
        assertEquals(5000L, initialState.durationMs)

        // Delete Clip B
        val stateAfterDelete = TimelineReducer.reduce(
            initialState,
            TimelineAction.DeleteClip(clipId = "clip_b")
        )

        val updatedTrack = stateAfterDelete.tracks.first { it.id == trackId }
        assertEquals("Only 1 clip should remain", 1, updatedTrack.clips.size)
        assertEquals("Remaining clip should be clip_a", "clip_a", updatedTrack.clips[0].id)
        assertEquals("Project duration should be recalculated to 2000ms", 2000L, stateAfterDelete.durationMs)
    }

    @Test
    fun testUpdateClipEffects() {
        val trackId = "track_1"
        val clip = createTestClip(
            id = "clip_eff",
            trackId = trackId,
            startTimeMs = 0L,
            durationMs = 2000L
        )
        val track = Track(id = trackId, projectId = "proj_1", type = TrackType.VIDEO, order = 0, clips = listOf(clip))
        val initialState = createInitialState(listOf(track)).copy(
            selectedClipId = clip.id
        )

        val newEffects = listOf(
            com.example.core.model.Effect(
                id = "eff_1",
                clipId = "clip_eff",
                type = com.example.core.model.EffectType.CONTRAST,
                parameters = mapOf("value" to 1.4f)
            )
        )

        val stateAfter = TimelineReducer.reduce(
            initialState,
            TimelineAction.UpdateClipEffects("clip_eff", newEffects)
        )

        val updatedClip = stateAfter.tracks.first().clips.first()
        assertEquals(1, updatedClip.effects.size)
        assertEquals(com.example.core.model.EffectType.CONTRAST, updatedClip.effects[0].type)
        assertEquals(1.4f, updatedClip.effects[0].parameters["value"]!!, 0.001f)
        assertEquals(1, stateAfter.selectedClip?.effects?.size)
    }

    @Test
    fun testToggleTrackVisibilityAndLock() {
        val trackId = "track_vis"
        val track = Track(id = trackId, projectId = "proj_1", type = TrackType.VIDEO, order = 0, isVisible = true, isLocked = false)
        val initialState = createInitialState(listOf(track))

        val hiddenState = TimelineReducer.reduce(initialState, TimelineAction.ToggleTrackVisibility(trackId))
        assertEquals(false, hiddenState.tracks.first().isVisible)

        val visibleAgainState = TimelineReducer.reduce(hiddenState, TimelineAction.ToggleTrackVisibility(trackId))
        assertEquals(true, visibleAgainState.tracks.first().isVisible)

        val lockedState = TimelineReducer.reduce(visibleAgainState, TimelineAction.ToggleTrackLock(trackId))
        assertEquals(true, lockedState.tracks.first().isLocked)

        val unlockedState = TimelineReducer.reduce(lockedState, TimelineAction.ToggleTrackLock(trackId))
        assertEquals(false, unlockedState.tracks.first().isLocked)
    }

    @Test
    fun testLockedTrackPreventsSplitAndTrimAndMove() {
        val trackId = "track_locked"
        val clip = createTestClip(id = "clip_1", trackId = trackId, startTimeMs = 0L, durationMs = 4000L)
        val lockedTrack = Track(id = trackId, projectId = "proj_1", type = TrackType.VIDEO, order = 0, isLocked = true, clips = listOf(clip))
        val initialState = createInitialState(listOf(lockedTrack))

        // Attempt split
        val splitAttempt = TimelineReducer.reduce(initialState, TimelineAction.SplitClip("clip_1", 2000L))
        assertEquals("Locked track should not split clip", 1, splitAttempt.tracks.first().clips.size)

        // Attempt trim
        val trimAttempt = TimelineReducer.reduce(initialState, TimelineAction.TrimStart("clip_1", 1000L))
        assertEquals("Locked track should not trim start", 0L, trimAttempt.tracks.first().clips.first().startTimeMs)

        // Attempt move
        val moveAttempt = TimelineReducer.reduce(initialState, TimelineAction.MoveClip("clip_1", trackId, 2000L))
        assertEquals("Locked track should not move clip", 0L, moveAttempt.tracks.first().clips.first().startTimeMs)

        // Attempt delete
        val deleteAttempt = TimelineReducer.reduce(initialState, TimelineAction.DeleteClip("clip_1"))
        assertEquals("Locked track should not delete clip", 1, deleteAttempt.tracks.first().clips.size)
    }

    @Test
    fun testMuteTrack() {
        val trackId = "track_audio"
        val clip1 = createTestClip(id = "clip_a1", trackId = trackId, startTimeMs = 0L, durationMs = 2000L).copy(volume = 1f)
        val clip2 = createTestClip(id = "clip_a2", trackId = trackId, startTimeMs = 2000L, durationMs = 2000L).copy(volume = 0.8f)
        val track = Track(id = trackId, projectId = "proj_1", type = TrackType.AUDIO, order = 0, clips = listOf(clip1, clip2))
        val initialState = createInitialState(listOf(track))

        val mutedState = TimelineReducer.reduce(initialState, TimelineAction.MuteTrack(trackId, mute = true))
        assertTrue(mutedState.tracks.first().clips.all { it.volume == 0f })

        val unmutedState = TimelineReducer.reduce(mutedState, TimelineAction.MuteTrack(trackId, mute = false))
        assertTrue(unmutedState.tracks.first().clips.all { it.volume == 1f })
    }

    @Test
    fun testSplitClipWithSpeedFactor() {
        val trackId = "track_1"
        // 4000ms duration at 2.0x speed means 8000ms of source media (from 1000ms to 9000ms)
        val clip = createTestClip(
            id = "clip_fast",
            trackId = trackId,
            startTimeMs = 0L,
            durationMs = 4000L,
            inPointMs = 1000L,
            outPointMs = 9000L
        ).copy(speed = 2.0f)
        val track = Track(id = trackId, projectId = "proj_1", type = TrackType.VIDEO, order = 0, clips = listOf(clip))
        val initialState = createInitialState(listOf(track))

        // Split at 2000ms (halfway on timeline)
        val splitState = TimelineReducer.reduce(
            initialState,
            TimelineAction.SplitClip(clipId = "clip_fast", splitPointMs = 2000L)
        )

        val updatedClips = splitState.tracks[0].clips
        assertEquals(2, updatedClips.size)

        val firstClip = updatedClips[0]
        val secondClip = updatedClips[1]

        // First clip: duration 2000ms, elapsed in source = 2000 * 2.0 = 4000ms -> outPoint = 1000 + 4000 = 5000ms
        assertEquals(0L, firstClip.startTimeMs)
        assertEquals(2000L, firstClip.durationMs)
        assertEquals(1000L, firstClip.inPointMs)
        assertEquals(5000L, firstClip.outPointMs)
        assertEquals(2.0f, firstClip.speed, 0.001f)

        // Second clip: duration 2000ms, inPoint = 5000ms, outPoint = 9000ms
        assertEquals(2000L, secondClip.startTimeMs)
        assertEquals(2000L, secondClip.durationMs)
        assertEquals(5000L, secondClip.inPointMs)
        assertEquals(9000L, secondClip.outPointMs)
        assertEquals(2.0f, secondClip.speed, 0.001f)
    }

    @Test
    fun testTrimStartAndEndWithSpeedFactor() {
        val trackId = "track_1"
        // 4000ms duration at 2.0x speed means 8000ms of source media
        val clip = createTestClip(
            id = "clip_trim_speed",
            trackId = trackId,
            startTimeMs = 0L,
            durationMs = 4000L,
            inPointMs = 1000L,
            outPointMs = 9000L
        ).copy(speed = 2.0f)
        val track = Track(id = trackId, projectId = "proj_1", type = TrackType.VIDEO, order = 0, clips = listOf(clip))
        val initialState = createInitialState(listOf(track)).copy(isSnappingEnabled = false)

        // Trim start from 0L to 1000L (+1000ms on timeline -> +2000ms in source asset)
        val trimmedStart = TimelineReducer.reduce(
            initialState,
            TimelineAction.TrimStart("clip_trim_speed", 1000L)
        )
        val clipAfterStartTrim = trimmedStart.tracks[0].clips[0]
        assertEquals(1000L, clipAfterStartTrim.startTimeMs)
        assertEquals(3000L, clipAfterStartTrim.durationMs)
        assertEquals(3000L, clipAfterStartTrim.inPointMs) // 1000 + (1000 * 2.0)

        // Trim end from 4000L to 2000L (duration becomes 1000ms, outPoint becomes 3000 + 1000*2 = 5000L)
        val trimmedEnd = TimelineReducer.reduce(
            trimmedStart,
            TimelineAction.TrimEnd("clip_trim_speed", 2000L)
        )
        val clipAfterEndTrim = trimmedEnd.tracks[0].clips[0]
        assertEquals(1000L, clipAfterEndTrim.startTimeMs)
        assertEquals(1000L, clipAfterEndTrim.durationMs)
        assertEquals(5000L, clipAfterEndTrim.outPointMs) // 3000 + (1000 * 2.0)
    }

    @Test
    fun testSplitClipClonesEffectsWithUniqueIds() {
        val trackId = "track_1"
        val effect = com.example.core.model.Effect(
            id = "eff_orig",
            clipId = "clip_with_fx",
            type = com.example.core.model.EffectType.BRIGHTNESS,
            parameters = mapOf("value" to 0.5f)
        )
        val clip = createTestClip(
            id = "clip_with_fx",
            trackId = trackId,
            startTimeMs = 0L,
            durationMs = 4000L
        ).copy(effects = listOf(effect))
        val track = Track(id = trackId, projectId = "proj_1", type = TrackType.VIDEO, order = 0, clips = listOf(clip))
        val initialState = createInitialState(listOf(track))

        val splitState = TimelineReducer.reduce(
            initialState,
            TimelineAction.SplitClip("clip_with_fx", 2000L)
        )
        val secondClip = splitState.tracks[0].clips[1]
        assertEquals(1, secondClip.effects.size)
        val secondEff = secondClip.effects[0]
        assertEquals(secondClip.id, secondEff.clipId)
        assertTrue("Cloned effect must have a new unique ID", secondEff.id != "eff_orig")
    }

    @Test
    fun testCalculateAnchoredScrollOffset() {
        // Timeline at 1.0x zoom (0.06 px/ms). User is looking at scroll = 600px with focal touch at 300px.
        // Total pixel X = 900px -> focal time = 900 / 0.06 = 15000ms.
        // Zoom in to 2.0x (0.12 px/ms).
        // 15000ms at 0.12 px/ms = 1800px.
        // Anchored new scroll should be 1800 - 300 = 1500px.
        val newScroll = TimelineUtils.calculateAnchoredScrollOffset(
            currentScrollPx = 600,
            oldZoom = 1.0f,
            newZoom = 2.0f,
            focalScreenXPx = 300f
        )
        assertEquals(1500, newScroll)
    }
}
