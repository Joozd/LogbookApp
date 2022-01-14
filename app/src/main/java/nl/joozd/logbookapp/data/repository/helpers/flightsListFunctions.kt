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

package nl.joozd.logbookapp.data.repository.helpers

import nl.joozd.logbookapp.model.dataclasses.Flight

/**
 * will return most recent flight that is not isPlanned
 * In case no most recent not-planned flight found, it will return an empty flight with flightID -1 (that needs to be adjusted before saving)
 */
fun mostRecentCompleteFlight(flights: List<Flight>?): Flight {
    return flights?.filter{ !it.isSim }?.maxByOrNull { if (!it.isPlanned && !it.DELETEFLAG) it.timeOut else 0 } ?: Flight.createEmpty()
}

