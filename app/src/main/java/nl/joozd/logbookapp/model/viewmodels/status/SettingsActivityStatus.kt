package nl.joozd.logbookapp.model.viewmodels.status

sealed class SettingsActivityStatus{
    object CalendarDialogNeeded: SettingsActivityStatus()

    class Error(val errorResource: Int): SettingsActivityStatus()
}
