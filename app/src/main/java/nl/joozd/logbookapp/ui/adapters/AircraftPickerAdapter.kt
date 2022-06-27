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


import android.graphics.Color.TRANSPARENT
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.ItemPickerAircraftTypeBinding
import nl.joozd.logbookapp.extensions.getColorFromAttr

/**
 * Needs a List<Pair<AircraftType, Boolean>>
 * Will highlight AircraftTypes that are marked with "true"
 */
class AircraftPickerAdapter(
    private val onListChangedListener: ListChangedListener = ListChangedListener{ },
    private val itemClick: OnCLickListener
): ListAdapter<Pair<AircraftType, Boolean>, AircraftPickerAdapter.ViewHolder>( DIFF_CALLBACK ) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(inflateAircraftTypePickerItem(parent), itemClick)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItem(getItem(position))
    }

    override fun onCurrentListChanged(
        previousList: MutableList<Pair<AircraftType, Boolean>>,
        currentList: MutableList<Pair<AircraftType, Boolean>>
    ) {
        onListChangedListener(currentList)
    }

    private fun inflateAircraftTypePickerItem(parent: ViewGroup) =
        LayoutInflater.from(parent.context).inflate(R.layout.item_picker_aircraft_type, parent, false)

    class ViewHolder(containerView: View, private val itemClick: OnCLickListener) :
        RecyclerView.ViewHolder(containerView) {
        val binding = ItemPickerAircraftTypeBinding.bind(containerView)

        fun bindItem(acToPicked: Pair<AircraftType, Boolean>) {
            val ac = acToPicked.first
            val picked = acToPicked.second

            with(binding) {
                nameTextView.text = ac.name
                typeTextView.text = ac.shortName

                itemView.setOnClickListener { itemClick(ac) }
                itemBackground.setBackgroundColor(
                    if (picked)
                        itemBackground.context.getColorFromAttr(android.R.attr.colorPrimary)
                    else TRANSPARENT
                )
            }
        }
    }

    fun interface OnCLickListener {
        operator fun invoke(aircraftType: AircraftType)
    }

    fun interface ListChangedListener {
        operator fun invoke(newList: List<Pair<AircraftType, Boolean>>)
    }

    companion object{
        private val DIFF_CALLBACK = object: DiffUtil.ItemCallback<Pair<AircraftType, Boolean>>() {
            override fun areItemsTheSame(oldItem: Pair<AircraftType, Boolean>, newItem: Pair<AircraftType, Boolean>): Boolean =
                oldItem.first.name == newItem.first.name

            override fun areContentsTheSame(oldItem: Pair<AircraftType, Boolean>, newItem: Pair<AircraftType, Boolean>): Boolean =
                oldItem.first == newItem.first && oldItem.second == newItem.second
        }
    }
}