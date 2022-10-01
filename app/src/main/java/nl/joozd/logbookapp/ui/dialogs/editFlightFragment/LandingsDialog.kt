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
import nl.joozd.logbookapp.databinding.DialogLandingsBinding
import nl.joozd.logbookapp.model.viewmodels.dialogs.LandingsDialogViewModel
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment

class LandingsDialog: JoozdlogFragment() {
    private val viewModel: LandingsDialogViewModel by viewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DialogLandingsBinding.bind(inflater.inflate(R.layout.dialog_landings, container, false)).apply{
            setOnClickListeners()
            launchFlowCollectors()
        }.root

    private fun DialogLandingsBinding.setOnClickListeners() {
        setUpDownButtonOnClickListeners()
        setInterfaceOnClickListeners()
    }

    private fun DialogLandingsBinding.launchFlowCollectors() {
        viewModel.takeoffLandingsFlow.launchCollectWhileLifecycleStateStarted { t ->
            toDayField.setText(t.takeoffDay.toString())
            toNightField.setText(t.takeoffNight.toString())
            ldgDayField.setText(t.landingDay.toString())
            ldgNightField.setText(t.landingNight.toString())
            autolandField.setText(t.autoLand.toString())
        }
    }

    private fun DialogLandingsBinding.setInterfaceOnClickListeners() {
        landingsDialogBackground.setOnClickListener {
            //do nothing
        }

        saveLandingsDialogTextview.setOnClickListener {
            closeFragment()
        }

        cancelLandingsDialogTextview.setOnClickListener {
            viewModel.undo()
            closeFragment()
        }
    }

    private fun DialogLandingsBinding.setUpDownButtonOnClickListeners() {
        toDayUpButton.setOnClickListener { viewModel.toDayUpButtonClick() }
        toNightUpButton.setOnClickListener { viewModel.toNightUpButtonClick() }
        ldgDayUpButton.setOnClickListener { viewModel.ldgDayUpButtonClick() }
        ldgNightUpButton.setOnClickListener { viewModel.ldgNightUpButtonClick() }
        autolandUpButton.setOnClickListener { viewModel.autolandUpButtonClick() }

        toDayDownButton.setOnClickListener { viewModel.toDayDownButtonClick() }
        toNightDownButton.setOnClickListener { viewModel.toNightDownButtonClick() }
        ldgDayDownButton.setOnClickListener { viewModel.ldgDayDownButtonClick() }
        ldgNightDownButton.setOnClickListener { viewModel.ldgNightDownButtonClick() }
        autolandDownButton.setOnClickListener { viewModel.autolandDownButtonClick() }
    }
}