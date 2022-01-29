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

package nl.joozd.logbookapp.model.viewmodels.dialogs.airportPicker

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import nl.joozd.logbookapp.data.dataclasses.Airport

@ExperimentalCoroutinesApi
class DestPickerViewModel: AirportPickerViewModel(){
    override val pickedAirport
        get() = MutableLiveData(Airport())

    override fun pickAirport(airport: Airport) {
        flightEditor.dest = airport
    }



    override fun setCustomAirport(airport: String) {
        flightEditor.dest = Airport(ident = airport)
    }

    /**
     * The airport that is set in [workingFlight] when viewmodel is initialized
     */
    override val initialAirport: Airport = flightEditor.dest

}