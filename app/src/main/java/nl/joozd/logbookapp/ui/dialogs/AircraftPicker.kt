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
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.picker_aircraft.view.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.dataclasses.Aircraft

import nl.joozd.logbookapp.extensions.getColorFromAttr
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.ui.adapters.AircraftPickerAdapter


@Deprecated ("Fix this")
class AircraftPicker : Fragment() {
    companion object{
        const val TAG = "AircraftPicker"
    }
    class FlightSelectedListener (private val f: (aircraft: Aircraft) -> Unit){
        fun flightSelected(aircraft: Aircraft){
            f(aircraft)
        }
    }
    private lateinit var thisView: View
    private var viewReady = false



    var allAircraft = emptyList<Aircraft>() //aircraftDB.requestAllAircraft()
    // var allAircraft: List<Aircraft> = emptyList()
    var setAircraft: Aircraft?
    get(){
        return pickedAircraft
    }
    set(ac){
        pickedAircraft = ac
        if (viewReady){
            ac?.let { ac ->
                setPickedAircraft(thisView, ac)
            }
        }
    }
    private var pickedAircraft: Aircraft? = null
    var flightSelectedListener: FlightSelectedListener? = null
    private val aircraftPickerAdapter = AircraftPickerAdapter(allAircraft) {ac ->
        pickedAircraft = ac
        setPickedAircraft(thisView, ac)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // allAircraft = aircraftDB.requestAllAircraft()
        aircraftPickerAdapter.allAircraft = allAircraft
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        thisView = inflater.inflate(R.layout.picker_aircraft, container,            false)

        (thisView.aircraftPickerTopHalf.background as GradientDrawable).colorFilter = PorterDuffColorFilter(requireActivity().getColorFromAttr(android.R.attr.colorPrimary), PorterDuff.Mode.SRC_IN)

        thisView.aircraftPickerList.layoutManager = LinearLayoutManager(context)
        thisView.aircraftPickerList.adapter = aircraftPickerAdapter

        thisView.aircraftPickerLayout.setOnClickListener { fragmentManager?.popBackStack() }

        thisView.aircraftPickerCancel.setOnClickListener { fragmentManager?.popBackStack() }

        thisView.aircraftPickerOk.setOnClickListener {
            pickedAircraft?.let { flightSelectedListener?.flightSelected(it) }
            fragmentManager?.popBackStack()
        }
        Log.d(TAG, "pickedAircraft is $pickedAircraft")
        pickedAircraft?.let{
            setPickedAircraft(thisView, it)
        }
        thisView.aircraftSearchField.onTextChanged { text ->
            aircraftPickerAdapter.allAircraft =
                if (text.isEmpty()) allAircraft
                else (allAircraft.filter {text.toUpperCase() in it.registration.toUpperCase()}.sortedBy { it.registration } + allAircraft.filter { text.toUpperCase() in it.manufacturer.toUpperCase() || text.toUpperCase() in it.model.toUpperCase()}).distinct()
        }


        viewReady = true
        return thisView
    }
    private fun setPickedAircraft(v: View, ac: Aircraft){
        v.pickedRegistrationText.text = ac.registration
        v.pickedManufacturerTextView.text = ac.manufacturer
        v.pickedModelTextView.text = ac.model
        v.pickedMpSpTextView.text = if(ac.multipilot > 0) "MP" else "SP"
        v.pickedMeSeTextView.text = if(ac.me > 0) "ME" else "SE"
        v.pickedEngineTypeTextView.text = ac.engine_type
        v.pickedIFRTextView.visibility = if (ac.isIfr > 0) View.VISIBLE else View.INVISIBLE
        aircraftPickerAdapter.pickAircraft(ac)
        aircraftPickerAdapter.notifyDataSetChanged()
    }

    fun setAircraft(registration: String){
        val reg = if ('(' in registration) registration.slice(0 until registration.indexOf('(')) else registration
        setAircraft = allAircraft.firstOrNull { it.registration == reg }
    }


}
