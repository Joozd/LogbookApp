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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.databinding.ActivityNewUserPage1Binding
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents
import nl.joozd.logbookapp.model.viewmodels.activities.NewUserActivityViewModel
import nl.joozd.logbookapp.ui.utils.toast

class NewUserActivityPage1: Fragment() {
    val viewModel: NewUserActivityViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        ActivityNewUserPage1Binding.bind(layoutInflater.inflate(R.layout.activity_new_user_page_1, container, false)).apply {
            emailAddressEditText.setText (Preferences.emailAddress)
            emailAddress2EditText.setText (Preferences.emailAddress)

            emailAddressEditText.onTextChanged {
                emailAddressLayout.error = ""
            }

            emailAddressEditText.setOnFocusChangeListener { _, hasFocus ->
                if(!hasFocus)
                    emailAddressEditText.text?.toString()?.let {
                        viewModel.updateEmail(it)
                    }
            }


            emailAddress2EditText.onTextChanged {
                emailAddress2Layout.error = ""
            }

            emailAddress2EditText.setOnFocusChangeListener { _, hasFocus ->
                if(!hasFocus)
                    emailAddress2EditText.text?.toString()?.let {
                        viewModel.updateEmail2(it)
                    }
            }

            viewModel.page1Feedback.observe(viewLifecycleOwner){
                Log.d("rreceived", "$it")
                when (it.getEvent()){
                    FeedbackEvents.GeneralEvents.DONE -> viewModel.nextPage(PAGE_NUMBER)
                    FeedbackEvents.GeneralEvents.ERROR -> {
                        toast("${it.getString()}")

                        when(it.getInt()){
                            1 -> emailAddressLayout.error = it.getString()
                            2 -> emailAddress2Layout.error = it.getString()
                            3 -> toast("ERROR 3: Bad match")
                            4 -> toast("ERROR 4: Not an email")
                        }
                    }
                }
            }


            continueTextView.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                it.requestFocus()
                viewModel.okClickedPage1()
            }




        }.root


    companion object{
        private const val PAGE_NUMBER = 1
    }
}