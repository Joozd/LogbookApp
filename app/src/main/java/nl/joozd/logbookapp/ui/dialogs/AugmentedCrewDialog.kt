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
import nl.joozd.logbookapp.databinding.DialogAugmentedCrewBinding
import nl.joozd.logbookapp.model.viewmodels.dialogs.AugmentedCrewDialogViewModel
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment

//TODO doesn't work yet, just skeleton
class AugmentedCrewDialog: JoozdlogFragment(){
    private val viewModel: AugmentedCrewDialogViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        with(DialogAugmentedCrewBinding.bind(inflater.inflate(R.layout.dialog_augmented_crew, container, false))){
            crewDownButton.setOnClickListener {
                viewModel.crewDown()
            }
            crewUpButton.setOnClickListener {
                viewModel.crewUp()
            }
            didTakeoffCheckbox.setOnCheckedChangeListener { _, b ->
                viewModel.setTakeoff(b)
            }
            didLandingCheckbox.setOnCheckedChangeListener { _, b ->
                viewModel.setLanding(b)
            }

            timeForTakeoffLandingEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus){
                    viewModel.setTakeoffLandingTime(timeForTakeoffLandingEditText.text)
                }
            }

            /**
             * observers:
             */
            viewModel.crewsize.observe(viewLifecycleOwner) {
                crewSizeEditText.setText(it.toString())
            }

            viewModel.didTakeoff.observe(viewLifecycleOwner) {
                didTakeoffCheckbox.isChecked = it
            }

            viewModel.didLanding.observe(viewLifecycleOwner) {
                didLandingCheckbox.isChecked = it
            }

            viewModel.takeoffLandingTimes.observe(viewLifecycleOwner) { t ->
                timeForTakeoffLandingEditText.setText(t)
            }


            /**
             * Buttons
             */
            cancelCrewDialogButton.setOnClickListener {
                viewModel.undo()
                closeFragment()
            }
            augmentedCrewDialogBackground.setOnClickListener {
                viewModel.undo()
                closeFragment()
            }
            saveCrewDialogButon.setOnClickListener {
                closeFragment()
            }
            return root
        }
    }
}

