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

package nl.joozd.logbookapp.ui.dialogs.editFlightFragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.viewModels
import kotlinx.coroutines.flow.map
import nl.joozd.logbookapp.R

import nl.joozd.logbookapp.databinding.DialogTimesInOutBinding
import nl.joozd.logbookapp.extensions.showAsActiveIf
import nl.joozd.logbookapp.extensions.showFragment
import nl.joozd.logbookapp.extensions.textInputLayout
import nl.joozd.logbookapp.model.enumclasses.PicPicusFlag
import nl.joozd.logbookapp.model.helpers.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.model.viewmodels.dialogs.TimePickerViewModel
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment
import nl.joozd.logbookapp.ui.utils.setDualInstructorField
import nl.joozd.logbookapp.ui.utils.setPicPicusField

open class TimePicker: JoozdlogFragment() {
    private val viewModel: TimePickerViewModel by viewModels()

    // variable to store previous text on any selected field

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DialogTimesInOutBinding.bind(inflater.inflate(R.layout.dialog_times_in_out, container, false)).apply{
            initializeEditTexts()
            initializeLSKs()
            initializeToggleButtons()
            setUIOnClickListeners()
            hideKeyboardWhenPressingEnterInLastField()
            catchStrayClicks()
        }.root

    private fun DialogTimesInOutBinding.initializeEditTexts(){
        initializeTotalTimeOfFlightTextview()
        initializeNightTimeTextview()
        initializeIfrTimeTextview()
        initializeRestimeTextview()
    }

    private fun DialogTimesInOutBinding.initializeLSKs(){
        initializeRestTimeLSK()
    }

    private fun DialogTimesInOutBinding.initializeToggleButtons(){
        initializeAugmented()
        initializePicusPic()
        initializeCopilot()
        initializeDualInstructor()
    }

    private fun DialogTimesInOutBinding.initializeTotalTimeOfFlightTextview(){
        totalTimeOfFlightTextview.apply {
            val flow = viewModel.totalTimeFlow().map { it.minutesToHoursAndMinutesString() }
            bindToFlowAndInputHandler(flow){
                if (it.isNotBlank()) viewModel.setTotalTimeOfFlight(it) // I assume nobody wants to a log a flight with 0 times. If so, they can type "0"
            }
        }
    }

    private fun DialogTimesInOutBinding.initializeNightTimeTextview(){
        nightTimeTextview.apply {
            val flow = viewModel.nightTimeFlow().map { it.minutesToHoursAndMinutesString() }
            bindToFlowAndInputHandler(flow){
                viewModel.setNightTime(it)
            }
        }
    }

    private fun DialogTimesInOutBinding.initializeIfrTimeTextview(){
        ifrTimeTextview.apply {
            val flow = viewModel.ifrTimeFlow().map { it.minutesToHoursAndMinutesString() }
            bindToFlowAndInputHandler(flow){
                viewModel.setIfrTime(it)
            }
        }
    }

    private fun DialogTimesInOutBinding.initializeRestimeTextview(){
        restTimeTextview.apply {
            val flow = viewModel.restTimeIfNotPIC().map { it.minutesToHoursAndMinutesString() }
            bindToFlowAndInputHandler(flow){
                viewModel.setRestTime(it)
            }

            viewModel.picPicusFlow().launchCollectWhileLifecycleStateStarted{
                // PIC always logs entire flight and cannot log rest time.
                val enabled = it != PicPicusFlag.PIC
                textInputLayout?.isEnabled = enabled
            }
        }
    }

    private fun DialogTimesInOutBinding.initializeRestTimeLSK(){
        restTimeSelector.apply{
            setOnClickListener {
                requireActivity().showFragment<AugmentedCrewDialog>()
            }
        }
    }

    private fun DialogTimesInOutBinding.initializeAugmented(){
        augmentedTextView.apply {
            viewModel.augmentedCrewFlow().launchCollectWhileLifecycleStateStarted {
                showAsActiveIf(it.isAugmented())
            }

            /**
             * OnCLick will disable augmented if enabled, re-enable it if it was disabled in this way, and opens dialog if neither.
             */
            setOnClickListener {
                clearFocus() // to get focus from any textbox if that was active. For some reason this doesn't work.
                if(!viewModel.toggleAugmented())
                    requireActivity().showFragment<AugmentedCrewDialog>()
            }
        }
    }

    private fun DialogTimesInOutBinding.initializePicusPic() {
        timesDialogPicPicusTextview.apply {
            viewModel.picPicusFlow().launchCollectWhileLifecycleStateStarted {
                setPicPicusField(it)
            }

            setOnClickListener {
                viewModel.togglePicusPicNone()
            }
        }

    }


    private fun DialogTimesInOutBinding.initializeCopilot(){
        coPilotTextView.apply {
            viewModel.copilotFlow().launchCollectWhileLifecycleStateStarted {
                showAsActiveIf(it)
            }

            setOnClickListener {
                viewModel.toggleCopilot()
            }
        }
    }

    private fun DialogTimesInOutBinding.initializeDualInstructor(){
        timesDialogDualInstructorTextview.apply {
            viewModel.dualInstructorFlow().launchCollectWhileLifecycleStateStarted {
                setDualInstructorField(it)
            }

            setOnClickListener {
                viewModel.toggleDualInstructorNone()
            }
        }
    }

    private fun DialogTimesInOutBinding.setUIOnClickListeners(){
        timesDialogSaveTextview.setOnClickListener {
            closeFragment()
        }

        timesDialogCancelTextview.setOnClickListener {
            viewModel.undo()
            closeFragment()
        }
    }

    private fun DialogTimesInOutBinding.hideKeyboardWhenPressingEnterInLastField() {
        ifrTimeTextview.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm: InputMethodManager =
                    v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                v.clearFocus()
            }
            true
        }
    }

    private fun DialogTimesInOutBinding.catchStrayClicks(){
        timePickerDialogBackground.setOnClickListener {
            //do nothing
        }
    }
}
