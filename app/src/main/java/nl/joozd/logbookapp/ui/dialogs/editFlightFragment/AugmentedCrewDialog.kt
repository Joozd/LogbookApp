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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.databinding.DialogAugmentedCrewBinding
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.extensions.setOnFocusLostListener
import nl.joozd.logbookapp.model.helpers.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.model.viewmodels.dialogs.AugmentedCrewDialogViewModel
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment

class AugmentedCrewDialog: JoozdlogFragment(){
    private val viewModel: AugmentedCrewDialogViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DialogAugmentedCrewBinding.bind(inflater.inflate(R.layout.dialog_augmented_crew, container, false)).apply {
            initializeViews()
            setUIOnClickListeners()
        }.root


    private fun DialogAugmentedCrewBinding.initializeViews(){
        initializeConstraintLayouts()
        initializeCrewSizeViews()
        initializeTakeoffLandingViews()
        initializeFixedTimesView()
        initializeFixedTimesSwitch()
    }

    private fun DialogAugmentedCrewBinding.initializeConstraintLayouts(){
        inSeatLayout.hideWhenFixedTimes()
        crewSizeLayout.hideWhenFixedTimes()
    }

    private fun DialogAugmentedCrewBinding.initializeCrewSizeViews(){
        initializeCrewDownButton()
        initializeCrewUpButton()
        initializeCrewSizeEditText()
    }

    private fun DialogAugmentedCrewBinding.initializeTakeoffLandingViews(){
        initializeDidTakeoffCheckbox()
        initializedidLandingCheckbox()
        initializeTakeoffLandingTimesTextView()
    }

    private fun DialogAugmentedCrewBinding.initializeFixedTimesView(){
        var textToSetWhenFocusLost = ""

        viewModel.isFixedTimeFlow.launchCollectWhileLifecycleStateStarted{ isEnabled ->
            restTimeLayout.isEnabled = isEnabled
            restTimeEditText.isEnabled = isEnabled
        }

        restTimeEditText.setOnFocusLostListener{
            it.setText(textToSetWhenFocusLost)
        }

        restTimeEditText.onTextChanged {
            if(restTimeEditText.isEnabled && restTimeEditText.hasFocus())
                viewModel.setTime(it)
        }

        viewModel.restTimeFlow.launchCollectWhileLifecycleStateStarted { minutes ->
            val t = minutes.minutesToHoursAndMinutesString()
            if (restTimeEditText.hasFocus())
                textToSetWhenFocusLost = t
            else
                restTimeEditText.setText(t)
        }
    }

    private fun DialogAugmentedCrewBinding.initializeFixedTimesSwitch(){
        viewModel.isFixedTimeFlow.launchCollectWhileLifecycleStateStarted{
            fixedTimesSwitch.isChecked = it
        }
        fixedTimesSwitch.setOnClickListener {
            clearFocus()
            viewModel.toggleUseFixedTime()
        }
    }

    private fun DialogAugmentedCrewBinding.initializeDidTakeoffCheckbox() {
        didTakeoffCheckbox.setOnClickListener {
            clearFocus()
            viewModel.toggleTakeoff() }
        viewModel.didTakeoffFlow.launchCollectWhileLifecycleStateStarted {
            didTakeoffCheckbox.isChecked = it
        }
    }

    private fun DialogAugmentedCrewBinding.initializedidLandingCheckbox() {
        didLandingCheckbox.setOnClickListener {
            clearFocus()
            viewModel.toggleLanding()
        }
        viewModel.didLandingFlow.launchCollectWhileLifecycleStateStarted {
            didLandingCheckbox.isChecked = it
        }
    }

    private fun DialogAugmentedCrewBinding.initializeTakeoffLandingTimesTextView(){
        var textToSetWhenFocusLost = ""
        timeForTakeoffLandingEditTextLayout.hideWhenFixedTimes()

        timeForTakeoffLandingEditText.onTextChanged {
            if(timeForTakeoffLandingEditText.hasFocus())
                viewModel.setTime(it)
        }

        timeForTakeoffLandingEditText.setOnFocusLostListener{
            it.setText(textToSetWhenFocusLost)
        }

        viewModel.takeoffLandingTimeFlow.launchCollectWhileLifecycleStateStarted { minutes ->
            if (minutes == 0 && Prefs.augmentedTakeoffLandingTimes() != 0){
                viewModel.setTime(Prefs.augmentedTakeoffLandingTimes()) // this will make viewModel.takeoffLandingTimeFlow emit again
            }
            else {
                val t = minutes.minutesToHoursAndMinutesString()
                if (timeForTakeoffLandingEditText.hasFocus())
                    textToSetWhenFocusLost = t
                else
                    timeForTakeoffLandingEditText.setText(t)
            }
        }
    }

    private fun DialogAugmentedCrewBinding.initializeCrewDownButton(){
        crewDownButton.setOnClickListener {
            clearFocus()
            viewModel.crewDown()
        }
    }

    private fun DialogAugmentedCrewBinding.initializeCrewUpButton(){
        crewUpButton.setOnClickListener {
            clearFocus()
            viewModel.crewUp()
        }
    }

    private fun DialogAugmentedCrewBinding.initializeCrewSizeEditText() {
        viewModel.crewSizeFlow.launchCollectWhileLifecycleStateStarted {
            crewSizeEditText.setText(it.toString())
        }
    }

    private fun DialogAugmentedCrewBinding.setUIOnClickListeners(){
        augmentedCrewDialogBackground.setOnClickListener { clearFocus() } // this also prevents things below this dialog from happening!
        dialogLayout.setOnClickListener { clearFocus() }

        setCancelButtonOnclickListener()
        setSaveButtonOnClickListener()
    }

    private fun DialogAugmentedCrewBinding.setSaveButtonOnClickListener() {
        saveCrewDialogButon.setOnClickListener {
            clearFocus()
            closeFragment()
        }
    }

    private fun DialogAugmentedCrewBinding.setCancelButtonOnclickListener() {
        cancelCrewDialogButon.setOnClickListener {
            clearFocus()
            viewModel.undo()
            closeFragment()
        }
    }


    private fun View.hideWhenFixedTimes(){
        viewModel.isFixedTimeFlow.launchCollectWhileLifecycleStateStarted{ isFixedTime ->
            this.visibility = if (isFixedTime) View.GONE else View.VISIBLE
        }
    }

    private fun clearFocus(){
        activity?.currentFocus?.clearFocus()
    }
}

