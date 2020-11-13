/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020 Joost Welle
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Affero General Public License as
 *      published by the Free Software Foundation, either version 3 of the
 *      License, or (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Affero General Public License for more details.
 *
 *      You should have received a copy of the GNU Affero General Public License
 *      along with this program.  If not, see https://www.gnu.org/licenses
 *
 */

package nl.joozd.logbookapp

import nl.joozd.logbookapp.utils.DelegatesExt
import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.workmanager.JoozdlogWorkersHub


class App : Application(), CoroutineScope by MainScope() {
    companion object {
        var instance: App by DelegatesExt.notNullSingleValue()
    }

    val ctx: Context by lazy {applicationContext}

    override fun onCreate() {
        super.onCreate()
        instance = this
        launch{
            JoozdlogWorkersHub.periodicGetAirportsFromServer(Preferences.updateLargerFilesOverWifiOnly)
            JoozdlogWorkersHub.periodicSynchronizeAircraftTypes(Preferences.updateLargerFilesOverWifiOnly)
            JoozdlogWorkersHub.periodicBackupFromServer()
        }
    }



}