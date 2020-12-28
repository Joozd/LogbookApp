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
import androidx.activity.viewModels
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.ActivityFeedbackBinding
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.GeneralEvents
import nl.joozd.logbookapp.model.viewmodels.FeedbackActivityViewModel
import nl.joozd.logbookapp.ui.utils.customs.JoozdlogAlertDialog
import nl.joozd.logbookapp.ui.utils.toast

class FeedbackActivity : JoozdlogActivity() {
    private val viewModel: FeedbackActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        with (ActivityFeedbackBinding.inflate(layoutInflater)){
            setSupportActionBarWithReturn(feedbackToolbar)?.apply {
                setDisplayHomeAsUpEnabled(true)
                title = "PLACEHOLDER_TITLE"
            }

            //reset EditTexts to anything already entered there on recreate
            feedbackEditText.setText(viewModel.feedbackText)
            contactEditText.setText(viewModel.contactInfo)


            //update typed text in viewModel during typing
            feedbackEditText.onTextChanged {
                viewModel.updateFeedbackText(it)
            }

            contactEditText.onTextChanged {
                viewModel.updateContactText(it)
            }

            submitButton.setOnClickListener {
                if (viewModel.feedbackText.isNotBlank())
                    confirmSubmitFeedback()
                else emptyFeedbackDialog()
            }


            viewModel.feedbackEvent.observe(activity){
                when (it.getEvent()){
                    GeneralEvents.DONE -> finish()
                    GeneralEvents.ERROR -> {
                        when (it.getInt()){
                            FeedbackActivityViewModel.NO_INTERNET -> noInternetDialog()
                            FeedbackActivityViewModel.CONNECTION_ERROR -> noServerdialog()
                            FeedbackActivityViewModel.EMPTY_FEEDBACK -> emptyFeedbackDialog()
                            else -> toast("UNHANDLED ERROR aub")
                        }
                    }
                }
            }

            setContentView(root)
        }
    }

    /**
     * Show dialog about not having an internet connection
     */
    private fun noInternetDialog() = JoozdlogAlertDialog().show(activity){
        titleResource = R.string.no_internet
        messageResource = R.string.need_internet
        setPositiveButton(android.R.string.ok) { finish() }
    }

    /**
     * Show dialog about server connection problems
     */
    private fun noServerdialog() = JoozdlogAlertDialog().show(activity){
        titleResource = R.string.no_internet
        messageResource = R.string.server_problem_message
        setPositiveButton(android.R.string.ok) { finish() }
    }

    /**
     * Show dialog about feedback not allowed to be empty
     */
    private fun emptyFeedbackDialog() = JoozdlogAlertDialog().show(activity){
        titleResource = R.string.empty_feedback
        messageResource = R.string.empty_feedback_message
        setPositiveButton(android.R.string.ok)
    }

    /**
     * Show dialog asking if this is indeed the feedback they want to submit
     */
    private fun confirmSubmitFeedback() = JoozdlogAlertDialog().show(activity){
        titleResource = R.string.submit_feedback_qmk
        message = viewModel.feedbackText
        setPositiveButton(android.R.string.ok){
            viewModel.submitClicked()
        }
        setNegativeButton(android.R.string.cancel)
    }

}