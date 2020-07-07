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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.dialog_airports.view.*
import kotlinx.coroutines.*

import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.model.viewmodels.dialogs.AirportPickerViewModel
import nl.joozd.logbookapp.model.viewmodels.MainViewModel
import nl.joozd.logbookapp.extensions.getColorFromAttr
import nl.joozd.logbookapp.extensions.nullIfEmpty
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.ui.adapters.AirportPickerAdapter
import nl.joozd.logbookapp.ui.fragments.JoozdlogFragment
import nl.joozd.logbookapp.ui.utils.longToast
import nl.joozd.logbookapp.model.helpers.FeedbackEvents.AirportPickerEvents.NOT_IMPLEMENTED
import nl.joozd.logbookapp.model.helpers.FeedbackEvents.AirportPickerEvents.ORIG_OR_DEST_NOT_SELECTED
import java.util.*
import kotlin.math.abs

/**
 * Use AirportPicker(orig = true/false)
 * This will set correct value in ViewModel, recreation will call constructor without params
 * but viewModel will persist.
 */
@ExperimentalCoroutinesApi
class AirportPicker(): JoozdlogFragment() {
    private var workingOnOrig: Boolean? = null


     // Constructor with parameter will not be called upon recreation
    constructor(orig: Boolean): this(){
         workingOnOrig = orig
    }
    private val mainViewModel: MainViewModel by activityViewModels()
    private val viewModel: AirportPickerViewModel by viewModels()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_airports, container, false).apply {
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
            viewModel.feedbackEvent.observe(viewLifecycleOwner, Observer{
                //if event already consumed, it.getEvent() == null
                when(it.getEvent()){
                    ORIG_OR_DEST_NOT_SELECTED -> {
                        longToast("AirportPicker error")
                        closeFragment()
                    }
                    NOT_IMPLEMENTED -> longToast("Not Implemented in viewModel")
                }
            })

            //observe airportList for recyclerview
            viewModel.airportsList.observe(viewLifecycleOwner, Observer{
                airportPickerAdapter.submitList(it)
            })

            //set picked airport in adapter:
            //TODO should I do this in here or in viewModel?

            viewModel.pickedAirport.observe(viewLifecycleOwner, Observer{ airport ->
                airportPickerAdapter.pickAirport(airport)
                airportPickerTitle.text  = if (viewModel.workingOnOrig == true) getString(R.string.origin).toUpperCase(Locale.ROOT) else getString(R.string.destination).toUpperCase(Locale.ROOT)
                @SuppressLint("SetTextI18n")
                icaoIataField.text = "${airport.ident} - ${airport.iata_code}"
                @SuppressLint("SetTextI18n")
                cityAirportNameField.text = "${airport.municipality} - ${airport.name}"
                val latString = latToString(airport.latitude_deg)
                val lonString = lonToString(airport.longitude_deg)
                @SuppressLint("SetTextI18n")
                latLonField.text = "$latString - $lonString"
                @SuppressLint("SetTextI18n")
                altitudeField.text = "alt: ${airport.elevation_ft}\'"

            })

        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        workingOnOrig?.let {
            viewModel.setWorkingOnOrig(it)
        }
    }

    private fun latToString(latitude: Double): String =
        "${abs(latitude).toInt().toString()
            .padStart(2,'0')}.${(latitude % 1).toString()
            .drop(2)
            .take(3)}${if (latitude > 0) "N" else "S"}"

    private fun lonToString(longitude: Double): String =
        "${abs(longitude).toInt().toString()
            .padStart(3,'0')}.${(longitude % 1).toString()
            .drop(2)
            .take(3)}${if (longitude > 0) "E" else "W"}"


    /*
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

     */
}

