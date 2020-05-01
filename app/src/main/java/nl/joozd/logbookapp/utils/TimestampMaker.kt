package nl.joozd.logbookapp.utils

import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.withMinimumValue
import java.time.Instant

object TimestampMaker {
    val nowForSycPurposes: Long
        get() = (Instant.now().epochSecond + Preferences.serverTimeOffset).withMinimumValue(Preferences.lastUpdateTime+1)
}