package com.example.feature.editor.history

import com.example.feature.timeline.engine.TimelineEngineState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Undo/Redo Manager implementing the Command Pattern for Video Editor actions (DEV-058, DEV-059).
 * Manages an undo stack and redo stack of [EditorCommand] pairs with their associated states.
 */
class UndoRedoManager(
    private val maxHistorySize: Int = 30
) {
    private data class CommandEntry(
        val command: EditorCommand,
        val beforeState: TimelineEngineState,
        val afterState: TimelineEngineState
    )

    private val undoStack = ArrayDeque<CommandEntry>()
    private val redoStack = ArrayDeque<CommandEntry>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    /**
     * Executes a new command from the [currentState] and records it into the undo history.
     * Clears the redo stack.
     */
    fun executeCommand(command: EditorCommand, currentState: TimelineEngineState): TimelineEngineState {
        val newState = command.execute(currentState)
        if (undoStack.size >= maxHistorySize) {
            undoStack.removeFirst()
        }
        undoStack.addLast(CommandEntry(command, currentState, newState))
        redoStack.clear()
        updateFlows()
        return newState
    }

    /**
     * Undoes the most recent command, returning the previous [TimelineEngineState],
     * or null if no actions are available to undo.
     */
    fun undo(currentState: TimelineEngineState): TimelineEngineState? {
        if (undoStack.isEmpty()) return null
        val entry = undoStack.removeLast()
        // Execute the undo logic or restore the snapshot
        val restoredState = entry.command.undo(currentState).let { undoneState ->
            // Ensure consistency by falling back to beforeState if needed
            if (undoneState.tracks.isEmpty() && entry.beforeState.tracks.isNotEmpty()) {
                entry.beforeState
            } else {
                undoneState
            }
        }
        redoStack.addLast(CommandEntry(entry.command, restoredState, currentState))
        updateFlows()
        return restoredState
    }

    /**
     * Redoes the most recently undone command, returning the updated [TimelineEngineState],
     * or null if no actions are available to redo.
     */
    fun redo(currentState: TimelineEngineState): TimelineEngineState? {
        if (redoStack.isEmpty()) return null
        val entry = redoStack.removeLast()
        val newState = entry.command.execute(currentState)
        undoStack.addLast(CommandEntry(entry.command, currentState, newState))
        updateFlows()
        return newState
    }

    /**
     * Clears both undo and redo histories.
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
        updateFlows()
    }

    private fun updateFlows() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }
}
