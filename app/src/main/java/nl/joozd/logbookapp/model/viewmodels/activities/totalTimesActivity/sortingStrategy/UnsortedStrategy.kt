package nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.sortingStrategy

import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.ui.activities.totalTimesActivity.TotalTimesListItem

class UnsortedStrategy(override val titleRes: Int = R.string.sorter_default) : SortingStrategy {
    override fun sort(originalList: List<TotalTimesListItem>) = originalList

}