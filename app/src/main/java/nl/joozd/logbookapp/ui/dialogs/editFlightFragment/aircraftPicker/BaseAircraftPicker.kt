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

package nl.joozd.logbookapp.ui.dialogs.editFlightFragment.aircraftPicker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.DialogPickAircraftTypeBinding
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment
import nl.joozd.logbookapp.model.viewmodels.dialogs.AircraftPickerViewModel
import nl.joozd.logbookapp.ui.adapters.AircraftAutoCompleteAdapter
import nl.joozd.logbookapp.ui.adapters.AircraftPickerAdapter


/**
 * This holds all the logic common to AircraftPicker and SimTypePicker
 */
abstract class BaseAircraftPicker: JoozdlogFragment(){
    /**
     * If this is set to true, no registration data will be used. Apart from this, dialogs are the same.
     */
    protected abstract val isSimPicker: Boolean
    private val viewModel: AircraftPickerViewModel by viewModels()
    private val layout get() = R.layout.dialog_pick_aircraft_type

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DialogPickAircraftTypeBinding.bind(inflater.inflate(layout, container, false)).apply{
            val typesPickerAdapter = setupAircraftTypesListAndReturnAdapter()
            val regFieldAdapter = setupRegFieldAutoCompleteAdapter()

            launchFlowCollectors(typesPickerAdapter, regFieldAdapter)
            setOnTextChangedListeners()
            setOnClickListeners()
            if (isSimPicker) hideUnusedFieldsWhenSim()

        }.root

    private fun DialogPickAircraftTypeBinding.launchFlowCollectors(
        typesPickerAdapter: AircraftPickerAdapter,
        regFieldAdapter: AircraftAutoCompleteAdapter?
    ){
        viewModel.aircraftTypesFlow().launchCollectWhileLifecycleStateStarted{
            typesPickerAdapter.submitList(it)
        }
        regFieldAdapter?.let{ a -> // null if isSimPicker
            viewModel.knownRegistrationsLiveData.launchCollectWhileLifecycleStateStarted{
                a.setItems(it)
            }
        }
        viewModel.selectedAircraftFlow().launchCollectWhileLifecycleStateStarted{
            pickedAircraftText.text = it.registration
            typeDescriptionTextView.text = it.type?.name ?: ""
        }


    }

    private fun DialogPickAircraftTypeBinding.setOnTextChangedListeners() {
        searchField.onTextChanged { viewModel.updateSearchString(it) }

        registrationField.onTextChanged {
            viewModel.updateRegistration(it)
        }
    }

    private fun DialogPickAircraftTypeBinding.setOnClickListeners() {
        aircraftPickerDialogBackground.setOnClickListener {
            //Do nothing, only catches stray clicks
        }

        aircraftPickerSave.setOnClickListener {
            closeFragment()
        }

        aircraftPickerCancel.setOnClickListener {
            viewModel.undo()
            closeFragment()
        }
    }

    private fun DialogPickAircraftTypeBinding.hideUnusedFieldsWhenSim(){
        registrationFieldLayout.visibility = View.GONE
    }

    private fun DialogPickAircraftTypeBinding.setupAircraftTypesListAndReturnAdapter()
    : AircraftPickerAdapter =
        AircraftPickerAdapter(makeOnListChangedListener()) { viewModel.selectAircraftType(it) }.also {
            typesPickerRecyclerView.layoutManager = LinearLayoutManager(context)
            typesPickerRecyclerView.adapter = it
        }

    private fun DialogPickAircraftTypeBinding.makeOnListChangedListener() =
        AircraftPickerAdapter.ListChangedListener { newList ->
            indexOfFirstPickedItemOrNull(newList) ?.let{
                typesPickerRecyclerView.scrollToPosition(it)
            }
        }

    private fun indexOfFirstPickedItemOrNull(newList: List<Pair<AircraftType, Boolean>>) =
        newList.indexOfFirst { it.second }
            .takeIf{ it != -1 }

    private fun DialogPickAircraftTypeBinding.setupRegFieldAutoCompleteAdapter() =
        if (isSimPicker) null else
        AircraftAutoCompleteAdapter(requireActivity(), R.layout.item_custom_autocomplete).also{
            registrationField.setAdapter(it)
        }

}