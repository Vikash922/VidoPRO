package com.example.feature.editor.history

import com.example.feature.timeline.engine.TimelineEngineState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Undo/Redo Manager implementing the Command Pattern for Video Editor actions (DEV-058, DEV-059).
 * Manages an undo stack and redo stack of [EditorCommand] pairs with their associated states.
 * Includes command coalescing to prevent flooding the history stack during drag gestures.
 */
class UndoRedoManager(
    private val maxHistorySize: Int = 30
) {
    private data class CommandEntry(
        val command: EditorCommand,
        val beforeState: TimelineEngineState,
        var afterState: TimelineEngineState
    )

    private val undoStack = ArrayDeque<CommandEntry>()
    private val redoStack = ArrayDeque<CommandEntry>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    /**
     * Executes a new command from the [currentState] and records it into the undo history.
     * Clears the redo stack. Coalesces rapid drag/trim commands for the same clip.
     */
    fun executeCommand(command: EditorCommand, currentState: TimelineEngineState): TimelineEngineState {
        val newState = command.execute(currentState)
        
        val topEntry = undoStack.lastOrNull()
        val canCoalesce = topEntry != null && shouldCoalesce(topEntry.command, command)

        if (canCoalesce) {
            // Update the top entry's afterState instead of pushing a new one
            topEntry!!.afterState = newState
        } else {
            if (undoStack.size >= maxHistorySize) {
                undoStack.removeFirst()
            }
            undoStack.addLast(CommandEntry(command, currentState, newState))
        }
        
        redoStack.clear()
        updateFlows()
        return newState
    }

    private fun shouldCoalesce(lastCmd: EditorCommand, newCmd: EditorCommand): Boolean {
        if (lastCmd.javaClass != newCmd.javaClass) return false
        
        return when {
            lastCmd is MoveClipCommand && newCmd is MoveClipCommand -> {
                lastCmd.clipId == newCmd.clipId
            }
            lastCmd is TrimStartCommand && newCmd is TrimStartCommand -> {
                lastCmd.clipId == newCmd.clipId
            }
            lastCmd is TrimEndCommand && newCmd is TrimEndCommand -> {
                lastCmd.clipId == newCmd.clipId
            }
            else -> false
        }
    }

    /**
     * Explicitly records a state change for operations outside the reducer (e.g. text clips).
     */
    fun recordStateChange(description: String, beforeState: TimelineEngineState, afterState: TimelineEngineState) {
        val command = StateSnapshotCommand(description, TimelineAction.SelectClip(afterState.selectedClipId), beforeState)
        if (undoStack.size >= maxHistorySize) {
            undoStack.removeFirst()
        }
        undoStack.addLast(CommandEntry(command, beforeState, afterState))
        redoStack.clear()
        updateFlows()
    }

    /**
     * Undoes the most recent command, returning the previous [TimelineEngineState],
     * or null if no actions are available to undo.
     */
    fun undo(currentState: TimelineEngineState): TimelineEngineState? {
        if (undoStack.isEmpty()) return null
        val entry = undoStack.removeLast()
        val restoredState = entry.beforeState

        redoStack.addLast(CommandEntry(entry.command, restoredState, entry.afterState))
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
        val newState = entry.afterState
        
        undoStack.addLast(CommandEntry(entry.command, entry.beforeState, newState))
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
