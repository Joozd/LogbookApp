package nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.sortingStrategy

import nl.joozd.logbookapp.ui.activities.totalTimesActivity.TotalTimesListItem

sealed interface SortingStrategy{
    val titleRes: Int
    fun sort(originalList: List<TotalTimesListItem>): List<TotalTimesListItem>
}