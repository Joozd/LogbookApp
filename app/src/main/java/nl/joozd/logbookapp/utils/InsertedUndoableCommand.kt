package nl.joozd.logbookapp.utils

class InsertedUndoableCommand(
    action: suspend () -> Unit,
    undoAction: suspend () -> Unit
): UndoableCommand(action, undoAction)
