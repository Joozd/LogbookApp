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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.collect
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.databinding.DialogPickAircraftTypeBinding
import nl.joozd.logbookapp.extensions.getColorFromAttr
import nl.joozd.logbookapp.extensions.nullIfBlank
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment
import nl.joozd.logbookapp.model.viewmodels.dialogs.AircraftPickerViewModel
import nl.joozd.logbookapp.ui.adapters.AircraftAutoCompleteAdapter
import nl.joozd.logbookapp.ui.adapters.AircraftPickerAdapter



class AircraftPicker: JoozdlogFragment(){
    private val viewModel: AircraftPickerViewModel by viewModels()
    private val layout get() = R.layout.dialog_pick_aircraft_type

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DialogPickAircraftTypeBinding.bind(inflater.inflate(layout, container, false)).apply{
            val typesPickerAdapter = setupAircraftTypesListAndReturnAdapter()
            val regFieldAdapter = setupRegFieldAutoCompleteAdapter()

            searchField.onTextChanged {
                viewModel.updateSearchString(it)
            }

            launchFlowCollectors(typesPickerAdapter, regFieldAdapter)


            /******************************************************************************
             * save or cancel
             ******************************************************************************/


            aircraftPickerDialogBackground.setOnClickListener {
                closeFragment()
            }

            /**
             * A bit more complex than other JoozdlogDialogs:
             * - save AircraftRegistrationWithTypeData
             * - update flight
             */
            aircraftPickerSave.setOnClickListener {
                viewModel.saveAircraftToRepository()
                closeFragment()
            }

            //empty listeners to catch clicks
            headerLayout.setOnClickListener { }
            bodyLayout.setOnClickListener { }

        }.root // end of inflater.inflate(...).apply
     // end of onCreateView()

    private fun DialogPickAircraftTypeBinding.setupAircraftTypesListAndReturnAdapter()
    : AircraftPickerAdapter =
        AircraftPickerAdapter { viewModel.selectAircraftType(it) }.also {
            typesPickerRecyclerView.layoutManager = LinearLayoutManager(context)
            typesPickerRecyclerView.adapter = it
        }

    private fun DialogPickAircraftTypeBinding.setupRegFieldAutoCompleteAdapter() =
        AircraftAutoCompleteAdapter(requireActivity(), R.layout.item_custom_autocomplete).also{
            registrationField.setAdapter(it)
        }

    private fun DialogPickAircraftTypeBinding.launchFlowCollectors(
        typesPickerAdapter: AircraftPickerAdapter,
        regFieldAdapter: AircraftAutoCompleteAdapter
    ){
        viewModel.aircraftTypesFlow().launchCollectWhileLifecycleStateStarted{
            typesPickerAdapter.submitList(it)
        }

        viewModel.knownRegistrationsLiveData.launchCollectWhileLifecycleStateStarted{
            regFieldAdapter.setItems(it)
        }
    }

}