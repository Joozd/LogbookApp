package nl.joozd.logbookapp.core

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.core.metadata.Version
import nl.joozd.logbookapp.data.sharedPrefs.Prefs

class Migrations(private val prefs: Prefs = Prefs) {
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

    // map of version numbers to functions migrating from that version number to one version newer.
    // this way we can migrate from 0 to 1 by doing migrations[0]!!()
    private val migrations: Map<Int, suspend () -> Unit> = mapOf(
        0 to this::migrate0to1
    // 1 to this::migrate1to2 // etc
    )

    // This migration "0 to 1" takes care of replacing any old email data (login name based) to updated version (id based)
    private suspend fun migrate0to1(){
        require(prefs.configuredVersion() == 0) { "Cannot migrate from 0 if current version is not 0" }
        EmailCenter().migrateEmailDataIfNeeded()
        prefs.configuredVersion(1)
    }
}