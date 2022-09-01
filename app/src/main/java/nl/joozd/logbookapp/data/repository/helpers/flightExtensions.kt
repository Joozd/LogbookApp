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

import nl.joozd.logbookapp.data.repository.airportrepository.AirportDataCache
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.extensions.nullIfEmpty
import nl.joozd.logbookapp.extensions.nullIfZero
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.TwilightCalculator
import java.time.*
import java.util.*

fun Flight.isSamedPlannedFlightAs(f: Flight) =
    isSameFlightAs(f) && isPlanned

/**
 * Checks if two flights are the same physical flight (ie the same orig to same dest at the same time with the same flightnumber)
 */
fun Flight.isSameFlightAs(f: Flight, withMargins: Boolean = false) = (Prefs.maxChronoAdjustment * 60L).let { margin ->
       orig == f.orig
            && dest == f.dest
            && if (withMargins) timeOut in (f.timeOut - margin..f.timeOut + margin) else timeOut == f.timeOut
            && if (withMargins) timeIn in (f.timeIn - margin..f.timeIn + margin) else timeIn == f.timeIn
            && hasSameFlightNumberAs(f)
}

/**
 * Merge data into other flight, if filled
 *  - Flight number
 *  - time out (not overwritten if 0)
 *  - time in (not overwritten if 0)
 *  - Registration
 *  - Aircraft Type
 *  - isPlanned flag if not planned
 */
@Deprecated("deprecated, use Flight.mergeOnto", replaceWith = ReplaceWith("Flight.mergeOnto"))
fun Flight.mergeInto(other: Flight) = other.copy(
    flightNumber = flightNumber.nullIfEmpty() ?: other.flightNumber,
    ifrTime = ifrTime,
    timeOut = timeOut.nullIfZero() ?: other.timeOut,
    timeIn = timeIn.nullIfZero() ?: other.timeIn,
    registration = registration.nullIfEmpty() ?: other.registration,
    aircraftType = aircraftType.nullIfEmpty() ?: other.aircraftType,
    isPlanned = isPlanned && other.isPlanned
)

/*
 * auto fills night time only for now
 */
fun Flight.setNightTime(airportDataCache: AirportDataCache): Flight {
    val origAirport = airportDataCache.getAirportByIcaoIdentOrNull(orig)
    val destAirport = airportDataCache.getAirportByIcaoIdentOrNull(dest)
    val nightTime = TwilightCalculator(tOut()).minutesOfNight(origAirport, destAirport, tOut(), tIn())
    return this.copy(nightTime = nightTime)
}

fun Flight.makeReturnFlight(): Flight {
    val now = ZonedDateTime.now().withSecond(0)
    val nowRoundedDownToFiveMinutes = now.withMinute((now.minute/5) * 5).toEpochSecond()
    return Flight(
        orig = dest,
        dest = orig,
        registration = registration,
        aircraftType = aircraftType,
        name = name,
        name2 = name2,
        isPIC = isPIC,
        isSim = isSim,
        flightNumber = increaseFlightNumberByOne(flightNumber),
        timeOut = nowRoundedDownToFiveMinutes,
        timeIn = nowRoundedDownToFiveMinutes + 3600,
        isPlanned = true
    )
}

//increase the last number in a string
//eg KL1234 becomes KL1235, HB901D becomes HB902D and HV999 becomes HV1000
private fun increaseFlightNumberByOne(fn: String): String {
    val regex = """\d+""".toRegex()
    val lastHit = regex.findAll(fn).lastOrNull()?.value ?: return fn
    return fn.replace(lastHit, (lastHit.toInt() + 1).toString())
}

fun Flight.hasSameFlightNumberAs(other: Flight) = flightNumber.uppercase(Locale.ROOT).trim() == other.flightNumber.uppercase(Locale.ROOT).trim()

fun Flight.iataToIcaoAirports(adc: AirportDataCache): Flight {
    val o = adc.iataToIcao(orig) ?: orig
    val d = adc.iataToIcao(dest) ?: dest
    return copy(orig = o, dest = d)
}