package nl.joozd.joozdlogimporter.supportedFileTypes.extractors

import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.joozdlogimporter.interfaces.CompletedFlightsExtractor
import nl.joozd.joozdlogimporter.interfaces.PlannedFlightsExtractor
import java.time.LocalDate
import java.time.ZoneOffset

class KlmIcaRosterExtractor: PlannedFlightsExtractor {
    override fun getPeriodFromLines(lines: List<String>): ClosedRange<Long>? {
        val dateRegexString = """\d{2}[A-Z]{3}\d{4}"""
        val periodRegex = "CREW ROSTER FROM ($dateRegexString) UPTO ($dateRegexString)".toRegex()
        val result = periodRegex.find(lines.first()) ?: return null
        val dates = result.groupValues.drop(1).map { makeLocalDate(it) }
        return(dates[0].epochSecondAtStartOfDay()..dates[1].epochSecondAtStartOfDay())
    }

    override fun extractFlightsFromLines(lines: List<String>): Collection<BasicFlight>? {
        return null
        //TODO make this
    }


    private fun makeLocalDate(s: String): LocalDate {
        val dd = s.substring(0..1).toInt()
        val mm = s.substring(2..4).getMonth()
        val yy = s.substring(5..8).toInt()
        return LocalDate.of(yy,mm,dd)
    }

    private fun String.getMonth(): Int{
        val months = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
        return months.indexOf(this) + 1
    }

    private fun LocalDate.epochSecondAtStartOfDay() =
        atStartOfDay().toInstant(ZoneOffset.UTC).epochSecond

}