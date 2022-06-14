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

package nl.joozd.logbookapp.ui.dialogs

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.DialogEmailAddressBinding
import nl.joozd.logbookapp.extensions.getColorFromAttr
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.extensions.setTextIfNotFocused
import nl.joozd.logbookapp.model.viewmodels.dialogs.EmailDialogViewModel
import nl.joozd.logbookapp.model.viewmodels.status.EmailDialogStatus
import nl.joozd.logbookapp.model.viewmodels.status.EmailDialogStatus.EmailDialogStatusError.*
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment

/*
 * Fragment that displays a dialog to enter email address
 * Initially will show currently entered email address and OK if that is verified or empty, or VERIFY of it is not verified.
 */
class EmailDialog(): JoozdlogFragment() {
    /*
     * [extra] will be executed on successfully entering an email address
     * It wil be stored in ViewModel for persisting through recreations
     */
    constructor(extra: () -> Unit) : this() {
        onComplete = extra
    }
    private var onComplete: (() -> Unit)? = null
    private val viewModel: EmailDialogViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DialogEmailAddressBinding.bind(inflater.inflate(R.layout.dialog_email_address, container, false)).apply{
            launchFLowCollectors()
            setTextChangedListeners()
            setOnClickListeners()
            launchStatusCollector()

            onComplete?.let {viewModel.onComplete = it }
        }.root

    private fun DialogEmailAddressBinding.setOnClickListeners() {
        headerLayout.setOnClickListener { } // do nothing, catch clicks
        bodyLayout.setOnClickListener { } // do nothing, catch clicks

        // Clicking outside dialog == clicking cancel
        emailDialogBackground.setOnClickListener { closeFragment() }
        cancelButton.setOnClickListener { closeFragment() }
    }

    private fun DialogEmailAddressBinding.setTextChangedListeners() {
        //Whenever text changed, check if OK button should be enabled and update it's text if necessary
        emailAddressEditText.onTextChanged { text ->
            emailAddressLayout.error = ""
            viewModel.updateEmail1(text)
        }

        emailAddressEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus)
                viewModel.completedEmail1()
        }

        emailAddress2EditText.onTextChanged { text ->
            emailAddress2Layout.error = ""
            viewModel.updateEmail2(text)
        }

        emailAddress2EditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus)
                viewModel.completedEmail2()
        }
    }

    private fun DialogEmailAddressBinding.launchFLowCollectors(){
        viewModel.email1Flow.launchCollectWhileLifecycleStateStarted{
            emailAddressEditText.setTextIfNotFocused(it)
        }

        viewModel.email2Flow.launchCollectWhileLifecycleStateStarted{
            emailAddress2EditText.setTextIfNotFocused(it)
        }

        viewModel.canBeAcceptedFlow.launchCollectWhileLifecycleStateStarted{
            enableOkButton(it)
        }

        viewModel.okOrVerifyFlow.launchCollectWhileLifecycleStateStarted{
            okButton.setText(it)
        }
    }

    private fun DialogEmailAddressBinding.launchStatusCollector(){
        viewModel.statusFlow.launchCollectWhileLifecycleStateStarted{
            when(it){
                null -> { }
                EmailDialogStatus.Done -> {
                    showResultDialog()
                    viewModel.onComplete()
                }
                EmailDialogStatus.DoneNoChanges -> {
                    viewModel.onComplete()
                    closeFragment()
                }
                is EmailDialogStatus.Error -> handleError(it)
            }
            if (it != null)
                viewModel.resetStatus()
        }
    }

    private fun DialogEmailAddressBinding.handleError(error: EmailDialogStatus.Error){
        when (error.reason){
            EMAILS_DO_NOT_MATCH -> showCriticalErrorDialog(R.string.addresses_do_not_match)
            INVALID_EMAIL_ADDRESS -> showCriticalErrorDialog(R.string.not_an_email_address)
            INVALID_EMAIL_ADDRESS_1 -> emailAddressLayout.error = getString(R.string.not_an_email_address)
            INVALID_EMAIL_ADDRESS_2 -> emailAddress2Layout.error = getString(R.string.not_an_email_address)
            ENTRIES_DO_NOT_MATCH -> emailAddress2Layout.error = getString(R.string.addresses_do_not_match)
        }
    }

    private fun showCriticalErrorDialog(errorResource: Int) =
        AlertDialog.Builder(requireActivity()).apply{
            setTitle(R.string.error)
            setMessage(errorResource)
            setPositiveButton(android.R.string.ok){ _, _ ->
                closeFragment()
            }
        }

    private fun DialogEmailAddressBinding.enableOkButton(enabled: Boolean) = with (okButton){
        if (!enabled){
            setOnClickListener{
                // get focus so errors get produced if currently active field has one and user can see what they are doing wrong)
                activity?.currentFocus?.clearFocus()
                requestFocus()
            }
            setTextColor(requireActivity().getColorFromAttr(android.R.attr.textColorTertiary))
        }
        else{
            setOnClickListener{
                viewModel.okClicked()
            }
            setTextColor(requireActivity().getColorFromAttr(android.R.attr.colorAccent))
        }
    }

    private fun showResultDialog() {
        if (viewModel.email1.isNotBlank())
            AlertDialog.Builder(requireActivity()).apply {
                setTitle(R.string.verification_mail)
                setMessage(R.string.email_verification_requested_long)
                setPositiveButton(android.R.string.ok) { _, _ ->
                    closeFragment()
                }
            }.create().show()
        else
            AlertDialog.Builder(requireActivity()).apply {
                setTitle(R.string.email_deleted)
                setMessage(R.string.email_deleted_long)
                setPositiveButton(android.R.string.ok) { _, _ ->
                    closeFragment()
                }
            }.create().show()
    }
}