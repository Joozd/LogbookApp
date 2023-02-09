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

package nl.joozd.logbookapp.ui.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.ActivityMakePdfBinding
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.MakePdfActivityEvents
import nl.joozd.logbookapp.model.viewmodels.activities.makePdfActivity.MakePdfActivityViewModel
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity
import nl.joozd.logbookapp.ui.utils.toast

class MakePdfActivity : JoozdlogActivity() {
    private val viewModel = MakePdfActivityViewModel()

    private val createFileResultLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("MakePdfActivity", "RESULT_OK received")
            // There are no request codes
            // The result data contains a URI for the document or directory that
            // the user selected.
            result.data?.data?.also { uri ->
                viewModel.targetUri = uri
                lifecycleScope.launch{
                    viewModel.saveLogbook(uri)
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ActivityMakePdfBinding.inflate(layoutInflater).apply{
            setSupportActionBarWithReturn(pdfMakeActivityToolbar)?.apply {
                setDisplayHomeAsUpEnabled(true)
                title = resources.getString(R.string.savePdf)
            }

            /***************************************************************************************
             * OnClicklisteners etc
             ***************************************************************************************/
            saveToPdfButton.setOnClickListener {
                saveToPdfButton.isEnabled = false
                createFile()
            }

            shareButton.setOnClickListener {
                sendShareIntent()
            }

            writingProgressBar.max = PROGRESS_BAR_RESOLUTION

            startCollectors()
            setContentView(root)
        }
    }

    private fun ActivityMakePdfBinding.startCollectors(){
        viewModel.progressFlow.launchCollectWhileLifecycleStateStarted{
            writingProgressBar.progress = (it * PROGRESS_BAR_RESOLUTION).toInt()
        }
        collectStatus()
    }

    private fun ActivityMakePdfBinding.collectStatus(){
        viewModel.statusFlow.launchCollectWhileLifecycleStateStarted{
            when(it){
                MakePdfActivityEvents.BUILDING_LOGBOOK -> buildingLogbook()
                MakePdfActivityEvents.LOGBOOK_READY -> logbookReady()
                MakePdfActivityEvents.WRITING -> savingFile()
                MakePdfActivityEvents.FILE_CREATED -> fileCreated()
            }
        }
    }

    /**
     * createFile starts a Create File dialog for result.
     */
    private fun createFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, "logbook.pdf")
        }
        createFileResultLauncher.launch(intent)
    }

    private fun ActivityMakePdfBinding.buildingLogbook(){
        saveToPdfButton.isEnabled = false
        writingProgressBar.visibility = View.VISIBLE
        writingTextView.setText(R.string.building_pdf)
        shareButton.visibility = View.GONE
    }

    private fun ActivityMakePdfBinding.logbookReady(){
        saveToPdfButton.isEnabled = true
        writingTextView.setText(R.string.ready_to_save_pdf)
    }

    private fun ActivityMakePdfBinding.savingFile(){
        writingProgressBar.visibility = View.INVISIBLE
        writingProgressBarCircle.visibility = View.VISIBLE
        writingTextView.setText(R.string.saving_pdf)
    }

    private fun ActivityMakePdfBinding.fileCreated(){
        writingTextView.setText(R.string.pdf_saved)
        writingProgressBarCircle.visibility = View.INVISIBLE
        shareButton.visibility = View.VISIBLE
    }

    private fun sendShareIntent(){
        lifecycleScope.launch {
            viewModel.targetUri?.let {
                startActivity(Intent.createChooser(Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, it)
                    type = PDF_MIME_TYPE
                }, null))
            } ?: toast("URI is null")
        }
    }

    companion object{
        const val PDF_MIME_TYPE = "application/pdf"

        private const val PROGRESS_BAR_RESOLUTION = Short.MAX_VALUE.toInt()
    }
}