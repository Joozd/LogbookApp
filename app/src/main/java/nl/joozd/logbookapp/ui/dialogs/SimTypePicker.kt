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
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.DialogPickSimTypeBinding
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment
import nl.joozd.logbookapp.model.viewmodels.dialogs.AircraftPickerViewModel
import nl.joozd.logbookapp.ui.adapters.AircraftPickerAdapter


class SimTypePicker: JoozdlogFragment(){
    private val viewModel: AircraftPickerViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DialogPickSimTypeBinding.bind(inflater.inflate(R.layout.dialog_pick_sim_type, container, false)).apply{
            val typesPickerAdapter = AircraftPickerAdapter {
                viewModel.selectAircraftType(it)
            }.also {
                typesPickerRecyclerView.layoutManager = LinearLayoutManager(context)
                typesPickerRecyclerView.adapter = it
            }

            /**
             * editText OnTextChanged and onFocuschanged
             */
            searchField.onTextChanged {
                viewModel.updateSearchString(it)
            }


            /******************************************************************************
             * Observers
             ******************************************************************************/

            viewModel.selectedAircraftType.observe(viewLifecycleOwner) {
                typesPickerAdapter.selectActiveItem(it)
                typesPickerRecyclerView.scrollToPosition(typesPickerAdapter.list.indexOf(it))
            }

            viewModel.selectedAircraft.observe(viewLifecycleOwner) {
                it.type?.let{t ->
                    typeDescriptionTextView.text = t.shortName
                }
            }

            viewModel.aircraftTypes.observe(viewLifecycleOwner) {
                Log.d(this::class.simpleName, "updating list with ${it.size} items")
                typesPickerAdapter.updateList(it)
            }

            /******************************************************************************
             * save or cancel
             ******************************************************************************/

            /**
             * A bit more complex than other JoozdlogDialogs:
             * - save AircraftRegistrationWithTypeData
             * - update flight
             */
            aircraftPickerSave.setOnClickListener {
                closeFragment()
            }

            //catch clicks on empty layout
            headerLayout.setOnClickListener { }
            bodyLayout.setOnClickListener { }

        }.root // end of inflater.inflate(...).apply
}