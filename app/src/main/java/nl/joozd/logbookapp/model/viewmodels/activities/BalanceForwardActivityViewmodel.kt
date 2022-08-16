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

package nl.joozd.logbookapp.model.viewmodels.activities

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.dataclasses.BalanceForward
import nl.joozd.logbookapp.data.repository.BalanceForwardRepositoryImpl
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.BalanceForwardActivityEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel

class BalanceForwardActivityViewmodel: JoozdlogActivityViewModel() {
    private val balanceForwardRepository = BalanceForwardRepositoryImpl.instance

    val balancesForward: LiveData<List<BalanceForward>> = balanceForwardRepository.balanceForwardsFlow.asLiveData()

    fun delete(bf: BalanceForward) = viewModelScope.launch{
        balanceForwardRepository.delete(bf)
        feedback(BalanceForwardActivityEvents.DELETED)
    }

    fun itemClicked(bf: BalanceForward, item: Int){
        //TODO implement this
        println("$bf / $item")
        feedback(BalanceForwardActivityEvents.NOT_IMPLEMENTED)
    }
}