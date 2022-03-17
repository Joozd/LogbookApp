package nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.sortingStrategy

import nl.joozd.logbookapp.ui.activities.totalTimesActivity.TotalTimesListItem

object UnsortedStrategy : SortingStrategy {
    override fun sort(originalList: List<TotalTimesListItem>) = originalList
}