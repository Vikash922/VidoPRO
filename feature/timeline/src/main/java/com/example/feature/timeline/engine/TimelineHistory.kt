package com.example.feature.timeline.engine

/**
 * Pure Kotlin history stack for timeline undo and redo operations (DEV-042).
 */
class TimelineHistory(
    private val maxHistorySize: Int = 30
) {
    private val undoStack = ArrayDeque<TimelineEngineState>()
    private val redoStack = ArrayDeque<TimelineEngineState>()

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    fun pushState(currentState: TimelineEngineState) {
        if (undoStack.size >= maxHistorySize) {
            undoStack.removeFirst()
        }
        undoStack.addLast(currentState)
        redoStack.clear()
    }

    fun undo(currentState: TimelineEngineState): TimelineEngineState? {
        if (undoStack.isEmpty()) return null
        redoStack.addLast(currentState)
        return undoStack.removeLast()
    }

    fun redo(currentState: TimelineEngineState): TimelineEngineState? {
        if (redoStack.isEmpty()) return null
        undoStack.addLast(currentState)
        return redoStack.removeLast()
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
