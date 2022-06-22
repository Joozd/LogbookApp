package nl.joozd.listmerger

class ExactMatchCompareStrategy<T>: CompareStrategy<T> {
    //BasicFLight is a data class so we can compare like this
    override fun isSameItem(itemFromOther: T, itemFromMaster: T) =
        itemFromOther == itemFromMaster
}