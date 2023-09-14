package nl.joozd.pdflogbookbuilder

import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.pdflogbookbuilder.extensions.totalTime

/**
 * All times in minutes.
 */
internal data class TotalsPrevious(
    val multiPilotTime: Int,
    val totalTime: Int,
    val landingsDay: Int,
    val landingsNight: Int,
    val nightTime: Int,
    val ifrTime: Int,
    val picTime: Int,
    val coPilotTime: Int,
    val dualTime: Int,
    val instructorTime: Int,
    val simTime: Int
    ) {
    operator fun plus(flightsToAdd: List<BasicFlight>): TotalsPrevious {
        val newMultiPilotTime = multiPilotTime + flightsToAdd.sumOf { it.multiPilotTime }
        val newTotalTime = totalTime + flightsToAdd.sumOf { it.totalTime() }
        val newLandingsDay = landingsDay + flightsToAdd.sumOf { it.landingDay }
        val newLandingsNight = landingsNight + flightsToAdd.sumOf { it.landingNight }
        val newNightTime = nightTime + flightsToAdd.sumOf { it.nightTime }
        val newIfrTime = ifrTime + flightsToAdd.sumOf { it.ifrTime }
        val newPicTime = picTime + flightsToAdd.sumOf { f -> if (f.isPIC) f.totalTime() else 0 }
        val newCoPilotTime = coPilotTime + flightsToAdd.sumOf { f -> if (f.isCoPilot) f.totalTime() else 0 }
        val newDualTime = dualTime + flightsToAdd.sumOf { f -> if (f.isDual) f.totalTime() else 0 }
        val newInstructorTime = instructorTime + flightsToAdd.sumOf { f -> if (f.isInstructor) f.totalTime() else 0 }
        val newSimTime = simTime + flightsToAdd.sumOf { it.simTime }

        return TotalsPrevious(newMultiPilotTime, newTotalTime, newLandingsDay, newLandingsNight, newNightTime, newIfrTime, newPicTime, newCoPilotTime, newDualTime, newInstructorTime, newSimTime)
    }

    companion object{
        val ZERO = TotalsPrevious(0,0,0,0, 0, 0, 0, 0, 0, 0, 0)
    }
}