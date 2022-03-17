package nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity

import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.sortingStrategy.SortingStrategy
import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.sortingStrategy.UnsortedStrategy
import nl.joozd.logbookapp.ui.activities.totalTimesActivity.TotalTimesListItem
import nl.joozd.recyclerviewexpandablelistadapter.ItemWithChildren

abstract class TotalTimesItem(
    title: String,
    items: List<TotalTimesListItem>,
    private val sortableBy: List<SortingStrategy> = listOf(UnsortedStrategy),
    isOpen: Boolean = false,
): ItemWithChildren<String, TotalTimesListItem>(title, items, isOpen) {
    private var currentSorterIndex = 0

    private val sorter: SortingStrategy get() = sortableBy[currentSorterIndex]


    override fun onGetChildren(children: List<TotalTimesListItem>): List<TotalTimesListItem> =
        sorter.sort(children)

    fun nextSorter() {
        if (++currentSorterIndex >= sortableBy.size )
            currentSorterIndex = 0
        notifyDataChanged()
    }

}