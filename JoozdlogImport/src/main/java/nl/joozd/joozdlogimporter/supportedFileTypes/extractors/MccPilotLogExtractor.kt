/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020-2022 Joost Welle
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

package nl.joozd.joozdlogimporter.supportedFileTypes.extractors

import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.joozdlogimporter.interfaces.CompleteLogbookExtractor
import java.time.*
import java.time.format.DateTimeFormatter

class MccPilotLogExtractor: CompleteLogbookExtractor {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    override fun extractFlightsFromLines(lines: List<String>): Collection<BasicFlight>? {
        if (lines.isEmpty()) return null
        val index = buildIndex(lines) ?: return null
        return lines.drop(1).map{
            makeFlight(it, index) ?: return null
        }
    }

    private fun buildIndex(lines: List<String>): Map<String, Int>? =
        lines.firstOrNull()?.split(';')?.mapIndexed { i, s ->
            s.filter{ it != '\"'}.uppercase() to i
        }?.toMap()

    private fun makeFlight(line: String, index: Map<String, Int>): BasicFlight? =
        try {
            with(line.split(';')) {
                BasicFlight.PROTOTYPE.copy(
                    flightNumber = getFlightNumber(index) ?: return null.also{ println("failed at 1")},
                    orig = getOrig(index) ?: return null.also{ println("failed at 2")},
                    dest = getDest(index) ?: return null.also{ println("failed at 3")},
                    timeOut = makeTimeOut(index) ?: return null.also{ println("failed at 4")},
                    timeIn = makeTimeIn(index) ?: return null.also{ println("failed at 5")},
                    aircraft = getType(index) ?: return null.also{ println("failed at 6")},
                    registration = getRegistration(index) ?: return null.also{ println("failed at 7")},
                    name = getPicName(index) ?: return null.also{ println("failed at 8")},
                    name2 = getOtherNames(index) ?: return null.also{ println("failed at 9")},
                    isPIC = isPic(index) ?: return null.also{ println("failed at 10")},
                    isPICUS = isPicus(index) ?: return null.also{ println("failed at 11")},
                    isCoPilot = isCopilot(index) ?: return null.also{ println("failed at 12")},
                    isDual = isDual(index) ?: return null.also{ println("failed at 13")},
                    isInstructor = isInstructor(index) ?: return null.also{ println("failed at 14")},
                    ifrTime = ifrTime(index) ?: return null.also{ println("failed at 15")},
                    nightTime = nightTime(index) ?: return null.also{ println("failed at 16")},
                    isPF = isPf(index) ?: return null.also{ println("failed at 17")},
                    isSim = isSim(index) ?: return null.also{ println("failed at 18")},
                    takeOffDay = takeoffDay(index) ?: return null.also{ println("failed at 19")},
                    takeOffNight = takeoffNight(index) ?: return null.also{ println("failed at 20")},
                    landingDay = landingDay(index) ?: return null.also{ println("failed at 21")},
                    landingNight = landingNight(index) ?: return null.also{ println("failed at 22")},
                    autoLand = autoLand(index) ?: return null.also{ println("failed at 23")},
                    remarks = getRemarks(index) ?: return null.also{ println("failed at 24")}

                )
            }
        } catch (e: Exception){
            println("Bad data in $line")
            null
        }

    // returns null on bad data.
    private fun List<String>.makeDate(index: Map<String, Int>): LocalDate? = try{
        index[DATE]?.let{ getOrNull(it) }
            ?.split('-')
            ?.map{ it.toInt() }
            ?.let { dmy ->
                if (dmy[0] > 1000) LocalDate.of(dmy[0], dmy[1], dmy[2])
                else LocalDate.of(dmy[2], dmy[1], dmy[0])
            }
    } catch (e: DateTimeException){
        println("Bad data received: ${index[DATE]?.let{ getOrNull(it) }} does not make a correct date.")
        null
    } catch (e: NumberFormatException){
        println("Bad data received: ${index[DATE]?.let{ getOrNull(it) }} is not a valid date string.")
        null
    }

    private fun List<String>.getOrig(index: Map<String, Int>): String? =
        index[ORIG]?.let{ getOrNull(it) }

    private fun List<String>.getDest(index: Map<String, Int>): String? =
        index[DEST]?.let{ getOrNull(it) }

    private fun List<String>.makeTimeOut(index: Map<String, Int>): Long? {
        val s = index[TIME_OUT]?.let{ getOrNull(it) } ?: return null.also { println("error 2")}
        val date = makeDate(index) ?: return null.also { println("error 3")}
        return makeTime(s, date)
    }

    private fun List<String>.makeTimeIn(index: Map<String, Int>): Long? {
        val timeOut = makeTimeOut(index) ?: return null
        val s = index[TIME_IN]?.let{ getOrNull(it) } ?: return null
        val date = makeDate(index) ?: return null
        val t = makeTime(s, date)
        return if (t > timeOut) t else t + ONE_DAY_IN_SECONDS
    }

    private fun makeTime(s: String, date: LocalDate): Long =
        ZonedDateTime.of(
            date,
            LocalTime.parse(s, timeFormatter),
            ZoneOffset.UTC
        ).toEpochSecond()

    private fun List<String>.getFlightNumber(index: Map<String, Int>): String? =
        index[FLIGHT_NUMBER]?.let{ getOrNull(it) }

    private fun List<String>.getType(index: Map<String, Int>): String? =
        index[AC_TYPE]?.let{ getOrNull(it) }

    private fun List<String>.getRegistration(index: Map<String, Int>): String? =
        index[AC_REG]?.let{ getOrNull(it) }

    private fun List<String>.getPicName(index: Map<String, Int>): String? =
        index[NAME1]?.let{ getOrNull(it) }

    private fun List<String>.getOtherNames(index: Map<String, Int>): String? =
            if (index[NAME2] == null || index[NAME3] == null || index[NAME4] == null) null
        else
            listOf(getOrNull(index[NAME2]!!) , getOrNull(index[NAME3]!!), getOrNull(index[NAME4]!!))
                .filter { !it.isNullOrBlank() }
                .joinToString(";")

    private fun List<String>.isPic(index: Map<String, Int>): Boolean? =
        index[TIME_PIC]
            ?.let{getOrNull(it) }
            ?.let{ it.toInt() > 0 }

    private fun List<String>.isPicus(index: Map<String, Int>): Boolean? =
        index[TIME_PICUS]
            ?.let{getOrNull(it) }
            ?.let{ it.toInt() > 0 }

    private fun List<String>.isCopilot(index: Map<String, Int>): Boolean? =
        index[TIME_COPILOT]
            ?.let{getOrNull(it) }
            ?.let{ it.toInt() > 0 }

    private fun List<String>.isDual(index: Map<String, Int>): Boolean? =
        index[TIME_DUAL]
            ?.let{getOrNull(it) }
            ?.let{ it.toInt() > 0 }

    private fun List<String>.isInstructor(index: Map<String, Int>): Boolean? =
        index[TIME_INSTRUCTOR]
            ?.let{getOrNull(it) }
            ?.let{ it.toInt() > 0 }

    private fun List<String>.ifrTime(index: Map<String, Int>): Int? =
        index[TIME_IFR]?.let { getOrNull(it) }?.toInt()

    private fun List<String>.nightTime(index: Map<String, Int>): Int? =
        index[TIME_NIGHT]?.let { getOrNull(it) }?.toInt()

    private fun List<String>.isPf(index: Map<String, Int>): Boolean? =
        index[IS_PF]
            ?.let{ getOrNull(it) }
            ?.let{ it.trim().equals("true", ignoreCase = true) }

    private fun List<String>.isSim(index: Map<String, Int>): Boolean? =
        index[IS_SIM]
            ?.let{ getOrNull(it) }
            ?.isNotBlank()

    private fun List<String>.takeoffDay(index: Map<String, Int>): Int? =
        index[TO_DAY]?.let { getOrNull(it) }?.toInt()

    private fun List<String>.takeoffNight(index: Map<String, Int>): Int? =
        index[TO_NIGHT]?.let { getOrNull(it) }?.toInt()

    private fun List<String>.landingDay(index: Map<String, Int>): Int? =
        index[LDG_DAY]?.let { getOrNull(it) }?.toInt()

    private fun List<String>.landingNight(index: Map<String, Int>): Int? =
        index[LDG_NIGHT]?.let { getOrNull(it) }?.toInt()

    private fun List<String>.autoLand(index: Map<String, Int>): Int? =
        index[AUTOLAND]?.let { getOrNull(it) }?.toInt()


    private fun List<String>.getRemarks(index: Map<String, Int>): String? =
        index[REMARKS]?.let{ getOrNull(it) }



    companion object{
        private const val DATE = "MCC_DATE"
        private const val IS_SIM = "AC_ISSIM"
        private const val FLIGHT_NUMBER = "FLIGHTNUMBER"
        private const val ORIG = "AF_DEP"
        private const val DEST = "AF_ARR"
        private const val TIME_OUT = "TIME_DEP"
        private const val TIME_IN = "TIME_ARR"
        private const val AC_TYPE = "AC_MODEL"
        private const val AC_REG = "AC_REG"
        private const val NAME1 = "PILOT1_NAME"
        private const val NAME2 = "PILOT2_NAME"
        private const val NAME3 = "PILOT3_NAME"
        private const val NAME4 = "PILOT4_NAME"
        private const val TIME_PIC = "TIME_PIC"
        private const val TIME_PICUS = "TIME_PICUS"
        private const val TIME_COPILOT = "TIME_SIC"
        private const val TIME_DUAL = "TIME_DUAL"
        private const val TIME_INSTRUCTOR = "TIME_INSTRUCTOR"
        private const val TIME_NIGHT = "TIME_NIGHT"
        private const val TIME_IFR = "TIME_IFR"
        private const val IS_PF = "PF"
        private const val TO_DAY = "TO_DAY"
        private const val TO_NIGHT = "TO_NIGHT"
        private const val LDG_DAY = "LDG_DAY"
        private const val LDG_NIGHT = "LDG_NIGHT"
        private const val AUTOLAND = "AUTOLAND"
        private const val REMARKS = "REMARKS"

        private const val ONE_DAY_IN_SECONDS = 86400
    }

}