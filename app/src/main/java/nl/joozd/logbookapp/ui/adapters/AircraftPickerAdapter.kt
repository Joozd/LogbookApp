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

package nl.joozd.logbookapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.ItemPickerAircraftTypeBinding
import nl.joozd.logbookapp.extensions.ctx
import nl.joozd.logbookapp.extensions.getColorFromAttr

/**
 * Adapter for RecyclerView that can highlight one entry
 * Needs
 * @param color: the color of [itemBackground] will be set to this value for [selectedEntry]
 * @param itemLayout: Layout to be used for an item. MUST have a view named [itemBackground]
 * @param itemClick: Action to be performed onClick on an item
 * itemLayout must have a field named [itemBackground]
 */
class AircraftPickerAdapter(
    private var l: List<AircraftType> = emptyList(),
    var color: Int? = null,
    private val itemLayout: Int = R.layout.item_picker_aircraft_type,
    private val itemClick: (AircraftType) -> Unit
): RecyclerView.Adapter<AircraftPickerAdapter.ViewHolder>() {
    var list: List<AircraftType>
        get() = l
        set(it){
            l = it
            selectActiveItem(selectedEntry) // nudge active item so it'll scroll to it
        }

    private var selectedEntry: AircraftType? = null
    private var mRecylerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        mRecylerView = recyclerView
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItem(l[position])
        with(holder.binding) {
            itemBackground.setOnClickListener {
                holder.ac?.let { itemClick(it) }
            }
            itemBackground.setBackgroundColor(if (nameTextView.text.toString() == selectedEntry?.name) color
                ?: itemBackground.ctx.getColorFromAttr(android.R.attr.colorPrimary) else 0x00000000)
        }
    }

    override fun getItemCount(): Int = l.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.ctx).inflate(itemLayout, parent, false)
        return ViewHolder(view)
    }

    fun updateList(l: List<AircraftType>){
        list = l
        notifyDataSetChanged()
    }

    /**
     * Select [activeItem] as ative item, or select nothing as active if null
     */
    fun selectActiveItem(activeItem: AircraftType?) {
        val previousIndex = l.indexOf(selectedEntry)
        val foundIndex = l.indexOf(activeItem)
        println("BANANA $foundIndex on $mRecylerView")
        selectedEntry = activeItem
        if (previousIndex >= 0)
            notifyItemChanged(previousIndex)
        if (foundIndex >= 0) { // if activeItem not found (because it is null, for example), don't notify an invalid item as changed
            notifyItemChanged(foundIndex)
            mRecylerView?.scrollToPosition(foundIndex)
            println("trying to scroll to $foundIndex on $mRecylerView")
        }
    }

    class ViewHolder(containerView: View) :
        RecyclerView.ViewHolder(containerView) {
        val binding = ItemPickerAircraftTypeBinding.bind(containerView)
        private var aircraft: AircraftType? = null
        val ac: AircraftType?
            get() = aircraft
        fun bindItem(ac: AircraftType) {
            aircraft = ac
            binding.nameTextView.text = ac.name
            binding.typeTextView.text = ac.shortName
        }
    }
}