package nl.joozd.logbookapp.workmanager.userManagementWorkers

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import nl.joozd.logbookapp.core.JoozdlogWorkersHubOld
import nl.joozd.logbookapp.core.JoozdlogWorkersHubOld.needsNetwork
import nl.joozd.logbookapp.workmanager.JoozdlogWorkersHub
import nl.joozd.logbookapp.workmanager.SendBackupEmailWorker

class UserManagementWorkersHub: JoozdlogWorkersHub() {
    fun scheduleCreateNewUser(){
        enqueueOneTimeWorker<CreateNewUserWorker>(Tags.CREATE_NEW_USER, needNetwork = true)
    }

    fun scheduleUpdateEmail(){
        enqueueOneTimeWorker<UpdateEmailWorker>(Tags.UPDATE_EMAIL, needNetwork = true)
    }

    fun scheduleConfirmEmail(){
        enqueueOneTimeWorker<ConfirmEmailWorker>(Tags.CONFIRM_EMAIL_CODE, needNetwork = true)
    }

    fun scheduleBackupEmail(){
        enqueueOneTimeWorker<SendBackupEmailWorker>(Tags.GET_BACKUP_EMAIL, needNetwork = true)
    }

    object Tags{
        const val CREATE_NEW_USER = "CREATE_NEW_USER"
        const val UPDATE_EMAIL = "UPDATE_EMAIL"
        const val CONFIRM_EMAIL_CODE = "CONFIRM_EMAIL_CODE"
        const val GET_BACKUP_EMAIL = "GET_BACKUP_EMAIL"
    }
}