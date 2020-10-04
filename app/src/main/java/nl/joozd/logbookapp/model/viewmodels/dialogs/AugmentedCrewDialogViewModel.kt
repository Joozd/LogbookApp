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

package nl.joozd.logbookapp.model.viewmodels.dialogs

import android.text.Editable
import androidx.lifecycle.Transformations
import nl.joozd.logbookapp.data.miscClasses.crew.Crew
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.nullIfZero
import nl.joozd.logbookapp.extensions.toInt
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModelWithWorkingFlight

class AugmentedCrewDialogViewModel: JoozdlogDialogViewModelWithWorkingFlight() {
    private val undoCrew = Crew.of(workingFlight.crew.toInt())
    private val crew // shortcut
        get() = workingFlight.crew

    val augmentedCrewData = crew

    fun crewDown() = crew.dec() // = crew-- but works on a val

    fun crewUp() = crew.inc() // = crew++ but works on a val

    fun setTakeoff(didTakeoff: Boolean) {
        workingFlight.crew.setDidTakeoff(didTakeoff)
    }
    fun setLanding(didLanding: Boolean){
        workingFlight.crew.setDidLanding(didLanding)
    }

    /**
     * Set time for takeoff and landing.
     * Will also save that amount of time to [Preferences.standardTakeoffLandingTimes]
     * @return [time]
     */
    fun setTakeoffLandingTime(time: Int) {
        workingFlight.crew.setNonstandardTakeoffLandingTimes(time)
        Preferences.standardTakeoffLandingTimes = time
    }

    /**
     * If time is blank, will set takeoffLandingTimes to preset in Preferences
     * Else, will set it to whatever is entered.
     * Needs to be able to do Editable.toInt() or will throw exception.
     */
    fun setTakeoffLandingTime(time: Editable) {
        if (time.isBlank()) setTakeoffLandingTime(Preferences.standardTakeoffLandingTimes)
        else {
            Preferences.standardTakeoffLandingTimes = time.toInt()
        }
    }

    /**
     * Observables:
     */

    val crewsize
        get() = crew.crewSize

    val didTakeoff
        get() = crew.didTakeoff

    val didLanding
        get() = crew.didLanding

    val takeoffLandingTimes = Transformations.map(crew.takeoffLandingTimes) {(it.nullIfZero() ?: Preferences.standardTakeoffLandingTimes).toString()}
}
