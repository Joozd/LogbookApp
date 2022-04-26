/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020-2022 Joost Welle
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

package nl.joozd.logbookapp.core

import nl.joozd.logbookapp.utils.DelegatesExt
import android.app.Application
import android.content.Context
import nl.joozd.logbookapp.ui.utils.DarkModeHub

class App : Application(){
    companion object {
        var instance: App by DelegatesExt.notNullSingleValue()
    }
    val ctx: Context by lazy {applicationContext}

    override fun onCreate() {
        super.onCreate()
        instance = this

        //Periodic workers get started as soon as app gets started
        JoozdlogWorkersHub.periodicGetAirportsFromServer()
        JoozdlogWorkersHub.periodicSynchronizeAircraftTypes()
        JoozdlogWorkersHub.periodicBackupFromServer()

        //Set long-running tasks for notifications
        BackupCenter.scheduleBackupNotification()

        // Set dark mode preference from Preferences
        DarkModeHub.setDarkMode()
    }
}