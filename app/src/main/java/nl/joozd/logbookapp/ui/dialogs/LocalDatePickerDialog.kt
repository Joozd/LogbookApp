/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2021 Joost Welle
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

package nl.joozd.logbookapp.ui.dialogs

import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import java.time.LocalDate

/**
 * Update flight when a date is picked
 * [wf] will take care of exactly that happens
 */
class LocalDatePickerDialog: LocalDatePickerFragment() {
    private val wf = FlightRepository.getInstance().getWorkingFlight()
    override fun onDateSelectedListener(date: LocalDate?) {
        date?.let { wf.date = it }
    }
}