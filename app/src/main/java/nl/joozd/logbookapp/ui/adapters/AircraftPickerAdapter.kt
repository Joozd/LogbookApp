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
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_picker_aircraft_type.*
import kotlinx.android.synthetic.main.item_picker_dialog.itemBackground
import kotlinx.android.synthetic.main.item_picker_dialog.nameTextView
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.logbookapp.R
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
    var list: List<AircraftType> = emptyList(),
    var color: Int? = null,
    private val itemLayout: Int = R.layout.item_picker_aircraft_type,
    private val itemClick: (AircraftType) -> Unit
): RecyclerView.Adapter<AircraftPickerAdapter.ViewHolder>() {
    private var selectedEntry: AircraftType? = null

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItem(list[position])
        holder.itemBackground.setOnClickListener {
            holder.ac?.let {itemClick(it)}
        }
        holder.itemBackground.setBackgroundColor(if (holder.nameTextView.text.toString() == selectedEntry?.name) color?: holder.itemBackground.ctx.getColorFromAttr(android.R.attr.colorPrimary) else 0x00000000 )
    }

    override fun getItemCount(): Int = list.size

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
        val previousIndex = list.indexOf(selectedEntry)
        val foundIndex = list.indexOf(activeItem)
        selectedEntry = activeItem
        if (previousIndex >= 0)
            notifyItemChanged(previousIndex)
        if (foundIndex >= 0) // if activeItem not found (because it is null, for example), don't notify an invalid item as changed
            notifyItemChanged(foundIndex)
    }

    class ViewHolder(override val containerView: View) :
        RecyclerView.ViewHolder(containerView),
        LayoutContainer {
        private var aircraft: AircraftType? = null
        val ac: AircraftType?
            get() = aircraft
        fun bindItem(ac: AircraftType) {
            aircraft = ac
            nameTextView.text = ac.name
            typeTextView.text = ac.shortName
        }
    }
}