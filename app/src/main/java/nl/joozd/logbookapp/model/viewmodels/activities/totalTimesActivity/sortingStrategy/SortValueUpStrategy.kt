package nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.sortingStrategy

import nl.joozd.logbookapp.ui.activities.totalTimesActivity.TotalTimesListItem

object SortValueUpStrategy: SortingStrategy {
    override fun sort(originalList: List<TotalTimesListItem>): List<TotalTimesListItem> =
        originalList.sortedBy{ it.numericValue }
}