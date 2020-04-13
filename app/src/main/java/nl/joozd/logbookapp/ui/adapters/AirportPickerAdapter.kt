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

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_airport_picker.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.extensions.ctx
import nl.joozd.logbookapp.extensions.getActivity
import nl.joozd.logbookapp.extensions.getColorFromAttr

class AirportPickerAdapter(private var airports: List<Airport>, private val itemClick: (Airport) -> Unit): RecyclerView.Adapter<AirportPickerAdapter.ViewHolder>() {
    private var pickedAirport = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AirportPickerAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.ctx).inflate(R.layout.item_airport_picker, parent, false)
        return ViewHolder(view, itemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindAirports(airports[position])
        val activity = holder.backgroundLayout.getActivity()
        activity?.let {
            if (airports[position].ident == pickedAirport) {
                holder.backgroundLayout.setBackgroundColor(it.getColorFromAttr(android.R.attr.colorPrimaryDark))
                holder.identifier.setTextColor(it.getColorFromAttr(android.R.attr.textColorSecondaryInverse))
                holder.cityName.setTextColor(it.getColorFromAttr(android.R.attr.textColorSecondaryInverse))
            }
            else{
                holder.backgroundLayout.setBackgroundColor(0x00000000)
                holder.identifier.setTextColor(it.getColorFromAttr(android.R.attr.textColorSecondary))
                holder.cityName.setTextColor(it.getColorFromAttr(android.R.attr.textColorSecondary))
            }
        }

    }

    override fun getItemCount(): Int = airports.size

    fun updateData(newAirports: List<Airport>) {
        airports = newAirports
        this.notifyDataSetChanged()
    }

    class ViewHolder(override val containerView: View, private val itemClick: (Airport) -> Unit) :
        RecyclerView.ViewHolder(containerView),
        LayoutContainer {

        @SuppressLint("SetTextI18n")
        fun bindAirports(airport: Airport) {
            with(airport) {
                identifier.text = "$ident - $iata_code"
                cityName.text = "$municipality  - $name"
                itemView.setOnClickListener {
                    itemClick(this)
                }
            }

        }
    }

    fun pickAirport(airport: Airport){
        pickedAirport = airport.ident
    }
}
