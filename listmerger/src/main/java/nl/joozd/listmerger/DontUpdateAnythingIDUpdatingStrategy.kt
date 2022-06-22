package nl.joozd.listmerger

class DontUpdateAnythingIDUpdatingStrategy<T>: IDUpdatingStrategy<T> {
    override fun updateIDForItem(item: T) = item
}