package nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.sortingStrategy

import nl.joozd.logbookapp.ui.activities.totalTimesActivity.TotalTimesListItem

object SortNameDownStrategy: SortingStrategy {
    override fun sort(originalList: List<TotalTimesListItem>): List<TotalTimesListItem> =
        originalList.sortedByDescending { it.description }
}