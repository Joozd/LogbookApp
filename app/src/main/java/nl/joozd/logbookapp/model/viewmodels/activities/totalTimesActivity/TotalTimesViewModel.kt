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

package nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nl.joozd.logbookapp.data.dataclasses.BalanceForward
import nl.joozd.logbookapp.data.repository.BalanceForwardRepositoryImpl
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.listBuilders.*

//TODO switch from LiveData to Flow
class TotalTimesViewModel: JoozdlogActivityViewModel() {
    val allLists = combine(
        totalTimesFlow(),
        timesPerYearFlow(),
        timesPerTypeFlow(),
        timesPerRegFlow(),
        visitsPerDestFlow()
    ){ totalTimes, timesPerYear, timesPerType, timesPerRegistration, airportsArrived ->
        listOfNotNull(totalTimes, timesPerYear, timesPerType, timesPerRegistration, airportsArrived)
    }


    private var allFlightsCache: List<Flight>? = null
    private val allFlightsMutex = Mutex()
    private suspend fun allFlightsCached() = allFlightsMutex.withLock {
        allFlightsCache ?: FlightRepository.instance.getAllFlights()
            .filter{ !it.isPlanned }
            .also{ allFlightsCache = it }
    }

    private var balancesForwardCache: List<BalanceForward>? = null
    private val balancesForwardMutex = Mutex()
    private suspend fun balancesForwardCached() = balancesForwardMutex.withLock {
        balancesForwardCache ?: BalanceForwardRepositoryImpl.instance.getBalancesForward().also{
            balancesForwardCache = it
        }
    }

    private fun totalTimesFlow() = flowStartingWithNull { TotalTimes.of(allFlightsCached(), balancesForwardCached()) }
    private fun timesPerYearFlow() = flowStartingWithNull { TimesPerYear.of(allFlightsCached()) }
    private fun timesPerTypeFlow() = flowStartingWithNull { TimesPerType.of(allFlightsCached()) }
    private fun timesPerRegFlow() = flowStartingWithNull{ TimesPerRegistration.of(allFlightsCached())}
    private fun visitsPerDestFlow() = flowStartingWithNull { AirportsArrived.of(allFlightsCached())}

    private fun <T> flowStartingWithNull(block: suspend FlowCollector<T>.() -> T): Flow<T?> = flow{
        emit(null)
        emit(block())
    }
}



