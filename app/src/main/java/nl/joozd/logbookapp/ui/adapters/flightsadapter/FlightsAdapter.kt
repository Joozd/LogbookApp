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

package nl.joozd.logbookapp.ui.adapters.flightsadapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller

import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.ItemFlightCardBinding
import nl.joozd.logbookapp.databinding.ItemSimBinding
import nl.joozd.logbookapp.extensions.ctx
import nl.joozd.logbookapp.model.dataclasses.DisplayFlight
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.ui.utils.customs.Swiper
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Adapter for RecyclerView for displaying Flights in JoozdLog
 * Needs
 * [itemClick]: Action to be performed onClick on an item
 */

class FlightsAdapter(
    var list: List<Flight> = emptyList()
): RecyclerViewFastScroller.OnPopupTextUpdate, ListAdapter<Flight, RecyclerView.ViewHolder>(
    FlightDiffCallback()) {
    /**
     * Text displayed when fastscrolling using RecyclerViewFastScroller
     */
    override fun onChange(position: Int): CharSequence =
        list[position].dateString()


    override fun getItemViewType(position: Int): Int = if(list[position].isSim) VIEW_TYPE_SIM else VIEW_TYPE_FLIGHT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType){
            VIEW_TYPE_SIM -> SimViewHolder(inflateSimListItemView(parent))
            VIEW_TYPE_FLIGHT -> FlightViewHolder(inflateFlightListItemView(parent))
            else -> error("SelectableStringAdapter error 0001: Type not SIM or FLIGHT")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ListItemViewHolder).bindItem(list[position], itemClick, onDelete)
    }


    private fun inflateFlightListItemView(parent: ViewGroup) =
        LayoutInflater.from(parent.ctx).inflate(R.layout.item_flight_card, parent, false)

    private fun inflateSimListItemView(parent: ViewGroup) =
        LayoutInflater.from(parent.ctx).inflate(R.layout.item_sim, parent, false)

    companion object{
        const val VIEW_TYPE_FLIGHT = 1
        const val VIEW_TYPE_SIM = 2
    }


}
/*
class FlightsAdapter(
    var list: List<DisplayFlight> = emptyList()
): RecyclerViewFastScroller.OnPopupTextUpdate, RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var onDelete: (Int) -> Unit = {}
    var itemClick: (Int) -> Unit = {}

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType){
            SIM -> (holder as SimViewHolder).bindItem(list[position], itemClick, onDelete)
            FLIGHT -> (holder as FlightViewHolder).bindItem(list[position], itemClick, onDelete)
        }
    }

    override fun getItemCount(): Int = list.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType){
            SIM -> SimViewHolder(LayoutInflater.from(parent.ctx).inflate(R.layout.item_sim, parent, false))
            FLIGHT -> FlightViewHolder(LayoutInflater.from(parent.ctx).inflate(R.layout.item_flight_card, parent, false))
            else -> error("SelectableStringAdapter error 0001: Type not SIM or FLIGHT")
        }
    }

    fun updateList(l: List<DisplayFlight>){
        list = l
        notifyDataSetChanged()
    }

    companion object{
        const val FLIGHT = 1
        const val SIM = 2
    }

    /**
     * For popup update
     */
    override fun onChange(position: Int): CharSequence {
        return with (list[position]) { monthAndYear}
    }
}

 */