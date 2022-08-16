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

package nl.joozd.logbookapp.model.viewmodels.activities.makePdfActivity

import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import nl.joozd.logbookapp.data.dataclasses.BalanceForward
import nl.joozd.logbookapp.data.repository.BalanceForwardRepository
import nl.joozd.logbookapp.data.repository.BalanceForwardRepositoryImpl
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.MakePdfActivityEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut
import nl.joozd.logbookapp.utils.DispatcherProvider
import java.util.*

//TODO migrate to Flow
class MakePdfActivityViewModel: JoozdlogActivityViewModel() {
    val statusFlow: StateFlow<MakePdfActivityEvents> = MutableStateFlow(MakePdfActivityEvents.BUILDING_LOGBOOK)
    private var status by CastFlowToMutableFlowShortcut(statusFlow)

    val progressFlow: StateFlow<Double> = MutableStateFlow(0.0)
    private var progress by CastFlowToMutableFlowShortcut(progressFlow)

    val createdLogbookFlow: StateFlow<PdfDocument?> = MutableStateFlow(null)
    private var createdLogbook by CastFlowToMutableFlowShortcut(createdLogbookFlow)

    var targetUri: Uri? = null

    init{
        viewModelScope.launch {
            makeLogbook(BalanceForwardRepository.instance.getBalancesForward(), FlightRepository.instance.getAllFlights())
        }
    }

    suspend fun makeLogbook(balancesForward: List<BalanceForward>, flights: List<Flight>)=
        with(PdfLogbookBuilder(balancesForward, flights)){
            status = MakePdfActivityEvents.BUILDING_LOGBOOK
            val listener = PdfLogbookBuilder.ProgressListener {
                println("banaan progress $it")
                progress = it
            }
            registerProgressListener(listener)
            try{
                createdLogbook = buildLogbook()
                status = MakePdfActivityEvents.LOGBOOK_READY
            }
            finally {
                unRegisterProgressListener(listener)
            }
        }


    /***********************************************************************************************
     * private parts
     ***********************************************************************************************/


    /***********************************************************************************************
     * Public functions
     ***********************************************************************************************/

    // no null checks, only execute when readyToSaveFlow emits true
    suspend fun saveLogbook(uri: Uri){
        status = MakePdfActivityEvents.WRITING
        withContext(DispatcherProvider.io()) {
            @Suppress("BlockingMethodInNonBlockingContext") // Dispatchers.IO is OK
            context.contentResolver?.openOutputStream(uri)?.use {
                createdLogbook!!.writeTo(it)
            }
        }

        status = MakePdfActivityEvents.FILE_CREATED
    }

    override fun onCleared() {
        createdLogbook?.close()
    }
}