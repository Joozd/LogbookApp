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

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.dialog_pick_aircraft_type.view.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.extensions.getColorFromAttr
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.ui.fragments.JoozdlogFragment
import nl.joozd.logbookapp.model.viewmodels.dialogs.AircraftPickerViewModel
import nl.joozd.logbookapp.ui.adapters.SelectableStringAdapter


class SimTypePicker: JoozdlogFragment(){
    private val viewModel: AircraftPickerViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_pick_sim_type, container, false).apply{
            //set color of dialog head
            (aircraftPickerTopHalf?.background as GradientDrawable).colorFilter = PorterDuffColorFilter(requireActivity().getColorFromAttr(android.R.attr.colorPrimary), PorterDuff.Mode.SRC_IN) // set background color to bakground with rounded corners

            val typesPickerAdapter = SelectableStringAdapter {
                Log.d(this::class.simpleName, "clicked on $it")
                viewModel.selectAircraftTypeByString(it)
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

            viewModel.selectedAircraftString.observe(viewLifecycleOwner, Observer{
                typesPickerAdapter.selectActiveItem(it)
                typesPickerRecyclerView.scrollToPosition(typesPickerAdapter.list.indexOf(it))
            })

            viewModel.selectedAircraft.observe(viewLifecycleOwner, Observer{
                it.type?.let{t ->
                    typeDescriptionTextView.text = t.shortName
                }
            })

            viewModel.aircraftTypes.observe(viewLifecycleOwner, Observer {
                Log.d(this::class.simpleName, "updating list with ${it.size} items")
                typesPickerAdapter.updateList(it)
            })

            /******************************************************************************
             * save or cancel
             ******************************************************************************/

            aircraftPickerCancel.setOnClickListener {
                viewModel.undo()
                closeFragment()
            }

            /**
             * A bit more complex than other JoozdlogDialogs:
             * - save AircraftRegistrationWithTypeData
             * - update flight
             */
            aircraftPickerSave.setOnClickListener {
                viewModel.saveTypeOnly()
                closeFragment()
            }
            aircraftPickerDialogBox.setOnClickListener {
                //Intentionally blank
            }

        } // end of inflater.inflate(...).apply

    } // end of onCreateView()

    override fun onStart() {
        super.onStart()
        viewModel.start() // initially set fields and get aircraft type data from repository
    }

}