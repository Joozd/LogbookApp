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
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller

import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.extensions.ctx
import nl.joozd.logbookapp.model.ModelFlight

/**
 * Adapter for RecyclerView for displaying Flights in JoozdLog
 * Needs
 * [itemClick]: Action to be performed onClick on an item
 */

class FlightsAdapter(
    val onDelete: (ModelFlight) -> Unit,
    val itemClick: (ModelFlight) -> Unit
): RecyclerViewFastScroller.OnPopupTextUpdate, ListAdapter<ModelFlight, RecyclerView.ViewHolder>(
    FlightDiffCallback()) {
    private val listListeners = ArrayList<ListListener> ()

            /**
     * Text displayed when fastscrolling using RecyclerViewFastScroller
     */
    override fun onChange(position: Int): CharSequence =
        getItem(position).dateString()


    override fun getItemViewType(position: Int): Int = if(getItem(position).isSim) VIEW_TYPE_SIM else VIEW_TYPE_FLIGHT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType){
            VIEW_TYPE_SIM -> SimViewHolder(inflateSimListItemView(parent))
            VIEW_TYPE_FLIGHT -> FlightViewHolder(inflateFlightListItemView(parent))
            else -> error("SelectableStringAdapter error 0001: Type not SIM or FLIGHT")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as FlightsListItemViewHolder).bindItem(getItem(position), itemClick, onDelete)
    }

    override fun onCurrentListChanged(
        previousList: MutableList<ModelFlight>,
        currentList: MutableList<ModelFlight>
    ) {
        super.onCurrentListChanged(previousList, currentList)
        //copy list so listeners can remove themselves
        listListeners.toList().forEach { listener ->
            listener.onListUpdated()
        }
    }

    fun addListListener(listListener: ListListener){
        listListeners.add(listListener)
    }

    fun removeListListener(listListener: ListListener){
        listListeners.remove(listListener)
    }


    private fun inflateFlightListItemView(parent: ViewGroup) =
        LayoutInflater.from(parent.ctx).inflate(R.layout.item_flight_card, parent, false)

    private fun inflateSimListItemView(parent: ViewGroup) =
        LayoutInflater.from(parent.ctx).inflate(R.layout.item_sim, parent, false)

    fun interface ListListener{
        fun onListUpdated()
    }

    companion object{
        const val VIEW_TYPE_FLIGHT = 1
        const val VIEW_TYPE_SIM = 2
    }


}
