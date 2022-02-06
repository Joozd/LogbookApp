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

package nl.joozd.logbookapp.model.viewmodels.dialogs.airportPicker

import androidx.lifecycle.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.repository.airportrepository.AirportDataCache
import nl.joozd.logbookapp.data.repository.airportrepository.AirportRepository
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel

@ExperimentalCoroutinesApi
abstract class AirportPickerViewModel: JoozdlogDialogViewModel() {
    /**
     * The picked airport
     */
    abstract val pickedAirport: LiveData<Airport?>

    /**
     * Pick an airport and set that as Airport
     */
    abstract fun pickAirport(airport: Airport)

    /**
     * Set a custom value as Icao Identifier for in logbook.
     */
    abstract fun setCustomAirport(airport: String)

    /**
     * The airport that is set in [workingFlight] when viewmodel is initialized
     */
    protected abstract val initialAirport: Airport?

    protected var airportDataCache: AirportDataCache? = null

    init {
        viewModelScope.launch {
            airportDataCache = AirportRepository.instance.getAirportDataCache()
        }
        viewModelScope.launch {
            AirportRepository.instance.airportDataCacheFlow().collect {
                airportDataCache = it
            }
        }
    }

    private val currentQueryFlow = MutableStateFlow("")

    val airportsListFlow: Flow<List<Airport>> =
        combine(AirportRepository.instance.airportsFlow(), currentQueryFlow) { airports, query ->
            airports to query
        }.flatMapLatest {
            makeAirportSearcherFlow(it.first, it.second)
        }

    fun updateSearch(query: String){
        currentQueryFlow.update { query }
    }


    private fun makeAirportSearcherFlow(airports: List<Airport>, query: String?) = flow {
        if (query == null) {
            emit(airports)
        } else {
            var result = airports.filter { it.iata_code.contains(query, ignoreCase = true) }
            emit(result)

            result = result + airports.filter { it.ident.contains(query, ignoreCase = true) }
            emit(result)

            result = result + airports.filter { it.municipality.contains(query, ignoreCase = true) }
            emit(result)

            result = result + airports.filter { it.name.contains(query, ignoreCase = true) }
            emit(result)
        }
    }
}
