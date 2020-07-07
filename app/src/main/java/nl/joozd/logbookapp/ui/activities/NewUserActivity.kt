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
import androidx.lifecycle.Observer
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.ActivityNewUserBinding
import nl.joozd.logbookapp.model.helpers.FeedbackEvents.NewUserActivityEvents
import nl.joozd.logbookapp.model.viewmodels.activities.NewUserActivityViewModel
import nl.joozd.logbookapp.ui.utils.toast

class NewUserActivity : JoozdlogActivity() {
    val viewModel: NewUserActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        val binding = ActivityNewUserBinding.inflate(layoutInflater)

        /*******************************************************************************************
         * OnClickedListeners
         *******************************************************************************************/
        with(binding) {
            signInTextView.setOnClickListener {
                viewModel.signInClicked()
            }

            signUpButton.setOnClickListener {
                viewModel.signUpClicked(userNameEditText.text.toString(), passwordEditText.text.toString(), repeatPasswordEditText.text.toString())
            }
        }



        /*******************************************************************************************
         * Observers:
         *******************************************************************************************/

        /**
         * Event observers:
         */

        viewModel.feedbackEvent.observe(this, Observer {
            when(it.getEvent()){
                NewUserActivityEvents.NOT_IMPLEMENTED -> { toast("Not implemented!")}
                NewUserActivityEvents.SHOW_SIGN_IN_DIALOG -> {}
                NewUserActivityEvents.USER_EXISTS_PASSWORD_INCORRECT -> {}
                NewUserActivityEvents.USER_EXISTS_PASSWORD_CORRECT -> {}
                NewUserActivityEvents.PASSWORDS_DO_NOT_MATCH -> {}
                NewUserActivityEvents.PASSWORD_DOES_NOT_MEET_STANDARDS -> {}
                NewUserActivityEvents.PASSWORD_TOO_SHORT -> {}
                NewUserActivityEvents.NO_INTERNET -> {}
                NewUserActivityEvents.FINISHED -> { closeAndstartMainActivity() }
            }
        })

        setContentView(binding.root)

    }
}