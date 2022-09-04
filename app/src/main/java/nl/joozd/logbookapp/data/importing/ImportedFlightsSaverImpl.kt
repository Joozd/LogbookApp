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
import nl.joozd.logbookapp.data.importing.merging.mergeFlights
import nl.joozd.logbookapp.data.importing.merging.mergeFlightsLists
import nl.joozd.logbookapp.data.importing.results.SaveCompleteLogbookResult
import nl.joozd.logbookapp.data.importing.results.SaveCompletedFlightsResult
import nl.joozd.logbookapp.data.importing.results.SavePlannedFlightsResult
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.data.repository.airportrepository.AirportRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithDirectAccess
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryWithUndo
import nl.joozd.logbookapp.data.repository.helpers.iataToIcaoAirports
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.InsertedUndoableCommand


/**
 * ImportedFlightsSaver saves imported flights into [FlightRepository]
 * It accepts both ICAO and IATA formats for airports.
 */
class ImportedFlightsSaverImpl(
    private val flightsRepoWithUndo: FlightRepositoryWithUndo,
    private val flightsRepoWithDirectAccess: FlightRepositoryWithDirectAccess,
    private val airportRepository: AirportRepository,
    private val aircraftRepository: AircraftRepository
): ImportedFlightsSaver {
    /**
     * Merge a complete logbook into current logbook.
     * Only checks for exact time, orig and dest matches, otherwise will allow overlapping flights.
     * Imported flights will be merged onto flights already in logbook
     * @see mergeFlights
     */
    override suspend fun replace(completeLogbook: ExtractedCompleteLogbook): SaveCompleteLogbookResult {
        val flights = makeFlightsWithIcaoAirportsAndRemoveNamesIfNeeded(completeLogbook)?: emptyList()
        val command = makeReplaceDBCommand(flights)
        command()
        flightsRepoWithUndo.insertUndoAction(command)
        return SaveCompleteLogbookResult(true)
    }

    override suspend fun merge(completeLogbook: ExtractedCompleteLogbook): SaveCompleteLogbookResult {
        val flights = makeFlightsWithIcaoAirportsAndRemoveNamesIfNeeded(completeLogbook)?: emptyList()
        val flightsOnDevice = flightsRepoWithUndo.getAllFlights()
        val mergedFlights = mergeFlightsLists(flightsOnDevice, flights)

        val command = makeReplaceDBCommand(mergedFlights)
        command()
        flightsRepoWithUndo.insertUndoAction(command)
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

        val repo = if (canUndo) flightsRepoWithUndo else flightsRepoWithDirectAccess

            //Deleting planned flights does not pollute DB as they are not synced to server and thus deleted hard.
        repo.delete(flightsToDelete)
        repo.save(mergedFlights + newFlights)

        return SavePlannedFlightsResult(true)
    }

    private suspend fun makeReplaceDBCommand(mergedFlights: List<Flight>): InsertedUndoableCommand {
        val action = suspend {
            flightsRepoWithDirectAccess.clear()
            flightsRepoWithDirectAccess.save(mergedFlights)
        }
        val currentFlightsInDB = flightsRepoWithDirectAccess.getAllFlightsInDB()
        val undoAction = suspend {
            flightsRepoWithDirectAccess.clear()
            flightsRepoWithDirectAccess.save(currentFlightsInDB)
        }
        return InsertedUndoableCommand(action = action, undoAction = undoAction)
    }

    /*
     * Preparing means:
     * - changing airports to ICAO if needed
     * - removing names if needed
     * - only flights in its period
     */
    private suspend fun prepareFlightsForSaving(
        completedFlights: ExtractedCompletedFlights
    ): List<Flight>? =
        makeFlightsWithIcaoAirportsAndRemoveNamesIfNeeded(completedFlights)?.filter {
            it.timeOut in (completedFlights.period ?: return null)
        }

    private suspend fun prepareFlightsForSaving(
        plannedFlights: ExtractedPlannedFlights
    ): List<Flight>? =
        makeFlightsWithIcaoAirportsAndRemoveNamesIfNeeded(plannedFlights)?.filter {
            it.timeOut in (plannedFlights.period!!)
        }

    private suspend fun getPlannedFlightsOnDevice(period: ClosedRange<Long>): List<Flight> =
        flightsRepoWithUndo.getAllFlights().filter { it.isPlanned }.filter { it.timeOut in (period) }


    private suspend fun makeFlightsWithIcaoAirportsAndRemoveNamesIfNeeded(flightsToPrepare: ExtractedFlights): List<Flight>?{
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
}