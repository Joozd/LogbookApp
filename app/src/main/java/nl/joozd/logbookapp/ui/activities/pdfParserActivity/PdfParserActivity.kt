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
import nl.joozd.logbookapp.ui.activities.JoozdlogActivity
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

            viewModel.feedbackEvent.observe(activity) {
                when (it.getEvent()) {
                    PdfParserActivityEvents.NOT_IMPLEMENTED -> {
                        toast("Not supported yet")
                        closeAndstartMainActivity()
                    }

                    PdfParserActivityEvents.IMPORTING_LOGBOOK -> {
                        pdfParserStatusTextView.text = getString(R.string.working_ellipsis)
                    }

                    PdfParserActivityEvents.ROSTER_SUCCESSFULLY_ADDED -> {
                        longToast("YAAAAY it worked  (Roster)")
                        closeAndstartMainActivity()
                    }
                    PdfParserActivityEvents.CHRONO_SUCCESSFULLY_ADDED -> {
                        longToast("YAAAAY it worked (Chrono)")
                        closeAndstartMainActivity()
                    }
                    PdfParserActivityEvents.CHRONO_CONFLICTS_FOUND -> {
                        showChronoConflictDialog(it.getInt())
                    }




                    PdfParserActivityEvents.ERROR, PdfParserActivityEvents.FILE_NOT_FOUND -> {
                        toast("Error reading file: ${it.getString()}")
                        closeAndstartMainActivity()
                    }
                    PdfParserActivityEvents.NOT_A_KNOWN_ROSTER, PdfParserActivityEvents.NOT_A_KNOWN_LOGBOOK -> {
                        JoozdlogAlertDialog().show(activity){
                            titleResource = R.string.unknown_file_title
                            messageResource = R.string.unknown_file_message
                            setPositiveButton(android.R.string.ok){
                                closeAndstartMainActivity()
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
                                closeAndstartMainActivity()
                            }
                        }
                    }
                    PdfParserActivityEvents.CALENDAR_SYNC_PAUSED -> {
                        if (!Preferences.alwaysPostponeCalendarSync)
                            JoozdlogAlertDialog().show(activity) {
                                messageResource = R.string.you_can_start_calendar_sync_again
                                setPositiveButton(android.R.string.ok) {
                                    closeAndstartMainActivity()
                                }
                            }
                        else closeAndstartMainActivity()
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

    private fun showChronoConflictDialog(conflicts: Int) {
        toast("showChronoConflictDialog: $conflicts")
        closeAndstartMainActivity()
    }
}



