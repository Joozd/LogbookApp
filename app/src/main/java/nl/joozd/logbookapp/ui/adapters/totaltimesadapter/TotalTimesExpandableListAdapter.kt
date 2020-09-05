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

package nl.joozd.logbookapp.ui.adapters.totaltimesadapter

import android.app.Service
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.ListElementTotalsBinding
import nl.joozd.logbookapp.databinding.ListGroupTotalsBinding


class TotalTimesExpandableListAdapter(): BaseExpandableListAdapter() {
    constructor(list: List<TotalTimesList>): this(){
        this.list = list.toMutableList()
    }

    private val context = App.instance.ctx
    private val inflater = context.getSystemService(Service.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private var _list: MutableList<TotalTimesList> = emptyList<TotalTimesList>().toMutableList()
    private var sortTracker = IntArray(_list.size) { 0 }

    var list: List<TotalTimesList>
        get() = _list
        set(it){
            _list = it.toMutableList()
            notifyDataSetChanged()
            updateSortTracker()
        }


    /**
     * Gets the number of groups.
     *
     * @return the number of groups
     */
    override fun getGroupCount(): Int = list.size

    /**
     * Gets the number of children in a specified group.
     *
     * @param groupPosition the position of the group for which the children
     * count should be returned
     * @return the children count in the specified group
     */
    override fun getChildrenCount(groupPosition: Int) = list[groupPosition].values.size

    /**
     * Gets the data associated with the given group.
     *
     * @param groupPosition the position of the group
     * @return the data child for the specified group
     */
    override fun getGroup(groupPosition: Int) = list[groupPosition]

    /**
     * Gets the data associated with the given child within the given group.
     *
     * @param groupPosition the position of the group that the child resides in
     * @param childPosition the position of the child with respect to other
     * children in the group
     * @return the data of the child
     */
    override fun getChild(groupPosition: Int, childPosition: Int) = list[groupPosition].values[childPosition]

    /**
     * Gets the ID for the group at the given position. This group ID must be
     * unique across groups. The combined ID (see
     * [.getCombinedGroupId]) must be unique across ALL items
     * (groups and all children).
     *
     * @param groupPosition the position of the group for which the ID is wanted
     * @return the ID associated with the group
     */
    override fun getGroupId(groupPosition: Int) = groupPosition.toLong()

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
    override fun getChildId(groupPosition: Int, childPosition: Int) = childPosition.toLong()

    /**
     * Indicates whether the child and group IDs are stable across changes to the
     * underlying data.
     *
     * @return whether or not the same ID always refers to the same object
     * @see Adapter.hasStableIds
     */
    override fun hasStableIds() = false

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
    ): View {
        with (ListGroupTotalsBinding.bind(convertView ?: inflater.inflate(R.layout.list_group_balance_forward, parent, false))){
            getGroup(groupPosition).let{
                listTitleTextView.text = it.title
                sortButton.setOnClickListener {
                    sortGroup(groupPosition)
                }
            }
            return root
        }
    }

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
    ): View {
        with(ListElementTotalsBinding.bind(inflater.inflate(R.layout.list_element_totals,parent,false))) {
            getChild(groupPosition, childPosition).let {
                listElementNameTextView.text = it.description
                listElementValueTextView.text = it.value
            }
            return root
        }
    }

    /**
     * Whether the child at the specified position is selectable.
     *
     * @param groupPosition the position of the group that contains the child
     * @param childPosition the position of the child within the group
     * @return whether the child is selectable.
     */
    override fun isChildSelectable(groupPosition: Int, childPosition: Int) = false

    /**
     * This should be called every time list is changed
     */
    private fun updateSortTracker(){
        if (sortTracker.size != list.size) sortTracker = IntArray(_list.size) { 0 }
    }

    private fun sortGroup(groupPosition: Int){
        when((++sortTracker[groupPosition]) % SORT_VARIATIONS){
            SORT_ORIGINAL -> {
                if (list[groupPosition].values.sumBy{it.originalPosition} == 0)
                    sortGroup(groupPosition) // If no original order defined, skip this sorting
                else {
                    _list[groupPosition] = GenericTotalTimesList(
                        list[groupPosition].title,
                        list[groupPosition].values.sortedBy { it.originalPosition })
                    notifyDataSetChanged()
                }
                //Maybe open group again?
            }

            SORT_VALUE_DOWN -> {
                _list[groupPosition] = GenericTotalTimesList(
                    list[groupPosition].title,
                    list[groupPosition].values.sortedBy { it.numericValue }.reversed())
                notifyDataSetChanged()
                //Maybe open group again?
            }

            SORT_VALUE_UP -> {
                _list[groupPosition] = GenericTotalTimesList(
                    list[groupPosition].title,
                    list[groupPosition].values.sortedBy { it.numericValue })
                notifyDataSetChanged()
                //Maybe open group again?
            }


            SORT_NAME -> {
                _list[groupPosition] = GenericTotalTimesList(
                    list[groupPosition].title,
                    list[groupPosition].values.sortedBy { it.description })
                notifyDataSetChanged()
                //Maybe open group again?
            }
        }

    }

    companion object{
        private const val SORT_ORIGINAL = 0
        private const val SORT_VALUE_DOWN = 1
        private const val SORT_VALUE_UP = 2
        private const val SORT_NAME = 3

        private const val SORT_VARIATIONS = 4
    }
}
