package com.example.feature.timeline.engine

import com.example.core.model.Clip
import com.example.core.model.ClipType
import com.example.core.model.Track
import com.example.core.model.TrackType
import com.example.core.model.Transition
import com.example.core.model.TransitionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitionTimelineTest {

    private fun createTestClip(
        id: String,
        trackId: String,
        startTimeMs: Long,
        durationMs: Long
    ): Clip {
        return Clip(
            id = id,
            trackId = trackId,
            type = ClipType.VIDEO,
            startTimeMs = startTimeMs,
            durationMs = durationMs,
            inPointMs = 0L,
            outPointMs = durationMs
        )
    }

    private fun createInitialState(): TimelineEngineState {
        val clipA = createTestClip("clipA", "track1", 0L, 3000L)
        val clipB = createTestClip("clipB", "track1", 3000L, 4000L)
        val track = Track(
            id = "track1",
            type = TrackType.VIDEO,
            name = "Main Video",
            clips = listOf(clipA, clipB),
            transitions = emptyList()
        )
        return TimelineEngineState(
            tracks = listOf(track),
            playheadPositionMs = 0L,
            durationMs = 7000L
        )
    }

    @Test
    fun `AddTransition creates validated transition on track`() {
        val initial = createInitialState()
        val transition = Transition(
            id = "trans1",
            type = TransitionType.FADE,
            durationMs = 800L,
            firstClipId = "clipA",
            secondClipId = "clipB"
        )

        val newState = TimelineReducer.reduce(initial, TimelineAction.AddTransition(transition))
        val targetTrack = newState.tracks.first()

        assertEquals(1, targetTrack.transitions.size)
        val saved = targetTrack.transitions.first()
        assertEquals("trans1", saved.id)
        assertEquals(TransitionType.FADE, saved.type)
        assertEquals(800L, saved.durationMs)
    }

    @Test
    fun `AddTransition clamps duration to shorter adjacent clip`() {
        val initial = createInitialState() // clipA is 3000ms, clipB is 4000ms
        val transition = Transition(
            id = "trans1",
            type = TransitionType.SLIDE,
            durationMs = 5000L, // Exceeds clipA duration (3000ms)
            firstClipId = "clipA",
            secondClipId = "clipB",
            properties = mapOf("direction" to "LEFT")
        )

        val newState = TimelineReducer.reduce(initial, TimelineAction.AddTransition(transition))
        val saved = newState.tracks.first().transitions.first()

        assertEquals(3000L, saved.durationMs) // Clamped to min(3000, 4000)
    }

    @Test
    fun `UpdateTransition modifies existing transition properties`() {
        val initial = createInitialState()
        val transition = Transition(
            id = "trans1",
            type = TransitionType.FADE,
            durationMs = 800L,
            firstClipId = "clipA",
            secondClipId = "clipB"
        )
        val withTransition = TimelineReducer.reduce(initial, TimelineAction.AddTransition(transition))

        val updated = transition.copy(type = TransitionType.ZOOM, durationMs = 1200L)
        val finalState = TimelineReducer.reduce(withTransition, TimelineAction.UpdateTransition(updated))

        val saved = finalState.tracks.first().transitions.first()
        assertEquals(TransitionType.ZOOM, saved.type)
        assertEquals(1200L, saved.durationMs)
    }

    @Test
    fun `RemoveTransition removes transition by ID`() {
        val initial = createInitialState()
        val transition = Transition(
            id = "trans1",
            type = TransitionType.FADE,
            durationMs = 800L,
            firstClipId = "clipA",
            secondClipId = "clipB"
        )
        val withTransition = TimelineReducer.reduce(initial, TimelineAction.AddTransition(transition))
        assertEquals(1, withTransition.tracks.first().transitions.size)

        val removed = TimelineReducer.reduce(withTransition, TimelineAction.RemoveTransition("trans1"))
        assertTrue(removed.tracks.first().transitions.isEmpty())
    }

    @Test
    fun `DeleteClip prunes associated transitions`() {
        val initial = createInitialState()
        val transition = Transition(
            id = "trans1",
            type = TransitionType.FADE,
            durationMs = 800L,
            firstClipId = "clipA",
            secondClipId = "clipB"
        )
        val withTransition = TimelineReducer.reduce(initial, TimelineAction.AddTransition(transition))

        val afterDelete = TimelineReducer.reduce(withTransition, TimelineAction.DeleteClip("clipA"))
        assertTrue(afterDelete.tracks.first().transitions.isEmpty())
    }

    @Test
    fun `TrimClip clamps transition if clip duration becomes smaller than transition`() {
        val initial = createInitialState() // clipA: 0-3000, clipB: 3000-7000
        val transition = Transition(
            id = "trans1",
            type = TransitionType.FADE,
            durationMs = 1500L,
            firstClipId = "clipA",
            secondClipId = "clipB"
        )
        val withTransition = TimelineReducer.reduce(initial, TimelineAction.AddTransition(transition))

        // Trim clipA end from 3000 down to 800ms
        val afterTrim = TimelineReducer.reduce(withTransition, TimelineAction.TrimEnd("clipA", 800L))
        val targetTransition = afterTrim.tracks.first().transitions.firstOrNull()

        assertNotNull(targetTransition)
        assertEquals(800L, targetTransition!!.durationMs)
    }

    @Test
    fun `SplitClip remaps transition boundary to second clip`() {
        val initial = createInitialState() // clipA: 0-3000, clipB: 3000-7000
        val transition = Transition(
            id = "trans1",
            type = TransitionType.FADE,
            durationMs = 500L,
            firstClipId = "clipA",
            secondClipId = "clipB"
        )
        val withTransition = TimelineReducer.reduce(initial, TimelineAction.AddTransition(transition))

        // Split clipA at 1500ms
        val afterSplit = TimelineReducer.reduce(withTransition, TimelineAction.SplitClip("clipA", 1500L))
        val track = afterSplit.tracks.first()

        assertEquals(3, track.clips.size) // firstPart, secondPart, clipB
        assertEquals(1, track.transitions.size)
        // Transition should now be between secondPart and clipB
        val trans = track.transitions.first()
        assertEquals("clipB", trans.secondClipId)
    }

    @Test
    fun `TimelineHistory undo and redo restores transition states`() {
        val history = TimelineHistory()
        var current = createInitialState()
        history.record(current)

        val transition = Transition(
            id = "trans1",
            type = TransitionType.FADE,
            durationMs = 600L,
            firstClipId = "clipA",
            secondClipId = "clipB"
        )
        current = TimelineReducer.reduce(current, TimelineAction.AddTransition(transition))
        history.record(current)

        assertEquals(1, current.tracks.first().transitions.size)

        // Undo
        val undone = history.undo(current)
        assertNotNull(undone)
        assertTrue(undone!!.tracks.first().transitions.isEmpty())

        // Redo
        val redone = history.redo(undone)
        assertNotNull(redone)
        assertEquals(1, redone!!.tracks.first().transitions.size)
        assertEquals("trans1", redone.tracks.first().transitions.first().id)
    }
}
