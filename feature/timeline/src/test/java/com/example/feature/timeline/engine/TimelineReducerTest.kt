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
}
