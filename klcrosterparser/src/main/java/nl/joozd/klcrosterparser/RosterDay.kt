package nl.joozd.klcrosterparser

import org.threeten.bp.*

data class RosterDay(val date: LocalDate, val events: List<KlcRosterEvent>, val extraInfo: String) {
    val startOfDay = LocalDateTime.of(date, LocalTime.MIDNIGHT).atZone(ZoneOffset.UTC).toInstant()
    val startOfDayEpochSecond= startOfDay.epochSecond

    val endOfDay = LocalDateTime.of(date.plusDays(1), LocalTime.MIDNIGHT).atZone(ZoneOffset.UTC).toInstant()
    val endOfDayEpochSecond = endOfDay.epochSecond

    override fun toString() = "Date: $date, events: $events, extraInfo: $extraInfo"
}