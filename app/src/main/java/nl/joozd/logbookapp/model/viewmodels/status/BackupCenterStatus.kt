package nl.joozd.logbookapp.model.viewmodels.status

import android.net.Uri

sealed class BackupCenterStatus {
    object BuildingCsv: BackupCenterStatus()
    class SharedUri(val uri: Uri): BackupCenterStatus()
}