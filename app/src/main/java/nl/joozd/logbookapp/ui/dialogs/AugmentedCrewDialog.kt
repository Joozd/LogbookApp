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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.DialogAugmentedCrewBinding
import nl.joozd.logbookapp.model.helpers.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.model.viewmodels.dialogs.AugmentedCrewDialogViewModel
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment

//TODO doesn't work yet, just skeleton
class AugmentedCrewDialog: JoozdlogFragment(){
    private val viewModel: AugmentedCrewDialogViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DialogAugmentedCrewBinding.bind(inflater.inflate(R.layout.dialog_augmented_crew, container, false)).apply {
            launchFlowCollectors()
            setOnClickListeners()
            setOnFocusChangedListeners()
        }.root


    private fun DialogAugmentedCrewBinding.launchFlowCollectors(){
        collectCrewSizeFlow()
        collectDidTakeoffFlow()
        collectDidLandingFlow()
        collectTakeoffLandingTimeFlow()
    }

    private fun DialogAugmentedCrewBinding.setOnClickListeners(){
        crewDownButton                  .setOnClickListener { viewModel.crewDown() }
        crewUpButton                    .setOnClickListener { viewModel.crewUp() }
        didTakeoffCheckbox              .setOnClickListener { viewModel.toggleTakeoff() }
        didLandingCheckbox              .setOnClickListener { viewModel.toggleLanding() }
        augmentedCrewDialogBackground   .setOnClickListener { /*do nothing*/ }
        setCancelButtonOnclickListener()
        setSaveButtonOnClickListener()
    }

    private fun DialogAugmentedCrewBinding.setOnFocusChangedListeners(){
        setTimeForTakeoffLandingOnFocusChangedListener()
    }

    private fun DialogAugmentedCrewBinding.setSaveButtonOnClickListener() {
        saveCrewDialogButon.setOnClickListener {
            it.requestFocus()
            closeFragment()
        }
    }

    private fun DialogAugmentedCrewBinding.setCancelButtonOnclickListener() {
        cancelCrewDialogButon.setOnClickListener {
            viewModel.undo()
            closeFragment()
        }
    }



    private fun DialogAugmentedCrewBinding.setTimeForTakeoffLandingOnFocusChangedListener() {
        timeForTakeoffLandingEditText.setOnFocusChangeListener { _, hasFocus ->
            timeForTakeoffLandingEditText.separateDataDisplayAndEntry(hasFocus) {
                viewModel.setTakeoffLandingTime(it?.toString())
            }
        }
    }

    private fun DialogAugmentedCrewBinding.collectCrewSizeFlow() {
        viewModel.crewSizeFlow().launchCollectWhileLifecycleStateStarted {
            crewSizeEditText.setText(it.toString())
        }
    }

    private fun DialogAugmentedCrewBinding.collectDidLandingFlow() {
        viewModel.didLandingFlow().launchCollectWhileLifecycleStateStarted {
            didLandingCheckbox.isChecked = it
        }
    }

    private fun DialogAugmentedCrewBinding.collectDidTakeoffFlow() {
        viewModel.didTakeoffFlow().launchCollectWhileLifecycleStateStarted {
            didTakeoffCheckbox.isChecked = it
        }
    }

    private fun DialogAugmentedCrewBinding.collectTakeoffLandingTimeFlow() {
        viewModel.takeoffLandingTimeFlow().launchCollectWhileLifecycleStateStarted {
            timeForTakeoffLandingEditText.setText(it.minutesToHoursAndMinutesString())
        }
    }
}

