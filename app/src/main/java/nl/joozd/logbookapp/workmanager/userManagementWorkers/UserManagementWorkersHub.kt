package nl.joozd.logbookapp.workmanager.userManagementWorkers

import nl.joozd.logbookapp.workmanager.JoozdlogWorkersHub

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

    object Tags{
        const val CREATE_NEW_USER = "CREATE_NEW_USER"
        const val UPDATE_EMAIL = "UPDATE_EMAIL"
        const val CONFIRM_EMAIL_CODE = "CONFIRM_EMAIL_CODE"
    }
}