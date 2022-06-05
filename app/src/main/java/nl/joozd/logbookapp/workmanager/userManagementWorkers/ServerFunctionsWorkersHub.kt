package nl.joozd.logbookapp.workmanager.userManagementWorkers

import nl.joozd.logbookapp.workmanager.JoozdlogWorkersHub
import nl.joozd.logbookapp.workmanager.SendBackupEmailWorker
import nl.joozd.logbookapp.workmanager.SendLoginLinkEmailWorker
import nl.joozd.logbookapp.workmanager.SyncDataFilesWorker

// This is a class and not an object so I can inject stuff for testing when I think about that sort of thing
class ServerFunctionsWorkersHub: JoozdlogWorkersHub() {
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

    fun scheduleLoginLinkEmail(){
        enqueueOneTimeWorker<SendLoginLinkEmailWorker>(Tags.REQUEST_LOGIN_LINK_MAIL, needNetwork = true)
    }

    fun scheduleSyncDataFiles(){
        enqueueOneTimeWorker<SyncDataFilesWorker>(Tags.REQUEST_LOGIN_LINK_MAIL, needNetwork = true)
    }

    object Tags{
        const val CREATE_NEW_USER = "CREATE_NEW_USER"
        const val UPDATE_EMAIL = "UPDATE_EMAIL"
        const val CONFIRM_EMAIL_CODE = "CONFIRM_EMAIL_CODE"
        const val GET_BACKUP_EMAIL = "GET_BACKUP_EMAIL"
        const val REQUEST_LOGIN_LINK_MAIL = "REQUEST_LOGIN_LINK_MAIL"
    }
}