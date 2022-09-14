package nl.joozd.logbookapp.workmanager


// This is a class and not an object so I can inject stuff for testing when I think about that sort of thing
class ServerFunctionsWorkersHub: JoozdlogWorkersHub() {
    fun scheduleUpdateEmail(){
        enqueueOneTimeWorker<UpdateEmailWorker>(Tags.UPDATE_EMAIL, needNetwork = true)
    }

    fun scheduleConfirmEmail(){
        enqueueOneTimeWorker<ConfirmEmailWorker>(Tags.CONFIRM_EMAIL_CODE, needNetwork = true)
    }

    fun scheduleBackupEmail(){
        enqueueOneTimeWorker<SendBackupEmailWorker>(Tags.GET_BACKUP_EMAIL, needNetwork = true)
    }

    fun scheduleSubmitFeedback(){
        enqueueOneTimeWorker<SubmitFeedbackWorker>(Tags.SUBMIT_FEEDBACK, needNetwork = true)
    }

    fun scheduleSyncDataFiles(){
        enqueueOneTimeWorker<SyncDataFilesWorker>(Tags.SYNC_DATA_FILES, needNetwork = true)
    }


    object Tags{
        const val UPDATE_EMAIL = "UPDATE_EMAIL"
        const val CONFIRM_EMAIL_CODE = "CONFIRM_EMAIL_CODE"
        const val GET_BACKUP_EMAIL = "GET_BACKUP_EMAIL"
        const val SUBMIT_FEEDBACK = "SUBMIT_FEEDBACK"
        const val SYNC_DATA_FILES = "SYNC_DATA_FILES"
    }
}