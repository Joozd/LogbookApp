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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.commit
import androidx.lifecycle.asLiveData
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.data.sharedPrefs.TaskPayloads
import nl.joozd.logbookapp.databinding.ActivityFeedbackBinding
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.GeneralEvents
import nl.joozd.logbookapp.model.viewmodels.FeedbackActivityViewModel
import nl.joozd.logbookapp.ui.dialogs.TextDisplayDialog
import nl.joozd.logbookapp.ui.dialogs.JoozdlogAlertDialog
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity
import nl.joozd.logbookapp.ui.utils.toast

class FeedbackActivity : JoozdlogActivity() {
    private val viewModel: FeedbackActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        with (ActivityFeedbackBinding.inflate(layoutInflater)){
            setSupportActionBarWithReturn(feedbackToolbar)?.apply {
                setDisplayHomeAsUpEnabled(true)
                title = getString(R.string.feedback)
            }

            collectFlows()
            setOnTextChangedListeners()
            setOnClickListeners()

            setContentView(root)
        }
    }

    private fun ActivityFeedbackBinding.setOnClickListeners() {
        knownIssuesButton.setOnClickListener {
            viewModel.loadKnownIssuesLiveData(R.raw.joozdlog_todo_list)
            supportFragmentManager.commit {
                add(R.id.layoutBelowToolbar, TextDisplayDialog(R.string.joozdlog_todo_title, viewModel.knownIssuesFlow.asLiveData()), null)
                addToBackStack(null)
            }
        }

        youCanAlsoSendAnEmailTextview.setOnClickListener {
            launchSendEmailIntent()
        }

        submitButton.setOnClickListener {
            confirmSubmitFeedback(feedbackEditText.text.toString())
        }
    }

    private fun launchSendEmailIntent() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            setDataAndType(Uri.parse("mailto:"), "*/*")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("joozdlog@joozd.nl"))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback))
        }
        intent.resolveActivity(packageManager)?.let{
            startActivity(intent)
        }?: toast("No email app found")

    }

    private fun ActivityFeedbackBinding.collectFlows() {
        TaskPayloads.feedbackWaiting.flow.launchCollectWhileLifecycleStateStarted { feedbackEditText.setText(it) }
        TaskPayloads.feedbackContactInfoWaiting.flow.launchCollectWhileLifecycleStateStarted { contactEditText.setText(it) }

        viewModel.finishedFlow.launchCollectWhileLifecycleStateStarted{
            if (it)
                showFeedbackWillBeSubmittedDialog()
        }
    }

    private fun ActivityFeedbackBinding.setOnTextChangedListeners() {
        feedbackEditText.onTextChanged {
            submitButton.isEnabled = it.isNotBlank()
            viewModel.enteredFeedback = it
        }

        contactEditText.onTextChanged {
            viewModel.enteredContactInfo = it
        }
    }

    private fun showFeedbackWillBeSubmittedDialog() = AlertDialog.Builder(this).apply{
        setTitle(R.string.submit)
        setMessage(R.string.your_feedback_will_be_submitted)
        setPositiveButton(android.R.string.ok){ _, _ -> finish() }
    }

    /**
     * Show dialog asking if this is indeed the feedback they want to submit
     */
    private fun confirmSubmitFeedback(feedback: String) = AlertDialog.Builder(this).apply{
        setTitle(R.string.submit_feedback_qmk)
        setMessage(feedback)
        setPositiveButton(android.R.string.ok){ _, _ ->
            viewModel.submitClicked()
        }
        setNegativeButton(android.R.string.cancel) { _, _ -> }
    }.create().show()

}