package nl.joozd.logbookapp.model.viewmodels.status

import android.net.Uri

sealed class SettingsActivityStatus{
    object SignedOut: SettingsActivityStatus()
    object AskIfNewAccountNeeded: SettingsActivityStatus()
    object CalendarDialogNeeded: SettingsActivityStatus()

    class Error(val errorResource: Int): SettingsActivityStatus()
}
