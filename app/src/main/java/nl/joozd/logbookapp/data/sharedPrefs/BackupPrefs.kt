package nl.joozd.logbookapp.data.sharedPrefs

import kotlinx.coroutines.flow.combine
import nl.joozd.logbookapp.core.Constants.ONE_DAY_IN_SECONDS

object BackupPrefs: JoozdLogPreferences() {
    override val preferencesFileKey = "nl.joozd.logbookapp.BACKUP_PREFS_FILE"
    /*
    private const val XXXXXXXXXX = "XXXXXXXXXX"
     */

    // NOTE reading this is a blocking IO operation
    val backupInterval get() = Prefs.backupInterval
    val backupIntervalFlow get() = Prefs.backupIntervalFlow

    private const val MOST_RECENT_BACKUP = "MOST_RECENT_BACKUP"
    var mostRecentBackup: Long by JoozdLogSharedPreferenceNotNull(MOST_RECENT_BACKUP,0L)
    val mostRecentBackupFlow by PrefsFlow(MOST_RECENT_BACKUP, 0L)

    private const val BACKUP_IGNORED_UNTIL = "BACKUP_IGNORED_UNTIL"
    var backupIgnoredUntil: Long by JoozdLogSharedPreferenceNotNull(BACKUP_IGNORED_UNTIL,0)
    val backupIgnoredUntilFlow by PrefsFlow(BACKUP_IGNORED_UNTIL, 0L)

    val nextBackupNeededFlow = combine(backupIntervalFlow, mostRecentBackupFlow, backupIgnoredUntilFlow)
    { interval, mostRecent, backupIgnoredUntil ->
        maxOf(mostRecent + interval * ONE_DAY_IN_SECONDS, backupIgnoredUntil)
    }
}