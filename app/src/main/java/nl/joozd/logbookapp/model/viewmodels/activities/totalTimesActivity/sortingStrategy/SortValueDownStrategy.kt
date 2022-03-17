package nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.sortingStrategy

import nl.joozd.logbookapp.ui.activities.totalTimesActivity.TotalTimesListItem

object SortValueDownStrategy: SortingStrategy {
    override fun sort(originalList: List<TotalTimesListItem>): List<TotalTimesListItem> =
        originalList.sortedByDescending{ it.numericValue }.also{
            println("sort value down: $it")
        }

}