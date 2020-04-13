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

package nl.joozd.logbookapp.utils

import nl.joozd.logbookapp.data.dataclasses.Flight
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

fun cleanPlannedFlights(f: List<Flight>) : List<Flight> =
    f.map { if (it.planned && it.tIn < LocalDateTime.ofInstant(Calendar.getInstance().toInstant(), ZoneId.of("UTC")).minusMinutes(10)) it.copy(DELETEFLAG = 1, changed = 1) else it}