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

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.DialogEmailAddressBinding
import nl.joozd.logbookapp.extensions.getColorFromAttr
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.GeneralEvents
import nl.joozd.logbookapp.model.viewmodels.dialogs.EmailDialogViewModel
import nl.joozd.logbookapp.ui.fragments.JoozdlogFragment
import nl.joozd.logbookapp.ui.utils.toast

class EmailDialog(): JoozdlogFragment() {
    /**
     * [extra] will be executed on successfully entering an email address
     */
    constructor(extra: () -> Unit) : this() {
        onComplete = extra
    }
    private var onComplete: (() -> Unit)? = null
    private val viewModel: EmailDialogViewModel by viewModels()

    private val okButtonListener = View.OnClickListener{
        activity?.currentFocus?.clearFocus()
        it.requestFocus()
        viewModel.okClicked()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        DialogEmailAddressBinding.bind(inflater.inflate(R.layout.dialog_email_address, container, false)).apply{
            aboutDialogTopHalf.joozdLogSetBackgroundColor()

            emailAddressEditText.setText(viewModel.email1)
            emailAddress2EditText.setText(viewModel.email2)

            emailAddressEditText.onTextChanged { text ->
                emailAddressLayout.error = ""
                enableOkButton(okButton, viewModel.checkSame1(text))
            }

            emailAddressEditText.setOnFocusChangeListener { _, hasFocus ->
                if(!hasFocus)
                    emailAddressEditText.text?.toString()?.let {
                        viewModel.updateEmail(it)
                    }
            }


            emailAddress2EditText.onTextChanged { text ->
                emailAddress2Layout.error = ""
                enableOkButton(okButton, viewModel.checkSame2(text))
            }

            emailAddress2EditText.setOnFocusChangeListener { _, hasFocus ->
                if(!hasFocus)
                    emailAddress2EditText.text?.toString()?.let {
                        viewModel.updateEmail2(it)
                    }
            }

            cloudBackupDialogBox.setOnClickListener{ } // do nothing, catch clicks

            cloudBackupDialogBackground.setOnClickListener { closeFragment() }


            okButton.setOnClickListener(okButtonListener)

            cancelButton.setOnClickListener { closeFragment() }

            viewModel.feedbackEvent.observe(viewLifecycleOwner){
                when (it.getEvent()){
                    GeneralEvents.DONE -> {
                        viewModel.onComplete()
                        closeFragment()
                    }
                    GeneralEvents.ERROR -> {
                        when(it.getInt()){
                            1 -> emailAddressLayout.error = it.getString()
                            2 -> emailAddress2Layout.error = it.getString()
                            3 -> toast("ERROR 3: Bad match")
                            4 -> toast("ERROR 4: Not an email")
                            5 -> toast ("ERROR 5: emailDialog error 5") // TODO make more specific errors
                        }
                    }
                }
            }

        }.root

    override fun onAttach(context: Context) {
        onComplete?.let {viewModel.onComplete = it }
        super.onAttach(context)
    }

    /**
     * Only use for OK button
     */
    private fun enableOkButton(v: TextView, enabled: Boolean){
        Log.d("TextView.enable", "enabled: $enabled")
        if (!enabled){
            v.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                it.requestFocus()
            }
            v.setTextColor(requireActivity().getColorFromAttr(android.R.attr.textColorTertiary))
        }
        else{
            v.setOnClickListener(okButtonListener)
            v.setTextColor(requireActivity().getColorFromAttr(android.R.attr.colorAccent))
        }
    }


}