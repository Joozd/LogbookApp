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

package nl.joozd.logbookapp.ui.activities.totalTimesActivity


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.ListElementTotalsBinding
import nl.joozd.logbookapp.databinding.ListGroupTotalsBinding
import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.TotalTimesItem
import nl.joozd.recyclerviewexpandablelistadapter.DiffFunctions
import nl.joozd.recyclerviewexpandablelistadapter.RecyclerViewExpandableListAdapter


class TotalTimesExpandableListAdapter:
    RecyclerViewExpandableListAdapter<String, TotalTimesListItem>(DIFF){

    override fun onBindChildViewHolder(holder: ChildViewHolder, item: TotalTimesListItem) {
        ListElementTotalsBinding.bind(holder.itemView).apply{
            listElementNameTextView.text = item.description
            listElementValueTextView.text = item.value
        }
    }

    override fun onBindItemViewHolder(holder: ItemViewHolder, item: String) {
        ListGroupTotalsBinding.bind(holder.itemView).apply{
            listTitleTextView.text = item
            sortButton.setOnClickListener {
                list.firstOrNull { it.parent == item }.let{
                    (it as TotalTimesItem).nextSorter()
                }
            }
        }
    }

    override fun onCreateChildViewHolder(parent: ViewGroup): View =
        LayoutInflater.from(parent.context).inflate(R.layout.list_element_totals, parent, false)


    override fun onCreateItemViewHolder(parent: ViewGroup): View =
        LayoutInflater.from(parent.context).inflate(R.layout.list_group_totals, parent, false)

    object DIFF: DiffFunctions<String, TotalTimesListItem>{
        override fun isSameChild(c1: TotalTimesListItem, c2: TotalTimesListItem): Boolean =
            c1 == c2


        override fun isSameChildWithSameContents(
            c1: TotalTimesListItem,
            c2: TotalTimesListItem
        ): Boolean =
            c1 == c2

        override fun isSameParent(p1: String, p2: String): Boolean =
            p1 == p2

        override fun isSameParentWithSameContents(p1: String, p2: String): Boolean =
            p1 == p2
    }
}