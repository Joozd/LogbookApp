/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020 Joost Welle
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Affero General Public License as
 *      published by the Free Software Foundation, either version 3 of the
 *      License, or (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Affero General Public License for more details.
 *
 *      You should have received a copy of the GNU Affero General Public License
 *      along with this program.  If not, see https://www.gnu.org/licenses
 *
 */

package nl.joozd.logbookapp.model.helpers

import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.extensions.atDate
import nl.joozd.logbookapp.extensions.nullIfEmpty
import nl.joozd.logbookapp.utils.TwilightCalculator
import java.time.*

object FlightDataEntryFunctions {
    fun toggleTrueAndFalse(value: Boolean) = !value

    fun Flight.withDate(localDate: LocalDate): Flight {
        val tOut = tOut().atDate(localDate)
        val tInToCheck = tIn().atDate(localDate)
        val tIn = if (tInToCheck < tOut)  tInToCheck.plusDays(1) else tInToCheck
        return this.copy(
            timeOut = tOut.toInstant(ZoneOffset.UTC).epochSecond,
            timeIn = tIn.toInstant(ZoneOffset.UTC).epochSecond
        )
    }
    fun Flight.withRegAndType(regAndType:String): Flight {
        require ("(" in regAndType && ")" in regAndType) { "Couldnt find \'(\' and \')\' in $regAndType"}
        val reg = regAndType.slice(0 until regAndType.indexOf('('))
        val type = regAndType.slice((regAndType.indexOf('(') +1) until regAndType.indexOf(')'))
        return this.copy(registration = reg, aircraftType = type)
    }

    /**
     * Updates a flight with takeoff and alndings.
     * If orig and dest given, it calculates whether those are during  day or night.
     * If disableAutoFill given, it disables autofill
     * @param landings: Number of landings
     * @param orig: [Airport] of origin
     * @param dest: [Airport] of destination
     * @param disableAutoFill: [this.autoFill] is set to false if true, kept the way it was if false (default)
     * @return: updated [Flight]
     */
    fun Flight.withTakeoffLandings(landings: Int, orig: Airport?, dest: Airport?, disableAutoFill: Boolean = false): Flight {
        if (orig == null || dest == null){
            return this.copy(
                takeOffDay = landings,
                landingDay = landings)
        }
        val timeOut = LocalDateTime.ofInstant(Instant.ofEpochSecond(timeOut), ZoneId.of("UTC"))
        val timeIn = LocalDateTime.ofInstant(Instant.ofEpochSecond(timeIn), ZoneId.of("UTC"))
        val twilightCalc = TwilightCalculator(timeOut)
        val takeoffsDuringDay = landings * (if (twilightCalc.itIsDayAt(orig, timeOut.toLocalTime())) 1 else 0)
        val landingsDuringDay = landings * (if (twilightCalc.itIsDayAt(dest, timeIn.toLocalTime())) 1 else 0)
        val af = if (disableAutoFill) false else this.autoFill
        return this.copy(
            takeOffDay = takeoffsDuringDay,
            takeOffNight = landings - takeoffsDuringDay,
            landingDay = landingsDuringDay,
            landingNight = landings - landingsDuringDay,
            autoFill = af)
    }

    /**
     * Return a flight with [timeOut] set, and [timeIn] perhaps adjusted to be within 0-1 days later than timeOut
     */
    fun Flight.withTimeOutStringToTime(timeOutString: String): Flight? {
        timeOutString.filter{it.isDigit()}.padStart(4, '0').let{numbersOnlyString -> // numbersOnlyString should now have 4 characters
            if(!validTimestring(numbersOnlyString)) return null
            val hours = numbersOnlyString.take(2).toInt()
            val mins = numbersOnlyString.takeLast(2).toInt()
            //newTimeOut is in [epochSecond]: Long
            val newTimeOut = LocalDateTime.ofInstant(Instant.ofEpochSecond(timeOut), ZoneOffset.UTC)
                .withHour(hours)
                .withMinute(mins)
                .toInstant(ZoneOffset.UTC)
                .epochSecond
            var newTimeIn = timeIn
            //check if timeIn is between 0 seconds and 23:59:59 later than timeOut
            val oneDay = 86400 //86400secs is one day
            while (newTimeIn-newTimeOut < 0) newTimeIn += oneDay        // keep adding days until positive amount of seconds
            while (newTimeIn- newTimeOut > oneDay) newTimeIn -= oneDay  // keep removing days until less than one left
            return this.copy(timeOut = newTimeOut, timeIn = newTimeIn)
        }
    }
    /**
     * Return a flight with [timeIn] set, and perhaps adjusted to be within 0-1 days later than timeOut
     */
    fun Flight.withTimeInStringToTime(timeInString: String): Flight? {
        timeInString.filter{it.isDigit()}.padStart(4, '0').let{ numbersOnlyString -> // numbersOnlyString should now have 4 characters
            if(!validTimestring(numbersOnlyString)) return null
            val hours = numbersOnlyString.take(2).toInt()
            val mins = numbersOnlyString.takeLast(2).toInt()
            //newTimeOut is in [epochSecond]: Long
            var newTimeIn = LocalDateTime.ofInstant(Instant.ofEpochSecond(timeIn), ZoneOffset.UTC)
                .withHour(hours)
                .withMinute(mins)
                .toInstant(ZoneOffset.UTC)
                .epochSecond
            //check if timeIn is between 0 seconds and 23:59:59 later than timeOut
            val oneDay = 86400 //86400secs is one day
            while (newTimeIn-timeOut < 0) newTimeIn += oneDay        // keep adding days until positive amount of seconds
            while (newTimeIn-timeOut > oneDay) newTimeIn -= oneDay  // keep removing days until less than one left
            return this.copy(timeIn = newTimeIn)
        }
    }

    /**
     * Adds a name to name2
     */
    fun Flight.addName2(newName: String): Flight =
        if (name2.isEmpty()) this.copy(name2 = newName)
        else this.copy(name2 = "$name2|$newName")

    fun Flight.removeLastName2(): Flight {
        return if ("|" !in name2) this.copy(name2 = "")
        else {
            val oneNameLess = name2.split("|").dropLast(1).joinToString("|")
            this.copy(name2= oneNameLess)
        }

    }



    fun hoursAndMinutesStringToInt(hoursAndMinutes: String?): Int? {
        if (hoursAndMinutes == null) return null
        if (hoursAndMinutes.all{it.isDigit()}) {
            return if (hoursAndMinutes.length <= 2)
                (hoursAndMinutes.nullIfEmpty() ?: "0").toInt()
            else hoursAndMinutes.takeLast(2).toInt() + hoursAndMinutes.dropLast(2).toInt()*60
        }
        val hoursAndMinutesSplits = hoursAndMinutes.split(*"+- :/.h".toCharArray())
        //check if only digits left
        if (hoursAndMinutesSplits.joinToString("").any{!it.isDigit()}) return null
        val hoursAndMinutesInts = hoursAndMinutesSplits.map{it.toInt()}
        if (hoursAndMinutesInts.size == 1) return hoursAndMinutesInts[0]
        return hoursAndMinutesInts[0]*60 + hoursAndMinutesInts[1]
    }

    private fun validTimestring(ts: String) = when {
        ts.length !=4 -> false
        ts.takeLast(2).toInt() > 59 -> false
        ts.take(2).toInt() > 23 -> false
        else -> true
    }

}