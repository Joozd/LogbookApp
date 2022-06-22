package nl.joozd.logbookapp.data.importing

import nl.joozd.listmerger.CompareStrategy
import nl.joozd.logbookapp.model.dataclasses.Flight

class OrigDestAircraftAndTimesCompareStrategy: CompareStrategy<Flight> {
    override fun isSameItem(itemFromOther: Flight, itemFromMaster: Flight): Boolean =
        itemFromOther.orig == itemFromMaster.orig
                && itemFromOther.dest == itemFromMaster.dest
                && itemFromOther.registration == itemFromMaster.registration
                && itemFromOther.aircraftType == itemFromMaster.aircraftType
                && itemFromOther.timeOut == itemFromMaster.timeOut
                && itemFromOther.timeIn == itemFromMaster.timeIn
}