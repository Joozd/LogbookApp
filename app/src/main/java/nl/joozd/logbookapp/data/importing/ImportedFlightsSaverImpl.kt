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

import nl.joozd.joozdlogimporter.dataclasses.*
import nl.joozd.joozdlogimporter.enumclasses.AirportIdentFormat
import nl.joozd.logbookapp.data.importing.merging.mergeFlights
import nl.joozd.logbookapp.data.importing.merging.mergeFlightsLists
import nl.joozd.logbookapp.data.importing.results.SaveCompleteLogbookResult
import nl.joozd.logbookapp.data.importing.results.SaveCompletedFlightsResult
import nl.joozd.logbookapp.data.importing.results.SavePlannedFlightsResult
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftDataCache
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.data.repository.airportrepository.AirportRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithUndo
import nl.joozd.logbookapp.data.repository.helpers.iataToIcaoAirports
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.model.dataclasses.Flight


/**
 * ImportedFlightsSaver saves imported flights into [FlightRepository]
 * It accepts both ICAO and IATA formats for airports.
 */
class ImportedFlightsSaverImpl(
    private val flightsRepoWithUndo: FlightRepositoryWithUndo,
    private val flightsRepoWithDirectAccess: FlightRepository,
    private val airportRepository: AirportRepository,
    private val aircraftRepository: AircraftRepository
): ImportedFlightsSaver {
    override suspend fun replace(completeLogbook: ExtractedCompleteLogbook): SaveCompleteLogbookResult {
        val flights = makeFlightsWithIcaoAirports(completeLogbook)?: emptyList()
        flightsRepoWithUndo.singleUndoableOperation {
            clear()
            save(flights)
        }
        return SaveCompleteLogbookResult(true)
    }

    /**
     * Merge a complete logbook into current logbook.
     * Only checks for exact time, orig and dest matches, otherwise will allow overlapping flights.
     * Imported flights will be merged onto flights already in logbook
     * @see mergeFlights
     */
    override suspend fun merge(completeLogbook: ExtractedCompleteLogbook): SaveCompleteLogbookResult {
        val flights = makeFlightsWithIcaoAirports(completeLogbook)?: emptyList()
        val flightsOnDevice = flightsRepoWithUndo.getAllFlights()
        val mergedFlights = mergeFlightsLists(flightsOnDevice, flights)

        flightsRepoWithUndo.singleUndoableOperation {
            clear()
            save(mergedFlights)
        }
        return SaveCompleteLogbookResult(true)
    }

    /**
     * Merge completed flights into logbook.
     * Will check for same flights (flightnumber, orig and dest) departing on the same
     *  (UTC) calendar day and update times if such a flight is found.
     */
    override suspend fun save(completedFlights: ExtractedCompletedFlights): SaveCompletedFlightsResult {
        val flights = prepareFlightsForSaving(completedFlights) ?: return SaveCompletedFlightsResult(false)
        val flightsOnDevice = flightsRepoWithUndo.getAllFlights()
        val relevantFlightsOnDevice = flightsOnDevice.filter {
            !it.isSim && it.timeOut in (completedFlights.period ?: return SaveCompletedFlightsResult(false))
        }
        val matchingFlights = getMatchingFlightsSameDay(relevantFlightsOnDevice, flights)
        val mergedFlights = mergeFlights(matchingFlights).autocomplete(airportRepository, aircraftRepository)
        val newFlights = getNonMatchingFlightsSameDay(relevantFlightsOnDevice, flights)
        val flightsNotInCompletedFlights = getNonMatchingFlightsSameDay(flights, relevantFlightsOnDevice)
        flightsRepoWithUndo.save(mergedFlights + newFlights)

        return SaveCompletedFlightsResult(
            success = true,
            flightsInCompletedButNotOnDevice = newFlights.size,
            flightsOnDeviceButNotInCompleted = flightsNotInCompletedFlights.size,
            totalFlightsImported = flights.size,
            flightsUpdated = mergedFlights.count { f -> flightsOnDevice.none { it.isExactMatchOf(f)} }
        )
    }

    /**
     * Save planned flights,
     * Will merge with existing exact matches (times, orig, dest) so we don't lose data
     * Will remove all other planned flights in its period.
     * TODO this generates two undo actions, needs fixing
     */
    override suspend fun save(plannedFlights: ExtractedPlannedFlights, canUndo: Boolean): SavePlannedFlightsResult {
        val flights = prepareFlightsForSaving(plannedFlights) ?: return SavePlannedFlightsResult(false)
        val plannedFlightsOnDevice = getPlannedFlightsOnDevice(plannedFlights.period ?: return SavePlannedFlightsResult(false))

        // flights that are an exact match do not get updated or deleted, just stay the way they are.
        // If anything is changed, flights will be merged.
        // I guess this saves about 99% of all calendar update saves.
        val matchingFlights = getMatchingFlightsExactTimes(plannedFlightsOnDevice, flights).filter { !it.isExactMatch() }
        val mergedFlights = mergeFlights(matchingFlights)

        val newFlights = getNonMatchingFlightsExactTimes(plannedFlightsOnDevice, flights)
        val flightsToDelete = getNonMatchingFlightsExactTimes(flights, plannedFlightsOnDevice)
            .filter { !it.isSim } // leave sim flights alone. Fix for issue #19

        val repo = if (canUndo) flightsRepoWithUndo else flightsRepoWithDirectAccess

            //Deleting planned flights does not pollute DB as they are not synced to server and thus deleted hard.
        repo.delete(flightsToDelete)
        repo.save(mergedFlights + newFlights)

        return SavePlannedFlightsResult(true)
    }

    /*
     * Preparing means:
     * - changing airports to ICAO if needed
     * - removing names if needed
     * - only flights in its period
     */
    private suspend fun prepareFlightsForSaving(
        flightsToPrepare: ExtractedFlightsWithPeriod
    ): List<Flight>? =
        makeFlightsWithIcaoAirports(flightsToPrepare)
            ?.removeNamesIfNeeded()
            ?.checkAircraftTypes()
            ?.filter {
            it.timeOut in (flightsToPrepare.period ?: return null)
        }

    private suspend fun getPlannedFlightsOnDevice(period: ClosedRange<Long>): List<Flight> =
        flightsRepoWithUndo.getAllFlights().filter { it.isPlanned }.filter { it.timeOut in (period) }


    private suspend fun makeFlightsWithIcaoAirports(flightsToPrepare: ExtractedFlights): List<Flight>?{
        val flights = flightsToPrepare.flights?.map { Flight(it) }?.removeNamesIfNeeded()
        return if (flightsToPrepare.identFormat == AirportIdentFormat.ICAO) flights
        else {
            val adc = airportRepository.getAirportDataCache()
            flights?.map { it.iataToIcaoAirports(adc) }
        }
    }

    private suspend fun List<Flight>.removeNamesIfNeeded(): List<Flight> =
        if (Prefs.getNamesFromRosters()) this
        else this.map { it.copy(name = "", name2 = "") }

    private suspend fun List<Flight>.checkAircraftTypes(): List<Flight>{
        val adc = AircraftRepository.instance.getAircraftDataCache()
        return this.map{ it.checkAircraftType(adc) }
    }

    private fun Flight.checkAircraftType(adc: AircraftDataCache): Flight =
        when {
            registration.isBlank() -> this
            aircraftType.isNotBlank() -> this
            else -> adc.getAircraftFromRegistration(registration)?.let { this.copy(aircraftType = it.type?.shortName ?: "", registration = it.registration) }
                ?: this
        }
}