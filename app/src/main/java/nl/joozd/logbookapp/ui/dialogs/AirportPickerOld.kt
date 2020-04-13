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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.dialog_airports.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.room.Repository
import nl.joozd.logbookapp.data.viewmodel.JoozdlogViewModel
import nl.joozd.logbookapp.extensions.getColorFromAttr
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.ui.adapters.AirportPickerAdapter
import kotlin.math.abs


@Deprecated ("use new AirportPicker")
class AirportPickerOld: androidx.fragment.app.Fragment() {
    private val viewModel: JoozdlogViewModel by viewModels()
    private val repository = Repository.getInstance()
    private val supportFragmentManager: FragmentManager by lazy { requireActivity().supportFragmentManager }

    class AirportSelectedListener(private val f: (airport: Airport) -> Unit){
        fun airportSelected(airport: Airport){
            f(airport)
        }
    }

    var onSelectListener: AirportSelectedListener? = null
    // var airportDb: AirportDb? = null        // to be filled before inflating with an initialized NamesWorker
    var selectedAirportIdent = ""            // holds Airport.ident if filled properly
    private var selectedAirport: Airport? = null
    var airportsList: List<Airport> = emptyList()
    private val airportPickerAdapter = AirportPickerAdapter(airportsList) { airport ->
        selectedAirport = airport
        setPickedFlight(thisView, airport)
    }
    private lateinit var thisView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        thisView = inflater.inflate(R.layout.dialog_airports, container, false)

        if (selectedAirportIdent.isNotEmpty()) {
            lifecycleScope.launch {
                val foundAirport = repository.searchAirport(selectedAirportIdent)
                lifecycleScope.launch(Dispatchers.Main) {
                    setPickedFlight(thisView, foundAirport)
                }
            }
        }


        val airportsDialogTopHalf: ConstraintLayout = thisView.airportsDialogTopHalf
        (airportsDialogTopHalf.background as GradientDrawable).colorFilter = PorterDuffColorFilter(
            requireActivity().getColorFromAttr(android.R.attr.colorPrimary),
            PorterDuff.Mode.SRC_IN
        )

        thisView.airportPickerDialogLayout.setOnClickListener { }

        thisView.airportPickerLayout.setOnClickListener { supportFragmentManager.popBackStack() }

        thisView.airportsPickerList.layoutManager = LinearLayoutManager(context)
        thisView.airportsPickerList.adapter = airportPickerAdapter


        //Search field changed and initial fill:
        var alreadySearching: Boolean = true
        var nextSearchString: String = ""
        var haveOneWaiting: Boolean = false


        lifecycleScope.launch {
            var foundAirports: List<Airport> = repository.requestAllAirports()
            lifecycleScope.launch(Dispatchers.Main) {
                Log.d("airportPicker", "found ${foundAirports.size} airports")
                airportPickerAdapter.updateData(foundAirports)
            }
            while (haveOneWaiting) {
                haveOneWaiting = false
                foundAirports =
                    if (nextSearchString.isNotEmpty()) repository.searchAirports(nextSearchString) else repository.requestAllAirports()
                lifecycleScope.launch(Dispatchers.Main) {
                    airportPickerAdapter.updateData(foundAirports)
                }
            }
            alreadySearching = false
        }



        thisView.airportsSearchField.onTextChanged { t ->
            lifecycleScope.launch {
                if (alreadySearching) {
                    haveOneWaiting = true
                    nextSearchString = t
                } else {

                    alreadySearching = true
                    var foundAirports: List<Airport> = repository.searchAirports(t)
                    lifecycleScope.launch(Dispatchers.Main) {
                        Log.d("airportPicker", "found ${foundAirports.size} airports")
                        airportPickerAdapter.updateData(foundAirports)
                    }
                    while (haveOneWaiting) {
                        haveOneWaiting = false
                        foundAirports =
                            if (nextSearchString.isNotEmpty()) repository.searchAirports(nextSearchString) else repository.requestAllAirports()
                        lifecycleScope.launch(Dispatchers.Main) {
                            airportPickerAdapter.updateData(foundAirports)
                        }
                    }
                    alreadySearching = false
                }
            }
        }



        thisView.setCurrentTextButton.setOnClickListener {
            if (thisView.airportsSearchField.text.toString().isNotEmpty()) {
                selectedAirport = Airport(
                    ident = thisView.airportsSearchField.text.toString(),
                    name = "Custom airport"
                )
                setPickedFlight(thisView, selectedAirport)
            }
        }


        // Save/Cancel onClickListeners:
        thisView.saveAirportDialog.setOnClickListener{
            selectedAirport?.let {
                onSelectListener?.airportSelected(selectedAirport!!)
            }
            supportFragmentManager.popBackStack()
        }

        thisView.cancelAirportDialog.setOnClickListener { supportFragmentManager.popBackStack() }

        return thisView
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
            airportPickerAdapter.pickAirport(airport)
            airportPickerAdapter.notifyDataSetChanged()
        }
    }
}