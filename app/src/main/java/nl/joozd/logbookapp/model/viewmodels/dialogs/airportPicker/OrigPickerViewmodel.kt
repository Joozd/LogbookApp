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

package nl.joozd.logbookapp.model.viewmodels.dialogs.airportPicker

import nl.joozd.logbookapp.data.dataclasses.Airport

class OrigPickerViewmodel: AirportPickerViewModel(){
    override val pickedAirport
        get() = workingFlight.originLiveData

    override fun pickAirport(airport: Airport) {
        workingFlight.orig = airport.ident
    }

    override fun setCustomAirport(airport: String) {
        workingFlight.orig = airport
    }

    /**
     * The airport that is set in [workingFlight] when viewmodel is initialized
     */
    override val initialAirport: Airport? = workingFlight.originLiveData.value

    /**
     * Set [initialAirport] back to [workingFlight]
     */
    override fun undo() {
        initialAirport?.let { workingFlight.orig = it.ident }
        Unit
    }

}