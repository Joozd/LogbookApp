package nl.joozd.logbookapp.model.viewmodels.status

sealed class CalendarDialogStatus{
    object DONE: CalendarDialogStatus()
    object NO_ICAL_LINK_FOUND: CalendarDialogStatus()

    class Error(val error: String): CalendarDialogStatus()
}