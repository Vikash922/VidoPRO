package com.example.feature.editor.history

import com.example.feature.timeline.engine.TimelineAction
import com.example.feature.timeline.engine.TimelineEngineState
import com.example.feature.timeline.engine.TimelineReducer

/**
 * Command interface representing an undoable operation in the video editor.
 * Follows the Command Pattern for DEV-058.
 */
interface EditorCommand {
    val description: String

    /**
     * Executes the command starting from the given [state], producing the new state.
     */
    fun execute(state: TimelineEngineState): TimelineEngineState

    /**
     * Reverses the command from the current [state], restoring the previous state.
     */
    fun undo(state: TimelineEngineState): TimelineEngineState
}

/**
 * Command to move a clip to a new start time or track.
 */
class MoveClipCommand(
    val clipId: String,
    val targetTrackId: String,
    val newStartTimeMs: Long,
    val oldTrackId: String,
    val oldStartTimeMs: Long
) : EditorCommand {
    override val description: String = "Move clip"

    override fun execute(state: TimelineEngineState): TimelineEngineState {
        return TimelineReducer.reduce(state, TimelineAction.MoveClip(clipId, targetTrackId, newStartTimeMs))
    }

    override fun undo(state: TimelineEngineState): TimelineEngineState {
        return TimelineReducer.reduce(state, TimelineAction.MoveClip(clipId, oldTrackId, oldStartTimeMs))
    }
}

/**
 * Command to trim the start of a clip.
 */
class TrimStartCommand(
    val clipId: String,
    val newStartTimeMs: Long,
    val oldStartTimeMs: Long
) : EditorCommand {
    override val description: String = "Trim start"

    override fun execute(state: TimelineEngineState): TimelineEngineState {
        return TimelineReducer.reduce(state, TimelineAction.TrimClipStart(clipId, newStartTimeMs))
    }

    override fun undo(state: TimelineEngineState): TimelineEngineState {
        return TimelineReducer.reduce(state, TimelineAction.TrimClipStart(clipId, oldStartTimeMs))
    }
}

/**
 * Command to trim the end of a clip.
 */
class TrimEndCommand(
    val clipId: String,
    val newEndTimeMs: Long,
    val oldEndTimeMs: Long
) : EditorCommand {
    override val description: String = "Trim end"

    override fun execute(state: TimelineEngineState): TimelineEngineState {
        return TimelineReducer.reduce(state, TimelineAction.TrimClipEnd(clipId, newEndTimeMs))
    }

    override fun undo(state: TimelineEngineState): TimelineEngineState {
        return TimelineReducer.reduce(state, TimelineAction.TrimClipEnd(clipId, oldEndTimeMs))
    }
}

/**
 * Command to split a clip into two.
 * Undo restores the pre-split state.
 */
class SplitClipCommand(
    val clipId: String,
    val splitPointMs: Long,
    val preSplitState: TimelineEngineState
) : EditorCommand {
    override val description: String = "Split clip"

    override fun execute(state: TimelineEngineState): TimelineEngineState {
        return TimelineReducer.reduce(state, TimelineAction.SplitClip(clipId, splitPointMs))
    }

    override fun undo(state: TimelineEngineState): TimelineEngineState {
        return preSplitState
    }
}

/**
 * Command to delete a clip.
 * Undo restores the pre-delete state with the clip intact.
 */
class DeleteClipCommand(
    val clipId: String,
    val preDeleteState: TimelineEngineState
) : EditorCommand {
    override val description: String = "Delete clip"

    override fun execute(state: TimelineEngineState): TimelineEngineState {
        return TimelineReducer.reduce(state, TimelineAction.DeleteClip(clipId))
    }

    override fun undo(state: TimelineEngineState): TimelineEngineState {
        return preDeleteState
    }
}

/**
 * Generic state-snapshot command wrapper for arbitrary structural actions (e.g. Duplicate, AddClip).
 */
class StateSnapshotCommand(
    override val description: String,
    val action: TimelineAction,
    private val preActionState: TimelineEngineState
) : EditorCommand {
    override fun execute(state: TimelineEngineState): TimelineEngineState {
        return TimelineReducer.reduce(state, action)
    }

    override fun undo(state: TimelineEngineState): TimelineEngineState {
        return preActionState
    }
}
