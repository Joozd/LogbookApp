package nl.joozd.logbookapp.data.sharedPrefs

import nl.joozd.logbookapp.data.sharedPrefs.utils.JoozdLogPreferences
import nl.joozd.logbookapp.data.sharedPrefs.utils.JoozdlogSharedPreferenceDelegate

object DataVersions: JoozdLogPreferences() {
    override val preferencesFileKey: String = "nl.joozd.logbookapp.DATA_VERSIONS_PREFS_KEY"

    private const val MOST_RECENT_SYNC_EPOCH_SECOND = "MOST_RECENT_SYNC_EPOCH_SECOND"
    private const val AIRCRAFT_TYPES_VERSION_KEY = "AIRCRAFT_TYPES_VERSION_KEY"
    private const val AIRCRAFT_FORCED_TYPES_VERSION_KEY = "AIRCRAFT_FORCED_TYPES_VERSION_KEY"
    private const val AIRPORTS_VERSION_KEY = "AIRPORTS_VERSION_KEY"

    val mostRecentDataFilesSyncEpochSecond by JoozdlogSharedPreferenceDelegate(MOST_RECENT_SYNC_EPOCH_SECOND, -1L)

    val aircraftTypesVersion by JoozdlogSharedPreferenceDelegate(AIRCRAFT_TYPES_VERSION_KEY, -1)
    val aircraftForcedTypesVersion by JoozdlogSharedPreferenceDelegate(AIRCRAFT_FORCED_TYPES_VERSION_KEY, -1)
    val airportsVersion by JoozdlogSharedPreferenceDelegate(AIRPORTS_VERSION_KEY, -1)
}