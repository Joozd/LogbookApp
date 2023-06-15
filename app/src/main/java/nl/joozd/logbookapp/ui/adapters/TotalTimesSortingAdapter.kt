package nl.joozd.logbookapp.ui.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.TotalTimesItem
import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.sortingStrategy.SortingStrategy

class TotalTimesSortingAdapter(context: Context, totalTimesItem: TotalTimesItem
): ArrayAdapter<SortingStrategy>(context, R.layout.spinner_sorter_item_view, totalTimesItem.sortableBy) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
        super.getView(position, convertView, parent).apply {
            findViewById<TextView>(R.id.text1).visibility = View.GONE
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
        getView(position, convertView, parent).apply {
            val sorter = getItem(position)

            // Customize the appearance of the selected item view
            sorter?.let {
                findViewById<TextView>(R.id.text1).apply {
                    visibility = View.VISIBLE
                    text = context.getString(it.titleRes)
                }
            }
        }
}