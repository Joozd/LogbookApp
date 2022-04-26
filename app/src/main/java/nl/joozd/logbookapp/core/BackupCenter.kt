package nl.joozd.logbookapp.core

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.export.JoozdlogExport
import nl.joozd.logbookapp.data.sharedPrefs.BackupPrefs
import nl.joozd.logbookapp.extensions.toDateStringForFiles
import nl.joozd.logbookapp.model.viewmodels.status.BackupCenterStatus
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut
import java.time.Instant
import java.time.LocalDate

object BackupCenter: CoroutineScope by MainScope() {
    val statusFlow: StateFlow<BackupCenterStatus?> = MutableStateFlow(null)
    private var status by CastFlowToMutableFlowShortcut(statusFlow)

    fun backUpNow() = launch {
        val dateString = LocalDate.now().toDateStringForFiles()
        status = BackupCenterStatus.BuildingCsv
        val uri = JoozdlogExport.shareCsvExport("joozdlog_backup_$dateString")
        status = BackupCenterStatus.SharedUri(uri)
        BackupPrefs.backupIgnoredExtraDays = 0
        BackupPrefs.mostRecentBackup = Instant.now().epochSecond
    }

    /**
     * Reset all (status, job for triggering notification)
     */
    fun reset(){
        status = null
        //TODO reset worker job
    }

    fun scheduleBackupNotification(){
        TODO()
    }


}