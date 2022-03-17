package nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.sortingStrategy

import nl.joozd.logbookapp.ui.activities.totalTimesActivity.TotalTimesListItem

object SortNameUpStrategy: SortingStrategy {
    override fun sort(originalList: List<TotalTimesListItem>): List<TotalTimesListItem> =
        originalList.sortedBy { it.description }
}