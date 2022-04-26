package nl.joozd.logbookapp.model.viewmodels.status

import android.net.Uri

sealed class SettingsActivityStatus{
    object SignedOut: SettingsActivityStatus()
    object LoginLinkCopied: SettingsActivityStatus()
    object AskIfNewAccountNeeded: SettingsActivityStatus()
    object CalendarDialogNeeded: SettingsActivityStatus()

    class Error(val errorResource: Int): SettingsActivityStatus()
}
