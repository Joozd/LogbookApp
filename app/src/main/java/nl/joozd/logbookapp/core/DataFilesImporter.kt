package nl.joozd.logbookapp.core

import nl.joozd.logbookapp.core.TaskFlags

//class instead of object for possible injection for tests
class DataFilesImporter(private val taskFlags: TaskFlags = TaskFlags) {
    fun importData(){
        taskFlags.postSyncDataFiles(true)
    }
}