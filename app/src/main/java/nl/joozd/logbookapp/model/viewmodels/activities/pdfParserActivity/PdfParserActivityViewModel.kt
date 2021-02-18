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

import nl.joozd.joozdlogfiletypedetector.interfaces.FileTypeDetector
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import nl.joozd.joozdlogfiletypedetector.CsvTypeDetector
import nl.joozd.joozdlogfiletypedetector.PdfTypeDetector
import nl.joozd.joozdlogfiletypedetector.SupportedTypes
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.data.parseSharedFiles.extensions.postProcess
import nl.joozd.logbookapp.data.parseSharedFiles.importsParser.MccPilotLogCsvParser
import nl.joozd.logbookapp.data.parseSharedFiles.interfaces.ImportedLogbook
import nl.joozd.logbookapp.data.parseSharedFiles.interfaces.MonthlyOverview
import nl.joozd.logbookapp.data.parseSharedFiles.interfaces.Roster
import nl.joozd.logbookapp.data.parseSharedFiles.pdfparser.KlcCheckinSheet
import nl.joozd.logbookapp.data.parseSharedFiles.pdfparser.KlcMonthlyParser
import nl.joozd.logbookapp.data.parseSharedFiles.pdfparser.KlcRoster
import nl.joozd.logbookapp.data.parseSharedFiles.pdfparser.KlmMonthlyParser
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.PdfParserActivityEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import nl.joozd.logbookapp.data.parseSharedFiles.pdfparser.helpers.ImportedFlightsCleaner
import nl.joozd.logbookapp.data.repository.helpers.isSameFlightAs
import nl.joozd.logbookapp.extensions.*
import nl.joozd.logbookapp.utils.TimestampMaker
import java.io.FileNotFoundException
import java.io.InputStream
import java.time.Instant

/**
 * This viewmodel does:
 * - Receive an intent with a PDF file through [runOnce] (this prevents rerunning on recreating activity)
 * - read that PDF file
 * - Decide what type it is (KLC Roster, Lufthansa Monthly Overview, etc)
 * - parse that type
 * - do whatever needs to be done with parsed data (insert roster from planned, check flights from monthlies etc)
 * - Fixes conflicts when importing Monthlies
 */



class PdfParserActivityViewModel: JoozdlogActivityViewModel() {
    private var _parsedChronoData: ParsedChronoData? = null

    private var intent: Intent? = null

    private var flightsToSave: List<Flight>? = null
    private var periodToSave: ClosedRange<Instant>? = null
    private var foundFlights: List<Flight>? = null

    //Iata to Icao map, for use in Post-Processing
    private val iataIcaoMapAsync = airportRepository.getIataIcaoMapAsync()

    /**
     * TODO: WIP Restructure this. It has become spaghetti.
     */

    /**
     * This will perform the work that we want to do:
     * - read PDF file
     * - Check Type
     * - Parse that type
     * - Do what needs boeing done with parsed data
     * - TODO fix conflicts with incoming completed flights
     */
    private suspend fun run() = withContext(Dispatchers.IO) {
        intent?.let {

            val uri = (it.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri ?: it.data)
            val typeDetector = getTypeDetector(uri) ?: run{
                feedback(PdfParserActivityEvents.FILE_NOT_FOUND) // TODO handle this in Activity
                return@withContext /* END HERE */
            }

            /*
             * Catch bad data:
             */
            if (typeDetector.seemsInvalid || typeDetector.typeOfFile is SupportedTypes.Unsupported) {
                feedback(PdfParserActivityEvents.UNSUPPORTED_FILE) // TODO handle this in Activity
                return@withContext  /* END HERE */
            }
            /*
             * We now have a supported file.
             * What happens next: Depending on if this is a roster or
             */
            typeDetector.typeOfFile.let { type ->
                when (type) {
                    is SupportedTypes.PlannedFlights -> parseRoster(type, uri)

                    is SupportedTypes.CompletedFlights -> parseCompletedFlights(type, uri)

                    is SupportedTypes.CompleteLogbook -> {
                        TODO("Handle complete logbook imports")
                    }
                    else -> {
                        feedback(PdfParserActivityEvents.ERROR).putString("Error -1: This should not happen.")
                        return@withContext
                    }

                }
            }
        }
    }

    /**
     * Parse a Roster, and pass it on to Repository for saving
     * Will provide feedback to Activity through [feedback]
     */
    private suspend fun parseRoster(type: SupportedTypes.PlannedFlights, uri: Uri?) {
        getParser(type, uri)?.let{ roster ->
            if (roster.isInvalid) {
                feedback(PdfParserActivityEvents.UNSUPPORTED_FILE)
                return
            }
            val processedRoster = roster.use{ r -> r.postProcess() } // this will close the original Roster's InputStream

            // Check if calendar sync enabled for this. If so, end this function. It can be restarted from activity.
            if (checkCalendarSync(processedRoster.period)){
                feedback(PdfParserActivityEvents.CALENDAR_SYNC_ENABLED)
                return
            }
            flightRepository.saveRoster(processedRoster)
        } ?: run{
            feedback(PdfParserActivityEvents.FILE_NOT_FOUND)
            return
        }
    }

    /**
     * Parse a CompletedFlights file (eg a Monthly Overview), and pass it on to Repository for saving
     * Will provide feedback to Activity through [feedback]
     */
    private suspend fun parseCompletedFlights(type: SupportedTypes.CompletedFlights, uri: Uri?) {
        getParser(type, uri)?.let { monthly ->
            TODO("Handle completed flights")
        } ?: run{
            feedback(PdfParserActivityEvents.FILE_NOT_FOUND) // TODO handle this in Activity
            return
        }
    }

    private suspend fun parseCompleteLogbook(type: SupportedTypes.CompleteLogbook, uri: Uri?) {
        getParser(type, uri)?.let { completeLogbook ->
            TODO("Handle completed flights")
        } ?: run{
            feedback(PdfParserActivityEvents.FILE_NOT_FOUND) // TODO handle this in Activity
            return
        }
    }

    /*
    private fun run() {
        intent?.let {
            viewModelScope.launch {
                val uri = (it.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri ?: it.data)

                getTypeDetector(uri)?.let { typeDetector ->
                    //check if it was actually a supported file
                    /**
                     * This starts the correct functions when a roster type is found
                     */
                    when (typeDetector.typeOfFile) {
                        SupportedTypes.KLC_ROSTER -> uri?.getInputStream()?.use {
                            //TODO check if calendar sync is active
                            if (!parseRoster(KlcRoster(it)))
                                feedback(PdfParserActivityEvents.NOT_A_KNOWN_ROSTER)
                        } ?: feedback(PdfParserActivityEvents.ERROR).apply{
                            putString("Error 1")
                        }
                        SupportedTypes.KLC_CHECKIN_SHEET -> uri?.getInputStream()?.use{
                            if (!parseRoster(KlcCheckinSheet.ofInputStream(it)))
                                feedback(PdfParserActivityEvents.NOT_A_KNOWN_ROSTER)
                        } ?: feedback(PdfParserActivityEvents.ERROR)

                        SupportedTypes.KLC_MONTHLY -> uri?.getInputStream()?.use {
                            parseMonthly(KlcMonthlyParser(it))
                        } ?: feedback(PdfParserActivityEvents.ERROR)

                        SupportedTypes.KLM_ICA_ROSTER -> {
                            toast("Sorry, KLM Roster Not supported atm, use ical calendar")
                        }

                        SupportedTypes.KLM_ICA_MONTHLY -> uri?.getInputStream()?.use {
                            parseMonthly(KlmMonthlyParser(it))
                        } ?: feedback(PdfParserActivityEvents.ERROR)

                        SupportedTypes.MCC_PILOT_LOG_LOGBOOK -> uri?.getInputStream()?.use {
                            feedback(PdfParserActivityEvents.IMPORTING_LOGBOOK)
                            parseCsv(MccPilotLogCsvParser.ofInputStream(it))
                        }

                        SupportedTypes.LOGTEN_PRO_LOGBOOK -> uri?.getInputStream()?.use{
                            feedback(PdfParserActivityEvents.IMPORTING_LOGBOOK)
                            parseCsv(LogTenProParser.ofInputStream(it))
                        }

                        SupportedTypes.JOOZDLOG_CSV_BACKUP -> uri?.getInputStream()?.use {
                            // TODO This is a temporary fix. Importing a large backup CSV while logbook already filled causes app to hang.
                            if (flightRepository.getAllFlights().filter {!it.isPlanned}.isNotEmpty()) feedback(PdfParserActivityEvents.BACKUP_ONLY_ON_NEW_INSTALL)
                            else {
                                feedback(PdfParserActivityEvents.IMPORTING_LOGBOOK)
                                parseCsv(JoozdlogParser.ofInputStream(it))
                            }
                        }

                        SupportedTypes.UNSUPPORTED_PDF, SupportedTypes.UNSUPPORTED_CSV -> {
                            Log.d("GOT FILE:", "\n\n${typeDetector.debugData}\n\n")
                            toast("Unsupported file")
                            feedback(PdfParserActivityEvents.NOT_A_KNOWN_ROSTER)
                        }
                        
                        else -> feedback(PdfParserActivityEvents.NOT_A_KNOWN_ROSTER)
                    }
                }
            }
        } ?: feedback(PdfParserActivityEvents.FILE_NOT_FOUND)
    }
    */

    /**
     * Get inpustream from Uri and handle exceptions
     */
    private fun Uri.getInputStream(): InputStream? = try {
        /*
         * Get the content resolver instance for this context, and use it
         * to get an InputStream
         */
        App.instance.contentResolver.openInputStream(this)
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
        feedback(PdfParserActivityEvents.FILE_NOT_FOUND)
        null
    }

    /**
     * Gets a FileTypeDetector which has some metadata about fileType
     */
    private fun getTypeDetector(uri: Uri?): FileTypeDetector? {
        uri?.getInputStream()?.use { inputStream ->
            val mimeType = App.instance.contentResolver.getType(uri) ?: "NONE"
            val detector = when {
                "application/pdf" in mimeType -> PdfTypeDetector(inputStream)
                "text/csv" in mimeType || "text/comma-separated-values" in mimeType || "text/plain" in mimeType-> CsvTypeDetector(inputStream)
                else -> {
                    feedback(PdfParserActivityEvents.ERROR).apply{
                        putString("Error 2: ${App.instance.contentResolver.getType(uri)}")
                    }
                    null
                }
            }
            return if (detector?.seemsValid != true) null else detector
        }
        return null
    }

    /**
     * Get correct parser for this Planned Flights file.
     * Rosters keep an opened inputstream so should be closed after using.
     * @return null when bad input data, [Roster] otherwise
     */
    private suspend fun getParser(type: SupportedTypes.PlannedFlights, uri: Uri?): Roster? =
        uri?.getInputStream()?.let{ inputStream ->
            when(type){
                SupportedTypes.KLC_ROSTER ->  KlcRoster(inputStream)
                SupportedTypes.KLC_CHECKIN_SHEET -> inputStream.use { KlcCheckinSheet.ofInputStream(it) }
                SupportedTypes.KLM_ICA_ROSTER -> {
                    feedback(PdfParserActivityEvents.UNSUPPORTED_FILE).putString("Sorry, KLM Roster Not supported atm, use ical calendar")
                    null
                }
                else -> null.also { feedback(PdfParserActivityEvents.ERROR).putString("Error -1: This should not happen.") }
            }
        }

    /**
     * Get correct parser for this Planned Flights file.
     * Rosters keep an opened inputstream so should be closed after using.
     * @return null when bad input data, [MonthlyOverview] otherwise
     */
    private fun getParser(type: SupportedTypes.CompletedFlights, uri: Uri?): MonthlyOverview? =
        uri?.getInputStream()?.let{ inputStream ->
            when(type){
                SupportedTypes.KLC_MONTHLY ->  KlcMonthlyParser(inputStream)
                SupportedTypes.KLM_ICA_MONTHLY -> KlmMonthlyParser(inputStream)
                else -> null.also { feedback(PdfParserActivityEvents.ERROR).putString("Error -1: This should not happen.") }
            }
        }

    /**
     * Get correct parser for this Planned Flights file.
     * Rosters keep an opened inputstream so should be closed after using.
     * @return null when bad input data, [MonthlyOverview] otherwise
     */
    private suspend fun getParser(type: SupportedTypes.CompleteLogbook, uri: Uri?): ImportedLogbook? =
        uri?.getInputStream()?.let{ inputStream ->
            when(type){
                SupportedTypes.JOOZDLOG_CSV_BACKUP -> TODO("get Parser")
                SupportedTypes.LOGTEN_PRO_LOGBOOK -> TODO("get Parser")
                SupportedTypes.MCC_PILOT_LOG_LOGBOOK -> inputStream.use { MccPilotLogCsvParser.ofInputStream(it) }
                else -> null.also { feedback(PdfParserActivityEvents.ERROR).putString("Error -1: This should not happen.") }
            }
        }


    /*********************************************************************************************
     * Parser functions. This work can (should) be done async in NonCancelable coroutine
     *********************************************************************************************/

    /*
    private fun parseRoster(roster: Roster): Boolean{
        if (!roster.isValid) return false
        viewModelScope.launch (Dispatchers.IO + NonCancellable) {
            val mostRecentFlight = flightRepository.getMostRecentFlightAsync()
            val cutoffTime = mostRecentFlight.await()?.timeIn ?: 0L
            flightsToSave = roster.flights?.filter {f -> f.timeOut > cutoffTime} ?: emptyList()
            periodToSave = roster.period ?: (Instant.EPOCH .. Instant.EPOCH)

            //If calendar Sync will interfere, show Dialog in Activity. Dialog can restart saving.
            if (Preferences.getFlightsFromCalendar && (Preferences.calendarDisabledUntil < periodToSave!!.endInclusive.epochSecond)){ // has just been set to non-null
                if (Preferences.alwaysPostponeCalendarSync){
                    disableCalendarUntilAfterLastFlight()
                }
                else
                feedback(PdfParserActivityEvents.CALENDAR_SYNC_ENABLED)
            } else
                saveFlights(true)
        }
        return true
    }
    */

    private suspend fun parseMonthly(monthlyOverview: MonthlyOverview) = with(monthlyOverview){
        if (!validMonthlyOverview){
            feedback(PdfParserActivityEvents.NOT_A_KNOWN_ROSTER)
            return
        }
        foundFlights = ImportedFlightsCleaner(flights).cleanFlights().also {
            if (it == null) {
                Log.w("PdfParserActivity", "KlcMonthlyParser.flights == null")
                feedback(PdfParserActivityEvents.ERROR).apply{
                    putString("Error 4")
                }
                return
            }
            processFlights(it)
        }
    }

    /**
     * Saves flights from roster, does check if CalendarSync is enabled
     */
    /*
    private fun saveFlightsFromRoster(finish: Boolean = true){
        if (flightsToSave == null){
            feedback(PdfParserActivityEvents.ERROR).apply{
                putString("Error 6")
            }
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
    */

    private fun checkCalendarSync(period: ClosedRange<Instant>) = Preferences.getFlightsFromCalendar && (Preferences.calendarDisabledUntil < periodToSave?.endInclusive?.epochSecond ?: -1)


    private suspend fun parseCsv(parser: ImportedLogbook) = withContext(Dispatchers.Default) {
        with(parser) {
            if (!validImportedLogbook) {
                feedback(PdfParserActivityEvents.NOT_A_KNOWN_LOGBOOK)
                return@withContext
            }
            flights?.let { fff ->
                if (fff.any { it == null }) { // Some flights failed to import
                    feedback(PdfParserActivityEvents.SOME_FLIGHTS_FAILED_TO_IMPORT).apply {
                        errorLines?.let { el ->
                            extraData.putStringArrayList(FAILED_IMPORTS_TAG, ArrayList(el))
                        }
                    }
                }
                foundFlights = (
                        if (parser.needsCleaning)
                            ImportedFlightsCleaner(fff.filterNotNull()).cleanFlights()
                        else
                            fff.filterNotNull()
                        )
                    .also {
                        if (it == null) {
                            Log.w(
                                "PdfParserActivity",
                                "MccPilotLogCsvParser.flights null after it wasn't ????"
                            )
                            feedback(PdfParserActivityEvents.ERROR).apply {
                                putString("Error 5")
                            }
                            return@withContext
                        }

                        processFlights(it, false)
                    }
            }
        }
    }

    //TODO: This will have to go
    @Deprecated("This will be replaced")
    private suspend fun processFlights(foundFlights: List<Flight>, addChronoMessageIfWanted: Boolean = true): Boolean {
        var adjustments = 0
        var newCount = 0
        do{
            val exactMatches = flightRepository.findMatches(foundFlights, checkEntireDay = false, checkRegistrations = true)
            val conflicts = flightRepository.findConflicts(foundFlights)
            TODO("BUG this loop keeps running because this line doesn't work")
            val flightsToAdjust = flightRepository.findMatches(foundFlights, true).filter {match -> match.first !in exactMatches.map{it.first}}.filter {it.first !in conflicts.map{c -> c.first}}
            val newFlights = flightRepository.findNewFlights(foundFlights)


            /**
             * Keep running this loop untill no more conflicts can be fixed by adjusting non-conflicting flights
             * (ie. if all times are 3 hours off and flights overlap, first round the earliest flight on a day will be adjusted, then the next etc)
             */
            //Save flights to adjust that have no conflicts and clear that list
            flightRepository.save(flightsToAdjust.map{match ->
                val oldRemarks = match.second.remarks
                val changeRemarks = changeRemarksBuilder(match)
                val newRemarks = oldRemarks + " | ".emptyIfNotTrue(oldRemarks.isNotEmpty() && Preferences.showOldTimesOnChronoUpdate) + changeRemarks.emptyIfNotTrue(Preferences.showOldTimesOnChronoUpdate)
                adjustments++
                match.second.copy(timeOut = match.first.timeOut, timeIn = match.first.timeIn, timeStamp = match.first.timeStamp, registration = match.first.registration, aircraftType = match.first.aircraftType, remarks = newRemarks)
            })
            flightRepository.saveFromRoster(newFlights.map{f -> if (addChronoMessageIfWanted) f.copy (remarks = "From Chrono, some info may be missing") else f }.also{newCount += it.size})
            _parsedChronoData = ParsedChronoData(exactMatches, flightsToAdjust, conflicts, newFlights)

        } while (flightsToAdjust.isNotEmpty())
        /*
        if (conflicts.isEmpty()) {
            flightRepository.save(flightsToAdjust.map{match ->
                val oldRemarks = if (match.second.remarks.isBlank()) "" else " - ".emptyIfNotTrue(Preferences.showOldTimesOnChronoUpdate) + match.second.remarks
                match.second.copy(timeOut = match.first.timeOut, timeIn = match.first.timeIn, timeStamp = match.first.timeStamp, remarks = "Times adjusted from Chrono. Old: ${match.second.tOut().toTimeString()}-${match.second.tIn().toTimeString()}".emptyIfNotTrue(Preferences.showOldTimesOnChronoUpdate) + oldRemarks)
            })
            flightRepository.saveFromRoster(newFlights.map{it.copy (remarks = "From Chrono, some info may be missing")})
            feedback(PdfParserActivityEvents.CHRONO_SUCCESSFULLY_ADDED).apply{
                extraData.putInt(ADJUSTED_FLIGHTS, flightsToAdjust.size)
                extraData.putInt(NEW_FLIGHTS, newFlights.size)
                extraData.putInt(TOTAL_FLIGHTS_IN_CHRONO, foundFlights.size)
            }
            return false
        }
        */
        if (_parsedChronoData?.conflicts?.nullIfEmpty() != null){
            //TODO WIP
            feedback(PdfParserActivityEvents.CHRONO_CONFLICTS_FOUND)
            return false
        }
        feedback(PdfParserActivityEvents.CHRONO_SUCCESSFULLY_ADDED).apply{
            extraData.putInt(ADJUSTED_FLIGHTS, adjustments)
            extraData.putInt(NEW_FLIGHTS, newCount)
            extraData.putInt(TOTAL_FLIGHTS_IN_CHRONO, foundFlights.size)
        }
        return true

    }

    /**
     * Builds a string with remarks for flights that are updated from monthly overviews.
     * It assumes `orig`, `dest` and `flightNumber` stay the same.
     * @param match: Pair<New Flight, Original Flight>
     * // TODO change hardcoded string to resource
     */
    private fun changeRemarksBuilder(match: Pair<Flight, Flight>): String{
        val timesChanged = !match.first.isSameFlightAs(match.second) // checks if same orig, dest, timeOut, timeIn and flightnumber
        val regOrTypeChanged = match.first.regAndType() != match.second.regAndType()
        return "Times adjusted from Chrono. Old: ${match.second.tOut().toTimeString()}-${match.second.tIn().toTimeString()}".emptyIfNotTrue(timesChanged) +
                " | ".emptyIfNotTrue(timesChanged && regOrTypeChanged) +
                "Aircraft adjusted from Chrono. Old: ${match.second.regAndType()}".emptyIfNotTrue(regOrTypeChanged)

    }



    /*********************************************************************************************
     * Public functions and variables
     *********************************************************************************************/


    val parsedChronoData: ParsedChronoData?
        get() = _parsedChronoData


    /**
     * Only runs if [intent] is null
     */
    fun runOnce(newIntent: Intent){
        if (intent == null){
            intent = newIntent
            viewModelScope.launch { run() }
        }
    }

    /**
     * Only call this from feedback handlers when run() is finished
     */
    fun runAgain(){
        viewModelScope.launch { run() }
    }

    fun disableCalendarImport(){
        Preferences.getFlightsFromCalendar = false
    }

    fun disableCalendarUntilAfterLastFlight(){
        val maxIn = periodToSave?.endInclusive?.epochSecond
        Log.d("PeriodToSave", "p2s: $periodToSave")
        Log.d("PeriodToSave", "end: ${periodToSave?.endInclusive}")
        Log.d("PeriodToSave", "second: ${periodToSave?.endInclusive?.epochSecond}")
        if (maxIn == null) {
            feedback(PdfParserActivityEvents.ERROR).apply{
                putString("Error 7")
            }
            return
        }
        Preferences.calendarDisabledUntil = maxIn
        feedback (PdfParserActivityEvents.CALENDAR_SYNC_PAUSED)
    }

    /*
    fun saveFlights(finish: Boolean = true){
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {  saveFlightsFromRoster(finish) }
    }
    */

    fun fixConflict(number: Int?){
        if (number == null) return
        parsedChronoData?.conflicts?.get(number)?.let {
            val flightNumberChanged = it.first.flightNumber != it.second.flightNumber
            val timesChanged = it.first.timeOut != it.second.timeOut || it.first.timeIn != it.second.timeIn
            val airportsChanged = it.first.orig != it.second.orig || it.first.dest != it.second.dest
            val newRemarks = listOf(
                it.second.remarks.trim().nullIfEmpty(),
                "Old flightnumber: ${it.second.flightNumber}".nullIfNotTrue(flightNumberChanged),
                "Old orig/dest: ${it.second.orig}/${it.second.dest}".nullIfNotTrue(airportsChanged),
                "Old times:  ${it.second.tOut().toTimeString()}-${it.second.tIn().toTimeString()}".nullIfNotTrue(timesChanged)
            ).filterNotNull().joinToString(" | ")
            flightRepository.save(it.second.copy(flightNumber = it.first.flightNumber, timeOut = it.first.timeOut, timeIn = it.first.timeIn, timeStamp = it.first.timeStamp, remarks = newRemarks.emptyIfNotTrue(Preferences.showOldTimesOnChronoUpdate)))
        }
        viewModelScope.launch {
            processFlights(foundFlights!!)
        }
    }


    companion object{
        const val ADJUSTED_FLIGHTS = "ADJUSTED_FLIGHTS"
        const val NEW_FLIGHTS = "NEW_FLIGHTS"
        const val TOTAL_FLIGHTS_IN_CHRONO = "TOTAL_FLIGHTS_IN_CHRONO"

        const val FAILED_IMPORTS_TAG = "FAILED_IMPORTS_TAG"
    }
}