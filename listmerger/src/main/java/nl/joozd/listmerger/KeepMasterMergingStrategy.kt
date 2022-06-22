package nl.joozd.listmerger

class KeepMasterMergingStrategy<T>: MergingStrategy<T> {
    override fun mergeItems(otherItem: T, masterItem: T) = masterItem
}