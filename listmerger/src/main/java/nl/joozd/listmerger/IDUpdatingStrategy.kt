package nl.joozd.listmerger

interface IDUpdatingStrategy<T> {
    /**
     * Use this function to update an ID from otherList
     */
    fun updateIDForItem(item: T): T

    /**
     * Use this function to determine if an item in otherList will need updating.
     * NOTE Items that were determined to be the same item by [CompareStrategy] (and therefore merged according to [MergingStrategy])
     *  are not checked as they will not end up in the final list.
     */
    fun idNeedsUpdating(item: T): Boolean
}