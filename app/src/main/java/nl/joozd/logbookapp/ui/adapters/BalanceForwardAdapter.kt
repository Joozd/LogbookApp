/*
 * JoozdLog Pilot's Logbook
 * Copyright (C) 2020 Joost Welle
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see https://www.gnu.org/licenses
 */

package nl.joozd.logbookapp.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import nl.joozd.logbookapp.R
import nl.joozd.joozdlogcommon.BalanceForward
import nl.joozd.logbookapp.data.miscClasses.TotalsListGroup
import nl.joozd.logbookapp.data.miscClasses.TotalsListItem

class BalanceForwardAdapter (private val context: Context, val initialBalancesForward: List<BalanceForward>, val listView: ExpandableListView) : BaseExpandableListAdapter() {

    /**
     * Constructor takes a list of BalanceForwards, but adapter needs a TotalsListGroup
     * This takes care of that:
     */
    var dataList = initialBalancesForward.map { it.toTotalsListGroup() }
    var balancesForward: List<BalanceForward> = initialBalancesForward
    set(b) {
        field = b
        dataList = b.map { it.toTotalsListGroup() }
        this.notifyDataSetChanged()
    }

    /**
     * Listeners for actions when items are clicked
     * (can add more if long clicked etc if needed)
     */
    class OnActionImageViewClicked(private val f: (balanceForward: BalanceForward) -> Unit){
        fun actionImageViewClicked (balanceForward: BalanceForward) {
            f(balanceForward)
        }
    }
    var onActionImageViewClicked: OnActionImageViewClicked? = null
    fun setOnActionImageViewClicked(f: (balanceForward: BalanceForward) -> Unit){
        onActionImageViewClicked = OnActionImageViewClicked(f)
    }
    class OnItemClicked(private val f: (balanceForward: BalanceForward) -> Unit){
        fun itemClicked (balanceForward: BalanceForward) {
            f(balanceForward)
        }
    }
    var onItemClicked: OnItemClicked? = null
    fun setOnItemClicked(f: (balanceForward: BalanceForward) -> Unit){
        onItemClicked = OnItemClicked(f)
    }


    override fun getChild(listPosition: Int, expandedListPosition: Int): Any {
        return this.dataList[listPosition].items[expandedListPosition]
    }

    override fun getChildId(listPosition: Int, expandedListPosition: Int): Long {
        return expandedListPosition.toLong()
    }
    @SuppressLint("SetTextI18n")
    override fun getChildView(listPosition: Int, expandedListPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val expandedListData = getChild(listPosition, expandedListPosition) as TotalsListItem
        if (convertView == null) {
            val layoutInflater = this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = layoutInflater.inflate(R.layout.totals_list_element, null)
        }
        convertView?.let {
            val listElementNameView = it.findViewById<TextView>(R.id.listElementName)
            val listElementValueView = it.findViewById<TextView>(R.id.listElementValue)
            val listElementLayout = it.findViewById<ConstraintLayout>(R.id.elementLayout)

            listElementNameView.text = expandedListData.valueName
            listElementValueView.text =
                "${expandedListData.totalTime / 60}:${(expandedListData.totalTime % 60).toString().padStart(
                    2,
                    '0'
                )}"
            listElementLayout.setOnClickListener {
                onItemClicked?.itemClicked(balancesForward[listPosition])
            }
        }
        return convertView!!
    }

    override fun getChildrenCount(listPosition: Int): Int {
        return this.dataList[listPosition].items.size
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
        var convertView = convertView
        val listTitle = (getGroup(listPosition) as TotalsListGroup).title
        if (convertView == null) {
            val layoutInflater = this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = layoutInflater.inflate(R.layout.totals_list_group, null)
        }
        val listTitleTextView = convertView!!.findViewById<TextView>(R.id.listTitle)
        val actionImageView = convertView!!.findViewById<ImageView>(R.id.actionImageView)

        listTitleTextView.setTypeface(null, Typeface.BOLD)
        listTitleTextView.text = listTitle
        actionImageView.setOnClickListener {
            onActionImageViewClicked?.actionImageViewClicked(balancesForward[listPosition])
        }
        return convertView
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun isChildSelectable(listPosition: Int, expandedListPosition: Int): Boolean {
        return true
    }

    override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
        for (i in 0 until groupCount){
            listView.expandGroup(i)
        }
    }

    private fun BalanceForward.toTotalsListGroup() = TotalsListGroup(
        this.logbookName,
        listOf(
            TotalsListItem("Total Time", this.grandTotal),
            TotalsListItem("Aircraft Time", this.aircraftTime),
            TotalsListItem("Simulator Time", this.simTime),
            TotalsListItem("PIC Time", this.picTime)
        )
    )
}