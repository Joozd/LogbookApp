package nl.joozd.logbookapp.model.viewmodels.dialogs.status

sealed class Status{
    object DONE: Status()
    object NO_ICAL_LINK_FOUND: Status()

    class Error(val error: String): Status()
}