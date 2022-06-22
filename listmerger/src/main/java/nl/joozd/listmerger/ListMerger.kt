package nl.joozd.listmerger

/**
 * Merge two lists of BasicFlights into one.
 * 
 * @param masterList: all flights in this list will end up in the result no matter what
 *  data in [otherList] can be added to merged list, depending on [compareStrategy]
 *
 * @param otherList: Items in this list will be appended to masterList if they are not the same according [compareStrategy].
 *  Flights that are the same will be merged onto their counterparts in [masterList] according [mergingStrategy]
 */
class ListMerger<T>(
    private val masterList: List<T>,
    private val otherList: List<T>,
    private val compareStrategy: CompareStrategy<T> = ExactMatchCompareStrategy(),
    private val mergingStrategy: MergingStrategy<T> = KeepMasterMergingStrategy(),
    private val idUpdatingStrategy: IDUpdatingStrategy<T> = DontUpdateAnythingIDUpdatingStrategy()
) {

    fun merge(): List<T>{
        val resultList = ArrayList<T>(masterList.size)

        masterList.forEach { masterItem ->
            val matchingOtherItem = otherList.firstOrNull { otherItem -> compareStrategy.isSameItem(otherItem, masterItem) }
            if (matchingOtherItem == null)
                resultList.add(masterItem)
            else resultList.add(mergingStrategy.mergeItems(matchingOtherItem, masterItem))
        }

        val relevantOtherItems = otherList
            .filter { otherItem ->
                resultList.none{ masterItem ->
                    compareStrategy.isSameItem(otherItem, masterItem)
                }
            }.updateIdsIfNeeded()

        return resultList + relevantOtherItems
    }

    private fun List<T>.updateIdsIfNeeded(): List<T> = map { otherItem ->
        if (idUpdatingStrategy.idNeedsUpdating(otherItem))
            idUpdatingStrategy.updateIDForItem(otherItem)
        else otherItem
    }
}