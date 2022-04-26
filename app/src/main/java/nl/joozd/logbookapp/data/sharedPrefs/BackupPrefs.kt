package nl.joozd.logbookapp.data.sharedPrefs

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

object BackupPrefs: JoozdLogPreferences() {
    override val preferencesFileKey = "nl.joozd.logbookapp.BACKUP_PREFS_FILE"

    // NOTE reading this is a blocking IO operation
    val backupInterval get() = Prefs.backupInterval
    val backupIntervalFlow get() = Prefs.backupIntervalFlow

    var mostRecentBackup: Long by JoozdLogSharedPreference(0L)
    val mostRecentBackupFlow by PrefsFlow(mostRecentBackup)

    var backupIgnoredExtraDays: Int by JoozdLogSharedPreference(0)
    val backupIgnoredExtraDaysFlow by PrefsFlow(backupIgnoredExtraDays, 0)

    val backupNeededFlow = combine(backupIntervalFlow, mostRecentBackupFlow, backupIgnoredExtraDaysFlow)
    { interval, mostRecent, ignoredDays ->

    }
}