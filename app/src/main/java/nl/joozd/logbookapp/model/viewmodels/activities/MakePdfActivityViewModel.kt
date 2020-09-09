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

package nl.joozd.logbookapp.model.viewmodels.activities

import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.miscClasses.TotalsForward
import nl.joozd.logbookapp.extensions.popFirst
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.MakePdfActivityEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import nl.joozd.logbookapp.utils.pdf.PdfLogbookBuilder
import nl.joozd.logbookapp.utils.pdf.PdfLogbookMakerValues
import java.util.*

class MakePdfActivityViewModel: JoozdlogActivityViewModel() {
    /***********************************************************************************************
     * private parts
     ***********************************************************************************************/

    /**
     * Private variables
     */
    private var pdfLogbook: PdfDocument? = null
    private var _uriWithLogbook: Uri? = null

    /**
     * Mutable LiveData
     */

    //Progress, 0-100
    private val _logbookBuilderProgress = MutableLiveData(0)
    private val _pdfLogbookReady = MutableLiveData(false)

    override fun onCleared() {
        super.onCleared()
        pdfLogbook?.close()
    }


    /**
     * Build the logbook and put it in [pdfLogbook] (async)
     * Progress is kept in [_logbookBuilderProgress]
     * Can be monitored if ready or not by looking at [_pdfLogbookReady]
     */
    fun buildLogbookAsync() {
        _pdfLogbookReady.value = false
        pdfLogbook?.close()
        viewModelScope.launch {
            pdfLogbook = PdfDocument().apply {
                //get all aircraft:
                val balancesForwardAsync = async(Dispatchers.IO) { balanceForwardRepository.getAll() }
                val aircraftMapAsync = aircraftRepository.getAircraftTypesMapShortNameAsync()
                val allFlightsAsync = async(Dispatchers.IO) { flightRepository.getAllFlights()}

                //fill Totals Forward with balance forward totals
                val totalsForward = TotalsForward().apply{
                    val balancesForward = balancesForwardAsync.await()
                    multiPilot = balancesForward.sumBy {it.multiPilotTime}
                    totalTime = balancesForward.sumBy {it.aircraftTime}
                    landingDay = balancesForward.sumBy {it.landingDay}
                    landingNight = balancesForward.sumBy {it.landingNight}
                    nightTime = balancesForward.sumBy {it.nightTime}
                    ifrTime = balancesForward.sumBy {it.ifrTime}
                    picTime = balancesForward.sumBy {it.picTime}
                    copilotTime = balancesForward.sumBy {it.copilotTime}
                    dualTime = balancesForward.sumBy {it.dualTime}
                    instructorTime = balancesForward.sumBy {it.instructortime}
                    simTime = balancesForward.sumBy {it.simTime}
                }
                //lets say this is 5% of the work
                _logbookBuilderProgress.value = 5

                val flightsPerPage = PdfLogbookBuilder.maxLines()
                val allFlights = LinkedList(allFlightsAsync.await().filter {!it.isPlanned}.sortedBy { it.timeOut })
                val aircraftMap = aircraftMapAsync.await()
                var pageNumber = 1
                val originalListSize = allFlights.size

                while (allFlights.isNotEmpty()){
                    val currentFlights = allFlights.popFirst(flightsPerPage)
                    //this call increases pagenumber after using it
                    finishPage(startPage(PdfDocument.PageInfo.Builder(PdfLogbookMakerValues.A4_LENGTH, PdfLogbookMakerValues.A4_WIDTH, pageNumber++).create()).apply{
                        PdfLogbookBuilder.drawLeftPage(canvas)
                        PdfLogbookBuilder.fillLeftPage(canvas, currentFlights, totalsForward)
                    })

                    finishPage(startPage(PdfDocument.PageInfo.Builder(PdfLogbookMakerValues.A4_LENGTH, PdfLogbookMakerValues.A4_WIDTH, pageNumber++).create()).apply{ // left page
                            PdfLogbookBuilder.drawRightPage(canvas)
                            PdfLogbookBuilder.fillRightPage(canvas, currentFlights, totalsForward)
                    })

                    val progress = (originalListSize-allFlights.size).toDouble() / originalListSize
                    _logbookBuilderProgress.value = 5+ (90*progress).toInt()
                }

            }
            _pdfLogbookReady.value = true
        }
    }


    /***********************************************************************************************
     * Public functions
     ***********************************************************************************************/

    fun useUri(uri: Uri){
        if (pdfLogbook == null){
            feedback(MakePdfActivityEvents.ERROR).apply{ putInt(PDF_NOT_CREATED)}
            return
        }
        context.contentResolver.openOutputStream(uri).use{
            pdfLogbook?.writeTo(it)
        }
        _uriWithLogbook = uri
        feedback(MakePdfActivityEvents.FILE_CREATED)
    }

    fun buildLogbook() = buildLogbookAsync()

    /***********************************************************************************************
     * Observables
     ***********************************************************************************************/

    val pdfLogbookReady: LiveData<Boolean>
        get() = _pdfLogbookReady

    val uriWithLogbook: Uri?
        get() = _uriWithLogbook

    companion object{
        const val PDF_NOT_CREATED = 1
    }
}