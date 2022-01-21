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

package nl.joozd.logbookapp.model.viewmodels.activities.pdfParserActivity

import nl.joozd.joozdlogfiletypedetector.interfaces.FileTypeDetector
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import nl.joozd.joozdlogfiletypedetector.CsvTypeDetector
import nl.joozd.joozdlogfiletypedetector.PdfTypeDetector
import nl.joozd.joozdlogfiletypedetector.SupportedTypes
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.data.parseSharedFiles.extensions.postProcess
import nl.joozd.logbookapp.data.parseSharedFiles.importsParser.JoozdlogParser
import nl.joozd.logbookapp.data.parseSharedFiles.importsParser.LogTenProParser
import nl.joozd.logbookapp.data.parseSharedFiles.importsParser.MccPilotLogCsvParser
import nl.joozd.logbookapp.data.parseSharedFiles.interfaces.ImportedLogbook
import nl.joozd.logbookapp.data.parseSharedFiles.interfaces.CompletedFlights
import nl.joozd.logbookapp.data.parseSharedFiles.interfaces.Roster
import nl.joozd.logbookapp.data.parseSharedFiles.pdfparser.*
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryImpl
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.PdfParserActivityEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import nl.joozd.logbookapp.extensions.*
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
    val statusLiveData: LiveData<Int>
        get() = _statusLiveData

    /**
     * Holds result of chrono import, null if no chrono was imported before this
     */
    var chronoImportResult: FlightRepositoryImpl.SaveCompleteFlightsResult? = null
        private set

    private val _statusLiveData = MutableLiveData(STARTING_UP)

    private var intent: Intent? = null

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
            updateStatus(READING_FILE)
            val uri = (it.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri ?: it.data)
            val typeDetector = getTypeDetector(uri) ?: run {
                feedback(PdfParserActivityEvents.FILE_NOT_FOUND) // TODO handle this in Activity
                updateStatus(ERROR)
                return@withContext /* END HERE */
            }

            /*
             * Catch bad data:
             */
            if (typeDetector.seemsInvalid || typeDetector.typeOfFile is SupportedTypes.Unsupported) {
                feedback(PdfParserActivityEvents.UNSUPPORTED_FILE) // TODO handle this in Activity
                if (typeDetector.seemsInvalid)
                    Log.w(this::class.simpleName, "Unsupported file 001")
                else
                    Log.w(this::class.simpleName, "Unsupported file 002")
                updateStatus(ERROR)
                return@withContext  /* END HERE */
            }

            /*
             * We now have a supported file.
             * What happens next: Depending on if this is a roster or something else, start the respective function
             */
            when (val type = typeDetector.typeOfFile) {
                is SupportedTypes.PlannedFlights -> {
                    updateStatus(PARSING_ROSTER)
                    parseRoster(type, uri)
                }

                is SupportedTypes.CompletedFlights -> {
                    updateStatus(PARSING_CHRONO)
                    parseCompletedFlights(type, uri)
                }

                is SupportedTypes.CompleteLogbook -> {
                    feedback(PdfParserActivityEvents.PARSING_COMPLETE_LOGBOOK)
                    updateStatus(PARSING_CHRONO)
                    parseCompleteLogbook(type, uri)
                }

                else -> {
                    feedback(PdfParserActivityEvents.ERROR).putString("Error -1: This should not happen.")
                    updateStatus(ERROR)
                    return@withContext
                }
            }
            updateStatus(DONE)
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
                Log.w(this::class.simpleName, "Unsupported file 006")
                return
            }
            val processedRoster = roster.postProcess()

            /**
             * Check if calendar sync enabled for this.
             * If so, disable it if [Preferences.alwaysPostponeCalendarSync], else end this function. It can be restarted from activity.
             * If calendar sync is enabled, CALENDAR_SYNC_ENABLED wil be sent to Activity, with the epochSecond of the end of the period.
             * Activity can decide to change CalendarSync through [disableCalendarSync] and run [runAgain] to try again.
             */
            if (checkCalendarSync(processedRoster.period)){
                if (Preferences.alwaysPostponeCalendarSync) {
                    disableCalendarSync(processedRoster.period.endInclusive.epochSecond)
                } else {
                    feedback(PdfParserActivityEvents.CALENDAR_SYNC_ENABLED).putLong(processedRoster.period.endInclusive.epochSecond)
                    return
                }
            }
            /**
             * Ask Repository to save this roster.
             * @see flightRepository -> saveRoster
             */
            flightRepository.saveRoster(processedRoster, canUndo = true)
            feedback(PdfParserActivityEvents.ROSTER_SUCCESSFULLY_ADDED)
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
        getParser(type, uri)?.let { completedFlights ->
            if (completedFlights.isInvalid){
                feedback(PdfParserActivityEvents.UNSUPPORTED_FILE)
                Log.w(this::class.simpleName, "Unsupported file 003")
                return
            }
            val processedCompleteFlights = completedFlights.use { cf -> cf.postProcess() }

            // Repository will save flights and return how many conflicts were found. User can fix them in MainActivity
            // TODO or undo this whole thing (make undo logic in repository)
            flightRepository.saveCompletedFlights(processedCompleteFlights).let{result ->
                chronoImportResult = result
                when {
                    result.conflicts != 0 && result.plannedRemaining != 0 ->
                        feedback(PdfParserActivityEvents.CHRONO_CONFLICTS_FOUND_AND_NOT_ALL_PLANNED)
                    result.conflicts != 0 ->
                        feedback(PdfParserActivityEvents.CHRONO_CONFLICTS_FOUND)
                    result.plannedRemaining != 0 ->
                        feedback(PdfParserActivityEvents.CHRONO_DOES_NOT_FILL_ALL_PLANNED)
                    else ->
                        feedback(PdfParserActivityEvents.CHRONO_SUCCESSFULLY_ADDED)
                }
            }

        } ?: run{
            feedback(PdfParserActivityEvents.FILE_NOT_FOUND) // TODO handle this in Activity
            return
        }
    }


    /**
     * Parse a CompleteLogbook file (eg a LogTenPro .txt file), and pass it on to Repository for saving
     * Will provide feedback to Activity through [feedback]
     * TODO check if this takes a long time and if so make a progress something
     * TODO ask if this should replace or merge with current data if data is present
     */
    private suspend fun parseCompleteLogbook(type: SupportedTypes.CompleteLogbook, uri: Uri?) {
        getParser(type, uri)?.let { completeLogbook ->
            //TODO("Handle completelogbook")
            if (!completeLogbook.validImportedLogbook){
                feedback(PdfParserActivityEvents.UNSUPPORTED_FILE)
                Log.w(this::class.simpleName, "Unsupported file 004")
                return
            }
            flightRepository.saveCompletedFlights(completeLogbook.postProcess())
            feedback(PdfParserActivityEvents.CHRONO_SUCCESSFULLY_ADDED)
        } ?: run{
            feedback(PdfParserActivityEvents.FILE_NOT_FOUND) // TODO handle this in Activity
            return
        }
    }


    /**
     * Get inpustream from Uri and handle exceptions
     */
    private fun Uri.getInputStream(): InputStream? = try {
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
                SupportedTypes.KLC_ROSTER ->  inputStream.use { KlcRoster.ofInputStream(it) }
                SupportedTypes.KLC_CHECKIN_SHEET -> inputStream.use { KlcCheckinSheet.ofInputStream(it) }
                SupportedTypes.KLM_ICA_ROSTER -> {
                    feedback(PdfParserActivityEvents.UNSUPPORTED_FILE).putString("Sorry, KLM Roster Not supported atm, use ical calendar")
                    Log.w(this::class.simpleName, "Unsupported file 005")
                    null
                }
                else -> null.also { feedback(PdfParserActivityEvents.ERROR).putString("Error -2: This should not happen.") }
            }
        }

    /**
     * Get correct parser for this Planned Flights file.
     * Rosters keep an opened inputstream so should be closed after using.
     * @return null when bad input data, [CompletedFlights] otherwise
     */
    private fun getParser(type: SupportedTypes.CompletedFlights, uri: Uri?): CompletedFlights? =
        uri?.getInputStream()?.let{ inputStream ->
            when(type){
                SupportedTypes.KLC_MONTHLY ->  KlcMonthlyParser(inputStream)
                SupportedTypes.KLM_ICA_MONTHLY -> KlmMonthlyParser(inputStream)
                else -> null.also { feedback(PdfParserActivityEvents.ERROR).putString("Error -3: This should not happen.") }
            }
        }

    /**
     * Get correct parser for this Planned Flights file.
     * Rosters keep an opened inputstream so should be closed after using.
     * @return null when bad input data, [CompletedFlights] otherwise
     */
    private suspend fun getParser(type: SupportedTypes.CompleteLogbook, uri: Uri?): ImportedLogbook? =
        uri?.getInputStream()?.let{ inputStream ->
            when(type){
                SupportedTypes.JOOZDLOG_CSV_BACKUP -> inputStream.use { JoozdlogParser.ofInputStream(it)}
                SupportedTypes.LOGTEN_PRO_LOGBOOK -> inputStream.use { LogTenProParser.ofInputStream(it)}
                SupportedTypes.MCC_PILOT_LOG_LOGBOOK -> inputStream.use { MccPilotLogCsvParser.ofInputStream(it) }
                else -> null.also { feedback(PdfParserActivityEvents.ERROR).putString("Error -4: This should not happen.") }
            }
        }

    /**
     * Sugar for updating status a little more clearly
     */
    private fun updateStatus(status: Int){
        _statusLiveData.postValue(status)
    }

    /*********************************************************************************************
     * Parser functions. This work can (should) be done async in NonCancelable coroutine
     *********************************************************************************************/

    /**
     * Check if Calendar Sync is enabled during this period
     * @param period the period to check
     * @return true if Calendar Sync is enabled during any part of [period]
     */
    private fun checkCalendarSync(period: ClosedRange<Instant>) = Preferences.useCalendarSync && (Preferences.calendarDisabledUntil < period.endInclusive.epochSecond)

    /**
     * Disables Calendar Sync
     * @param end: Time from which calendar Sync will be enabled again. If null, calendar Sync will be switched off permanently.
     */
    fun disableCalendarSync(end: Long? = null){
        if (end == null) Preferences.useCalendarSync = false
        else Preferences.calendarDisabledUntil = end+1 // one second after [end] so [end] is inclusive in the disabling
    }

    /*********************************************************************************************
     * Public functions and variables
     *********************************************************************************************/

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

    companion object{
        /**
         * Status constants:
         */
        const val ERROR = -1
        const val STARTING_UP = 0
        const val READING_FILE = 1
        const val PARSING_ROSTER = 2
        const val PARSING_CHRONO = 3
        const val DONE = 999
    }
}