package nl.joozd.joozdlogimporter.supportedFileTypes.extractors

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

        val isPic = checkIfIsPic(lines)

        val fixedLines = fixLines(lines) // sometimes notes can break a line in multiple lines. this is fixed here.
        val flightLines = fixedLines.filter { it matches flightLineMatcher }
        val simLines = fixedLines.filter { simLineMatcher.containsMatchIn(it) }
        val flights = flightLines.mapNotNull { makeFlight(it, startDate, isPic) }
        val sims = simLines.mapNotNull { makeSim(it, startDate) }
        return flights + sims
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

    private fun makeFlight(line: String, startDateOfMonth: LocalDate, isPic: Boolean): BasicFlight? = try {
        //Input data should be checked before being passed to this function. Invalid data will return null.
        makeFlightOrThrowException(line.trim(), startDateOfMonth, isPic)
    } catch (e: Throwable){
        null
    }

    // Aircraft type is not known in roster so it will be empty.
    private fun makeSim(line: String, startDateOfMonth: LocalDate): BasicFlight? = try {
        //Input data should be checked before being passed to this function. Invalid data will return null.
        makeSimOrThrowException(line.trim(), startDateOfMonth)
    } catch (e: Throwable){
        null
    }


    private fun makeFlightOrThrowException(line: String, startDateOfMonth: LocalDate, isPic: Boolean): BasicFlight{
    //this function expects a trimmed line
        /*
            examples:
            09 KL 831 PHBHO   05:30 06:31 ICN +9 FO HGH +8 08:55 2:24 46:47
            09 KL 832 PHBHO   12:21 HGH +8 FO ICN +9 14:18 14:48 1:57 9:18 9:18 8:48
            10 KL 868 PHBHF   14:20 15:29 ICN +9 FO AMS +1 05:15 +1 05:45 +1 13:46 15:25 23:32 15:25 14:55
            16 KL 714D PHBVV 17:40 19:11 PBM -3 FO AMS LCI +2 03:36 +1 04:06 +1 8:25 10:26 19:25 10:26 9:56
         */
        val dayOfMonth = line.take(2).toInt()
        val dateOut = startDateOfMonth.withDayOfMonth(dayOfMonth)

        val (flightNumber, registration) = getFlightNumberAndRegistration(line)
        val (tOut, tIn) = getTimeOutAndTimeIn(line, dateOut)
        val (orig, dest) = getOrigAndDest(line)

        return BasicFlight.PROTOTYPE.copy(
            flightNumber = flightNumber,
            timeOut = tOut,
            timeIn = tIn,
            registration = registration,
            orig = orig,
            dest = dest,
            isPIC = isPic,
            isPlanned = false
        )
    }

    private fun makeSimOrThrowException(line: String, startDateOfMonth: LocalDate): BasicFlight{
        val dayOfMonth = line.take(2).toInt() // first two characters in a line are the day of month
        val dateOut = startDateOfMonth.withDayOfMonth(dayOfMonth)
        val tOut = dateOut.atStartOfDay().toInstant(ZoneOffset.UTC).epochSecond
        val duration = 3*60 + 30
        val description = when{
            SIM_TQ_MATCH.containsMatchIn(line) -> "Type Qualification"
            SIM_TQ_EXAM_MATCH.containsMatchIn(line) -> "Skill Test"
            SIM_787_RECURRENT_MATCH.containsMatchIn(line) -> "787 Recurrent"
            SIM_LOE_MATCH.containsMatchIn(line) -> "LOE"
            SIM_TR_MATCH.containsMatchIn(line) -> "Type Recurrent"
            SIM_LPC_MATCH.containsMatchIn(line) -> "LPC/OPC"
            else -> ""
        }
        val type = if(SIM_787_RECURRENT_MATCH.containsMatchIn(line)) "B789" else ""

        return BasicFlight.PROTOTYPE.copy(
            isSim = true,
            timeOut = tOut,
            simTime = duration,
            remarks = description,
            aircraft = type
        )
    }


    //Can throw an exception or produce undefined result when invalid data provided.
    //This function does two things which is meh, but they are closely related (2 strings right next to each other) so I'll be a bad boy and do it like this.
    private fun getFlightNumberAndRegistration(line: String): Pair<String, String> = // flightNumber to reg
        line.words().let { it[1] + it[2] to it[3] }

    //this one also gets two times, as the result of timeOut is needed to get timeIn.
    private fun getTimeOutAndTimeIn(line: String, reportingDate: LocalDate): Pair<Long, Long> { // tOut to tIn; epochSeconds
        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        val tOutMatchResult = tOutRegex.find(line) ?: throw(IllegalArgumentException("No tOut found in $line"))
        val lineAfterTOut = line.drop(tOutMatchResult.range.last)
        //The first time after timeOut is timeIn.
        val tInMatchResult = timeRegex.find(lineAfterTOut) ?: throw(IllegalArgumentException("No tIn found in $lineAfterTOut"))

        val tOut = LocalTime.parse(tOutMatchResult.groupValues[1], formatter)
        val tIn = LocalTime.parse(tInMatchResult.value, formatter)

        // If departure date is after reporting date (e.g. reporting just before midnight UTC), there is a "+1" in tOutMatchResult
        val dateOut = if ("+1" in tOutMatchResult.value) reportingDate.plusDays(1) else reportingDate

        // Flight time is always 0..24 hours. If timeIn is before timeOut, we add a day.
        // There is data in the chrono about that, but this is easier to parse.
        val dateIn = if(tIn > tOut) dateOut else dateOut.plusDays(1)

        val epochSecondOut = tOut.atDate(dateOut).toEpochSecond(ZoneOffset.UTC)
        val epochSecondIn = tIn.atDate(dateIn).toEpochSecond(ZoneOffset.UTC)

        return epochSecondOut to epochSecondIn
    }

    private fun getOrigAndDest(line: String) =
        airportRegex.findAll(line).map { it.value.trim() }.toList()

    private fun checkIfIsPic(lines: List<String>): Boolean {
        val postingLine = lines.firstOrNull{ it.startsWith("Posting") } ?: return false // find line that is like `Posting: FO - 778 Crew Type: Cockpit`
        val posting = postingLine.split(' ').takeIf { it.size > 1 } ?: return false // split line in words. If not at least 2 words in line, return false
        return posting[1] == "CP" // if the second word in this line is "CP", return true, if it is anything else return false
    }

    /*
     * Sometimes a line can get broken in two, for instance on my initial linecheck:
     * 31 KL 714 PHBVS 18:30 20:15 PBM -3 SO AMS LCI,
     * LC
     * +1 04:50 +1 05:20 +1 8:35 10:50 21:12 10:50 10:20
     * This function adds all lines that do not start with two digits and a space (ie. a day-of-month) to the previous line, fixing that.
     */
    private fun fixLines(lines: List<String>): List<String> = buildList{
        var currentLine = ""
        lines.forEach{ line ->
            if (line matches dayLine) {
                add(currentLine)
                currentLine = line
            }
            else{
                currentLine += line
            }
        }
        add(currentLine)
    }

    /**
     * Parses sa string into words. Ignores double whitespaces.
     */
    private fun String.words() = split(' ').filter { it.isNotBlank() }.map { it.trim() }

    private val dayLine = """^\d{2}\s+.*""".toRegex()

    private val periodRegex = """Periode: ($DATE) t/m ($DATE)""".toRegex()
    private val flightLineMatcher = """^\d{2}\s+$FLIGHT_NUMBER\s+$REGISTRATION\s+$TIME.*""".toRegex() // only to check if line is a flight

    // create the matcher for simulator duties
    private val simMatches = arrayOf(SIM_TQ_MATCH, SIM_TQ_EXAM_MATCH, SIM_787_RECURRENT_MATCH, SIM_LOE_MATCH, SIM_TR_MATCH, SIM_LPC_MATCH)
    private val simLineMatcher = simMatches.joinToString(separator = "|").toRegex()


    private val tOutRegex = """($TIME)\s*(?:\+1)?\s*$AIRPORT""".toRegex()
    private val timeRegex = TIME.toRegex()

    private val airportRegex = AIRPORT.toRegex()

    companion object{
        private const val DATE = """\d{2}-\d{2}-\d{4}"""
        private const val TIME = """\d\d:\d\d"""
        private const val REGISTRATION = """[A-Z]{5}"""
        private const val FLIGHT_NUMBER = """KL\s+\d{3,4}[dD]?"""
        private const val AIRPORT = """\s[A-Z]{3}\s"""

        // sim identifiers are all regexes. The check for a digit after 0 or more whitespaces it to filter out instruction.
        private val SIM_TQ_MATCH = """VK - Type Kwalificatie sim\s*\d""".toRegex()
        private val SIM_TQ_EXAM_MATCH = """VXN - Type Kwalificatie\s*\d""".toRegex()
        private val SIM_787_RECURRENT_MATCH = """VT8 - 787 RECURRENT\s*\d""".toRegex()
        private val SIM_LOE_MATCH = """VS?A - LOE\s*\d""".toRegex()
        private val SIM_TR_MATCH = """VT\d - Type recurrent \d\s*\d""".toRegex()
        private val SIM_LPC_MATCH = """VC - OPC / LPC\s*\d""".toRegex()

    }
}
