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

import android.app.Activity
import android.app.Service
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.ListElementTotalsBinding
import nl.joozd.logbookapp.databinding.ListGroupTotalsBinding


class TotalTimesExpandableListAdapter(private val activity: Activity): BaseExpandableListAdapter() {
    constructor(activity: Activity, list: List<TotalTimesList>): this(activity){
        this.list = list.toMutableList()
    }

    private val inflater
        get() = activity.layoutInflater

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
        with (ListGroupTotalsBinding.bind(convertView ?: inflater.inflate(R.layout.list_group_totals, parent, false))){
            getGroup(groupPosition).let{
                listTitleTextView.text = it.title
                if (it.sortableBy == TotalTimesList.NOT_SORTABLE || it.sortableBy == TotalTimesList.ORIGINAL) // of not sortable or only sortable as it comes, hide sort button
                    sortButton.visibility = View.GONE
                else {
                    sortButton.visibility = View.VISIBLE
                    sortButton.setOnClickListener {
                        sortGroup(groupPosition)
                    }
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
        with(ListElementTotalsBinding.bind(convertView ?: inflater.inflate(R.layout.list_element_totals,parent,false))) {
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


    /**
     * This will iterate though available sorting methods until it hits one that it can use.
     * If no sorting available, it will just return
     */
    @Suppress("UNCHECKED_CAST")
    private fun sortGroup(groupPosition: Int){
        val group = list[groupPosition]
        if (group.sortableBy == TotalTimesList.NOT_SORTABLE || group.sortableBy == TotalTimesList.ORIGINAL) return
        when((++sortTracker[groupPosition]) % SORT_VARIATIONS){
            SORT_ORIGINAL -> {
                if (checkIfSortable(group, TotalTimesList.ORIGINAL))
                {
                    _list[groupPosition] = with (group) {
                        GenericTotalTimesList(title, values.sortedBy { it.originalPosition },sortableBy, autoOpen)
                    }
                    notifyDataSetChanged()
                }
                else sortGroup(groupPosition)
            }

            SORT_VALUE_DOWN -> {
                if (checkIfSortable(group, TotalTimesList.VALUE_DOWN)) {
                    _list[groupPosition] = with(group) {
                        GenericTotalTimesList(
                            title,
                            values.sortedBy { it.numericValue as Comparable<Any> }.reversed(),
                            sortableBy,
                            autoOpen
                        )
                    }
                    notifyDataSetChanged()
                }
                else sortGroup(groupPosition)
            }

            SORT_VALUE_UP -> {
                if (checkIfSortable(group, TotalTimesList.VALUE_UP)) {
                    _list[groupPosition] = with(group) {
                        GenericTotalTimesList(
                            title,
                            values.sortedBy { it.numericValue as Comparable<Any> },
                            sortableBy,
                            autoOpen
                        )
                    }
                    notifyDataSetChanged()
                }
                else sortGroup(groupPosition)
            }


            SORT_NAME_UP -> {
                if (checkIfSortable(group, TotalTimesList.NAME_UP)) {
                    _list[groupPosition] = with(group) {
                        GenericTotalTimesList(title, values.sortedBy { it.description }, sortableBy, autoOpen)
                    }
                    notifyDataSetChanged()
                }
                else sortGroup(groupPosition)
            }
            SORT_NAME_DOWN -> {
                if (checkIfSortable(group, TotalTimesList.NAME_DOWN)) {
                    _list[groupPosition] = with(group) {
                        GenericTotalTimesList(title, values.sortedBy { it.description }.reversed(), sortableBy, autoOpen)
                    }
                    notifyDataSetChanged()
                }
                else sortGroup(groupPosition)
            }
        }

    }

    private fun checkIfSortable(group: TotalTimesList, sortType: Long) = (group.sortableBy and sortType) > 0

    companion object{
        private const val SORT_ORIGINAL = 0
        private const val SORT_VALUE_DOWN = 1
        private const val SORT_VALUE_UP = 2
        private const val SORT_NAME_UP = 3
        private const val SORT_NAME_DOWN = 4

        private const val SORT_VARIATIONS = 5
    }
}
