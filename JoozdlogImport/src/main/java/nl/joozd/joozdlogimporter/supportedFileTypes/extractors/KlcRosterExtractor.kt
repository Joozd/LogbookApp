package nl.joozd.joozdlogimporter.supportedFileTypes.extractors

import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.joozdlogimporter.interfaces.PlannedFlightsExtractor
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

class KlcRosterExtractor: PlannedFlightsExtractor {
    private val dateRangeRegEx = """Period: ($DAY_REGEX_STRING) - ($DAY_REGEX_STRING) contract:""".toRegex()
    private val dayRegex = """^(?:Mon|Tue|Wed|Thu|Fri|Sat|Sun)(\d\d).*""".toRegex()
    private val flightRegex = """($CARRIER\s\d+)\sR?\s?([A-Z]{3})\s(\d{4})\s(\d{4})\s([A-Z]{3})\s(\S{3})""".toRegex()
    //eg. KL 1582 BLQ 0400 0600 AMS E90 89113 RI -> KL 1582 BLQ 0400 0600 AMS / rest of line is extraMessage for that event

    override fun getPeriodFromLines(lines: List<String>): ClosedRange<Long>? =
        dateRangeRegEx.find(lines.joinToString("\n"))?.let {
            makePeriodFromRegexResult(it)
        }


    override fun extractFlightsFromLines(lines: List<String>): Collection<BasicFlight>? {
        val period = getPeriodFromLines(lines) ?: return null
        val days: List<List<String>> = buildDaysFromLines(lines)
        return buildFlightsFromDays(days, period)
    }

    // Chop a roster into days
    // This will also result in a bunch of days without flights from the roster's header
    // but as we only get the flights, that is not a problem. We can just ignore this.
    private fun buildDaysFromLines(lines: List<String>): List<List<String>>{
        val days = ArrayList<List<String>>()

        val currentDay = ArrayList<String>()

        val iterator = lines.iterator()
        while (iterator.hasNext()){
            val l = iterator.next()
            if (l matches dayRegex){
                days.add(currentDay.toList())
                currentDay.clear()
            }
            currentDay.add(l)
        }
        days.add(currentDay)
        return days.drop(1) // first entry is just header data or empty
    }

    private fun buildFlightsFromDays(days: List<List<String>>, period: ClosedRange<Long>): List<BasicFlight> =
        days.map { getFlightsFromDay(it, period) }.flatten()

    private fun getFlightsFromDay(day: List<String>, period: ClosedRange<Long>): List<BasicFlight> =
        getDateFromDay(day, period)?.let{ date ->
            day.drop(1).filter{ it matches flightRegex }.map{
                makeFlightFromLine(it, date)
            }
        }?.filterNotNull()
            ?: emptyList()


    private fun getDateFromDay(day:List<String>, period: ClosedRange<Long>): LocalDate? =
        day.firstOrNull()?.let{
            dayRegex.find(it)?.groupValues?.get(1)?.toInt()
        }?.let { dayOfMonth ->
            val daysRange =
                localDateOfEpochSecond(period.start)..localDateOfEpochSecond(period.endInclusive)
            val todayCandidate = daysRange.start.withDayOfMonth(dayOfMonth)

            return if (todayCandidate in daysRange)
                todayCandidate
            else
                todayCandidate.plusMonths(1)
        }

    private fun makeFlightFromLine(line: String, date: LocalDate): BasicFlight? =
        flightRegex.find(line)?.groupValues?.let{ r ->
            val flightNumber = r[INDEX_FLIGHT_NUMBER].filter{ it != ' ' }
            val tOut = date.atTime(makeTime(r[INDEX_TIME_OUT])).toEpochSecond(ZoneOffset.UTC)
            val tIn = date.atTime(makeTime(r[INDEX_TIME_IN])).toEpochSecond(ZoneOffset.UTC)
            val type = aircraftTypes[r[INDEX_AIRCRAFT_TYPE]] ?: ""

            return BasicFlight.PROTOTYPE.copy(
                flightNumber = flightNumber,
                orig = r[INDEX_ORIG],
                dest = r[INDEX_DEST],
                timeOut = tOut,
                timeIn = tIn,
                aircraft = type
            )
    }

    private fun makeTime(timeString: String): LocalTime{
        val t = timeString.toInt()
        val hh = t/100
        val mm = t%100
        return LocalTime.of(hh, mm)
    }


    private fun localDateOfEpochSecond(epochSecond: Long): LocalDate =
        LocalDateTime.ofEpochSecond(epochSecond, 0, ZoneOffset.UTC).toLocalDate()


    private fun makePeriodFromRegexResult(dateRangeResult: MatchResult): ClosedRange<Long>{
        val startEnd = dateRangeResult.groups[1]!!.value to dateRangeResult.groups[2]!!.value
        val startOfRoster = LocalDate.parse(startEnd.first, DateTimeFormatter.ofPattern("ddMMMyy", Locale.US))
            .atStartOfDay()
            .atZone(ZoneOffset.UTC)
            .toInstant()
        val endOfRoster = LocalDate.parse(startEnd.second, DateTimeFormatter.ofPattern("ddMMMyy", Locale.US))
            .plusDays(1)
            .atStartOfDay()
            .atZone(ZoneOffset.UTC)
            .toInstant()
        return startOfRoster.epochSecond..endOfRoster.epochSecond
    }

    companion object{
        private const val DAY_REGEX_STRING = """\d\d[A-Z][a-z]{2}\d\d"""
        private const val CARRIER = "(?:DH/[A-Z]{2}|WA|KL)"

        private const val INDEX_FLIGHT_NUMBER = 1
        private const val INDEX_ORIG = 2
        private const val INDEX_TIME_OUT = 3
        private const val INDEX_TIME_IN = 4
        private const val INDEX_DEST = 5
        private const val INDEX_AIRCRAFT_TYPE = 6

        private val aircraftTypes = mapOf(
            "E75" to "E75L",
            "E90" to "E190",
            "295" to "E295"
        )
    }
}