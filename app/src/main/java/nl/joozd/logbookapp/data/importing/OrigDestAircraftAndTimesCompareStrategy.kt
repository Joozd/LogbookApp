package nl.joozd.logbookapp.data.importing

import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.listmerger.CompareStrategy

class OrigDestAircraftAndTimesCompareStrategy: CompareStrategy<BasicFlight> {
    override fun isSameItem(itemFromOther: BasicFlight, itemFromMaster: BasicFlight): Boolean =
        itemFromOther.orig == itemFromMaster.orig
                && itemFromOther.dest == itemFromMaster.dest
                && itemFromOther.registration == itemFromMaster.registration
                && itemFromOther.aircraft == itemFromMaster.aircraft
                && itemFromOther.timeOut == itemFromMaster.timeOut
                && itemFromOther.timeIn == itemFromMaster.timeIn
}