package nl.joozd.logbookapp.data.importing.merging


import nl.joozd.listmerger.MergingStrategy
import nl.joozd.logbookapp.model.dataclasses.Flight

class MergeOntoMergingStrategy: MergingStrategy<Flight> {
    override fun mergeItems(otherItem: Flight, masterItem: Flight): Flight =
        masterItem.mergeOnto(otherItem, keepIdOfFlightOnDevice = false)
}