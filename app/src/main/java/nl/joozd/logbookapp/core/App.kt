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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.data.repository.airportrepository.AirportRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository

class App : Application(){
    companion object {
        var instance: App by DelegatesExt.notNullSingleValue()
    }
    val ctx: Context by lazy {applicationContext}

    override fun onCreate() {
        super.onCreate()
        instance = this
        //Make sure repositories are up and running
        initializeRepositories()

        // Set dark mode preference from Preferences
        DarkModeCenter.setDarkMode()
    }

    private fun initializeRepositories() {
        MainScope().launch {
            AirportRepository.instance
        }
        MainScope().launch {
            AircraftRepository.instance
        }
        MainScope().launch {
            FlightRepository.instance
        }
    }
}