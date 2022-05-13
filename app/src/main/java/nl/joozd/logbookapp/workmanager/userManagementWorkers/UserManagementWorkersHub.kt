package nl.joozd.logbookapp.workmanager.userManagementWorkers

import nl.joozd.logbookapp.workmanager.JoozdlogWorkersHub

class UserManagementWorkersHub: JoozdlogWorkersHub() {
    fun scheduleCreateNewUser(){
        enqueueOneTimeWorker<CreateNewUserWorker>(Tags.CREATE_NEW_USER, needNetwork = true)
    }

    fun scheduleRequestEmailVerificationMail() {
        enqueueOneTimeWorker<RequestEmailVerificationMailWorker>(Tags.REQUEST_EMAIL_VERIFICATION_MAIL, needNetwork = true)
    }

    object Tags{
        const val CREATE_NEW_USER = "CREATE_NEW_USER"
        const val REQUEST_EMAIL_VERIFICATION_MAIL = "REQUEST_EMAIL_VERIFICATION_MAIL"
    }
}