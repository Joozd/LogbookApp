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

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.core.view.children
import nl.joozd.logbookapp.data.repository.helpers.findSortedHitsForRegistration

class AircraftAutoCompleteAdapter(val context: Context, private val resource: Int, private val textViewID: Int? = null, items: List<Any>? = null): BaseAdapter(), Filterable {
    private val filter = AircraftAutoCompleteAdapterFilter()
    override fun getFilter(): Filter = filter

    private var completeList = items?.map{it.toString()} ?: emptyList<String>()
    private var outputList = emptyList<String>()

    fun setItems(items: List<Any>){
        completeList = items.map{it.toString()}
        notifyDataSetChanged()
    }

    private inner class AircraftAutoCompleteAdapterFilter: Filter(){
        /**
         *
         * Invoked in a worker thread to filter the data according to the
         * constraint. Subclasses must implement this method to perform the
         * filtering operation. Results computed by the filtering operation
         * must be returned as a [android.widget.Filter.FilterResults] that
         * will then be published in the UI thread through
         * [.publishResults].
         *
         *
         * **Contract:** When the constraint is null, the original
         * data must be restored.
         *
         * @param constraint the constraint used to filter the data
         * @return the results of the filtering operation
         *
         * @see .filter
         * @see .publishResults
         * @see android.widget.Filter.FilterResults
         */
        override fun performFiltering(constraint: CharSequence?): FilterResults = FilterResults().apply{
            synchronized(this){
                values = (if (constraint.isNullOrEmpty()) completeList else findSortedHitsForRegistration(constraint.toString(), completeList)).also{
                    outputList = it
                    count = it.size
                }
            }
        }

        /**
         *
         * Invoked in the UI thread to publish the filtering results in the
         * user interface. Subclasses must implement this method to display the
         * results computed in [.performFiltering].
         *
         * @param constraint the constraint used to filter the data
         * @param results the results of the filtering operation
         *
         * @see .filter
         * @see .performFiltering
         * @see android.widget.Filter.FilterResults
         */
        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            @Suppress("UNCHECKED_CAST")
            outputList = (results.values as List<String>)
            if (results.count > 0) {
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }
        }

    }

    /**
     * How many items are in the data set represented by this Adapter.
     *
     * @return Count of items.
     */
    override fun getCount(): Int = outputList.size

    /**
     * Get the data item associated with the specified position in the data set.
     *
     * @param position Position of the item whose data we want within the adapter's
     * data set.
     * @return The data at the specified position.
     */
    override fun getItem(position: Int) = outputList[position]

    /**
     * Get the row id associated with the specified position in the list.
     *
     * @param position The position of the item within the adapter's data set whose row id we want.
     * @return The id of the item at the specified position.
     */
    override fun getItemId(position: Int): Long = position.toLong()

    /**
     * Get a View that displays the data at the specified position in the data set. You can either
     * create a View manually or inflate it from an XML layout file. When the View is inflated, the
     * parent View (GridView, ListView...) will apply default layout parameters unless you use
     * [android.view.LayoutInflater.inflate]
     * to specify a root view and to prevent attachment to the root.
     *
     * @param position The position of the item within the adapter's data set of the item whose view
     * we want.
     * @param convertView The old view to reuse, if possible. Note: You should check that this view
     * is non-null and of an appropriate type before using. If it is not possible to convert
     * this view to display the correct data, this method can create a new view.
     * Heterogeneous lists can specify their number of view types, so that this View is
     * always of the right type (see [.getViewTypeCount] and
     * [.getItemViewType]).
     * @param parent The parent that this view will eventually be attached to
     * @return A View corresponding to the data at the specified position.
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View =
        (convertView ?: LayoutInflater.from(context).inflate(resource, parent, false)).apply {
            val textview: TextView = try {
                when (this) {
                    is TextView -> this
                    is ViewGroup -> textViewID?.let { findViewById(it) } ?: children.toList()
                        .firstOrNull { it is TextView } as TextView?
                    else -> null
                } ?: throw (IllegalStateException("No textview found"))
            } catch (e: ClassCastException) {
                Log.e(
                    this::class.simpleName,
                    "You must provide a textview or a ViewGroup containing a TextView"
                )
                throw(IllegalStateException(
                    "${this::class.simpleName} requires the resource ID to be a TextView",
                    e
                ))
            }
            textview.text = getItem(position)
        }
}