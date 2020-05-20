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

import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import nl.joozd.joozdlogpdfdetector.JoozdlogPdfDetector
import nl.joozd.klcrosterparser.Activities
import nl.joozd.klcrosterparser.KlcRosterParser
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.helpers.FeedbackEvents.PdfParserActivityEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import nl.joozd.logbookapp.ui.activities.helpers.PdfRosterFunctions.makeFlightsList
import nl.joozd.logbookapp.utils.reversed
import java.io.FileNotFoundException
import java.io.InputStream

class PdfParserActivityViewModel: JoozdlogActivityViewModel() {
    private val _progressTextResource = MutableLiveData<Int>()
    val progressTextResource: LiveData<Int>
        get() = _progressTextResource
    //no public setter
    private fun setProgressText(it: Int){
        _progressTextResource.value = it
    }

    private val _progress = MutableLiveData<Int>()
    val progress: LiveData<Int>
        get() = _progress
    //no public setter
    private fun setProgress(progress: Int){
        _progress.value = progress
    }

    private fun updateProgress(percentage: Int, textResource: Int){
        setProgress(percentage)
        setProgressText(textResource)
    }

    private var intent: Intent? = null
    fun runOnce(newIntent: Intent){
        if (intent == null){
            intent = newIntent
            run()
        }
    }

    private fun run(){
        //TODO this is where the magic happens
        intent?.let {
            val iataIcaoMapAsync = getIataIcaoMapAsync()
            updateProgress(0, R.string.receivedIntent)
            viewModelScope.launch {
                val getMostRecentFlight = flightRepository.getMostRecentFlight()
                val getHighestID = flightRepository.getHighestId()
                (it.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let { uri ->
                    val inputStream = uri.getInputStream()
                    if (inputStream == null) {
                        feedback(PdfParserActivityEvents.ERROR)
                        return@launch
                    }
                    //TODO do something with progress(received data)
                    updateProgress(20, R.string.readingFile)

                    // TODO REMOVE BELOW THIS

                    // val TEST = JoozdlogPdfDetector(App.instance.contentResolver.openInputStream(uri)!!)


                    // TODO REMOVE ABOVE THIS

                    val roster = withContext(Dispatchers.IO) { KlcRosterParser(inputStream) }
                    if (!roster.seemsValid) {
                        feedback(PdfParserActivityEvents.NOT_A_KNOWN_ROSTER)
                        return@launch
                    }

                    //TODO do something with progress (file read)
                    updateProgress(40, R.string.readingRoster)
                    val cutoffTime = getMostRecentFlight.await()?.timeIn ?: 0
                    val alreadyPlannedFlights = flightRepository.getAllFlights().filter{it.isPlanned}

                    val flightsToPlan = roster.days.map { it.events }.flatten()
                        .filter { it.type == Activities.FLIGHT }
                        .filter { it.startEpochSecond > cutoffTime }
                    // simsToPlan are all actual sim times in roster after most recent completed flight on-blocks time
                    val simsToPlan = roster.days.map { it.events }.flatten()
                        .filter { it.type == Activities.ACTUALSIM }
                        .filter { it.startEpochSecond > cutoffTime }

                    updateProgress(60, R.string.removingOldFlights)
                    //days is a list of pairs (start of day, end of day)
                    val days = roster.days.map { it.startOfDayEpochSecond to it.endOfDayEpochSecond }
                    flightRepository.delete(alreadyPlannedFlights.filter { pf -> days.any { day -> pf.timeIn in (day.first..day.second) } })

                    //TODO do something with progress (old flights removed)
                    updateProgress(60, R.string.insertNewFlights)

                    val nextFlightId = getHighestID.await() + 1

                    val newFlights: List<Flight> = makeFlightsList(flightsToPlan, nextFlightId, iataIcaoMapAsync.await())

                    //TODO do something with progress (flights are created from roster)
                    updateProgress(80, R.string.writingToStorage)

                    flightRepository.save(newFlights)

                    //TODO do something with progress (done)
                    updateProgress(100, R.string.done)

                    feedback(PdfParserActivityEvents.ROSTER_SUCCESSFULLY_ADDED)

                }
            }
        } ?: feedback(PdfParserActivityEvents.FILE_NOT_FOUND)

        //Now, we have an inputstream containing (hopefully) a roster
    }

    private fun getIataIcaoMapAsync(): Deferred<Map<String, String>> = viewModelScope.async{
        airportRepository.getIcaoToIataMap().reversed()
    }

    private fun Uri.getInputStream(): InputStream? = try {
        /*
         * Get the content resolver instance for this context, and use it
         * to get a ParcelFileDescriptor for the file.
         */
        App.instance.contentResolver.openInputStream(this)
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
        feedback(PdfParserActivityEvents.FILE_NOT_FOUND)
        null
    }


}