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

package nl.joozd.logbookapp.ui.adapters

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_aircraft_picker.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.extensions.ctx
import nl.joozd.logbookapp.extensions.getActivity
import nl.joozd.logbookapp.extensions.getColorFromAttr

class AircraftPickerAdapter(allAircraft: List<Aircraft>, private val itemClick: (Aircraft) -> Unit): RecyclerView.Adapter<AircraftPickerAdapter.ViewHolder>() {
    var allAircraft: List<Aircraft> = allAircraft
    set(acList){
        field = acList
        notifyDataSetChanged()
    }
    private var pickedAircraft = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.ctx).inflate(R.layout.item_aircraft_picker, parent, false)
        return ViewHolder(view, itemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindAircraft(allAircraft[position])
        val activity = holder.backgroundLayout.getActivity()
        activity?.let {
            if (allAircraft[position].registration == pickedAircraft) {
                holder.backgroundLayout.setBackgroundColor(it.getColorFromAttr(android.R.attr.colorPrimaryDark))
                holder.makeModelText.setTextColor(it.getColorFromAttr(android.R.attr.textColorSecondaryInverse))
                holder.airportPickerTitle.setTextColor(it.getColorFromAttr(android.R.attr.textColorSecondaryInverse))
            }
            else {
                holder.backgroundLayout.setBackgroundColor(0x00000000)
                holder.makeModelText.setTextColor(it.getColorFromAttr(android.R.attr.textColorSecondary))
                holder.airportPickerTitle.setTextColor(it.getColorFromAttr(android.R.attr.textColorSecondary))
            }
        }
    }

    override fun getItemCount(): Int = allAircraft.size

    class ViewHolder(override val containerView: View, private val itemClick: (Aircraft) -> Unit) :
        RecyclerView.ViewHolder(containerView),
        LayoutContainer {

        fun bindAircraft(aircraft: Aircraft) {
            with(aircraft) {
                airportPickerTitle.text = registration
                makeModelText.text = "$manufacturer $model"
                itemView.setOnClickListener {
                    itemClick(this)
                }
            }

        }
    }
    fun pickAircraft(ac: Aircraft){
        pickedAircraft = ac.registration
    }
}
