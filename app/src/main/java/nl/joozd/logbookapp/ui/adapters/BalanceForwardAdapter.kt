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

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import nl.joozd.logbookapp.data.dataclasses.BalanceForward
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.ListElementTotalsBinding
import nl.joozd.logbookapp.databinding.ListGroupBalanceForwardBinding
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions.minutesToHoursAndMinutesString

class BalanceForwardAdapter(private val activity: Activity, private var list: List<BalanceForward> = emptyList()): BaseExpandableListAdapter() {
    private val layoutInflater = activity.layoutInflater

    var onDeleteClicked: (balanceForward: BalanceForward) -> Unit = { }
    var onListItemClicked: (balanceForward: BalanceForward, itemID: Int) -> Unit = { _, _ -> }

    fun updateList(bff: List<BalanceForward>){
        list = bff
        notifyDataSetChanged()
    }

    /**
     * Gets the data associated with the given group.
     *
     * @param groupPosition the position of the group
     * @return the data child for the specified group
     */
    override fun getGroup(groupPosition: Int) = list[groupPosition]

    /**
     * Whether the child at the specified position is selectable.
     *
     * @param groupPosition the position of the group that contains the child
     * @param childPosition the position of the child within the group
     * @return whether the child is selectable.
     */
    override fun isChildSelectable(groupPosition: Int, childPosition: Int) = true

    /**
     * Indicates whether the child and group IDs are stable across changes to the
     * underlying data.
     *
     * @return whether or not the same ID always refers to the same object
     * @see BaseExpandableListAdapter.hasStableIds
     */
    override fun hasStableIds(): Boolean = true

    /**
     * Gets a View that displays the given group. This View is only for the
     * group--the Views for the group's children will be fetched using
     * [.getChildView].
     *
     * @param groupPosition the position of the group for which the View is
     * returned
     * @param isExpanded whether the group is expanded or collapsed
     * @param convertView the old view to reuse, if possible. You should check
     * that this view is non-null and of an appropriate type before
     * using. If it is not possible to convert this view to display
     * the correct data, this method can create a new view. It is not
     * guaranteed that the convertView will have been previously
     * created by
     * [.getGroupView].
     * @param parent the parent that this view will eventually be attached to
     * @return the View corresponding to the group at the specified position
     */
    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View = ListGroupBalanceForwardBinding.bind(convertView ?:
        layoutInflater.inflate(R.layout.list_group_balance_forward, parent, false)
    ).apply{
            getGroup(groupPosition).let{bf ->
                // totalsListGroupLayout.setBackgroundColor(context.getColorFromAttr(R.attr.colorPrimaryDark))
                listTitleTextView.text = bf.logbookName
                actionImageView.setOnClickListener {
                    onDeleteClicked(bf)
                }
            }
        }.root


    /**
     * Gets the number of children in a specified group.
     *
     * @param groupPosition the position of the group for which the children
     * count should be returned
     * @return the children count in the specified group
     */
    override fun getChildrenCount(groupPosition: Int): Int =
        getGroup(groupPosition).timesToStringPairs().size

    /**
     * Gets the data associated with the given child within the given group.
     *
     * @param groupPosition the position of the group that the child resides in
     * @param childPosition the position of the child with respect to other
     * children in the group
     * @return the data of the child
     */
    override fun getChild(groupPosition: Int, childPosition: Int) =
        getGroup(groupPosition).timesToStringPairs()[childPosition]

    /**
     * Gets the ID for the group at the given position. This group ID must be
     * unique across groups. The combined ID (see
     * [.getCombinedGroupId]) must be unique across ALL items
     * (groups and all children).
     *
     * @param groupPosition the position of the group for which the ID is wanted
     * @return the ID associated with the group
     */
    override fun getGroupId(groupPosition: Int) = list[groupPosition].id.toLong()

    /**
     * Gets a View that displays the data for the given child within the given
     * group.
     *
     * @param groupPosition the position of the group that contains the child
     * @param childPosition the position of the child (for which the View is
     * returned) within the group
     * @param isLastChild Whether the child is the last child within the group
     * @param convertView the old view to reuse, if possible. You should check
     * that this view is non-null and of an appropriate type before
     * using. If it is not possible to convert this view to display
     * the correct data, this method can create a new view. It is not
     * guaranteed that the convertView will have been previously
     * created by
     * [.getChildView].
     * @param parent the parent that this view will eventually be attached to
     * @return the View corresponding to the child at the specified position
     */
    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View = ListElementTotalsBinding.bind(convertView ?: layoutInflater.inflate(R.layout.list_element_totals, parent, false)).apply{
        getGroup(groupPosition).timesToStringPairs()[childPosition].let{
            listElementNameTextView.text = it.first
            listElementValueTextView.text = it.second
            elementLayout.setOnClickListener {
                onListItemClicked(getGroup(groupPosition), childPosition)
            }
        }
    }.root

    /**
     * Gets the ID for the given child within the given group. This ID must be
     * unique across all children within the group. The combined ID (see
     * [.getCombinedChildId]) must be unique across ALL items
     * (groups and all children).
     *
     * @param groupPosition the position of the group that contains the child
     * @param childPosition the position of the child within the group for which
     * the ID is wanted
     * @return the ID associated with the child
     */
    override fun getChildId(groupPosition: Int, childPosition: Int): Long = childPosition.toLong()

    /**
     * Gets the number of groups.
     *
     * @return the number of groups
     */
    override fun getGroupCount(): Int = list.size

    private fun BalanceForward.timesToStringPairs(): List<Pair<String, String>> = with (activity) { listOf(
        getString(R.string.multipilotTime) to minutesToHoursAndMinutesString(multiPilotTime),
        getString(R.string.totalTimeOfFlight) to minutesToHoursAndMinutesString(aircraftTime),
        getString(R.string.landingDay) to landingDay.toString(),
        getString(R.string.landingNight) to landingNight.toString(),
        getString(R.string.nightTime) to minutesToHoursAndMinutesString(nightTime),
        getString(R.string.ifrTime) to minutesToHoursAndMinutesString(ifrTime),
        getString(R.string.picIncludingPicus) to minutesToHoursAndMinutesString(picTime),
        getString(R.string.copilot) to minutesToHoursAndMinutesString(copilotTime),
        getString(R.string.dualTime) to minutesToHoursAndMinutesString(dualTime),
        getString(R.string.instructorTime) to minutesToHoursAndMinutesString(instructortime),
        getString(R.string.simtTime) to minutesToHoursAndMinutesString(simTime)
    )

        /*val aircraftTime: Int,
        val landingDay: Int,
        val landingNight: Int,
        val nightTime: Int,
        val ifrTime: Int,
        val picTime: Int,
        val copilotTime: Int,
        val dualTime: Int,
        val instructortime: Int,
        val simTime: Int

         */
    }
}