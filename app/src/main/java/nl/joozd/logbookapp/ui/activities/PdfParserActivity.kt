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

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_pdf_parser.*
import kotlinx.coroutines.*
import nl.joozd.logbookapp.R


import nl.joozd.logbookapp.model.helpers.FeedbackEvents.PdfParserActivityEvents
import nl.joozd.logbookapp.model.viewmodels.activities.PdfParserActivityViewModel
import nl.joozd.logbookapp.ui.utils.toast
import nl.joozd.logbookapp.utils.startMainActivity

class PdfParserActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    private val viewModel: PdfParserActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("PdfParserActivity", "started")
        setContentView(R.layout.activity_pdf_parser)

        viewModel.runOnce(intent)

        /**
         * Observers:
         */
        viewModel.progress.observe(this, Observer {
            pdfParserProgressBar.progress  = it
        })

        viewModel.progressTextResource.observe(this, Observer {
            pdfParserStatusTextView.text = getString(it)
        })

        viewModel.feedbackEvent.observe(this, Observer {
            when (it.getEvent()){
                PdfParserActivityEvents.ROSTER_SUCCESSFULLY_ADDED -> {
                    startMainActivity(this)
                    finish()
                }
                PdfParserActivityEvents.ERROR, PdfParserActivityEvents.FILE_NOT_FOUND -> {
                    toast("Error reading file")
                    startMainActivity(this)
                    finish()
                }
                PdfParserActivityEvents.NOT_A_KNOWN_ROSTER -> {
                    toast("Unsupported file")
                    startMainActivity(this)
                    finish()
                }
                null -> {}
                else -> toast("SOMETHING NOT IMPLEMENTED HAPPENED")

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



