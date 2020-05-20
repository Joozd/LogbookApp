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

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import nl.joozd.logbookapp.data.miscClasses.Crew
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel

class AugmentedCrewDialogViewModel: JoozdlogDialogViewModel() {
    val augmentedCrewData: LiveData<Crew> = Transformations.map(flight) { Crew.of(it.augmentedCrew)}

    fun crewDown(){
        workingFlightRepository.crew?.let{
            workingFlightRepository.crew = it - 1
        }
    }
    fun crewUp(){
        workingFlightRepository.crew?.let{
            workingFlightRepository.crew = it + 1
        }
    }
    fun setTakeoff(takeoff: Boolean) {
        workingFlightRepository.crew?.let {
            workingFlightRepository.crew = it.apply {
                didTakeoff = takeoff
            }
        }
    }
    fun setLanding(landing: Boolean){
        workingFlightRepository.crew?.let {
            workingFlightRepository.crew = it.apply {
                didLanding = landing
            }
        }
    }
    fun setTakeoffLandingTime(time: Int){
        workingFlightRepository.crew?.let {
            workingFlightRepository.crew = it.apply {
                takeoffLandingTimes = time
            }
        }
    }
}
