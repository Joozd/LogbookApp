package nl.joozd.logbookapp.model.viewmodels.status

sealed class EmailDialogStatus{
    object Done: EmailDialogStatus()
    object DoneNoChanges: EmailDialogStatus()

    class Error(val reason: EmailDialogStatusError): EmailDialogStatus()

    enum class EmailDialogStatusError{
        EMAILS_DO_NOT_MATCH,
        INVALID_EMAIL_ADDRESS,
        INVALID_EMAIL_ADDRESS_1,
        INVALID_EMAIL_ADDRESS_2,
        ENTRIES_DO_NOT_MATCH
    }
}

