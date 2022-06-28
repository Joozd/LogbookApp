package nl.joozd.joozdlogimporter.supportedFileTypes.extractors

import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.joozdlogimporter.interfaces.CompletedFlightsExtractor

class KlmIcaMonthlyExtractor: CompletedFlightsExtractor {
    override fun getPeriodFromLines(lines: List<String>): ClosedRange<Long>? {
        return null
        //TODO("Not yet implemented")
    }

    override fun extractFlightsFromLines(lines: List<String>): Collection<BasicFlight>? {
        return null
        //TODO("Not yet implemented")
    }
}