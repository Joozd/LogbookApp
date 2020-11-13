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
import nl.joozd.logbookapp.databinding.DialogCloudBackupBinding
import nl.joozd.logbookapp.extensions.onFocusChange
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.GeneralEvents
import nl.joozd.logbookapp.model.viewmodels.dialogs.CloudBackupDialogViewModel
import nl.joozd.logbookapp.ui.fragments.JoozdlogFragment

class CloudBackupDialog: JoozdlogFragment() {
    private val viewModel: CloudBackupDialogViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        DialogCloudBackupBinding.bind(inflater.inflate(R.layout.dialog_cloud_backup, container, false)).apply{
            aboutDialogTopHalf.joozdLogSetBackgroundColor()

            emailAddressEditText.setText(viewModel.emailAddress)

            emailAddressEditText.setOnFocusChangeListener { _, hasFocus ->
                if(!hasFocus)
                    emailAddressEditText.text?.toString()?.let {
                        viewModel.updateEmail(it)
                    }
            }

            cloudBackupDialogBox.setOnClickListener{ } // do nothing, catch clicks

            cloudBackupDialogBackground.setOnClickListener { closeFragment() }


            okButton.setOnClickListener(){
                activity?.currentFocus?.clearFocus()
                it.requestFocus()
                viewModel.okClicked()
            }

            cancelButton.setOnClickListener { closeFragment() }

            viewModel.feedbackEvent.observe(viewLifecycleOwner){
                when (it.getEvent()){
                    GeneralEvents.DONE -> closeFragment()
                    GeneralEvents.ERROR -> {
                        emailAddressLayout.error = it.getString()
                    }
                }
            }

        }.root


}