package com.example.feature.editor.history

import com.example.core.model.Clip
import com.example.core.model.ClipType
import com.example.core.model.InterpolationType
import com.example.core.model.Keyframe
import com.example.core.model.KeyframeProperty
import com.example.core.model.Track
import com.example.core.model.TrackType
import com.example.feature.timeline.engine.TimelineAction
import com.example.feature.timeline.engine.TimelineEngineState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyframeUndoRedoTest {

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

    private fun createInitialState(clip: Clip): TimelineEngineState {
        val track = Track(
            id = clip.trackId,
            projectId = "test_project",
            type = TrackType.VIDEO,
            order = 0,
            clips = listOf(clip)
        )
        return TimelineEngineState(
            tracks = listOf(track),
            playheadPositionMs = 2000L,
            durationMs = 10000L,
            selectedClipId = clip.id
        )
    }

    @Test
    fun testAddKeyframeUndoAndRedo() {
        val manager = UndoRedoManager()
        val clip = createTestClip(keyframes = emptyList())
        val initialState = createInitialState(clip)

        val addAction = TimelineAction.AddKeyframe(
            clipId = clip.id,
            property = KeyframeProperty.SCALE_X,
            timeMs = 2000L,
            value = 1.5f,
            interpolation = InterpolationType.LINEAR
        )
        val addCmd = StateSnapshotCommand("Add keyframe", addAction, initialState)

        // Execute Add
        val stateAfterAdd = manager.executeCommand(addCmd, initialState)
        val clipAfterAdd = stateAfterAdd.findClip(clip.id)
        assertNotNull(clipAfterAdd)
        assertEquals(1, clipAfterAdd!!.keyframes.size)
        assertTrue(manager.canUndo.value)
        assertFalse(manager.canRedo.value)

        // Undo
        val stateAfterUndo = manager.undo(stateAfterAdd)
        assertNotNull(stateAfterUndo)
        val clipAfterUndo = stateAfterUndo!!.findClip(clip.id)
        assertTrue("Undo must revert keyframes to empty list", clipAfterUndo!!.keyframes.isEmpty())
        assertFalse(manager.canUndo.value)
        assertTrue(manager.canRedo.value)

        // Redo
        val stateAfterRedo = manager.redo(stateAfterUndo)
        assertNotNull(stateAfterRedo)
        val clipAfterRedo = stateAfterRedo!!.findClip(clip.id)
        assertEquals("Redo must restore added keyframe", 1, clipAfterRedo!!.keyframes.size)
        assertEquals(KeyframeProperty.SCALE_X, clipAfterRedo.keyframes[0].property)
        assertEquals(1.5f, clipAfterRedo.keyframes[0].value, 0.001f)
    }

    @Test
    fun testDeleteKeyframeUndoAndRedo() {
        val manager = UndoRedoManager()
        val initialKf = Keyframe("kf_1", "clip_1", KeyframeProperty.ROTATION, 2500L, 45f)
        val clip = createTestClip(keyframes = listOf(initialKf))
        val stateWithKf = createInitialState(clip)

        val deleteAction = TimelineAction.DeleteKeyframe(clip.id, initialKf.id)
        val deleteCmd = StateSnapshotCommand("Delete keyframe", deleteAction, stateWithKf)

        // Execute Delete
        val stateAfterDelete = manager.executeCommand(deleteCmd, stateWithKf)
        val clipAfterDelete = stateAfterDelete.findClip(clip.id)
        assertTrue("Keyframe must be deleted", clipAfterDelete!!.keyframes.isEmpty())

        // Undo Delete
        val stateAfterUndo = manager.undo(stateAfterDelete)
        assertNotNull(stateAfterUndo)
        val clipAfterUndo = stateAfterUndo!!.findClip(clip.id)
        assertEquals("Undo delete must restore keyframe", 1, clipAfterUndo!!.keyframes.size)
        assertEquals("kf_1", clipAfterUndo.keyframes[0].id)
        assertEquals(45f, clipAfterUndo.keyframes[0].value, 0.001f)

        // Redo Delete
        val stateAfterRedo = manager.redo(stateAfterUndo)
        assertNotNull(stateAfterRedo)
        val clipAfterRedo = stateAfterRedo!!.findClip(clip.id)
        assertTrue("Redo must re-delete keyframe", clipAfterRedo!!.keyframes.isEmpty())
    }

    @Test
    fun testMoveKeyframeCoalescingOnDrag() {
        val manager = UndoRedoManager()
        val initialKf = Keyframe("kf_1", "clip_1", KeyframeProperty.POSITION_X, 2000L, 0f)
        val clip = createTestClip(keyframes = listOf(initialKf))
        var currentState = createInitialState(clip)

        // Rapid drag movements: 2050 -> 2100 -> 2200 -> 2500
        val dragPositions = listOf(2050L, 2100L, 2200L, 2500L)
        for (pos in dragPositions) {
            val moveAction = TimelineAction.MoveKeyframe(clip.id, initialKf.id, pos)
            val moveCmd = StateSnapshotCommand("Move keyframe", moveAction, currentState)
            currentState = manager.executeCommand(moveCmd, currentState)
        }

        // Final keyframe time should be 2500L
        assertEquals(2500L, currentState.findClip(clip.id)!!.keyframes[0].timeMs)

        // A single undo should restore back to initial state (2000L), NOT intermediate drag steps!
        val stateAfterUndo = manager.undo(currentState)
        assertNotNull(stateAfterUndo)
        assertEquals(2000L, stateAfterUndo!!.findClip(clip.id)!!.keyframes[0].timeMs)
        // Redo should jump to final 2500L
        val stateAfterRedo = manager.redo(stateAfterUndo)
        assertNotNull(stateAfterRedo)
        assertEquals(2500L, stateAfterRedo!!.findClip(clip.id)!!.keyframes[0].timeMs)
    }
}
