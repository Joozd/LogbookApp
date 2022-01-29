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

package nl.joozd.logbookapp.model.viewmodels.dialogs

import android.text.Editable
import android.util.Log
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import kotlinx.coroutines.flow.map
import nl.joozd.logbookapp.data.miscClasses.crew.AugmentedCrew
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.nullIfZero
import nl.joozd.logbookapp.extensions.toInt
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel
import nl.joozd.logbookapp.model.workingFlight.FlightEditor

class AugmentedCrewDialogViewModel: JoozdlogDialogViewModel() {
    fun crewDown() {
      TODO("WIP")
    }

    fun crewUp() {
        TODO("WIP")
    }
    /**
     * Set whether pilot did takeoff in seat or not
     */
    fun setTakeoff(didTakeoff: Boolean) {
        TODO("WIP")
    }

    /**
     * Set whether pilot did landing in seat or not
     */
    fun setLanding(didLanding: Boolean) {
        TODO("WIP")
    }

    /**
     * Set time for takeoff and landing.
     * Will also save that amount of time to [Preferences.standardTakeoffLandingTimes]
     * @return [time]
     */
    fun setTakeoffLandingTime(time: Int) {
        TODO("WIP")
    }

    /**
     * If time is blank, will set takeoffLandingTimes to preset in Preferences
     * Else, will set it to whatever is entered.
     * Needs to be able to do Editable.toInt() or will throw exception.
     */
    fun setTakeoffLandingTime(time: Editable?) {
        if (time == null){
            Log.w("setTakeOffLanding()", "called with null reference for time, ignoring.")
            return
        }
        if (!time.isDigitsOnly()){
            Log.w("setTakeOffLanding()", "called with bad time: $time, must be digits only, ignoring.")
            return
        }
        setTakeoffLandingTime(if (time.isBlank()) 0 else time.toInt())
    }

    /**
     * Observables:
     */
    private val tempCrewLiveData = flightEditor.flightFlow.map { AugmentedCrew.of(it.augmentedCrew)}.asLiveData()

    val crewSizeLiveData
        get() = tempCrewLiveData.map { it.size }

    val didTakeoffLiveData
        get() = tempCrewLiveData.map {it.takeoff}

    val didLandingLiveData
        get() = tempCrewLiveData.map {it.landing}

    val takeoffLandingTimesLiveData
        get() = tempCrewLiveData.map {(it.times.nullIfZero() ?: Preferences.standardTakeoffLandingTimes).toString()}
}
