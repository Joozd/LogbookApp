package nl.joozd.logbookapp.model.helpers

import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.data.miscClasses.Crew
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * To be used as part of viewModels that want to use these functions
 */
object FlightDataPresentationFunctions {
    val flightTimeFormatter = DateTimeFormatter.ofPattern("HH:mm") // should always be in Z
    val flightDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    /**
     * gets a date string from epoch seconds
     */
    fun getDateStringFromEpochSeconds(epochSeconds: Long): String{
        val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
        val time = LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.of("UTC"))
        return time.format(dateFormatter)
    }

    fun getTimestringFromEpochSeconds(epochSeconds: Long): String{
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val time = LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.of("UTC"))
        return time.format(timeFormatter)
    }

    fun minutesToHoursAndMinutesString(minutes: Int): String = "${minutes/60}:${(minutes%60).toString().padStart(2,'0')}"

    /**
     * Make a string like "1:23" into minutes (83 in this case). Also works for "1:23:45.678"
     */
    fun hoursAndMinutesStringToInt(hoursAndMinutes: String): Int? {
        val hoursAndMinutesSplits = hoursAndMinutes.split(*"+- :/.h".toCharArray())
        //check if only digits left
        if (hoursAndMinutesSplits.joinToString("").any{!it.isDigit()}) return null
        val hoursAndMinutesInts = hoursAndMinutesSplits.map{it.toInt()}
        if (hoursAndMinutesInts.size == 1) return hoursAndMinutesInts[0]
        return hoursAndMinutesInts[0]*60 + hoursAndMinutesInts[1]
    }





}