package nl.joozd.logbookapp.data.repository.helpers

import nl.joozd.logbookapp.extensions.toInt
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.TimestampMaker
import java.time.Instant

const val PLANNING_MARGIN = 300 // seconds = 5 minutes. Flights saved with timeIn more than this
                                // amount of time into the future will be marked isPlanned

fun Flight.prepareForSave(): Flight{
    val now = Instant.now().epochSecond
    return this.copy(isPlanned = (timeIn > now + PLANNING_MARGIN).toInt(), timeStamp = TimestampMaker.nowForSycPurposes)
}