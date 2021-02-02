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

package nl.joozd.logbookapp.data.repository.flightRepository

import android.util.Log
import nl.joozd.logbookapp.model.dataclasses.Flight

/**
 * for getting flights only once (avoid triggering livedata on rotation etc).
 * [flight] will stay available for reasone
 */
class SingleUseFlight(val flight: Flight) {
    private var unOpened = true

    fun get(): Flight? = if (unOpened) flight.also{
        Log.d("SingleUseFlight", "opened!")
        unOpened = false} else null.also{
        Log.d("SingleUseFlight", "tried to open AGAIN!")
    }
}