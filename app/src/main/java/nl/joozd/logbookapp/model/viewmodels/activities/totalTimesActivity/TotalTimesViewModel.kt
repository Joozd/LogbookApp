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

package nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.extensions.replaceValueAt
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.listBuilders.TimesPerYear
import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.listBuilders.TotalTimes
import nl.joozd.logbookapp.ui.activities.totalTimesActivity.TotalTimesList


class TotalTimesViewModel: JoozdlogActivityViewModel() {
    private val _allLists = MutableLiveData(List<TotalTimesList?>(NUMBER_OF_LISTS) { null })


    val allLists: LiveData<List<TotalTimesList?>> = Transformations.map(_allLists) { it.filterNotNull() }


    /**
     * In here, add all the listBuilders. Don't forget to update NUMBER_OF_LISTS as well
     */

    init{
        viewModelScope.launch{
            //Get all flights and balances forward
            val allFlights = async { flightRepository.getAllFlights().filter{ !it.isPlanned} }
            val allBalancesForward = async { balanceForwardRepository.getAll() }

            // build lists
            val timesPerYear = async(Dispatchers.Default) { TimesPerYear(allFlights.await()) }
            val totalTimes = async(Dispatchers.Default) { TotalTimes(allFlights.await(), allBalancesForward.await()) }

            //Add totals lists as they become available
            _allLists.value = _allLists.value!!.replaceValueAt(POSITION_TOTALS, totalTimes.await())
            _allLists.value = _allLists.value!!.replaceValueAt(POSITION_YEARS, timesPerYear.await())

        }

    }

    companion object{
        //These values are used to make sure totals lists will appear in the same order every time.
        const val NUMBER_OF_LISTS = 2 // amount of expandable lists we will show
        const val POSITION_TOTALS = 0
        const val POSITION_YEARS = 1
    }
}



