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

package nl.joozd.logbookapp.model.dataclasses

import android.util.Log
import nl.joozd.logbookapp.data.miscClasses.crew.Crew
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.nullIfEmpty
import nl.joozd.logbookapp.extensions.toMonthYear
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions.minutesToHoursAndMinutesString
import java.util.*

/**
 * CLass with only a flightID and strings and booleans for display in MainActivity RecyclerView
 */
data class DisplayFlight(
    val flightID: Int,
    val orig: String = "",
    val dest: String = "",
    val timeOut: String = "",
    val timeIn: String = "",
    val totalTime: String = "",
    val dateDay: String = "32",
    val monthAndYear: String = "jan 2020",
    val nightTime: String = "",
    val simTime: String = "",
    val registration: String = "",
    val type: String = "",
    val names: String = "",
    val takeoffsAndLandings: String = "",
    val flightNumber: String = "",
    val remarks: String = "",
    val augmented: Boolean = false,
    val ifr: Boolean = false,
    val dual: Boolean = false,
    val picus: Boolean = false,
    val pic: Boolean = false,
    val pf: Boolean = false,
    val instructor: Boolean = false,
    val sim: Boolean = false,
    val planned: Boolean = false
){
    val aircraftTextMerged = listOf(registration, type).filter{it.isNotBlank()}.joinToString(" - ")

    /**
     * Returns false if any of the checked data points is not filled
     */
    fun checkIfIncomplete(checkNames: Boolean = Preferences.picNameNeedsToBeSet): Boolean =
        orig.isBlank()
                || dest.isBlank()
                || type.isBlank()
                || registration.isBlank()
                || (checkNames && names.isBlank())


    companion object{
        fun of(f: Flight, icaoIataMap: Map<String, String>, useIATA: Boolean) = DisplayFlight(
            flightID = f.flightID,
            orig = if (useIATA) icaoIataMap[f.orig]?.nullIfEmpty() ?: f.orig else f.orig,
            dest = if (useIATA) icaoIataMap[f.dest]?.nullIfEmpty() ?: f.dest else f.dest,
            timeOut = f.timeOutString(),
            timeIn = f.timeInString(),
            totalTime = f.durationString(),
            dateDay = f.tOut().dayOfMonth.toString(),
            monthAndYear = f.tOut().toMonthYear().toUpperCase(Locale.ROOT),
            simTime = minutesToHoursAndMinutesString(f.simTime),
            registration = f.registration,
            type = f.aircraftType,
            names = buildNames(f.name, f.name2),
            takeoffsAndLandings = "${f.takeoffs()}/${f.landings()}",
            flightNumber = f.flightNumber,
            remarks = f.remarks,
            augmented = Crew.of(f.augmentedCrew).size > 2,
            ifr = f.ifrTime > 0,
            dual = f.isDual,
            picus = f.isPICUS,
            pic = f.isPIC,
            pf = f.isPF,
            instructor = f.isInstructor,
            sim = f.isSim,
            planned = f.isPlanned
        )
        private fun buildNames(vararg names: String): String{
            val nn  = names.filter{it.isNotBlank()}.map{
                if (';' in it) it.split(';').map{it.trim()} else listOf(it)
            }.flatten()
            return if (nn.size <=2) nn.joinToString(", ")
            else nn.take(2).joinToString(", ") + " + ${nn.drop(2).size}"
        }
    }
}
