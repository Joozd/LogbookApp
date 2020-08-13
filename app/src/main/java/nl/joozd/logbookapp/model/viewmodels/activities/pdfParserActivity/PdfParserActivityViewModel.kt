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

package nl.joozd.logbookapp.model.viewmodels.activities.pdfParserActivity

import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import nl.joozd.joozdlogpdfdetector.JoozdlogPdfDetector
import nl.joozd.joozdlogpdfdetector.SupportedTypes
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.PdfParserActivityEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import nl.joozd.logbookapp.data.pdfparser.JoozdlogKlcRosterParser
import nl.joozd.logbookapp.data.pdfparser.KlcMonthlyParser
import nl.joozd.logbookapp.data.pdfparser.helpers.KlcMonthlyFlightsCleaner
import nl.joozd.logbookapp.data.repository.helpers.isSameFlightAs
import nl.joozd.logbookapp.extensions.toTimeString
import java.io.FileNotFoundException
import java.io.InputStream
import java.time.Instant



class PdfParserActivityViewModel: JoozdlogActivityViewModel() {
    private var _parsedChronoData: ParsedChronoData? = null
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


    private var flightsToSave: List<Flight>? = null
    private var periodToSave: ClosedRange<Instant>? = null

    private fun run() {
        //TODO Move this work to repository?
        intent?.let {
            updateProgress(0, R.string.receivedIntent)
            viewModelScope.launch {
                val uri = it.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
                getTypeDetector(uri)?.let { typeDetector ->
                    updateProgress(20, R.string.readingFile)
                    Log.d("GOT PDF:", "\n\n${typeDetector.firstPageText}\n\n")

                    //check if it was actually a supported file




                    /**
                     * This starts the correct functions when a roster type is found
                     */
                    when (typeDetector.typeOfFile) {
                        SupportedTypes.KLC_ROSTER -> uri?.getInputStream()?.use {
                            //TODO check if calendar sync is active
                            if (!parseKlcRoster(it))
                                feedback(PdfParserActivityEvents.NOT_A_KNOWN_ROSTER)
                        } ?: feedback(PdfParserActivityEvents.ERROR)

                        SupportedTypes.KLC_MONTHLY -> uri?.getInputStream()?.use {
                            val result = parseKlcMonthly(it)
                        } ?: feedback(PdfParserActivityEvents.ERROR)
                        else -> feedback(PdfParserActivityEvents.NOT_A_KNOWN_ROSTER)
                    }
                }
            }
        } ?: feedback(PdfParserActivityEvents.FILE_NOT_FOUND)
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

    private fun getTypeDetector(uri: Uri?): JoozdlogPdfDetector? {
        if (uri?.getInputStream() == null) {
            feedback(PdfParserActivityEvents.ERROR)
            return null
        }
        return uri.getInputStream()?.let {
            JoozdlogPdfDetector(it).let{detector ->
                if (!detector.seemsValid) null else detector
            }
        } ?: run {
            feedback(PdfParserActivityEvents.ERROR)
            null
        }
    }

    /*********************************************************************************************
     * Parser functions. This work can (should) be done async in NonCancelable coroutine
     *********************************************************************************************/

    /**
     * Parse KLC roster and do all the magic.
     * Return true if work successfully launched, false if file not OK
     */
    private fun parseKlcRoster(inputStream: InputStream):  Boolean{
        val roster = JoozdlogKlcRosterParser(
            inputStream
        )
        if (!roster.isValid) return false
        viewModelScope.launch (Dispatchers.IO + NonCancellable){
            val mostRecentFlight = flightRepository.getMostRecentFlightAsync()
            val flightsFromRoster = roster.getFlights(airportRepository.getIcaoToIataMap())

            //Decision: Only flights newer than newest complete flight fill be saved
            val cutoffTime = mostRecentFlight.await()?.timeIn ?: 0L
            flightsToSave = flightsFromRoster.filter {f -> f.timeOut > cutoffTime}
            periodToSave = roster.period
            Log.d("HHHHHHHHHHHHHHHHHHHHHHH", "period: $periodToSave")
            saveFlightsFromRoster()
        }
        return true
    }

    private suspend fun parseKlcMonthly(inputStream: InputStream): Boolean{
        //TODO: move Strings to resource file
        with (KlcMonthlyParser(inputStream)){
            if (!validMonthlyOverview){
                feedback(PdfParserActivityEvents.NOT_A_KNOWN_ROSTER)
                return false
            }
            val foundFlights = KlcMonthlyFlightsCleaner(flights).cleanFlights()
            if (foundFlights == null){
                Log.w("PdfParserActivity", "KlcMonthlyParser.flights == null")
                feedback(PdfParserActivityEvents.ERROR)
                return false
            }
            val exactMatches = flightRepository.findMatches(foundFlights, false)
            val flightsToAdjust = flightRepository.findMatches(foundFlights, true).filter {match -> match.first !in exactMatches.map{it.first}}
            val conflicts = flightRepository.findConflicts(foundFlights)
            val newFlights = flightRepository.findNewFlights(foundFlights)

            if (conflicts.isEmpty()) {
                flightRepository.save(flightsToAdjust.map{match ->
                    val oldRemarks = if (match.second.remarks.isBlank()) "" else " - " + match.second.remarks
                    match.second.copy(timeOut = match.first.timeOut, timeIn = match.first.timeIn, timeStamp = match.first.timeStamp, remarks = "Times adjusted from Chrono. Old: ${match.second.tOut().toTimeString()}-${match.second.tIn().toTimeString()}$oldRemarks")
                })
                flightRepository.saveFromRoster(newFlights.map{it.copy (remarks = "From Chrono, some info may be missing")})
                feedback(PdfParserActivityEvents.CHRONO_SUCCESSFULLY_ADDED).apply{
                    extraData.putInt(ADJUSTED_FLIGHTS, flightsToAdjust.size)
                    extraData.putInt(NEW_FLIGHTS, newFlights.size)
                    extraData.putInt(TOTAL_FLIGHTS_IN_CHRONO, foundFlights.size)
                }
                return false
            }
            else{
                //TODO WIP
                _parsedChronoData = ParsedChronoData(exactMatches, flightsToAdjust, conflicts, newFlights)
                feedback(PdfParserActivityEvents.CHRONO_CONFLICTS_FOUND)
                return false
            }
        }
    }

    /**
     * Saves flights from roster, does check if CalendarSync is enabled
     */
    private suspend fun saveFlightsFromRoster(finish: Boolean = true){
        if (flightsToSave == null){
            feedback(PdfParserActivityEvents.ERROR)
            return
        }
        val checkedCalendarSync = Preferences.getFlightsFromCalendar && (Preferences.calendarDisabledUntil < periodToSave?.endInclusive?.epochSecond ?: -1)

        if (checkedCalendarSync){
            feedback(PdfParserActivityEvents.CALENDAR_SYNC_ENABLED)
        } else {
            flightsToSave?.let { fff ->
                flightRepository.saveFromRoster(fff, period = periodToSave)
                if (finish)
                    feedback(PdfParserActivityEvents.ROSTER_SUCCESSFULLY_ADDED)
            }
        }
    }

    /*********************************************************************************************
     * Public functions and variables
     *********************************************************************************************/

    val parsedChronoData: ParsedChronoData?
    get() = _parsedChronoData

    fun runOnce(newIntent: Intent){
        if (intent == null){
            intent = newIntent
            run()
        }
    }

    fun disableCalendarImport(){
        Preferences.getFlightsFromCalendar = false
    }

    fun disableCalendarUntilAfterLastFlight(){
        val maxIn = periodToSave?.endInclusive?.epochSecond
        if (maxIn == null) {
            feedback(PdfParserActivityEvents.ERROR)
            return
        }
        Preferences.calendarDisabledUntil = maxIn
        feedback (PdfParserActivityEvents.CALENDAR_SYNC_PAUSED)
    }

    fun saveFlights(finish: Boolean = true){
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {  saveFlightsFromRoster(finish) }
    }


    companion object{
        const val ADJUSTED_FLIGHTS = "ADJUSTED_FLIGHTS"
        const val NEW_FLIGHTS = "NEW_FLIGHTS"
        const val TOTAL_FLIGHTS_IN_CHRONO = "TOTAL_FLIGHTS_IN_CHRONO"
    }

}