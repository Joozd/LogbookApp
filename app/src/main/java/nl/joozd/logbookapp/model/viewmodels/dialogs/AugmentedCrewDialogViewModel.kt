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
import androidx.lifecycle.map
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.nullIfZero
import nl.joozd.logbookapp.extensions.toInt
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModelWithWorkingFlight

class AugmentedCrewDialogViewModel: JoozdlogDialogViewModelWithWorkingFlight() {

    fun crewDown() = workingFlight.crew-- // = crew-- but works on a val

    fun crewUp() = workingFlight.crew++ // = crew++ but works on a val

    /**
     * Set whether pilot did takeoff in seat or not
     */
    fun setTakeoff(didTakeoff: Boolean) {
        workingFlight.crew = workingFlight.crew.withTakeoff(didTakeoff)
    }

    /**
     * Set whether pilot did landing in seat or not
     */
    fun setLanding(didLanding: Boolean){
        workingFlight.crew = workingFlight.crew.withLanding(didLanding)
    }

    /**
     * Set time for takeoff and landing.
     * Will also save that amount of time to [Preferences.standardTakeoffLandingTimes]
     * @return [time]
     */
    fun setTakeoffLandingTime(time: Int) {
        workingFlight.crew = workingFlight.crew.withTimes(time)
        time.nullIfZero()?.let { Preferences.standardTakeoffLandingTimes = it }
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

    val crewSizeLiveData
        get() = workingFlight.crewLiveData.map { it.size }

    val didTakeoffLiveData
        get() = workingFlight.crewLiveData.map {it.takeoff}

    val didLandingLiveData
        get() = workingFlight.crewLiveData.map {it.landing}

    val takeoffLandingTimesLiveData
        get() = workingFlight.crewLiveData.map {(it.times.nullIfZero() ?: Preferences.standardTakeoffLandingTimes).toString()}
}
