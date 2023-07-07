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
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.ItemPickerDialogBinding
import nl.joozd.logbookapp.extensions.getColorFromAttr

/**
 * Adapter for RecyclerView that can highlight one entry
 * Needs
 * @param color: the color of 'item_background' will be set to this value for entries marked as "true"
 * @param itemLayout: Layout to be used for an item. MUST have a view named 'item_background'
 * @param itemClick: Action to be performed onClick on an item
 */
class SelectableStringAdapter(
    private val color: Int? = null,
    private val itemLayout: Int = R.layout.item_picker_dialog,
    private val itemClick: (String) -> Unit
): ListAdapter<Pair<String, Boolean>, SelectableStringAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bindItem(item, color)
        with(holder.binding) {
            itemBackground.setOnClickListener {
                itemClick(nameTextView.text.toString())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(itemLayout, parent, false)
        return ViewHolder(view)
    }

    class ViewHolder(containerView: View) :
        RecyclerView.ViewHolder(containerView) {
        val binding = ItemPickerDialogBinding.bind(containerView)

        fun bindItem(item: Pair<String, Boolean>, color: Int?) {
            binding.apply {
                nameTextView.text = item.first
                itemBackground.setBackgroundColor(
                    if (item.second)
                        color ?: itemBackground.context.getColorFromAttr(android.R.attr.colorPrimary)
                    else
                        TRANSPARENT
                )
            }
        }
    }

    companion object{
        private val DIFF_CALLBACK = object: DiffUtil.ItemCallback<Pair<String, Boolean>>() {
            override fun areItemsTheSame(oldItem: Pair<String, Boolean>, newItem: Pair<String, Boolean>): Boolean =
                oldItem.first == newItem.first

            override fun areContentsTheSame(oldItem: Pair<String, Boolean>, newItem: Pair<String, Boolean>): Boolean =
                oldItem.first == newItem.first && oldItem.second == newItem.second
        }
    }
}