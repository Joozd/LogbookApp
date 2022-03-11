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
import nl.joozd.joozdlogimporter.interfaces.FileImporter
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import nl.joozd.joozdlogimporter.CsvImporter
import nl.joozd.joozdlogimporter.PdfImporter
import nl.joozd.joozdlogimporter.supportedFileTypes.*
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.data.importing.ImportedFlightsSaver
import nl.joozd.logbookapp.data.importing.extensions.postProcess
import nl.joozd.logbookapp.data.importing.importsParser.JoozdlogParser
import nl.joozd.logbookapp.data.importing.importsParser.LogTenProParser
import nl.joozd.logbookapp.data.importing.importsParser.MccPilotLogCsvParser
import nl.joozd.logbookapp.data.importing.interfaces.ImportedLogbook
import nl.joozd.logbookapp.data.importing.interfaces.CompletedFlights
import nl.joozd.logbookapp.data.importing.interfaces.Roster
import nl.joozd.logbookapp.data.importing.pdfparser.*
import nl.joozd.logbookapp.data.importing.results.SaveCompletedFlightsResult
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.PdfParserActivityEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import nl.joozd.logbookapp.extensions.*
import nl.joozd.logbookapp.utils.DispatcherProvider
import java.io.FileNotFoundException
import java.io.InputStream
import java.time.Instant

class PdfParserActivityViewModel: JoozdlogActivityViewModel() {
    private val handler = SingleUseImportIntentHandler()
    val status = handler.statusFlow

    fun handleIntent(intent: Intent, contentResolver: ContentResolver){
        handler.handleIntent(intent, contentResolver)
    }


}