/*
 * JoozdLog Pilot's Logbook
 * Copyright (C) 2020 Joost Welle
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see https://www.gnu.org/licenses
 */

package nl.joozd.logbookapp.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.dialog_augmented_crew.view.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.extensions.toInt
import nl.joozd.logbookapp.model.viewmodels.dialogs.AugmentedCrewDialogViewModel
import nl.joozd.logbookapp.ui.fragments.JoozdlogFragment

//TODO doesn't work yet, just skeleton
class AugmentedCrewDialog: JoozdlogFragment(){
    private val augmentedCrewDialogViewModel: AugmentedCrewDialogViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.dialog_augmented_crew, container, false).apply {
            crewDownButton.setOnClickListener {
                augmentedCrewDialogViewModel.crewDown()
            }
            crewUpButton.setOnClickListener {
                augmentedCrewDialogViewModel.crewUp()
            }
            didTakeoffCheckbox.setOnCheckedChangeListener { _, b ->
                augmentedCrewDialogViewModel.setTakeoff(b)
            }
            didLandingCheckbox.setOnCheckedChangeListener { _, b ->
                augmentedCrewDialogViewModel.setLanding(b)
            }

            timeForTakeoffLandingEditText.setOnFocusChangeListener { v, hasFocus ->
                if (!hasFocus){
                    augmentedCrewDialogViewModel.setTakeoffLandingTime(timeForTakeoffLandingEditText.text.toInt())
                }
            }

            /**
             * observers:
             */


            augmentedCrewDialogViewModel.augmentedCrewData.observe(viewLifecycleOwner, Observer{
                crewSizeEditText.setText(it.crewSize.toString())
                didTakeoffCheckbox.isChecked = it.didTakeoff
                didLandingCheckbox.isChecked = it.didLanding
                timeForTakeoffLandingEditText.setText(it.takeoffLandingTimes.toString())
            })


            cancelCrewDialogButton.setOnClickListener {
                // TODO
            }
            augmentedCrewDialogBackground.setOnClickListener {
                // TODO
            }
            saveCrewDialogButon.setOnClickListener {
                augmentedCrewDialogViewModel.undo()
                closeFragment()
            }
        }
    }
}

