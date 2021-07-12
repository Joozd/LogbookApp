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

package nl.joozd.logbookapp.ui.activities.pdfParserActivity

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import kotlinx.coroutines.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.databinding.ActivityPdfParserBinding
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.PdfParserActivityEvents
import nl.joozd.logbookapp.model.viewmodels.activities.pdfParserActivity.PdfParserActivityViewModel
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity
import nl.joozd.logbookapp.ui.dialogs.JoozdlogAlertDialog
import nl.joozd.logbookapp.ui.utils.longToast
import nl.joozd.logbookapp.ui.utils.toast

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

        if (Preferences.aircraftTypesVersion == 0 || Preferences.airportDbVersion == 0) {
            toast("Cannot import without Airport DB, try again later")
            //TODO try to download it first before failing?
            finish()
        }


        Log.d("PdfParserActivity", "started")
        ActivityPdfParserBinding.inflate(layoutInflater).apply {

            viewModel.runOnce(intent)


            /****************************************************************************************
             * Observers:
             ****************************************************************************************/

            viewModel.statusLiveData.observe(activity){
                pdfParserStatusTextView.text = when (it){
                    PdfParserActivityViewModel.STARTING_UP -> getString(R.string.waiting_for_data)
                    PdfParserActivityViewModel.READING_FILE -> getString(R.string.readingFile)
                    PdfParserActivityViewModel.PARSING_ROSTER -> getString(R.string.reading_roster)
                    PdfParserActivityViewModel.PARSING_CHRONO -> getString(R.string.reading_chrono)
                    PdfParserActivityViewModel.DONE -> getString(R.string.done)
                    else -> "This should not happen"
                }
            }

            viewModel.feedbackEvent.observe(activity) {
                when (it.getEvent()) {
                    PdfParserActivityEvents.NOT_IMPLEMENTED -> {
                        toast("Not supported yet")
                        closeAndStartMainActivity()
                    }

                    PdfParserActivityEvents.IMPORTING_LOGBOOK -> {
                        pdfParserStatusTextView.text = getString(R.string.working_ellipsis)
                    }

                    PdfParserActivityEvents.ROSTER_SUCCESSFULLY_ADDED -> {
                        longToast("Imported roster")
                        closeAndStartMainActivity()
                    }
                    PdfParserActivityEvents.CHRONO_SUCCESSFULLY_ADDED -> {
                        longToast("Imported Chrono")
                        closeAndStartMainActivity()
                    }
                    PdfParserActivityEvents.CHRONO_CONFLICTS_FOUND -> {
                        showChronoConflictDialog()
                    }
                    PdfParserActivityEvents.CHRONO_DOES_NOT_FILL_ALL_PLANNED -> {
                        showChronoIncompleteDialog()
                    }
                    PdfParserActivityEvents.CHRONO_CONFLICTS_FOUND_AND_NOT_ALL_PLANNED -> {
                        showChronoConflictAndIncompleteDialog()
                    }
                    /**
                     * Tempo notification. TODO improve on this
                     */
                    PdfParserActivityEvents.PARSING_COMPLETE_LOGBOOK -> {
                        showWholeLogbookInfoMessage()
                    }





                    PdfParserActivityEvents.ERROR, PdfParserActivityEvents.FILE_NOT_FOUND -> {
                        toast("Error reading file: ${it.getString()}")
                        closeAndStartMainActivity()
                    }
                    PdfParserActivityEvents.NOT_A_KNOWN_ROSTER, PdfParserActivityEvents.NOT_A_KNOWN_LOGBOOK -> {
                        JoozdlogAlertDialog().show(activity){
                            titleResource = R.string.unknown_file_title
                            messageResource = R.string.unknown_file_message
                            setPositiveButton(android.R.string.ok){
                                closeAndStartMainActivity()
                            }
                        }
                    }
                    PdfParserActivityEvents.CALENDAR_SYNC_ENABLED -> {
                        JoozdlogAlertDialog().show(activity) {
                            messageResource = R.string.calendar_update_active
                            setPositiveButton(R.string.always) { _ ->
                                viewModel.disableCalendarSync(it.getLong())
                                Preferences.alwaysPostponeCalendarSync = true
                                viewModel.runAgain()
                            }
                            setNegativeButton(android.R.string.ok) {  _ ->  // Using "negative" button as positive so it gets placed the way I want to. Nothing negative about it.
                                viewModel.disableCalendarSync(it.getLong())
                                viewModel.runAgain()
                            }
                            setNeutralButton(android.R.string.cancel) {
                                closeAndStartMainActivity()
                            }
                        }
                    }
                    PdfParserActivityEvents.CALENDAR_SYNC_PAUSED -> {
                        if (!Preferences.alwaysPostponeCalendarSync)
                            JoozdlogAlertDialog().show(activity) {
                                messageResource = R.string.you_can_start_calendar_sync_again
                                setPositiveButton(android.R.string.ok) {
                                    closeAndStartMainActivity()
                                }
                            }
                        else closeAndStartMainActivity()
                    }
                    null -> {
                    }
                    else -> {
                        toast("SOMETHING NOT IMPLEMENTED HAPPENED")
                        startMainActivity(activity)
                        finish()
                    }
                }
            }

            setContentView(root)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    /**
     * Show dialog stating conflicts were found in chrono import
     */

    private fun showChronoConflictDialog() {
        JoozdlogAlertDialog().show(this){
            titleResource = R.string.chrono_conflict_found
            message = this@PdfParserActivity.getString(R.string.chrono_conflict_found_long, viewModel.chronoImportResult?.conflicts ?: -1)
            setPositiveButton(android.R.string.ok){
                closeAndStartMainActivity()
            }
        }
    }
    private fun showChronoIncompleteDialog() {
        JoozdlogAlertDialog().show(this){
            titleResource = R.string.planned_flights_found
            message = this@PdfParserActivity.getString(R.string.planned_flights_found_long, viewModel.chronoImportResult?.plannedRemaining ?: -1)
            setPositiveButton(android.R.string.ok){
                closeAndStartMainActivity()
            }
        }
    }
    private fun showChronoConflictAndIncompleteDialog() {
        JoozdlogAlertDialog().show(this){
            titleResource = R.string.planned_flights_and_conflict_found
            message = this@PdfParserActivity.getString(R.string.planned_flights_and_conflict_found_long, viewModel.chronoImportResult?.conflicts ?: -1, viewModel.chronoImportResult?.plannedRemaining ?: -1)
            setPositiveButton(android.R.string.ok){
                closeAndStartMainActivity()
            }
        }
    }

    private fun showWholeLogbookInfoMessage(){
        JoozdlogAlertDialog().show(this){
            titleResource = R.string.importing_complete_logbook
            message = this@PdfParserActivity.getString(R.string.importing_complete_logbook_long)
            setPositiveButton(android.R.string.ok)
        }
    }




}



