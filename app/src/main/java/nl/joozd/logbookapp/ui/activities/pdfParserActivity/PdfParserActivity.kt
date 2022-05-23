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
import kotlinx.coroutines.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.comm.OldCloud
import nl.joozd.logbookapp.comm.ServerFunctionResult
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.databinding.ActivityPdfParserBinding
import nl.joozd.logbookapp.model.viewmodels.activities.pdfParserActivity.PdfParserActivityViewModel
import nl.joozd.logbookapp.model.viewmodels.activities.pdfParserActivity.status.DoneCompletedFlightsWithResult
import nl.joozd.logbookapp.model.viewmodels.activities.pdfParserActivity.status.HandlerError
import nl.joozd.logbookapp.model.viewmodels.activities.pdfParserActivity.status.HandlerStatus
import nl.joozd.logbookapp.model.viewmodels.activities.pdfParserActivity.status.UserChoice
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity

/**
 * PdfParserActivity does the following:
 * - Get an intent with a PDF file
 * - Send that intent to ViewModel
 * - Get result from Viewmodel and display feedback to user
 * - launch MainActivity
 */
class PdfParserActivity : JoozdlogActivity(), CoroutineScope by MainScope() {
    private val viewModel: PdfParserActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("PdfParserActivity", "started")
        ActivityPdfParserBinding.inflate(layoutInflater).apply {
            launchIntentHandler()
            collectHandlerStatus()
            setContentView(root)
        }
    }

    private fun ActivityPdfParserBinding.launchIntentHandler() {
        if (checkAirportDatabaseAvailable())
            viewModel.handleIntent(intent, contentResolver)
        else downloadAirportsThenLaunchIntentHandler()
    }

    private fun ActivityPdfParserBinding.collectHandlerStatus() {
        viewModel.status.launchCollectWhileLifecycleStateStarted {
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
            setTitle(R.string.not_implemented_body)
            setPositiveButton(android.R.string.ok){ _, _ ->
                finish()
            }
        }.create().show()

    private fun checkAirportDatabaseAvailable(): Boolean =
        Prefs.airportDbVersion > 0

    private fun ActivityPdfParserBinding.downloadAirportsThenLaunchIntentHandler(){
        launch{
            pdfParserStatusTextView.text = getString(R.string.downloading_airports)
            if(downloadAirportDatabase())
                viewModel.handleIntent(intent, contentResolver)
            else AlertDialog.Builder(activity).apply{
                setMessage(R.string.no_airport_db_for_importing)
                setTitle(R.string.error)
                setPositiveButton(android.R.string.ok){ _, _ ->
                    finish()
                }
            }
        }
    }

    private suspend fun downloadAirportDatabase(): Boolean{
        val result = OldCloud.downloadAirportsDatabase{
            if (it%10 == 0)
                Log.d(this::class.simpleName, "Downloading airports DB - $it%")
        }
        return result == ServerFunctionResult.OK
    }


}



