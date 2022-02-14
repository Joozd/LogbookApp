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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R

import nl.joozd.logbookapp.databinding.DialogTimesInOutBinding
import nl.joozd.logbookapp.extensions.ctx
import nl.joozd.logbookapp.extensions.nullIfBlank
import nl.joozd.logbookapp.extensions.showAsActiveIf
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
            launchFlowCollectors()
            setOnClickListeners()
            setOnFocusChangedListeners()
            hideKeyboardWhenPressingEnterInLastField()
            catchStrayClicks()
        }.root

    private fun DialogTimesInOutBinding.launchFlowCollectors(){
        collectTotalTimeFlowToTotalTimeView()
        collectNightTimeFlowToNightTimeView()
        collectIfrTimeFlowToIfrTimeView()
        collectAugmentedCrewFlowToToggle()
        collectPicPicusFlowToToggle()
        collectCopilotFlowToToggle()
        collectDualInstructorFlowToToggle()
        launch {
            viewModel.debugF.collect {
                println("collected $it")
            }
        }

    }

    private fun DialogTimesInOutBinding.collectTotalTimeFlowToTotalTimeView() {
        viewModel.totalTimeFlow().launchCollectWhileLifecycleStateStarted {
            totalTimeOfFlightTextview.setText(it.minutesToHoursAndMinutesString())
        }
    }
    private fun DialogTimesInOutBinding.collectNightTimeFlowToNightTimeView() {
        viewModel.nightTimeFlow().launchCollectWhileLifecycleStateStarted {
            nightTimeTextview.setText(it.minutesToHoursAndMinutesString())
        }
    }
    private fun DialogTimesInOutBinding.collectIfrTimeFlowToIfrTimeView() {
        viewModel.ifrTimeFlow().launchCollectWhileLifecycleStateStarted {
            println("collected $it for IFR Time")
            ifrTimeTextview.setText(it.minutesToHoursAndMinutesString())
        }
    }

    private fun DialogTimesInOutBinding.collectAugmentedCrewFlowToToggle(){
        viewModel.augmentedCrewFlow().launchCollectWhileLifecycleStateStarted{
            augmentedTextView.showAsActiveIf(it.isAugmented())
        }
    }

    private fun DialogTimesInOutBinding.collectPicPicusFlowToToggle(){
        viewModel.picPicusFlow().launchCollectWhileLifecycleStateStarted{
            timesDialogPicPicusTextview.setPicPicusField(it)
        }
    }

    private fun DialogTimesInOutBinding.collectCopilotFlowToToggle(){
        viewModel.copilotFlow().launchCollectWhileLifecycleStateStarted{
            coPilotTextView.showAsActiveIf(it)
        }
    }

    private fun DialogTimesInOutBinding.collectDualInstructorFlowToToggle(){
        viewModel.dualInstructorFlow().launchCollectWhileLifecycleStateStarted{
            timesDialogDualInstructorTextview.setDualInstructorField(it)
        }
    }

    private fun DialogTimesInOutBinding.setOnClickListeners(){
        augmentedTextView.setOnClickListener {
            supportFragmentManager.commit {
                add(R.id.mainActivityLayout, AugmentedCrewDialog())
                addToBackStack(null)
            }
        }

        timesDialogPicPicusTextview.setOnClickListener {
            viewModel.togglePicusPicNone()
        }

        coPilotTextView.setOnClickListener {
            viewModel.toggleCopilot()
        }

        timesDialogDualInstructorTextview.setOnClickListener {
            viewModel.toggleDualInstructorNone()
        }

        timesDialogSaveTextview.setOnClickListener {
            closeFragment()
        }

        timesDialogCancelTextview.setOnClickListener {
            viewModel.undo()
            closeFragment()
        }
    }

    private fun DialogTimesInOutBinding.setOnFocusChangedListeners(){
        settTotalTimeOfFLightTextViewOnFocusChangedListener()
        setNightTimeTextViewOnFocusChangedListener()
        setIfrTimeTextViewOnFocusChangedListener()
    }

    private fun DialogTimesInOutBinding.settTotalTimeOfFLightTextViewOnFocusChangedListener() {
        totalTimeOfFlightTextview.onFocusChangeListener =
            View.OnFocusChangeListener { _, hasFocus ->
                totalTimeOfFlightTextview.separateDataDisplayAndEntry(hasFocus) {
                    viewModel.setTotalTimeOfFlight(it?.toString()?.nullIfBlank())
                }
            }
    }

    private fun DialogTimesInOutBinding.setNightTimeTextViewOnFocusChangedListener() {
        nightTimeTextview.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            nightTimeTextview.separateDataDisplayAndEntry(hasFocus) {
                viewModel.setNightTime(it?.toString())
            }
        }
    }

    private fun DialogTimesInOutBinding.setIfrTimeTextViewOnFocusChangedListener() {
        ifrTimeTextview.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            ifrTimeTextview.separateDataDisplayAndEntry(hasFocus) {
                viewModel.setIfrTime(it?.toString())
            }
        }
    }


    private fun DialogTimesInOutBinding.hideKeyboardWhenPressingEnterInLastField() {
        ifrTimeTextview.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm: InputMethodManager =
                    v.ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
