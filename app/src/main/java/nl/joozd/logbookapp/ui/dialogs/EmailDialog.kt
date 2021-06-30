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
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment
import nl.joozd.logbookapp.ui.utils.toast

/**
 * Fragment that displays a dialog to enter email address
 * Initially will show currently entered email address and OK if that is verified or empty, or VERIFY of it is not verified.
 * @see EmailDialogViewModel.okButtonText
 */
class EmailDialog(): JoozdlogFragment() {
    /**
     * [extra] will be executed on successfully entering an email address
     * It wil be stored in ViewModel for persisting through recreations
     */
    constructor(extra: () -> Unit) : this() {
        onComplete = extra
    }
    private var onComplete: (() -> Unit)? = null
    private val viewModel: EmailDialogViewModel by viewModels()

    /**
     * OK button when enabled will get focus (so onFocusChangeListeners will trigger) and tell viewModel it has been clicked.
     * If not enabled, [okButtonListener] shall not be called
     */
    private val okButtonListener = View.OnClickListener{
        activity?.currentFocus?.clearFocus()
        it.requestFocus()
        viewModel.okClicked()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DialogEmailAddressBinding.bind(inflater.inflate(R.layout.dialog_email_address, container, false)).apply{

            //Set initial values from viewModel on (re)creation
            emailAddressEditText.setText(viewModel.email1)
            emailAddress2EditText.setText(viewModel.email2)

            //Whenever text changed, check if OK button should be enabled and update it's text if necessary
            emailAddressEditText.onTextChanged { text ->
                emailAddressLayout.error = ""
                enableOkButton(okButton, viewModel.checkSame1(text))
            }

            // When focus removed from field, update email1 in viewModel
            emailAddressEditText.setOnFocusChangeListener { _, hasFocus ->
                if(!hasFocus)
                    emailAddressEditText.text?.toString()?.let {
                        viewModel.updateEmail(it)
                    }
            }

            //Whenever text changed, check if OK button should be enabled and update it's text if necessary
            emailAddress2EditText.onTextChanged { text ->
                emailAddress2Layout.error = ""
                enableOkButton(okButton, viewModel.checkSame2(text))
            }

            // When focus removed from field, update email2 in viewModel
            emailAddress2EditText.setOnFocusChangeListener { _, hasFocus ->
                if(!hasFocus)
                    emailAddress2EditText.text?.toString()?.let {
                        viewModel.updateEmail2(it)
                    }
            }

            headerLayout.setOnClickListener{ } // do nothing, catch clicks
            bodyLayout.setOnClickListener{ } // do nothing, catch clicks

            // Clicking outside dialog == clicking cancel
            emailDialogBackground.setOnClickListener { closeFragment() }

            // Initially set OKbutton activated or not
            enableOkButton(okButton, viewModel.okButtonShouldBeEnabled())
            viewModel.updateOKButtonText()

            cancelButton.setOnClickListener { closeFragment() }

            /***************
             * Observers:
             **************/

            viewModel.okButtonText.observe(viewLifecycleOwner){
                okButton.text= getString(it)
            }

            viewModel.feedbackEvent.observe(viewLifecycleOwner){
                when (it.getEvent()){
                    GeneralEvents.DONE -> {
                        viewModel.onComplete()
                        if (viewModel.email1.isNotBlank())
                            JoozdlogAlertDialog().show(requireActivity()){
                                titleResource = R.string.email_verification_requested_short
                                messageResource = R.string.email_verification_requested_long
                                setPositiveButton(android.R.string.ok){
                                    closeFragment()
                                }
                            }
                        else JoozdlogAlertDialog().show(requireActivity()) {
                            titleResource = R.string.email_deleted
                            messageResource = R.string.email_deleted_long
                            setPositiveButton(android.R.string.ok) {
                                closeFragment()
                            }
                        }
                    }
                    GeneralEvents.OK -> {
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

    /**
     * Pass [onComplete] from secondary constructor to ViewModel so it will persist after recreation
     */
    override fun onAttach(context: Context) {
        onComplete?.let {viewModel.onComplete = it }
        super.onAttach(context)
    }

    /**
     * Only use for OK button
     */
    private fun enableOkButton(okButton: TextView, enabled: Boolean){
        if (!enabled){
            okButton.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                it.requestFocus()
            }
            okButton.setTextColor(requireActivity().getColorFromAttr(android.R.attr.textColorTertiary))
        }
        else{
            okButton.setOnClickListener(okButtonListener)
            okButton.setTextColor(requireActivity().getColorFromAttr(android.R.attr.colorAccent))
        }
    }


}