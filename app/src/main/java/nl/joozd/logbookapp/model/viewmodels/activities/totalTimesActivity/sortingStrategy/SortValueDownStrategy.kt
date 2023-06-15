package nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.sortingStrategy

import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.ui.activities.totalTimesActivity.TotalTimesListItem

class SortValueDownStrategy(override val titleRes: Int = R.string.sorter_value_down): SortingStrategy {
    override fun sort(originalList: List<TotalTimesListItem>): List<TotalTimesListItem> =
        originalList.sortedByDescending{ it.numericValue }


}