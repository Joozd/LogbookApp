package nl.joozd.listmerger

/**
 * Merge two lists of BasicFlights into one.
 * 
 * @param masterList: all flights in this list will end up in the result no matter what
 *  data in [otherList] can be added to merged list, depending on [compareStrategy]
 *
 * @param otherList: Items in this list will be merged into masterList if they are not the same according [compareStrategy].
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
        val itemsOnlyInMaster: List<T> = masterList.filter{ master -> otherList.none { other -> compareStrategy.isSameItem(other, master) } }
        val itemsOnlyInOtherList = ArrayList<T>()
        val mergedItems = ArrayList<T>()

        otherList.forEach{ other ->
            val matchingMasterItem = masterList.firstOrNull { master -> compareStrategy.isSameItem(other, master) }
            if (matchingMasterItem == null) itemsOnlyInOtherList.add(idUpdatingStrategy.updateIDForItem(other))
            else mergedItems.add(mergingStrategy.mergeItems(other, matchingMasterItem))
        }

        return itemsOnlyInMaster + mergedItems + itemsOnlyInOtherList
    }
}