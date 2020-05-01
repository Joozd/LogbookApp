package nl.joozd.logbookapp.model.helpers

import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.extensions.atDate
import nl.joozd.logbookapp.utils.TwilightCalculator
import java.time.*

object FlightDataEntryFunctions {
    fun toggleZeroAndOne(value: Int) = if (value > 0) 0 else 1

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
        return this.copy(registration = reg, aircraft = type)
    }

    fun Flight.withTakeoffLandings(landings: Int, orig: Airport?, dest: Airport?): Flight {
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
        return this.copy(
            takeOffDay = takeoffsDuringDay,
            takeOffNight = landings - takeoffsDuringDay,
            landingDay = landingsDuringDay,
            landingNight = landings - landingsDuringDay)
    }
}