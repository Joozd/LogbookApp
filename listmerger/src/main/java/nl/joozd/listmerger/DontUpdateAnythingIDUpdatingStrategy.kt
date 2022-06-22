package nl.joozd.listmerger

class DontUpdateAnythingIDUpdatingStrategy<T>: IDUpdatingStrategy<T> {
    override fun updateIDForItem(item: T) = item
    override fun idNeedsUpdating(item: T): Boolean = false
}