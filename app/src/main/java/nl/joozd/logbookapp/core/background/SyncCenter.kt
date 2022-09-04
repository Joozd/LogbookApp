package nl.joozd.logbookapp.core.background

import nl.joozd.logbookapp.core.TaskFlags

class SyncCenter (private val taskFlags: TaskFlags = TaskFlags) {
    fun syncDataFiles(){
        taskFlags.syncDataFiles(true)
    }
}