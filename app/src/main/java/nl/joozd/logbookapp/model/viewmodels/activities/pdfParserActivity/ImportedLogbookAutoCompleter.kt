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

package nl.joozd.logbookapp.model.viewmodels.activities.pdfParserActivity

import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.joozdlogimporter.dataclasses.ExtractedCompleteLogbook
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftDataCache
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.data.repository.airportrepository.AirportDataCache
import nl.joozd.logbookapp.data.repository.airportrepository.AirportRepository
import nl.joozd.logbookapp.extensions.nullIfBlank
import nl.joozd.logbookapp.model.ModelFlight
import nl.joozd.logbookapp.model.dataclasses.Flight


/**
 * Complete imported lgbook entries:
 * - Night Time
 * - Multi-pilot time
 * - Missing aircraft types from other flights or [AirportRepository]
 */
class ImportedLogbookAutoCompleter(
    val aircraftRepository: AircraftRepository = AircraftRepository.instance,
    val airportRepository: AirportRepository = AirportRepository.instance
) {
    suspend fun autocomplete(importedLogbook: ExtractedCompleteLogbook): SanitizedCompleteLogbook{
        val aircraftDataCache = aircraftRepository.getAircraftDataCache()
        val airportDataCache = airportRepository.getAirportDataCache()

        val dirtyFlights = importedLogbook.flightsWithUppercaseRegs() ?: return SanitizedCompleteLogbook(null)

        val rtMap = makeRegistrationToTypesMap(dirtyFlights)

        val cleanedFlights = dirtyFlights.map{
            println("Cleaning $it")
            if (it.isSim) it else
            it.fixAircraftType(rtMap, aircraftDataCache)
                .autoValues(airportDataCache, aircraftDataCache)
        }
        return SanitizedCompleteLogbook(cleanedFlights)
    }

    //make sure all registrations are uppercase because we want it to be not case-sensitive
    private fun ExtractedCompleteLogbook.flightsWithUppercaseRegs() =
        flights?.map { it.copy(registration = it.registration.uppercase()) }

    /*
     * Priority:
     * - Entered data if complete
     * - Data completed from other flights in this logbook
     * - Data from repository
     * - Incomplete data
     */
    private fun BasicFlight.fixAircraftType(rtMap: Map<String, String>, adc: AircraftDataCache): BasicFlight =
        when {
            registration.isBlank() -> this
            aircraft.isNotBlank() -> this
            else -> rtMap[registration]?.let { this.copy(aircraft = it) }
                ?: adc.getAircraftFromRegistration(registration)?.type?.let { this.copy(aircraft = it.shortName) }
                ?: this.also { printWhyNoTypeFound(rtMap, adc)}
        }

    private fun BasicFlight.printWhyNoTypeFound(rtMap: Map<String, String>, adc: AircraftDataCache){
        println("reg:   $registration")
        println("type:  $aircraft")
        println("rtMap: ${rtMap[registration]}")
        println("adc:   ${adc.getAircraftFromRegistration(registration)}\n\n")
    }


    private fun BasicFlight.autoValues( airportDC: AirportDataCache, aircraftDC: AircraftDataCache): BasicFlight =
        ModelFlight.ofFlightAndDataCaches(Flight(this), airportDC, aircraftDC)
            .autoValues()
            .toFlight()
            .toBasicFlight()

    /*
     * Returns a map of Registrations to Type,
     * most recent entered registration-type combo is saved.
     */
    private fun makeRegistrationToTypesMap(flights: Collection<BasicFlight>): Map<String, String> {
        val result = HashMap<String, String>()
        flights.sortedBy { it.timeOut } // oldest flight first means newer data will overwrite older data
            .forEach {
                it.makeRegToTypePairIfFound()?.let{ rt ->
                    result[rt.first] = rt.second
                }
            }
        return result
    }

    private fun BasicFlight.makeRegToTypePairIfFound(): Pair<String, String>? {
        val r = registration.nullIfBlank() ?: return null
        val t = aircraft.nullIfBlank() ?: return null
        return r to t
    }

}