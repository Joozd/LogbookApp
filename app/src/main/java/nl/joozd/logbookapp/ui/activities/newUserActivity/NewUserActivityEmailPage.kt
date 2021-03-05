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

package nl.joozd.logbookapp.ui.activities.newUserActivity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.ActivityNewUserPageEmailBinding
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.NewUserActivityEvents
import nl.joozd.logbookapp.model.viewmodels.activities.NewUserActivityViewModel
import nl.joozd.logbookapp.ui.utils.toast

// TODO disable CONTINUE button untill two valid email adresses have been entered. Improve logic behind that icm viewModel
class NewUserActivityEmailPage: Fragment() {
    val viewModel: NewUserActivityViewModel by activityViewModels()

    val pageNumber = NewUserActivityViewModel.PAGE_EMAIL

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ActivityNewUserPageEmailBinding.bind(layoutInflater.inflate(R.layout.activity_new_user_page_email, container, false)).apply {
            emailAddressEditText.setText (viewModel.email1)
            emailAddress2EditText.setText (viewModel.email2)

            emailAddressEditText.onTextChanged {
                emailAddressLayout.error = ""
                viewModel.page1InputChanged(it, emailAddress2EditText.text.toString())
            }

            emailAddress2EditText.onTextChanged {
                emailAddress2Layout.error = ""
                viewModel.page1InputChanged(emailAddressEditText.text.toString(), it)
            }

            emailAddressEditText.setOnFocusChangeListener { view, hasFocus ->
                if (!hasFocus) viewModel.checkEmail1()
            }

            /**
             * If emails match and are OK, enable CONTINUE button, else set CONTINUE button to show error
             */
            viewModel.emailsMatch.observe(viewLifecycleOwner){
                viewModel.makeContinueActive(pageNumber, it)
            }



            /**
             * Observe feedback from viewModel
             */
            viewModel.getFeedbackChannel(pageNumber).observe(viewLifecycleOwner){
                when (it.getEvent()){
                    NewUserActivityEvents.BAD_EMAIL -> emailAddressLayout.error = getString(R.string.not_an_email_address)
                    NewUserActivityEvents.EMAILS_DO_NOT_MATCH -> emailAddress2Layout.error = getString(R.string.does_not_match)

                    FeedbackEvents.GeneralEvents.ERROR -> {
                        toast("${it.getString()}")
                        when(it.getInt()){
                            1 -> emailAddressLayout.error = it.getString()
                            2 -> emailAddress2Layout.error = it.getString()
                            3 -> toast("ERROR 3: Bad match")
                            4 -> toast("ERROR 4: Not an email")
                        }
                    }

                    NewUserActivityEvents.CLEAR_PAGE -> {
                        emailAddressEditText.setText (viewModel.email1)
                        emailAddress2EditText.setText (viewModel.email2)
                    }
                }
            }

        }.root
    // end of onCreateView
}
