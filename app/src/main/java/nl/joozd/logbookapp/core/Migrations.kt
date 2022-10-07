package nl.joozd.logbookapp.core

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.comm.migrateLoginDataIfNeeded
import nl.joozd.logbookapp.core.metadata.Version
import nl.joozd.logbookapp.data.sharedPrefs.EmailPrefs
import nl.joozd.logbookapp.data.sharedPrefs.Prefs

class Migrations(private val prefs: Prefs = Prefs, private val emailPrefs: EmailPrefs = EmailPrefs) {
    // Launches migrations async. For now, no migration needs to be finished before app starts.
    // Migrations in Database structure will be handled by Room.
    fun migrateToCurrent(){
        MainScope().launch {
            val oldVersion = prefs.configuredVersion()
            val currentVersion = Version.currentVersion
            (oldVersion until currentVersion).forEach {
                migrations[it]!!()
            }
        }
    }

    private val migrations: Map<Int, suspend () -> Unit> = mapOf(
        0 to this::migrate0to1
    )

    /*
        Migration takes care of replacing any old email data (login name based) to updated version (id based)
     */
    private suspend fun migrate0to1(){
        migrateLoginDataIfNeeded(emailPrefs)
    }
}