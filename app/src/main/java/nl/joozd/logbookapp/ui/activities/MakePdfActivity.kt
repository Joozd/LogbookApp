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

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import androidx.annotation.RequiresApi
import androidx.lifecycle.Observer
import nl.joozd.logbookapp.databinding.ActivityMakePdfBinding
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.MakePdfActivityEvents
import nl.joozd.logbookapp.model.viewmodels.activities.MakePdfActivityViewModel
import nl.joozd.logbookapp.ui.utils.toast

class MakePdfActivity : JoozdlogActivity() {
    private val activity = this

    private val viewModel = MakePdfActivityViewModel()

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == CREATE_FILE
            && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            resultData?.data?.also { uri ->
                viewModel.useUri(uri)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //start building logbook on (re-)creating so up-to-date data is always used
        viewModel.buildLogbook()

        ActivityMakePdfBinding.inflate(layoutInflater).apply{

            /***************************************************************************************
             * OnClicklisteners etc
             ***************************************************************************************/
            tempButton.setOnClickListener {
                createFile()
            }

            shareButton.setOnClickListener {
                sendShareIntent()
            }


            /***************************************************************************************
             * Observers
             ***************************************************************************************/

            viewModel.pdfLogbookReady.observe(activity, Observer {
                if (it) {
                    tempButton.visibility = View.VISIBLE
                    shareButton.visibility = View.INVISIBLE
                }
                else {
                    tempButton.visibility = View.GONE
                    shareButton.visibility = View.GONE
                }

            })


            viewModel.feedbackEvent.observe(activity, Observer {
                when (it.getEvent()){
                    MakePdfActivityEvents.FILE_CREATED -> {
                        shareButton.visibility = View.VISIBLE
                    }
                }
            })


            setContentView(root)
        }



    }

    /**
     * createFile starts a Create File dialog for result.
     * This version is for use on devices lower than API 26
     */
    private fun createFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, "invoice.pdf")
        }
        startActivityForResult(intent, CREATE_FILE)
    }

    private fun sendShareIntent(){
        viewModel.uriWithLogbook?.let{
            startActivity(Intent.createChooser(Intent().apply{
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, it)
                type = PDF_MIME_TYPE
            }, null))
        } ?: toast("URI is null")
    }

    companion object{
        const val CREATE_FILE = 1
        const val PDF_MIME_TYPE = "application/pdf"
    }
}