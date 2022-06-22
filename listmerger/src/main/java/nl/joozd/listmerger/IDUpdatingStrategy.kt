package nl.joozd.listmerger

interface IDUpdatingStrategy<T> {
    fun updateIDForItem(item: T): T
}