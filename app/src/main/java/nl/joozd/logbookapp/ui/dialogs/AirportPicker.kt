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

import android.annotation.SuppressLint
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
import kotlinx.android.synthetic.main.dialog_airports.view.*
import kotlinx.coroutines.*

import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.viewmodel.AirportPickerViewModel
import nl.joozd.logbookapp.extensions.getColorFromAttr
import nl.joozd.logbookapp.extensions.nullIfEmpty
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.ui.adapters.AirportPickerAdapter
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment
import java.util.*
import kotlin.math.abs

//TODO: Fix this. Should update though viewModel
class AirportPicker: JoozdlogFragment() {
    private val apViewModel: AirportPickerViewModel by viewModels()
    private val workingOnOrig: Boolean
        get() = viewModel.workingOnOrig == true
    private val airports: List<Airport>
        get() = apViewModel.airports

    private var currentSearchJob: Job? = null
    private var selectedAirport: Airport? = null
    private var airportString: String
        get() = if (workingOnOrig) flight.orig else flight.dest
        set(newAirportString) {
            flight = when (viewModel.workingOnOrig) {
                true -> flight.copy(orig = newAirportString)
                false -> flight.copy(dest = newAirportString)
                null -> error("AirportPicker trying to save airport but viewModel.workingOnOrig == null")
            }
        }

    private val allAirports: List<Airport>
        get() = apViewModel.airports


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_airports, container, false).apply {
            //Set background color for title bar
            (airportsDialogTopHalf.background as GradientDrawable).colorFilter =
                PorterDuffColorFilter(
                    requireActivity().getColorFromAttr(android.R.attr.colorPrimary),
                    PorterDuff.Mode.SRC_IN
                )

            /**
             * Initialize recyclerView and it's stuff
             */
            val airportPickerAdapter = AirportPickerAdapter { airport ->
                airportString = airport.ident
                selectedAirport = airport
            }
            airportsPickerList.layoutManager = LinearLayoutManager(this@AirportPicker.context)
            airportsPickerList.adapter = airportPickerAdapter


            /**
             * Things that this dialog actually does:
             */

            // Update recyclerView while typing
            airportsSearchField.onTextChanged { t ->
                Log.d("${this::class.simpleName}", "starting to search for $t")
                currentSearchJob?.cancel()
                currentSearchJob = launch(Dispatchers.Default) {
                    val result = queryAirportsAsync(t)
                    launch(Dispatchers.Main) {
                        result.await().let {
                            apViewModel.filteredAirports.value = it
                        }
                    }
                }
            }


            /**
             * Set the current text as an airport
             */
            setCurrentTextButton.setOnClickListener {
                airportsSearchField.text.toString().nullIfEmpty()?.let {
                    selectedAirport = Airport(
                        ident = it,
                        name = "Custom airport"
                    ).also { a ->
                        airportString = a.ident
                        airportPickerAdapter.pickAirport(a)
                    }
                }
            }

            /**
             * Save/cancel functions
             */
            //no action is misclicked on window
            airportPickerDialogLayout.setOnClickListener { }

            airportPickerLayout.setOnClickListener {
                viewModel.workingOnOrig = null
                undoAndClose()
            }
            cancelAirportDialog.setOnClickListener {
                viewModel.workingOnOrig = null
                undoAndClose()
            }

            saveAirportDialog.setOnClickListener{
                viewModel.workingOnOrig = null
                saveAndClose()
            }


            /**
             * observers:
             */
            //Update [apViewModel] when data gets loaded
            repository.completeLiveAirports.observe(
                viewLifecycleOwner,
                Observer {apViewModel.airports = it})

            //fill adapter wil complete airports list
            repository.completeLiveAirports.value?.let {airportPickerAdapter.submitList(it)} ?: launch{
                airportPickerAdapter.submitList(apViewModel.filteredAirports.value ?: repository.customAirports.await() + repository.requestAllAirports())
            }

            apViewModel.filteredAirports.observe(viewLifecycleOwner, Observer{airportPickerAdapter.submitList(it)})
        }
    }

    @SuppressLint("SetTextI18n")
    override fun setViews(v: View?) {
        v?.let {notNullView ->
            launch {
                val airport: Airport =
                    (if (selectedAirport?.ident == airportString) selectedAirport
                    else repository.searchAirport(airportString))
                        ?: return@launch // if none found, don't set anything
                with(notNullView) {
                    airportPickerTitle.text  = "${airport.ident} - ${airport.iata_code}"
                    icaoIataField.text = "${airport.ident} - ${airport.iata_code}"
                    cityAirportNameField.text = "${airport.municipality} - ${airport.name}"
                    val latString = "${abs(airport.latitude_deg).toInt().toString().padStart(
                        2,
                        '0'
                    )}.${(airport.latitude_deg % 1).toString().drop(2)
                        .take(3)}${if (airport.latitude_deg > 0) "N" else "S"}"
                    val lonString = "${abs(airport.longitude_deg).toInt().toString().padStart(
                        3,
                        '0'
                    )}.${(airport.longitude_deg % 1).toString().drop(2)
                        .take(3)}${if (airport.longitude_deg > 0) "E" else "W"}"
                    latLonField.text = "$latString - $lonString"
                    altitudeField.text = "alt: ${airport.elevation_ft}\'"
                }

            }
        }
    }

    private suspend fun queryAirportsAsync(query: String): Deferred<List<Airport>> = async(Dispatchers.Default) {
        Log.d("${this::class.simpleName}", "started queryAirports($query)")
        if (query.isEmpty()) return@async airports
        val query = query.toUpperCase(Locale.US)
        val airportsWithout = { without: List<Airport> ->
            if (!isActive) {
                Log.d("${this::class.simpleName}", "search for $query is no longer active")
                emptyList<Airport>()
            }
            airports.filter { it !in without } }
        val result = mutableListOf<Airport>()
        //1a. get matches in ident starting with query
        result.addAll(airports.filter { it.ident.toUpperCase(Locale.US).startsWith(query) })
        Log.d("AirportPicker", "queryAirports($query) step 1a complete")
        //1b. get other matches in ident
        result.addAll(airportsWithout(result).filter { query in it.ident.toUpperCase(Locale.US) })
        Log.d("AirportPicker", "queryAirports($query) step 1b complete")

        //2a. get matches in iata_code starting with query
        result.addAll(airports.filter { it.iata_code.toUpperCase(Locale.US).startsWith(query) })
        Log.d("AirportPicker", "queryAirports($query) step 2a complete")
        //2b. get other matches in iata_code
        result.addAll(airportsWithout(result).filter { query in it.iata_code.toUpperCase(Locale.US) })
        Log.d("AirportPicker", "queryAirports($query) step 2b complete")

        //3a. get matches in municipality starting with query
        result.addAll(airports.filter {
            it.municipality.toUpperCase(Locale.US).startsWith(query)
        })
        Log.d("AirportPicker", "queryAirports($query) step 3a complete")
        //3b. get other matches in municipality
        result.addAll(airportsWithout(result).filter { query in it.municipality.toUpperCase(Locale.US) })
        Log.d("AirportPicker", "queryAirports($query) step 3b complete")

        //4a. get matches in name starting with query
        result.addAll(airports.filter { it.name.toUpperCase(Locale.US).startsWith(query) })
        Log.d("AirportPicker", "queryAirports($query) step 4a complete")
        //4b. get other matches in name
        result.addAll(airportsWithout(result).filter { query in it.name.toUpperCase(Locale.US) })
        Log.d("AirportPicker", "queryAirports($query) step 4b complete")
        Log.d("AirportPicker", "queryAirports($query) isActive = $isActive")

        result.distinct()
    }
}

