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
import nl.joozd.logbookapp.databinding.ActivityLoginBinding
import nl.joozd.logbookapp.model.helpers.FeedbackEvents.LoginActivityEvents
import nl.joozd.logbookapp.model.viewmodels.activities.LoginActivityViewModel
import nl.joozd.logbookapp.ui.utils.toast


class LoginActivity : JoozdlogActivity(){
    val viewModel: LoginActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        val binding = ActivityLoginBinding.inflate(layoutInflater)

        /**
         * Every time this is recreated, check if server is online.
         * If not, viewModel will send a feedbackEvent for NO_INTERNET or SERVER_ERROR
         * In both cases, entering login/pass should still be possible, but it won't be checked
         * until next time it gets online.
         * TODO make a graphic feedback about this (ie. a message somewhere that server not reached)
         *
         * TODO MainActivity should get functionality to deal with server login errors,
         * TODO giving people the option to go to this screen and fix things.
         */

        viewModel.checkServerOnline()

        /*******************************************************************************************
         * OnClickedListeners
         *******************************************************************************************/
        with(binding) {
            singInButton.setOnClickListener {
                viewModel.signIn(usernameEditText.text.toString(), passwordEditText.text.toString())

            }
        }



        /*******************************************************************************************
         * Observers:
         *******************************************************************************************/



        /**
         * Feedback events:
         */
        viewModel.feedbackEvent.observe(this, Observer {
            when (it.getEvent()){
                LoginActivityEvents.NOT_IMPLEMENTED -> toast("Not implemented!")
                //TODO
            }
        })

        //

        setContentView(binding.root)
    }
}
