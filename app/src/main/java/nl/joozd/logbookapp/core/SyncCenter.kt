package nl.joozd.logbookapp.core

class SyncCenter (private val taskFlags: TaskFlags = TaskFlags) {
    fun syncDataFiles(){
        taskFlags.syncDataFiles(true)
    }
}