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
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.picker_landings.view.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.model.viewmodels.dialogs.LandingsDialogViewModel
import nl.joozd.logbookapp.ui.fragments.JoozdlogFragment

class LandingsDialog: JoozdlogFragment() {
    private val landingsDialogViewModel: LandingsDialogViewModel by viewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.picker_landings, container, false).apply{

            toDayUpButton.setOnClickListener { landingsDialogViewModel.toDayUpButtonClick() }
            toNightUpButton.setOnClickListener { landingsDialogViewModel.toNightUpButtonClick() }
            ldgDayUpButton.setOnClickListener { landingsDialogViewModel.ldgDayUpButtonClick() }
            ldgNightUpButton.setOnClickListener { landingsDialogViewModel.ldgNightUpButtonClick() }
            autolandUpButton.setOnClickListener { landingsDialogViewModel.autolandUpButtonClick() }

            toDayDownButton.setOnClickListener { landingsDialogViewModel.toDayDownButtonClick() }
            toNightDownButton.setOnClickListener { landingsDialogViewModel.toNightDownButtonClick() }
            ldgDayDownButton.setOnClickListener { landingsDialogViewModel.ldgDayDownButtonClick() }
            ldgNightDownButton.setOnClickListener { landingsDialogViewModel.ldgNightDownButtonClick() }
            autolandDownButton.setOnClickListener { landingsDialogViewModel.autolandDownButtonClick() }


            /**
             * observers
             */
            landingsDialogViewModel.toDay.observe(viewLifecycleOwner, Observer{
                toDayField.setText(it.toString())
            })
            landingsDialogViewModel.toNight.observe(viewLifecycleOwner, Observer{
                toNightField.setText(it.toString())
            })
            landingsDialogViewModel.ldgDay.observe(viewLifecycleOwner, Observer{
                ldgDayField.setText(it.toString())
            })
            landingsDialogViewModel.ldgNight.observe(viewLifecycleOwner, Observer{
                ldgNightField.setText(it.toString())
            })
            landingsDialogViewModel.autoland.observe(viewLifecycleOwner, Observer{
                autolandField.setText(it.toString())
            })

            //catch clicks on empty parts of dialog
            landingCardsWrapper.setOnClickListener {  }

            //set cancel functions
            cancelTextButton.setOnClickListener {
                landingsDialogViewModel.undo()
                closeFragment()
            }
            landingPickerBackground.setOnClickListener {
                landingsDialogViewModel.undo()
                closeFragment()
            }

            saveTextButon.setOnClickListener {
                closeFragment()
            }
        }
    }
}