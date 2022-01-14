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


//TODO needs work
//TODO remake this dialog as complete aircraft editor
class AircraftPicker: JoozdlogFragment(){
    private val viewModel: AircraftPickerViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DialogPickAircraftTypeBinding.bind(inflater.inflate(R.layout.dialog_pick_aircraft_type, container, false)).apply{

            //set color of dialog head
            val typesPickerAdapter = AircraftPickerAdapter {
                Log.d(this::class.simpleName, "clicked on $it")
                viewModel.selectAircraftType(it)
            }.also {
                typesPickerRecyclerView.layoutManager = LinearLayoutManager(context)
                typesPickerRecyclerView.adapter = it
            }
            val regFieldAdapter = AircraftAutoCompleteAdapter(requireActivity(), R.layout.item_custom_autocomplete)
            registrationField.setAdapter(regFieldAdapter)

            /**
             * editText OnTextChanged and onFocuschanged
             */
            //TODO check consensus and set selectedType if found
            var regFieldActive = false
            registrationField.setOnFocusChangeListener { _, hasFocus ->
                regFieldActive = hasFocus
            }
            registrationField.onTextChanged {
                if (regFieldActive)
                    viewModel.updateRegistration(it)
            }

            searchField.onTextChanged {
                viewModel.updateSearchString(it)
            }


            /******************************************************************************
             * Observers
             ******************************************************************************/

            viewModel.selectedAircraft.observe(viewLifecycleOwner){
                pickedAircraftText.text = it.registration.nullIfBlank()?.also{
                    if (!regFieldActive) registrationField.setText(it)
                } ?: getString(R.string.aircraft)
                typeDescriptionTextView.text = it.type?.name ?: "" // set type text to found type or to empty string
                typesPickerAdapter.selectActiveItem(it.type)

                val errorText = registrationFieldLayout.findViewById<TextView>(R.id.textinput_error).apply{
                    visibility=View.VISIBLE
                }
                val normalColor = requireActivity().getColorFromAttr(android.R.attr.textColorSecondary)
                when(it.source){
                    Aircraft.KNOWN -> {
                        errorText.text = getString(R.string.aircraft_type_found)
                        errorText.setTextColor(normalColor)
                    }

                    Aircraft.FLIGHT -> {
                        errorText.text = getString(R.string.aircraft_type_in_flights)
                        errorText.setTextColor(normalColor)
                    }

                    Aircraft.FLIGHT_CONFLICTING -> {
                        errorText.text = getString(R.string.aircraft_type_in_flights_conflict)
                        errorText.setTextColor(ContextCompat.getColor(errorText.context, R.color.orange))
                    }

                    Aircraft.PRELOADED -> {
                        errorText.text = getString(R.string.aircraft_type_from_server)
                        errorText.setTextColor(normalColor)
                    }

                    Aircraft.CONSENSUS -> {
                        errorText.text = getString(R.string.aircraft_type_in_consensus)
                        errorText.setTextColor(ContextCompat.getColor(errorText.context, R.color.orange))
                    }

                    Aircraft.NONE -> {
                        errorText.text = getString(R.string.aircraft_type_not_found)
                        errorText.setTextColor(ContextCompat.getColor(errorText.context, R.color.red))
                    }

                    else -> registrationFieldLayout.error = getString(R.string.error)

                }
            }

            viewModel.aircraftTypes.observe(viewLifecycleOwner) {
                Log.d(this::class.simpleName, "updating list with ${it.size} items")
                typesPickerAdapter.updateList(it)
            }

            viewModel.knownRegistrations.observe(viewLifecycleOwner){
                regFieldAdapter.setItems(it)
            }

            /******************************************************************************
             * save or cancel
             ******************************************************************************/

            aircraftPickerCancel.setOnClickListener {
                viewModel.undo()
                closeFragment()
            }

            aircraftPickerDialogBackground.setOnClickListener {
                viewModel.undo()
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
}