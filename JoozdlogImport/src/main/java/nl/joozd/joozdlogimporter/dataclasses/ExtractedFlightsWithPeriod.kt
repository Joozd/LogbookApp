package nl.joozd.joozdlogimporter.dataclasses

import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.joozdlogimporter.enumclasses.AirportIdentFormat

abstract class ExtractedFlightsWithPeriod(val period: ClosedRange<Long>?,
                                          flights: Collection<BasicFlight>?,
                                          identFormat: AirportIdentFormat
): ExtractedFlights(flights, identFormat) {
}