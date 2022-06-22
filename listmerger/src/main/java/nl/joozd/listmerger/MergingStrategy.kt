package nl.joozd.listmerger

interface MergingStrategy<T> {
    fun mergeItems(otherItem: T, masterItem: T): T
}