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

package nl.joozd.logbookapp.ui.dialogs.airportPicker

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.lifecycle.lifecycleScope

import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.databinding.DialogAirportsBinding
import nl.joozd.logbookapp.model.viewmodels.dialogs.airportPicker.AirportPickerViewModel

import nl.joozd.logbookapp.extensions.nullIfEmpty
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.model.viewmodels.dialogs.airportPicker.AirportPickerConstants.MAX_RESULT_SIZE
import nl.joozd.logbookapp.ui.adapters.AirportPickerAdapter
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment
import nl.joozd.logbookapp.utils.DispatcherProvider

import kotlin.math.abs

/**
 * Use AirportPicker(orig = true/false)
 * This will set correct value in ViewModel, recreation will call constructor without params
 * but viewModel will persist.
 */
abstract class AirportPicker: JoozdlogFragment() {
    protected abstract val dialogTitle: String
    protected abstract val viewModel: AirportPickerViewModel




    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        DialogAirportsBinding.bind(inflater.inflate(R.layout.dialog_airports, container, false)).apply {
            //touch undo to (lazy) initialize it
            viewModel.undoAirport

            airportPickerTitle.text = dialogTitle

            val airportPickerAdapter = AirportPickerAdapter { airport ->
                viewModel.pickAirport(airport)
            }
            setupAirportsPickerList(airportPickerAdapter)
            setOnTextChangedListeners()
            catchClicksOnBackground()

            //could make a cancel button by saving first value of [airport] in viewModel

            setOnClickListeners()
            launchCollectors(airportPickerAdapter)
        }.root

    private fun DialogAirportsBinding.setOnClickListeners() {
        setCurrentTextButton.setOnClickListener {
            airportsSearchField.text.toString().nullIfEmpty()?.let {
                viewModel.setCustomAirport(it)
            }
        }

        airportDialogSaveTextview.setOnClickListener {
            closeFragment()
        }

        airportDialogCancelTextview.setOnClickListener{
            viewModel.undo()
            closeFragment()
        }
    }

    private fun DialogAirportsBinding.catchClicksOnBackground() {
        headerLayout.setOnClickListener { }
        bodyLayout.setOnClickListener { }
        airportPickerDialogBackground.setOnClickListener { }
    }

    private fun DialogAirportsBinding.setOnTextChangedListeners() {
        airportsSearchField.onTextChanged { t ->
            viewModel.updateSearch(t)
        }
    }

    private fun DialogAirportsBinding.setupAirportsPickerList(
        airportPickerAdapter: AirportPickerAdapter
    ) {
        airportsPickerList.layoutManager = LinearLayoutManager(context)
        airportsPickerList.adapter = airportPickerAdapter
    }

    private fun DialogAirportsBinding.launchCollectors(airportPickerAdapter: AirportPickerAdapter){
        viewModel.pickedAirportFlow.launchCollectWhileLifecycleStateStarted{
            airportsSearchField.setTextIfBlank(it.ident)
            setPickedAircraftBoxData(it)
        }

        viewModel.airportsToIsPickedListFlow.launchCollectWhileLifecycleStateStarted(DispatcherProvider.default()) {
            lifecycleScope.launch(DispatcherProvider.main()) {
                airportPickerAdapter.submitList(it.take(MAX_RESULT_SIZE))
            }
        }
    }

    private fun EditText.setTextIfBlank(t: String) {
        if (text?.isBlank() == true) setText(t)
    }

    @SuppressLint("SetTextI18n")
    private fun DialogAirportsBinding.setPickedAircraftBoxData(ap: Airport) {
        icaoIataField.text = "${ap.ident} - ${ap.iata_code}"
        cityAirportNameField.text = "${ap.municipality} - ${ap.name}"
        val latString = latToString(ap.latitude_deg)
        val lonString = lonToString(ap.longitude_deg)
        latLonField.text = "$latString - $lonString"
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

