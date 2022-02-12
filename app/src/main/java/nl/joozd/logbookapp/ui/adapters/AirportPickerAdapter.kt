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

package nl.joozd.logbookapp.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.databinding.ItemAirportPickerBinding
import nl.joozd.logbookapp.extensions.ctx
import nl.joozd.logbookapp.extensions.findActivity
import nl.joozd.logbookapp.extensions.getColorFromAttr

class AirportPickerAdapter(private val itemClick: (Airport) -> Unit)
    : ListAdapter<Pair<Airport, Boolean>, AirportPickerAdapter.APViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): APViewHolder {
        val view = LayoutInflater.from(parent.ctx).inflate(R.layout.item_airport_picker, parent, false)
        return APViewHolder(view, itemClick)
    }

    override fun onBindViewHolder(holder: APViewHolder, position: Int) {
        val airportToPicked = getItem(position)
        val airport = airportToPicked.first
        val picked = airportToPicked.second
        holder.bindAirport(airport)
        with (holder.binding) {
            backgroundLayout.findActivity()?.let {
                if (picked) {
                    backgroundLayout.setBackgroundColor(it.getColorFromAttr(android.R.attr.colorPrimaryDark))
                    identifier.setTextColor(it.getColorFromAttr(android.R.attr.textColorSecondaryInverse))
                    cityName.setTextColor(it.getColorFromAttr(android.R.attr.textColorSecondaryInverse))
                } else {
                    backgroundLayout.setBackgroundColor(0x00000000)
                    identifier.setTextColor(it.getColorFromAttr(android.R.attr.textColorSecondary))
                    cityName.setTextColor(it.getColorFromAttr(android.R.attr.textColorSecondary))
                }
            }
        }
    }

    class APViewHolder(containerView: View, private val itemClick: (Airport) -> Unit) :
        RecyclerView.ViewHolder(containerView) {
        val binding = ItemAirportPickerBinding.bind(containerView)

        @SuppressLint("SetTextI18n")
        fun bindAirport(airport: Airport) {
            with(airport) {
                binding.identifier.text = "$ident - $iata_code"
                binding.cityName.text = "$municipality  - $name"
                itemView.setOnClickListener {
                    itemClick(this)
                }
            }
        }
    }

    companion object{
        private val DIFF_CALLBACK = object: DiffUtil.ItemCallback<Pair<Airport, Boolean>>() {
            override fun areItemsTheSame(oldItem: Pair<Airport, Boolean>, newItem: Pair<Airport, Boolean>): Boolean =
                oldItem.first.ident == newItem.first.ident


            override fun areContentsTheSame(oldItem: Pair<Airport, Boolean>, newItem: Pair<Airport, Boolean>): Boolean =
                oldItem.first == newItem.first && oldItem.second == newItem.second
        }
    }
}
