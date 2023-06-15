package nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity

import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.sortingStrategy.SortingStrategy
import nl.joozd.logbookapp.model.viewmodels.activities.totalTimesActivity.sortingStrategy.UnsortedStrategy
import nl.joozd.logbookapp.ui.activities.totalTimesActivity.TotalTimesListItem
import nl.joozd.recyclerviewexpandablelistadapter.ItemWithChildren

abstract class TotalTimesItem(
    title: String,
    items: List<TotalTimesListItem>,
    val sortableBy: List<SortingStrategy> = listOf(UnsortedStrategy()),
    isOpen: Boolean = false,
): ItemWithChildren<String, TotalTimesListItem>(title, items, isOpen) {
    private var sorter: SortingStrategy? = sortableBy.firstOrNull()

    override fun onGetChildren(children: List<TotalTimesListItem>): List<TotalTimesListItem> =
        sorter?.sort(children) ?: children

    fun sortBy(sortingStrategy: SortingStrategy){
        sorter = sortingStrategy
        notifyDataChanged()
    }
}