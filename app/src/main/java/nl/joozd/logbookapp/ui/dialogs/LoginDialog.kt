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

package nl.joozd.logbookapp.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.DialogLoginBinding
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents
import nl.joozd.logbookapp.model.viewmodels.activities.LoginActivityViewModel
import nl.joozd.logbookapp.ui.fragments.JoozdlogFragment
import nl.joozd.logbookapp.ui.utils.customs.JoozdlogAlertDialog
import nl.joozd.logbookapp.ui.utils.toast

//TODO probably replace this with JoozdlogAlertDialog?
class LoginDialog: JoozdlogFragment() {
    //reuse viewModel from LoginActivity as that already has all the logic we need here
    private val viewModel: LoginActivityViewModel by viewModels()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        DialogLoginBinding.bind(inflater.inflate(R.layout.dialog_login, container, false)).apply {
            topHalf.joozdLogSetBackgroundColor()


            /*******************************************************************************************
             * Buttons & Backgrounds
             *******************************************************************************************/

            signOutButton.setOnClickListener {
                viewModel.signOut()
            }

            background.setOnClickListener {} // do nothing, just catch missed clicks


            /*************************************************************************************
             * Observers
             *************************************************************************************/

            viewModel.feedbackEvent.observe(viewLifecycleOwner) {
                when (it.getEvent()) {
                    FeedbackEvents.LoginActivityEvents.FINISHED -> closeFragment()
                    else -> toast("unhandled feedback: ${it.type}")
                }
            }
        }.root
}