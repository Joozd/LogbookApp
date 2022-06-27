package nl.joozd.logbookapp.data.sharedPrefs

import kotlinx.coroutines.flow.combine
import nl.joozd.logbookapp.core.Constants.ONE_DAY_IN_SECONDS

object BackupPrefs: JoozdLogPreferences() {
    override val preferencesFileKey = "nl.joozd.logbookapp.BACKUP_PREFS_FILE"
    /*
    private const val XXXXXXXXXX = "XXXXXXXXXX"
     */

    private const val MOST_RECENT_BACKUP = "MOST_RECENT_BACKUP"
    private const val BACKUP_IGNORED_UNTIL = "BACKUP_IGNORED_UNTIL"

    val mostRecentBackup by JoozdlogSharedPreferenceDelegate(MOST_RECENT_BACKUP,0L)
    val backupIgnoredUntil by JoozdlogSharedPreferenceDelegate(BACKUP_IGNORED_UNTIL,0L)

    val nextBackupNeededFlow = combine(Prefs.backupInterval.flow, mostRecentBackup.flow, backupIgnoredUntil.flow)
    { interval, mostRecent, backupIgnoredUntil ->
        maxOf(mostRecent + interval * ONE_DAY_IN_SECONDS, backupIgnoredUntil)
    }
}