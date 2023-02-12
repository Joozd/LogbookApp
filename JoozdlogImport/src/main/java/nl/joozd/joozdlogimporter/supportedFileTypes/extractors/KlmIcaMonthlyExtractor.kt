package nl.joozd.joozdlogimporter.supportedFileTypes.extractors

import android.util.Log
import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.joozdlogimporter.interfaces.CompletedFlightsExtractor
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Note: KLM ICA monthlies do not have aircraft types in them.
 * Fortunately, all KLM ICA aircraft registrations are preloaded into JoozdLog.
 */
class KlmIcaMonthlyExtractor: CompletedFlightsExtractor {
    override fun getPeriodFromLines(lines: List<String>): ClosedRange<Long>? =
        lines.firstOrNull { it.trim() matches periodRegex }?.let { periodLine ->
            getRangeFromPeriodLine(periodRegex.find(periodLine)!!)
        }


    override fun extractFlightsFromLines(lines: List<String>): Collection<BasicFlight>? {
        val startDate = getStartDateFromLines(lines) ?: return null // a monthly is only one month, so all dates are this date withDayOfMonth(day)
        val flightLines = lines.filter { it matches flightLineMatcher }
        return flightLines.mapNotNull { makeFlight(it, startDate) }
    }

    private fun getStartDateFromLines(lines: List<String>): LocalDate? =
        getPeriodFromLines(lines)?.start?.let { LocalDateTime.ofEpochSecond(it, 0, ZoneOffset.UTC).toLocalDate() }


    private fun getRangeFromPeriodLine(matchResult: MatchResult): ClosedRange<Long>{
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val (start,end) = matchResult.destructured
        val startInstant = LocalDate.parse(start, formatter).atStartOfDay().toEpochSecond(ZoneOffset.UTC)
        val endInstant = LocalDate.parse(end, formatter).plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC) // plus one at start of day == at end of day
        return startInstant..endInstant
    }

    private fun makeFlight(line: String, startDateOfMonth: LocalDate): BasicFlight? = try {
        //Input data should be checked before being passed to this function. Invalid data will return null.
        makeFlightOrThrowException(line.trim(), startDateOfMonth)
    } catch (e: Throwable){
        Log.w("KlmIcaMonthlyExtractor", "parsing bad line: $line")
        null
    }


    private fun makeFlightOrThrowException(line: String, startDateOfMonth: LocalDate): BasicFlight{
    //this function expects a trimmed line
        /*
            examples:
            09 KL 831 PHBHO   05:30 06:31 ICN +9 FO HGH +8 08:55 2:24 46:47
            09 KL 832 PHBHO   12:21 HGH +8 FO ICN +9 14:18 14:48 1:57 9:18 9:18 8:48
            10 KL 868 PHBHF   14:20 15:29 ICN +9 FO AMS +1 05:15 +1 05:45 +1 13:46 15:25 23:32 15:25 14:55
         */
        val dayOfMonth = line.take(2).toInt()
        val (flightNumber, registration) = getFlightNumberAndRegistration(line)
        val (ltOut, ltIn) = getTimeOutAndTimeIn(line)
        val (orig, dest) = getOrigAndDest(line)

        val dateOut = startDateOfMonth.withDayOfMonth(dayOfMonth)
        val tOut = ltOut.atDate(dateOut).toEpochSecond(ZoneOffset.UTC)

        val dateIn = if (ltIn > ltOut) dateOut else dateOut.plusDays(1)
        val tIn = ltIn.atDate(dateIn).toEpochSecond(ZoneOffset.UTC)

        return BasicFlight.PROTOTYPE.copy(
            flightNumber = flightNumber,
            timeOut = tOut,
            timeIn = tIn,
            registration = registration,
            orig = orig,
            dest = dest,
            isPlanned = false
        )
    }


    //Can throw an exception or produce undefined result when invalid data provided.
    //This function does two things which is meh, but they are closely related (2 strings right next to each other) so I'll be a bad boy and do it like this.
    private fun getFlightNumberAndRegistration(line: String): Pair<String, String> { // flightNumber to reg
        val flightNumberBuilder = StringBuilder().apply{ append("KL") }
        val registrationBuilder = StringBuilder()
        val l = line.drop(6).trim() // get rid of date, KL and any whitespaces after KL
        var pointer = 0

        //add flightnumber digits to flightnumber:
        while(l[pointer].isDigit()) // not checking because this expects a valid flightline.
            flightNumberBuilder.append(pointer++)

        // skip spaces after flightnumber.
        while(l[pointer] == ' ') pointer++

        // registration comes after flightnumber, get all characters until next whitespace:
        while(l[pointer] != ' ')
            registrationBuilder.append(l[pointer++])

        return flightNumberBuilder.toString() to registrationBuilder.toString()
    }

    //this one also gets two times, as the result of timeOut is needed to get timeIn.
    private fun getTimeOutAndTimeIn(line: String): Pair<LocalTime, LocalTime> { // tOut to tIn
        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        val tOutMatchResult = tOutRegex.find(line) ?: throw(IllegalArgumentException("No tOut found in $line"))
        val lineAfterTOut = line.drop(tOutMatchResult.range.last)
        //The first time after timeOut is timeIn.
        val tInMatchResult = timeRegex.find(lineAfterTOut) ?: throw(IllegalArgumentException("No tIn found in $lineAfterTOut"))

        val tOut = LocalTime.parse(tOutMatchResult.groupValues[1], formatter)
        val tIn = LocalTime.parse(tInMatchResult.value, formatter)

        return tOut to tIn
    }

    private fun getOrigAndDest(line: String) =
        airportRegex.findAll(line).map { it.value.trim() }.toList()


    private val periodRegex = """Periode: ($DATE) t/m ($DATE)""".toRegex()
    private val flightLineMatcher = """^\d{2}\s+$FLIGHT_NUMBER\s+$REGISTRATION\s+$TIME.*""".toRegex() // only to check if line is a flight


    private val tOutRegex = """($TIME)\s*$AIRPORT""".toRegex()
    private val timeRegex = TIME.toRegex()

    private val airportRegex = AIRPORT.toRegex()

    companion object{
        private const val DATE = """\d{2}-\d{2}-\d{4}"""
        private const val TIME = """\d\d:\d\d"""
        private const val REGISTRATION = """[A-Z]{5}"""
        private const val FLIGHT_NUMBER = """KL\s+\d{3,4}[dD]?"""
        private const val AIRPORT = """\s[A-Z]{3}\s"""
    }
}
