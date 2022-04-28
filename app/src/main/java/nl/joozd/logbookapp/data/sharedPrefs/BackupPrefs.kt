package nl.joozd.logbookapp.data.sharedPrefs

import kotlinx.coroutines.flow.combine
import nl.joozd.logbookapp.core.Constants.ONE_DAY_IN_SECONDS

object BackupPrefs: JoozdLogPreferences() {
    override val preferencesFileKey = "nl.joozd.logbookapp.BACKUP_PREFS_FILE"

    // NOTE reading this is a blocking IO operation
    val backupInterval get() = Prefs.backupInterval
    val backupIntervalFlow get() = Prefs.backupIntervalFlow

    var mostRecentBackup: Long by JoozdLogSharedPreference(0L)
    val mostRecentBackupFlow by PrefsFlow(mostRecentBackup, 0L)

    var backupIgnoredUntil: Long by JoozdLogSharedPreference(0)
    val backupIgnoredUntilFlow by PrefsFlow(backupIgnoredUntil, 0)

    val nextBackupNeededFlow = combine(backupIntervalFlow, mostRecentBackupFlow, backupIgnoredUntilFlow)
    { interval, mostRecent, backupIgnoredUntil ->
        maxOf(mostRecent + interval * ONE_DAY_IN_SECONDS, backupIgnoredUntil)
    }
}