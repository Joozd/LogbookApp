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


/**
 * Cleans flights parsed by [MonthlyOverview] instances
 * Gets airport and aircraft data from repository.
 * usage:
 * val cleaner =
 */
package nl.joozd.logbookapp.data.parseSharedFiles.pdfparser.helpers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.parseSharedFiles.interfaces.MonthlyOverview
import nl.joozd.logbookapp.data.repository.AircraftRepository
import nl.joozd.logbookapp.data.repository.AirportRepository
import nl.joozd.logbookapp.data.repository.helpers.findBestHitForRegistration
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.TimestampMaker
import nl.joozd.logbookapp.utils.TwilightCalculator
import nl.joozd.logbookapp.utils.reversed

class ImportedFlightsCleaner(private val dirtyFlights: List<Flight>?, private val carrier: String? = null): CoroutineScope by MainScope() {
    private val airportRepository = AirportRepository.getInstance()
    private val aircraftRepository = AircraftRepository.getInstance()
    private val icaoIataMapAsync = async(Dispatchers.IO) {airportRepository.getIcaoToIataMap()}
    private val aircraftMapAsync = async(Dispatchers.IO) { aircraftRepository.requireMap() }

    suspend fun cleanFlights(): List<Flight>?{
        val iataToIcaoMap = icaoIataMapAsync.await().reversed()
        val aircraftMap = aircraftMapAsync.await()
        val now = TimestampMaker.nowForSycPurposes
        return dirtyFlights?.map{cleanFlight(it, aircraftMap, iataToIcaoMap, now)}
    }

    /**
     * changes IATA to ICAO idents (leaves ICAO alone)
     * changes aircraft type to the one from database
     * completes registration if needed (eg. KLC monthlies state "EXY" which should be "PH-EXY"
     * Calculates night time if autoFill is true
     * Sets timestamp to now
     */
    private suspend fun cleanFlight(f: Flight, acMap: Map<String, Aircraft>, iataIcaoMap: Map<String, String>, now: Long): Flight{
        with (f){
            val twilightCalculator = TwilightCalculator(timeOut)

            val cleanOrig = iataIcaoMap[orig] ?: orig
            val cleanDest = iataIcaoMap[dest] ?: dest
            val bestHitRegistration = registration.findBestHitForRegistration(acMap.keys) // null if no hit found
            val cleanRegistation = bestHitRegistration ?: completeRegistration(registration)
            val cleanType = bestHitRegistration?.let {acMap[it]?.type?.shortName} ?: f.aircraftType
            val nightTime = if (autoFill) twilightCalculator.minutesOfNight(airportRepository.getAirportByIcaoIdentOrNull(cleanOrig), airportRepository.getAirportByIcaoIdentOrNull(cleanDest), timeOut, timeIn) else 0

            return f.copy(orig = cleanOrig, dest = cleanDest, registration = cleanRegistation, aircraftType = cleanType, nightTime = nightTime, timeStamp = now)
        }
    }

    private fun completeRegistration(partialReg: String): String = when (carrier){
        Carriers.KLC ->  KLC_REGISTRATION_PREFIX + partialReg + KLC_REGISTRATION_SUFFIX
        else -> partialReg
    }

    companion object{
        object Carriers{
            const val NONE = "NONE"
            const val KLC = "KLC"
        }

        private const val UNKNOWN_TYPE = "UNKNOWN"
        private const val KLC_REGISTRATION_PREFIX = "PH-"
        private const val KLC_REGISTRATION_SUFFIX = ""
    }

}