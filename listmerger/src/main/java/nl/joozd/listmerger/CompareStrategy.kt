package nl.joozd.listmerger

interface CompareStrategy<T> {
    /**
     * true if [itemFromOther] is the same as [itemFromMaster].
     * In this case, [itemFromOther] will be removed from the list as it is already in the master list.
     */
    fun isSameItem(itemFromOther: T, itemFromMaster: T): Boolean
}