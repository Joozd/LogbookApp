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
import android.os.Parcelable
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
import nl.joozd.logbookapp.model.viewmodels.activities.pdfParserActivity.status.HandlerStatus
import nl.joozd.logbookapp.model.viewmodels.activities.pdfParserActivity.status.WaitingForUserChoice
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
            is CompleteLogbookFile -> askIfReplaceOrMerge(file)
            is CompletedFlightsFile -> parseCompletedFlights(file)
            is PlannedFlightsFile -> parsePlannedFlights(file)
            is UnsupportedFile -> {
                status = HandlerStatus.RECEIVED_UNSUPPORTED_FILE
                return
            }
        }
    }

    /*
     * Set status to [WaitingForUserChoice]
     * which launches either replaceCompleteLogbook or mergeCompleteLogbook
     */
    private fun askIfReplaceOrMerge(file: CompleteLogbookFile){
        //start extracting flights while waiting for user response
        val extractedFlightsAsync = async { file.extractCompletedFlights() }
        status = WaitingForUserChoice.Builder().apply{
            titleResource = R.string.importing_complete_logbook
            descriptionResource = R.string.importing_complete_logbook_long
            choice1Resource = R.string.replace
            choice2Resource = R.string.merge
            setAction1{
                replaceCompleteLogbook(extractedFlightsAsync)
            }
            setAction2{
                mergeCompleteLogbook(extractedFlightsAsync)
            }
        }.build()
    }

    private fun replaceCompleteLogbook(extractedFlightsAsync: Deferred<ExtractedCompleteLogbook>){
        launch {
            val extractedFlights = extractedFlightsAsync.await()

            TODO("Stub")
        }
    }

    private fun mergeCompleteLogbook(extractedFlightsAsync: Deferred<ExtractedCompleteLogbook>){
        launch {
            val extractedFlights = extractedFlightsAsync.await()

            TODO("Stub")
        }
    }

    private fun parseCompletedFlights(file: CompletedFlightsFile){
        val extractedFlights: ExtractedCompletedFlights = file.extractCompletedFlights()
        TODO("Stub")
    }

    private fun parsePlannedFlights(file: PlannedFlightsFile){
        val extractedFlights: ExtractedPlannedFlights = file.extractPlannedFlights()
        TODO("Stub")
    }

    private suspend fun getFileFromIntent(intent: Intent, contentResolver: ContentResolver) =
        getTypeDetector(intent.getUri(), contentResolver)
            ?.getFile()

    private fun Intent.getUri() =
        getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
        ?: data

    private suspend fun getTypeDetector(
        uri: Uri?,
        contentResolver: ContentResolver
    ): FileImporter? = withContext(DispatcherProvider.io()){
        uri?.getInputStream(contentResolver)?.use { inputStream ->
            val mimeType = contentResolver.getType(uri) ?: "NONE"
            val detector = when {
                mimeType.isPdfMimeType() -> PdfImporter(inputStream)
                mimeType.isCsvMimeType() -> CsvImporter(inputStream)
                else -> {
                    status = HandlerStatus.RECEIVED_UNSUPPORTED_FILE
                    null
                }
            }
            detector
        }
    }

    /**
     * Get inpustream from Uri and handle exceptions
     */
    private fun Uri?.getInputStream(contentResolver: ContentResolver): InputStream? =
        if (this == null){
            status = HandlerStatus.RECEIVED_BAD_INTENT
            null
        }
        else try {
            contentResolver.openInputStream(this)
        } catch (e: FileNotFoundException) {
            status = HandlerStatus.FILE_NOT_FOUND
            null
    }

    private fun String.isCsvMimeType() =
        "text/csv" in this || "text/comma-separated-values" in this || "text/plain" in this

    private fun String.isPdfMimeType() =
        "application/pdf" in this


}