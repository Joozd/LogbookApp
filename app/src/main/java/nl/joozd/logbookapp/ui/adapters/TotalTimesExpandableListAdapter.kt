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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import kotlinx.android.synthetic.main.totals_list_element.view.*
import kotlinx.android.synthetic.main.totals_list_group.view.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.miscClasses.TotalsListGroup
import nl.joozd.logbookapp.data.miscClasses.TotalsListItem


class TotalTimesExpandableListAdapter (private val context: Context, private var dataList: List<TotalsListGroup>) : BaseExpandableListAdapter() {
    companion object{
        const val TAG = "TotalTimesExpandableListAdapter"
    }

    override fun getChild(listPosition: Int, expandedListPosition: Int): Any {
        return dataList[listPosition].items[expandedListPosition]
    }

    override fun getChildId(listPosition: Int, expandedListPosition: Int): Long {
        return expandedListPosition.toLong()
    }


    override fun getChildView(listPosition: Int, expandedListPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup): View {
        return (convertView ?: LayoutInflater.from(context).inflate(R.layout.totals_list_element, parent)).apply {
            val expandedListData = getChild(listPosition, expandedListPosition) as TotalsListItem

            listElementNameTextView.text = expandedListData.valueName
            @SuppressLint("SetTextI18n")
            listElementValueTextView.text =
                "${expandedListData.totalTime / 60}:${(expandedListData.totalTime % 60).toString()
                    .padStart(2, '0')}"
        }
    }

    override fun getChildrenCount(listPosition: Int): Int {
        return 0
        //return this.dataList[listPosition].items.size
    }

    override fun getGroup(listPosition: Int): Any {
        return this.dataList[listPosition]
    }

    override fun getGroupCount(): Int {
        return this.dataList.size
    }

    override fun getGroupId(listPosition: Int): Long {
        return listPosition.toLong()
    }

    override fun getGroupView(listPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup): View {
        return (convertView ?: LayoutInflater.from(context).inflate(R.layout.totals_list_group, parent)).apply {
            val listTitle = (getGroup(listPosition) as TotalsListGroup).title
            listTitleTextView.setTypeface(null, Typeface.BOLD)
            listTitleTextView.text = listTitle
        }
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun isChildSelectable(listPosition: Int, expandedListPosition: Int): Boolean {
        return false
    }

    fun updateData(newData: List<TotalsListGroup>){
        dataList = newData
        notifyDataSetChanged() // maybe make this better in the future so not whole list gets updated, ListAdapter or something.
    }
}
