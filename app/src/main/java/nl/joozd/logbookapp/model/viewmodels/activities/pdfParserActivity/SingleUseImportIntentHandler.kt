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

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import nl.joozd.joozdlogimporter.CsvImporter
import nl.joozd.joozdlogimporter.PdfImporter
import nl.joozd.joozdlogimporter.dataclasses.ExtractedCompleteLogbook
import nl.joozd.joozdlogimporter.dataclasses.ExtractedCompletedFlights
import nl.joozd.joozdlogimporter.dataclasses.ExtractedPlannedFlights
import nl.joozd.joozdlogimporter.interfaces.FileImporter
import nl.joozd.joozdlogimporter.supportedFileTypes.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.importing.ImportedFlightsSaver
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.model.viewmodels.activities.pdfParserActivity.status.DoneCompletedFlightsWithResult
import nl.joozd.logbookapp.model.viewmodels.activities.pdfParserActivity.status.HandlerError
import nl.joozd.logbookapp.model.viewmodels.activities.pdfParserActivity.status.HandlerStatus
import nl.joozd.logbookapp.model.viewmodels.activities.pdfParserActivity.status.UserChoice
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut
import nl.joozd.logbookapp.utils.DispatcherProvider
import java.io.FileNotFoundException
import java.io.InputStream

/**
 * Get imported data from an Intent and handle that data
 * e.g. import rostered flights, add monthly overview data or import backed-up flights.
 * This handles one Intent.
 * If another Intent is received, that other Intent is ignored.
 */
class SingleUseImportIntentHandler: CoroutineScope {
    override val coroutineContext = Dispatchers.Default + SupervisorJob()
    private var unused = true

    val statusFlow: StateFlow<HandlerStatus> = MutableStateFlow(HandlerStatus.WAITING_FOR_INTENT)
    private var status: HandlerStatus by CastFlowToMutableFlowShortcut(statusFlow)

    fun handleIntent(intent: Intent, contentResolver: ContentResolver){
        if (unused){
            unused = false
            launch{
                getFileAndStartAppropriateParser(intent, contentResolver)
            }
        }
    }

    private suspend fun getFileAndStartAppropriateParser(intent: Intent, contentResolver: ContentResolver){
        status = HandlerStatus.READING_URI
        val file = getFileFromIntent(intent, contentResolver) ?: return

        when(file){
            is CompleteLogbookFile -> parseCompleteLogbook(file)
            is CompletedFlightsFile -> parseCompletedFlights(file)
            is PlannedFlightsFile -> parsePlannedFlights(file)
            is UnsupportedFile -> {
                status = HandlerError(R.string.unknown_file_message)
                return
            }
        }
    }

    private suspend fun parseCompleteLogbook(file: CompleteLogbookFile) {
        status = HandlerStatus.EXTRACTING_FLIGHTS

        //start extracting flights while waiting for user response
        val extractedLogbookAsync = withContext (Dispatchers.Default) { async { file.extractCompletedFlights() } }

        //This will continue the process
        askIfDataShouldBeSanitized(extractedLogbookAsync)
    }

    private suspend fun parseCompletedFlights(file: CompletedFlightsFile){
        status = HandlerStatus.EXTRACTING_FLIGHTS
        val extractedFlights: ExtractedCompletedFlights = file.extractCompletedFlights()
        status = HandlerStatus.SAVING_FLIGHTS
        val result = ImportedFlightsSaver.instance.save(extractedFlights)
        status = if (result.success) DoneCompletedFlightsWithResult(result)
            else HandlerError(R.string.error_saving_flights)

    }

    private suspend fun parsePlannedFlights(file: PlannedFlightsFile){
        status = HandlerStatus.EXTRACTING_FLIGHTS
        val extractedFlights: ExtractedPlannedFlights = extractAndAutocomplete(file)
        if (Prefs.alwaysPostponeCalendarSync()) {
            status = HandlerStatus.SAVING_FLIGHTS
            saveAndPostponeCalendarSync(extractedFlights)
            status = HandlerStatus.DONE
        }
        else status = buildPostponeCalendarSyncUserChoice(extractedFlights)
    }

    private suspend fun extractAndAutocomplete(file: PlannedFlightsFile) =
        ImportedLogbookAutoCompleter().autocomplete(file.extractPlannedFlights())

    private fun askIfDataShouldBeSanitized(extractedLogbookAsync: Deferred<ExtractedCompleteLogbook>){
        // Will give user a choice to run [sanitizeLogbook] before running [askIfReplaceOrMerge]
        status = buildAutoValuesCompleteLogbookChoice(extractedLogbookAsync)
    }

    private suspend fun sanitizeLogbook(logbook: ExtractedCompleteLogbook): ExtractedCompleteLogbook =
        ImportedLogbookAutoCompleter().autocomplete(logbook)

    private fun askIfReplaceOrMerge(extractedLogbook: ExtractedCompleteLogbook){
        // Will give user the choice between running  [replaceCompleteLogbook] or [mergeCompleteLogbook]
        status = buildMergeOrReplaceCompleteLogbookChoice(extractedLogbook)
    }

    private fun confirmReplaceCompleteLogbook(extractedLogbook: ExtractedCompleteLogbook) =
        UserChoice.Builder().apply {
            titleResource = R.string.are_you_sure_qmk
            descriptionResource = R.string.this_will_replace_your_entire_logbook
            choice1Resource = R.string.replace
            choice2Resource = R.string.merge
            setAction1 { replaceCompleteLogbook(extractedLogbook) }
            setAction2 { mergeCompleteLogbook(extractedLogbook) }
        }.build()


    private fun replaceCompleteLogbook(extractedFlights: ExtractedCompleteLogbook){
        launch {
            status = HandlerStatus.SAVING_FLIGHTS
            ImportedFlightsSaver.instance.replace(extractedFlights)
            status = HandlerStatus.DONE
        }
    }

    private fun mergeCompleteLogbook(extractedFlights: ExtractedCompleteLogbook){
        launch {
            status = HandlerStatus.SAVING_FLIGHTS

            ImportedFlightsSaver.instance.merge(extractedFlights)
            status = HandlerStatus.DONE
        }
    }

    private fun buildMergeOrReplaceCompleteLogbookChoice(extractedLogbook: ExtractedCompleteLogbook) =
        UserChoice.Builder().apply {
            titleResource = R.string.importing_complete_logbook
            descriptionResource = R.string.importing_complete_logbook_long
            choice1Resource = R.string.replace
            choice2Resource = R.string.merge
            setAction1 {
                status = confirmReplaceCompleteLogbook(extractedLogbook)
                replaceCompleteLogbook(extractedLogbook)
            }
            setAction2 {
                mergeCompleteLogbook(extractedLogbook)
            }
        }.build()

    private fun buildAutoValuesCompleteLogbookChoice(extractedLogbookAsync: Deferred<ExtractedCompleteLogbook>) =
        UserChoice.Builder().apply {
            titleResource = R.string.importing_complete_logbook
            descriptionResource = R.string.import_complete_logbook_sanitize
            choice1Resource = R.string.yes
            choice2Resource = R.string.no
            setAction1 {
                launch {
                    status = HandlerStatus.CLEANING_LOGBOOK
                    val sanitizedLogbook = sanitizeLogbook(extractedLogbookAsync.await())
                    askIfReplaceOrMerge(sanitizedLogbook)
                }
            }
            setAction2 {
                launch {
                    askIfReplaceOrMerge(extractedLogbookAsync.await())
                }
            }
        }.build()

    private fun buildPostponeCalendarSyncUserChoice(extractedFlights: ExtractedPlannedFlights) =
        UserChoice.Builder().apply {
            titleResource = R.string.calendar_sync_conflict
            descriptionResource = R.string.calendar_update_active
            choice1Resource = R.string.postpone_sync
            choice2Resource = android.R.string.cancel
            setAction1 {
                launch {
                    Prefs.alwaysPostponeCalendarSync(true)
                    status = HandlerStatus.SAVING_FLIGHTS
                    saveAndPostponeCalendarSync(extractedFlights)
                    status = HandlerStatus.DONE
                }
            }
            setAction2 {
                status = HandlerStatus.DONE
            }
        }.build()

    private suspend fun saveAndPostponeCalendarSync(extractedFlights: ExtractedPlannedFlights) {
        Prefs.calendarDisabledUntil(extractedFlights.period?.endInclusive ?: return) // no period = no sync
        ImportedFlightsSaver.instance.save(extractedFlights)
    }

    private suspend fun getFileFromIntent(intent: Intent, contentResolver: ContentResolver) =
        withContext(DispatcherProvider.io()) {
            getTypeDetector(intent.getUri(), contentResolver)
                ?.getFile()
        }

    private fun Intent.getUri() = when {
        SDK_INT >= 33 -> getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        else -> @Suppress("DEPRECATION") getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
    }


    private suspend fun getTypeDetector(
        uri: Uri?,
        contentResolver: ContentResolver
    ): FileImporter? =
        uri?.getInputStream(contentResolver)?.use { inputStream ->
            val mimeType = contentResolver.getType(uri) ?: "NONE"

            @Suppress("BlockingMethodInNonBlockingContext") // unblocked with Dispatchers.IO
            val detector = when {
                mimeType.isPdfMimeType() -> withContext(DispatcherProvider.io()) {
                    PdfImporter.ofInputStream(inputStream)
                }
                mimeType.isCsvMimeType() -> withContext(DispatcherProvider.io()) {
                    CsvImporter.ofInputStream(inputStream)
                }
                else -> {
                    status = HandlerError(R.string.unknown_file_message)
                    null
                }
            }
            detector
    }

    /**
     * Get inpustream from Uri and handle exceptions
     */
    private fun Uri?.getInputStream(contentResolver: ContentResolver): InputStream? =
        if (this == null){
            status = HandlerError(R.string.bad_intent_error)
            null
        }
        else try {
            contentResolver.openInputStream(this)
        } catch (e: FileNotFoundException) {
            status = HandlerError(R.string.file_not_found_message)
            null
    }

    private fun String.isCsvMimeType() =
        "text/csv" in this || "text/comma-separated-values" in this || "text/plain" in this

    private fun String.isPdfMimeType() =
        "application/pdf" in this


}