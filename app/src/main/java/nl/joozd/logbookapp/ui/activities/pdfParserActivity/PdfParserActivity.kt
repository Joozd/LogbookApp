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

package nl.joozd.logbookapp.ui.activities.pdfParserActivity

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.ActivityPdfParserBinding
import nl.joozd.logbookapp.model.viewmodels.activities.pdfParserActivity.PdfParserActivityViewModel
import nl.joozd.logbookapp.model.viewmodels.activities.pdfParserActivity.status.DoneCompletedFlightsWithResult
import nl.joozd.logbookapp.model.viewmodels.activities.pdfParserActivity.status.HandlerError
import nl.joozd.logbookapp.model.viewmodels.activities.pdfParserActivity.status.HandlerStatus
import nl.joozd.logbookapp.model.viewmodels.activities.pdfParserActivity.status.UserChoice
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity

/**
 * PdfParserActivity parses PDF files from an Intent. After parsing, it starts MainActivity.
 */
class PdfParserActivity : JoozdlogActivity(), CoroutineScope by MainScope() {
    private val viewModel: PdfParserActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("PdfParserActivity", "started")
        ActivityPdfParserBinding.inflate(layoutInflater).apply {
            collectHandlerStatus()
            launchIntentHandler()
            setContentView(root)
        }
    }

    private fun ActivityPdfParserBinding.launchIntentHandler() {
        @Suppress("RedundantExplicitType")  // it is not redundant; lifecycleScope.launch needs a Job instead of a CompleteableJob
        var collectJob: Job = Job()
        collectJob = lifecycleScope.launch {
            viewModel.repositoriesReadyFlow.collect{
                if (it) {
                    viewModel.handleIntent(intent, contentResolver)
                    collectJob.cancel()
                }
                else pdfParserStatusTextView.setText(R.string.loading_aircraft_and_airports_database)
            }
        }
    }

    private fun ActivityPdfParserBinding.collectHandlerStatus() {
        viewModel.handlerStatus.launchCollectWhileLifecycleStateStarted {
            when (it) {
                HandlerStatus.WAITING_FOR_INTENT -> pdfParserStatusTextView.text = getString(R.string.waiting_for_data)
                HandlerStatus.READING_URI -> pdfParserStatusTextView.text = getString(R.string.reading_file)
                HandlerStatus.EXTRACTING_FLIGHTS -> pdfParserStatusTextView.text = getString(R.string.extracting_flights)
                HandlerStatus.CLEANING_LOGBOOK -> pdfParserStatusTextView.text = getString(R.string.cleaning_logbook)
                HandlerStatus.SAVING_FLIGHTS -> pdfParserStatusTextView.text = getString(R.string.saving_flights)
                HandlerStatus.NOT_IMPLEMENTED -> showNotImplementedDialog()
                HandlerStatus.DONE -> closeAndStartMainActivity()
                is HandlerError -> showHandlerError(it)
                is UserChoice -> showUserChoiceDialog(it)
                is DoneCompletedFlightsWithResult -> showCompletedFlightsResult(it)
            }
        }
    }

    private fun showUserChoiceDialog(choice: UserChoice) =
        choice.toAlertDialog(this).show()

    private fun showCompletedFlightsResult(result: DoneCompletedFlightsWithResult) =
        AlertDialog.Builder(this).apply{
            setTitle(R.string.imported_complete_flights)
            setMessage(getString(R.string.imported_complete_flights_results, result.status.totalFlightsImported, result.status.flightsUpdated, result.status.flightsInCompletedButNotOnDevice, result.status.flightsOnDeviceButNotInCompleted))
            setPositiveButton(android.R.string.ok){ _, _ ->
                closeAndStartMainActivity()
            }
        }.create().show()

    private fun showHandlerError(error: HandlerError) =
        AlertDialog.Builder(this).apply{
            setTitle(R.string.error)
            setMessage(error.errorMessageResource)
            setPositiveButton(android.R.string.ok){ _, _ ->
                finish()
            }
        }.create().show()

    private fun showNotImplementedDialog() =
        AlertDialog.Builder(this).apply{
            setTitle(R.string.not_implemented_title)
            setMessage(R.string.not_implemented_body)
            setPositiveButton(android.R.string.ok){ _, _ ->
                finish()
            }
        }.create().show()
}



