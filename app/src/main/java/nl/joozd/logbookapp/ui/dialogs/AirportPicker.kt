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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.dialog_airports.view.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.extensions.getColorFromAttr
import nl.joozd.logbookapp.extensions.nullIfEmpty
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.ui.adapters.AirportPickerAdapter
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment
import kotlin.math.abs

class AirportPicker: JoozdlogFragment() {
    class OnSaveListener(val onSave: (Airport) -> Unit)
    var onSaveListener: OnSaveListener? = null
    fun setOnSaveListener(onSave:  (Airport) -> Unit) { onSaveListener = OnSaveListener(onSave) }

    private var currentSearchJob: Job = Job()
    private var selectedAirport: Airport? = null

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
            val liveAirports = repository.requestLiveAirports()
            val airportPickerAdapter = AirportPickerAdapter(
                (viewModel.distinctLiveCustomAirports.value ?: emptyList()) + (liveAirports.value
                    ?: emptyList())
            ) { airport ->
                selectedAirport = airport
                setPickedFlight(this, airport)
            }
            //Update recyclerView when airport data gets loaded
            viewModel.distinctLiveCustomAirports.observe(
                viewLifecycleOwner,
                Observer {
                    airportPickerAdapter.updateData(it + (liveAirports.value ?: emptyList()))
                })
            liveAirports.observe(
                viewLifecycleOwner,
                Observer {
                    airportPickerAdapter.updateData(
                        (viewModel.distinctLiveCustomAirports.value ?: emptyList()) + it
                    )
                })
            airportsPickerList.layoutManager = LinearLayoutManager(this@AirportPicker.context)
            airportsPickerList.adapter = airportPickerAdapter


            /**
             * Things that this dialog actually does:
             */

            // Update recyclerView while typing
            airportsSearchField.onTextChanged { t ->
                currentSearchJob.cancel()
                launch{
                    currentSearchJob.join()
                    currentSearchJob = launch{
                        val foundAirports: List<Airport> = repository.searchAirports(t)
                        airportPickerAdapter.updateData(foundAirports)
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
                    ).also { selectedAirport ->
                        setPickedFlight(this, selectedAirport)
                        airportPickerAdapter.pickAirport(selectedAirport)
                        airportPickerAdapter.notifyDataSetChanged()
                    }
                }
            }

                /*



                launch {
                    if (alreadySearching) {
                        haveOneWaiting = true
                        nextSearchString = t
                    } else {

                        alreadySearching = true
                        var foundAirports: List<Airport> = repository.searchAirports(t)
                        launch {
                            Log.d("airportPicker", "found ${foundAirports.size} airports")
                            airportPickerAdapter.updateData(foundAirports)
                        }
                        while (haveOneWaiting) {
                            haveOneWaiting = false
                            foundAirports =
                                if (nextSearchString.isNotEmpty()) repository.searchAirports(nextSearchString) else repository.requestAllAirports()
                            launch {
                                airportPickerAdapter.updateData(foundAirports)
                            }
                        }
                        alreadySearching = false
                    }
                }
            }
            */



            /**
             * Save/cancel functions
             */
            //no action is misclicked on window
            airportPickerDialogLayout.setOnClickListener { }

            airportPickerLayout.setOnClickListener {
                supportFragmentManager.popBackStack()
            }
            cancelAirportDialog.setOnClickListener { supportFragmentManager.popBackStack() }

            saveAirportDialog.setOnClickListener{
                selectedAirport?.let { selectedAirport ->
                    onSaveListener?.onSave!!(selectedAirport)
                }
                supportFragmentManager.popBackStack()
            }

        }
    }


    @SuppressLint("SetTextI18n")
    private fun setPickedFlight(view: View, airport: Airport?) {
        airport?.let {airport ->
            with(view) {
                icaoIataField.text = "${airport.ident} - ${airport.iata_code}"
                cityAirportNameField.text = "${airport.municipality} - ${airport.name}"
                val latString = "${abs(airport.latitude_deg).toInt().toString().padStart(
                    2,
                    '0'
                )}.${(airport.latitude_deg % 1).toString().drop(2).take(3)}${if (airport.latitude_deg > 0) "N" else "S"}"
                val lonString = "${abs(airport.longitude_deg).toInt().toString().padStart(
                    3,
                    '0'
                )}.${(airport.longitude_deg % 1).toString().drop(2).take(3)}${if (airport.longitude_deg > 0) "E" else "W"}"
                latLonField.text = "$latString - $lonString"
                altitudeField.text = "alt: ${airport.elevation_ft}\'"
            }
        }
    }
}

