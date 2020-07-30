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

package nl.joozd.logbookapp.ui.activities

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_pdf_parser.*
import kotlinx.coroutines.*
import nl.joozd.logbookapp.R


import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.PdfParserActivityEvents
import nl.joozd.logbookapp.model.viewmodels.activities.pdfParserActivity.PdfParserActivityViewModel
import nl.joozd.logbookapp.ui.utils.customs.JoozdlogAlertDialog
import nl.joozd.logbookapp.ui.utils.toast

/**
 * PdfParserActivity does the following:
 * - Get an intent with a PDF file
 * - read that PDF file
 * - Decide what type it is (KLC Roster, Lufthansa Monthly Overview, etc)
 * - parse that type
 * - do whatever needs to be done with parsed data (insert roster from planned, check flights from monthlies etc)
 * - launch MainActivity
 */
class PdfParserActivity : JoozdlogActivity(), CoroutineScope by MainScope() {
    private val viewModel: PdfParserActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("PdfParserActivity", "started")
        setContentView(R.layout.activity_pdf_parser)

        viewModel.runOnce(intent)

        /****************************************************************************************
         * Observers:
         ****************************************************************************************/
        viewModel.progress.observe(this, Observer {
            pdfParserProgressBar.progress  = it
        })

        viewModel.progressTextResource.observe(this, Observer {
            pdfParserStatusTextView.text = getString(it)
        })

        viewModel.feedbackEvent.observe(this, Observer {
            when (it.getEvent()){
                PdfParserActivityEvents.NOT_IMPLEMENTED -> {
                    toast ("Not supported yet")
                    closeAndstartMainActivity()
                }

                PdfParserActivityEvents.ROSTER_SUCCESSFULLY_ADDED -> {
                    closeAndstartMainActivity()
                }
                PdfParserActivityEvents.ERROR, PdfParserActivityEvents.FILE_NOT_FOUND -> {
                    toast("Error reading file")
                    closeAndstartMainActivity()
                }
                PdfParserActivityEvents.NOT_A_KNOWN_ROSTER -> {
                    toast("Unsupported file")
                    closeAndstartMainActivity()
                }
                PdfParserActivityEvents.CALENDAR_SYNC_ENABLED -> {
                    JoozdlogAlertDialog(this).apply{
                        messageResource = R.string.calendar_update_active
                        setPositiveButton(android.R.string.yes){
                            viewModel.disableCalendarImport()
                            viewModel.saveFlights()
                        }
                        setNegativeButton(R.string.calendar_update_pause_untill_end_of_roster){
                            viewModel.disableCalendarUntilAfterLastFlight()
                            viewModel.saveFlights(finish = false)
                        }
                        setNeutralButton(android.R.string.cancel){
                            startMainActivity(this@PdfParserActivity)
                        }
                    }.show()
                }
                PdfParserActivityEvents.CALENDAR_SYNC_PAUSED -> {
                    JoozdlogAlertDialog(this).apply{
                        messageResource = R.string.you_can_start_calendar_sync_again
                        setPositiveButton(android.R.string.ok){
                            closeAndstartMainActivity()
                        }
                    }.show()
                }
                null -> {}
                else -> {
                    toast("SOMETHING NOT IMPLEMENTED HAPPENED")
                    startMainActivity(this)
                    finish()
                }

            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    companion object {
        const val TAG = "PdfParserActivity"
    }
}



