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

package nl.joozd.logbookapp.ui.dialogs.airportPicker

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.*
import nl.joozd.joozdutils.JoozdlogAlertDialog

import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.DialogAirportsBinding
import nl.joozd.logbookapp.model.viewmodels.dialogs.airportPicker.AirportPickerViewModel

import nl.joozd.logbookapp.extensions.nullIfEmpty
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.ui.adapters.AirportPickerAdapter
import nl.joozd.logbookapp.ui.fragments.JoozdlogFragment
import nl.joozd.logbookapp.ui.utils.longToast
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.AirportPickerEvents
import nl.joozd.logbookapp.ui.utils.customs.JoozdlogAlertDialogV1

import java.util.*
import kotlin.math.abs

/**
 * Use AirportPicker(orig = true/false)
 * This will set correct value in ViewModel, recreation will call constructor without params
 * but viewModel will persist.
 */
@ExperimentalCoroutinesApi
abstract class AirportPicker(): JoozdlogFragment() {
    protected abstract val workingOnOrig: Boolean // this must be set before first-time attachment
    protected abstract val viewModel: AirportPickerViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return DialogAirportsBinding.bind(inflater.inflate(R.layout.dialog_airports, container, false)).apply {
            //Set background color for title bar
            airportsDialogTopHalf.joozdLogSetBackgroundColor()

            /**
             * Initialize recyclerView and it's stuff
             */
            val airportPickerAdapter = AirportPickerAdapter { airport ->
                viewModel.pickAirport(airport)
            }
            airportsPickerList.layoutManager = LinearLayoutManager(context)
            airportsPickerList.adapter = airportPickerAdapter


            /**
             * Things that this dialog actually does:
             */

            // Update recyclerView while typing
            airportsSearchField.onTextChanged { t ->
                viewModel.updateSearch(t)
            }


            /**
             * Set the current text as an airport
             */
            setCurrentTextButton.setOnClickListener {
                airportsSearchField.text.toString().nullIfEmpty()?.let {
                    viewModel.setCustomAirport(it)
                }
            }

            /**
             * Save/cancel functions
             */
            //no action is misclicked on window
            airportPickerDialogLayout.setOnClickListener { }

            airportPickerLayout.setOnClickListener {
                viewModel.undo()
                closeFragment()
            }
            cancelAirportDialog.setOnClickListener {
                viewModel.undo()
                closeFragment()
            }

            saveAirportDialog.setOnClickListener{
                closeFragment()
            }


            /**
             * observers:
             */
            //observer events
            viewModel.feedbackEvent.observe(viewLifecycleOwner){
                //if event already consumed, it.getEvent() == null
                when(it.getEvent()){
                    AirportPickerEvents.ORIG_OR_DEST_NOT_SELECTED -> {
                        longToast("AirportPicker error")
                        closeFragment()
                    }
                    AirportPickerEvents.CUSTOM_AIRPORT_NOT_EDITED -> {
                        JoozdlogAlertDialog().show(requireActivity()) {
                            titleResource = R.string.warning
                            messageResource = R.string.custom_airport_caution_text // TODO remove TODO from string once no longer necessary
                            setPositiveButton(android.R.string.ok)
                        }
                    }
                    AirportPickerEvents.NOT_IMPLEMENTED -> longToast("Not Implemented in viewModel")
                }
            }

            //observe airportList for recyclerview
            viewModel.airportsList.observe(viewLifecycleOwner){
                airportPickerAdapter.submitList(it)
            }

            /**
             * Set text in search field, as well as in "selected" airport.
             * Only set search field if it is blank.
             * TODO make that happen in a better way.
             */
            viewModel.pickedAirport.observe(viewLifecycleOwner) {
                airportPickerAdapter.pickAirport(it)

                // if ((airportsSearchField.text?.toString() ?: "null").isBlank()) airportsSearchField.setText(it.ident)
                airportPickerTitle.text =
                    if (workingOnOrig) getString(R.string.origin).toUpperCase(Locale.ROOT)
                    else getString(R.string.destination).toUpperCase(Locale.ROOT)
                @SuppressLint("SetTextI18n")
                icaoIataField.text = "${it.ident} - ${it.iata_code}"
                @SuppressLint("SetTextI18n")
                cityAirportNameField.text = "${it.municipality} - ${it.name}"
                val latString = latToString(it.latitude_deg)
                val lonString = lonToString(it.longitude_deg)
                @SuppressLint("SetTextI18n")
                latLonField.text = "$latString - $lonString"

                //This one should actually be a string resource
                altitudeField.text = getString(R.string.alt_with_placeholder, it.elevation_ft.toString()) // "alt: ${it.elevation_ft}\'"
            }
        }.root
    }

    private fun latToString(latitude: Double): String =
        abs(latitude).toString(2,3) + if (latitude > 0) "N" else "S"

    private fun lonToString(longitude: Double): String =
        abs(longitude).toString(3,3) + if (longitude > 0) "E" else "W"

    private fun Double.toString(minDigitsBeforeDecimal: Int = 3, maxDigitsAfterDecimal: Int = 3): String{
        this.toString().split('.').let{it[0] to it[1]}.apply{
            return "${first.padStart(minDigitsBeforeDecimal, '0')}.${second.take(maxDigitsAfterDecimal)}"
        }
    }
}

