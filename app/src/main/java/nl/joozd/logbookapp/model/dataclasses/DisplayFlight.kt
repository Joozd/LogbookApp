package nl.joozd.logbookapp.model.dataclasses

import nl.joozd.logbookapp.data.miscClasses.Crew
import nl.joozd.logbookapp.extensions.toBoolean
import nl.joozd.logbookapp.extensions.toMonthYear
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions.minutesToHoursAndMinutesString

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
    companion object{
        fun of(f: Flight, icaoIataMap: Map<String, String>, useIATA: Boolean) = DisplayFlight(
            flightID = f.flightID,
            orig = if (useIATA) icaoIataMap[f.orig] ?: f.orig else f.orig,
            dest = if (useIATA) icaoIataMap[f.dest] ?: f.dest else f.dest,
            timeOut = f.timeOutString(),
            timeIn = f.timeInString(),
            totalTime = f.durationString(),
            dateDay = f.tOut().dayOfMonth.toString(),
            monthAndYear = f.tOut().toMonthYear().toUpperCase(),
            simTime = minutesToHoursAndMinutesString(f.simTime),
            registration = f.registration,
            type = f.aircraft,
            names = listOf(f.name, f.name2).filter{it.isNotEmpty()}.joinToString(", "),
            takeoffsAndLandings = "${f.takeoffs()}/${f.landings()}",
            flightNumber = f.flightNumber,
            remarks = f.remarks,
            augmented = Crew.of(f.augmentedCrew).crewSize > 2,
            ifr = f.ifrTime > 0,
            dual = f.isDual.toBoolean(),
            picus = f.isPICUS.toBoolean(),
            pic = f.isPIC.toBoolean(),
            pf = f.isPF.toBoolean(),
            instructor = f.isInstructor.toBoolean(),
            sim = f.isSim.toBoolean(),
            planned = f.isPlanned.toBoolean()
        )
    }
}
