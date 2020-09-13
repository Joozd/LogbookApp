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

package nl.joozd.logbookapp.model.viewmodels

import android.content.Context
import androidx.lifecycle.Transformations
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.InitialSetFlight

open class JoozdlogDialogViewModel: JoozdlogViewModel() {
    private val undoFlight = InitialSetFlight().apply{ flight = workingFlightRepository.workingFlight.value }
    protected val context: Context
        get() = App.instance.ctx

    fun undo(){
        undoFlight.flight?.let {workingFlightRepository.updateWorkingFlight(it)}
    }

    val flight = workingFlightRepository.workingFlight
    protected var workingFlight: Flight?
        get() = workingFlightRepository.workingFlight.value
        set(f) {
            f?.let {
                workingFlightRepository.updateWorkingFlight(it)
            }
        }
}