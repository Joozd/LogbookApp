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

package nl.joozd.logbookapp.data.importing

import kotlinx.coroutines.withContext
import nl.joozd.joozdlogimporter.dataclasses.ExtractedCompleteLogbook
import nl.joozd.joozdlogimporter.dataclasses.ExtractedCompletedFlights
import nl.joozd.joozdlogimporter.dataclasses.ExtractedFlights
import nl.joozd.joozdlogimporter.dataclasses.ExtractedPlannedFlights
import nl.joozd.joozdlogimporter.enumclasses.AirportIdentFormat
import nl.joozd.logbookapp.data.importing.results.SaveCompletedFlightsResult
import nl.joozd.logbookapp.data.repository.airportrepository.AirportRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.data.repository.helpers.iataToIcaoAirports
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.DispatcherProvider


/**
 * ImportedFlightsSaver saves imported flights into [FlightRepository]
 * It accepts both ICAO and IATA formats for airports.
 */
class ImportedFlightsSaverImpl(
    private val flightsRepo: FlightRepository,
    private val airportRepository: AirportRepository
): ImportedFlightsSaver {
    /**
     * Merge a complete logbook into current logbook.
     * Only checks for exact time, orig and dest matches, otherwise will allow overlapping flights.
     * Imported flights will be merged onto flights already in logbook
     * @see mergeFlights
     */
    override suspend fun save(completeLogbook: ExtractedCompleteLogbook) {
        val flights = makeFlightsWithIcaoAirports(completeLogbook)?: emptyList()
        val flightsOnDevice = flightsRepo.getAllFlights()
        // This makes a list of pairs.
        // mergeFlights will merge second onto first, overwriting any non-empty data.
        // This can take some time for large datasets
        // (eg. checking 10K flights against 10K other flights might be 100M checks)
        // so I offloaded it to a different coroutine to keep Main thread from filling up.
        val matchingFlights = withContext(DispatcherProvider.default()){ getMatchingFlightsExactTimes(flightsOnDevice, flights) }
        val mergedFlights = mergeFlights(matchingFlights)
        val newFlights = getNonMatchingFlightsExactTimes(flightsOnDevice, flights)

        flightsRepo.save(mergedFlights + newFlights)
    }

    /**
     * Merge completed flights into logbook.
     * Will check for same flights (flightnumber, orig and dest) departing on the same
     *  (UTC) calendar day and update times if such a flight is found.
     */
    override suspend fun save(completedFlights: ExtractedCompletedFlights): SaveCompletedFlightsResult? {
        val flights = getFlightsWithIcaoAirportsInPeriod(completedFlights) ?: return null
        val flightsOnDevice = flightsRepo.getAllFlights()
        val relevantFlightsOnDevice = flightsOnDevice.filter { !it.isSim && it.timeOut in completedFlights.period ?: return null }
        val matchingFlights = getMatchingFlightsSameDay(relevantFlightsOnDevice, flights)
        val mergedFlights = mergeFlights(matchingFlights)
        val newFlights = getNonMatchingFlightsSameDay(relevantFlightsOnDevice, flights)
        val flightsNotInCompletedFlights = getNonMatchingFlightsExactTimes(flights, relevantFlightsOnDevice)
        flightsRepo.save(mergedFlights + newFlights)
        return SaveCompletedFlightsResult(
            flightsInCompletedButNotOnDevice = newFlights.size,
            flightsOnDeviceButNotInCompleted = flightsNotInCompletedFlights.size,
            totalFlightsImported = flights.size,
            flightsUpdated = matchingFlights.filter { it.hasChanges() }.size
        )
    }

    /**
     * Save planned flights,
     * Will merge with existing exact matches (times, orig, dest) so we don't lose data
     * Will remove all other planned flights in its period.
     * TODO don't add completed flights that are planned in here, eg. prevent double flights from getting into logbook
     * TODO this generates two undo actions, needs fixing
     */
    override suspend fun save(plannedFlights: ExtractedPlannedFlights) {
        val flights = getFlightsWithIcaoAirportsInPeriod(plannedFlights) ?: return
        val plannedFlightsOnDevice = getPlannedFlightsOnDevice(plannedFlights.period ?: return)

        val matchingFlights = getMatchingFlightsExactTimes(plannedFlightsOnDevice, flights)
        val mergedFlights = mergeFlights(matchingFlights)

        val newFlights = getNonMatchingFlightsExactTimes(plannedFlightsOnDevice, flights)

        //Deleting planned flights does not pollute DB as they are not synced to server and thus deleted hard.
        flightsRepo.delete(plannedFlightsOnDevice)
        flightsRepo.save(mergedFlights + newFlights)
    }

    private suspend fun getFlightsWithIcaoAirportsInPeriod(
        completedFlights: ExtractedCompletedFlights
    ): List<Flight>? =
        makeFlightsWithIcaoAirports(completedFlights)?.filter {
            it.timeOut in (completedFlights.period ?: return null)
        }

    private suspend fun getFlightsWithIcaoAirportsInPeriod(
        plannedFlights: ExtractedPlannedFlights
    ): List<Flight>? =
        makeFlightsWithIcaoAirports(plannedFlights)?.filter {
            it.timeOut in (plannedFlights.period!!)
        }

    private suspend fun getPlannedFlightsOnDevice(period: ClosedRange<Long>): List<Flight> =
        flightsRepo.getAllFlights().filter { it.isPlanned }.filter { it.timeOut in (period) }


    private suspend fun makeFlightsWithIcaoAirports(flightsToPrepare: ExtractedFlights): List<Flight>?{
        val flights = flightsToPrepare.flights?.map { Flight(it) }
        return if (flightsToPrepare.identFormat == AirportIdentFormat.ICAO) flights
        else {
            val adc = airportRepository.getAirportDataCache()
            flights?.map { it.iataToIcaoAirports(adc) }
        }
    }
}