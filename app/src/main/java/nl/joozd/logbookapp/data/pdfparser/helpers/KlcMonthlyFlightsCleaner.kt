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
 * Cleans flights parsed by [KlcMonthlyParser]
 * Gets airport and aircraft data from repository.
 * usage:
 * val cleaner =
 */
package nl.joozd.logbookapp.data.pdfparser.helpers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.repository.AircraftRepository
import nl.joozd.logbookapp.data.repository.AirportRepository
import nl.joozd.logbookapp.data.repository.helpers.findBestHitForRegistration
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.TimestampMaker
import nl.joozd.logbookapp.utils.reversed
import kotlin.coroutines.CoroutineContext

class KlcMonthlyFlightsCleaner(private val dirtyFlights: List<Flight>?): CoroutineScope by MainScope() {
    private val airportRepository = AirportRepository.getInstance()
    private val aircraftRepository = AircraftRepository.getInstance()
    private val icaoIataMapAsync = async(Dispatchers.IO) {airportRepository.getIcaoToIataMap()}
    private val aircraftMapAsync = async(Dispatchers.IO) { aircraftRepository.requestAircraftMap() }

    suspend fun cleanFlights(): List<Flight>?{
        val iataToIcaoMap = icaoIataMapAsync.await().reversed()
        val aircraftMap = aircraftMapAsync.await()
        val now = TimestampMaker.nowForSycPurposes
        return dirtyFlights?.map{cleanFlight(it, aircraftMap, iataToIcaoMap, now)}
    }

    private fun cleanFlight(f: Flight, acMap: Map<String, Aircraft>, iataIcaoMap: Map<String, String>, now: Long): Flight{
        with (f){
            val cleanOrig = iataIcaoMap[orig] ?: orig
            val cleanDest = iataIcaoMap[dest] ?: dest
            val bestHitRegistration = registration.findBestHitForRegistration(acMap.keys) // null if no hit found
            val cleanRegistation = bestHitRegistration ?: KLC_REGISTRATION_PREFIX + registration + KLC_REGISTRATION_SUFFIX
            val cleanType = bestHitRegistration?.let {acMap[it]?.type?.shortName} ?: UNKNOWN_TYPE

            return f.copy(orig = cleanOrig, dest = cleanDest, registration = cleanRegistation, aircraftType = cleanType, timeStamp = now)
        }
    }

    companion object{
        const val UNKNOWN_TYPE = "UNKNOWN"
        const val KLC_REGISTRATION_PREFIX = "PH-"
        const val KLC_REGISTRATION_SUFFIX = ""
    }

}