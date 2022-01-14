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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.ItemPickerDialogBinding
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
class SelectableStringAdapter(
    var list: List<String> = emptyList(),
    var color: Int? = null,
    private val itemLayout: Int = R.layout.item_picker_dialog,
    private val itemClick: (String) -> Unit
): RecyclerView.Adapter<SelectableStringAdapter.ViewHolder>() {
    private var selectedEntry: String? = null

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItem(list[position])
        with(holder.binding) {
            itemBackground.setOnClickListener {
                itemClick(nameTextView.text.toString())
            }
            itemBackground.setBackgroundColor(if (nameTextView.text.toString() == selectedEntry) color
                ?: itemBackground.ctx.getColorFromAttr(android.R.attr.colorPrimary) else 0x00000000)
        }
    }

    override fun getItemCount(): Int = list.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.ctx).inflate(itemLayout, parent, false)
        return ViewHolder(view)
    }

    fun updateList(l: List<String>){
        list = l
        notifyDataSetChanged()
    }

    /**
     * Select [activeItem] as ative item, or select nothing as active if null
     */
    fun selectActiveItem(activeItem: String?) {
        val previousIndex = list.indexOf(selectedEntry)
        val foundIndex = list.indexOf(activeItem)
        selectedEntry = activeItem
        if (previousIndex >= 0)
            notifyItemChanged(previousIndex)
        if (foundIndex >= 0) // if activeItem not found (because it is null, for example), don't notify an invalid item as changed
            notifyItemChanged(foundIndex)
    }

    class ViewHolder(containerView: View) :
        RecyclerView.ViewHolder(containerView) {
        val binding = ItemPickerDialogBinding.bind(containerView)

        fun bindItem(name: String) {
            binding.nameTextView.text = name
        }
    }
}