/*
 * JoozdLog Pilot's Logbook
 * Copyright (C) 2020 Joost Welle
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see https://www.gnu.org/licenses
 */

package nl.joozd.logbookapp.extensions

import nl.joozd.logbookapp.data.dataclasses.Flight

fun Flight.asSimIfNeeded(): Flight {
    return if (this.isSim == 0) this
    else this.copy(orig="SIMULATOR", dest="SIMULATOR", timeIn = timeOut, registration = "", nightTime = 0, ifrTime = 0, isPIC = 0, isPICUS = 0, isCoPilot = 0, isDual = 0, isInstructor = 0)
}