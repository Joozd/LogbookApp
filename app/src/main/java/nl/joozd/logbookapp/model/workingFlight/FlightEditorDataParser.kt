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

package nl.joozd.logbookapp.model.workingFlight

import kotlinx.coroutines.*
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftDataCache
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.data.repository.airportrepository.AirportRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepository
import nl.joozd.logbookapp.data.repository.helpers.findBestHitForRegistration
import nl.joozd.logbookapp.extensions.plusDays
import java.time.*
import kotlin.coroutines.CoroutineContext

//This class takes input strings and provides model data for a FlightEditor
class FlightEditorDataParser(
    private val editor: FlightEditor,
    private val flightRepository: FlightRepository = FlightRepository.instance,
    private val airportRepository: AirportRepository = AirportRepository.instance,
    private val aircraftRepository: AircraftRepository = AircraftRepository.instance
): CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = Dispatchers.Default + job

    fun setOrig(orig: String){
        launch {
            val origAirport = findAirportFromIdentOrMakePlaceholder(orig)
            editor.orig = origAirport
        }
    }

    fun setDest(dest: String){
        launch {
            val destAirport = findAirportFromIdentOrMakePlaceholder(dest)
            editor.dest = destAirport
        }
    }

    fun setTimeOut(time: String){
        editor.timeOut = parseTimeStringToInstant(time, editor.date)
    }

    fun setTimeIn(time: String){
        var tIn = parseTimeStringToInstant(time, editor.date)
        //make sure tIn is always later than editor.timeOut, so if tOut is at 1600 and tIn at 1500, flight is 23 hours.
        while (tIn <= editor.timeOut) tIn = tIn.plusDays(1)
        editor.timeIn = tIn
    }

    fun setAircraft(aircraftString: String) {
        launch {
            editor.aircraft = when {
                aircraftString.isBlank() -> Aircraft()
                "(" !in aircraftString -> searchRegistration(aircraftString)  // only registration entered
                else -> makeRegAndType(aircraftString)                        // reg and type entered
            }
        }
    }

    fun setTakeoffLandings(tl: String){
        val trimmed = tl.trim()
        editor.takeoffLandings = parseTakeoffLandingString(trimmed)
    }

    fun setSimTimeFromString(s: String?){
        editor.simTime = hoursAndMinutesStringToInt(s) ?: 0
    }

    fun setSimAircraftType(type: String){
        val t = AircraftType(shortName = type)
        editor.aircraft = Aircraft(type = t)
    }

    suspend fun saveAndClose(){
        val runningJobs = job.children.toList()
        runningJobs.joinAll()
        editor.save()
        editor.close()
    }

    fun close(){
        job.cancel()
        editor.close()
    }

    private suspend fun findAirportFromIdentOrMakePlaceholder(ident: String) =
        (airportRepository.getAirportByIcaoIdentOrNull(ident)
            ?: airportRepository.getAirportByIataIdentOrNull(ident)
            ?: Airport.placeholderWithIdentOnly(ident))

    private fun hoursAndMinutesStringToInt(hoursAndMinutes: String?): Int? {
        if (hoursAndMinutes == null || hoursAndMinutes.isBlank()) return null

        val hoursAndMinutesSplits = splitIntoHoursAndMinutes(hoursAndMinutes)

        val hoursAndMinutesInts = try {
            hoursAndMinutesSplits.map{it.toInt()}
        } catch (e: NumberFormatException) { return null } // if not only digits left,

        if (hoursAndMinutesInts.isEmpty()) return null // This happens when only characters in "+- :/.h" were in string

        return if (hoursAndMinutesInts.size == 1) {
            val v = hoursAndMinutesInts.first()
            // last two digits are minutes, any before that are hours (so 123456 == 1234:56)
            // 76 minutes is fine, 104 is 64 minutes.
            // users will probably enter something like 330 when they mean 3:30
            if (v <= 99) v
            else v%100 + v/100*60
        } else hoursAndMinutesInts[0]*60 + hoursAndMinutesInts[1]
    }

    private fun parseTimeStringToInstant(s: String, date: LocalDate): Instant =
        s.filter{it.isDigit()}.padStart(4, '0').take(4).toInt().let{
            val hours = it/100 + (it%100)/60    // hours is first two digits plus one if last two digits >= 60
            val mins = (it%100)%60              // mins is last two digits % 60
            val t = LocalTime.of(hours, mins)
            return t.atDate(date).toInstant(ZoneOffset.UTC)
        }

    private fun parseTakeoffLandingString(trimmed: String) = when {
        trimmed.any { it !in "0123456789/" } -> TakeoffLandings(0)
        '/' in trimmed -> trimmed.split('/').let { parts ->
            TakeoffLandings(parts[0].toInt(), parts[1].toInt())
        }
        else -> TakeoffLandings(trimmed.toInt())
    }

    private fun splitIntoHoursAndMinutes(hoursAndMinutes: String) =
        hoursAndMinutes.trim()
            .split(*"+- :/.h".toCharArray())
            .filter { it.isNotBlank() } // remove any excess blank spaces that might have been in here

    private suspend fun searchRegistration(regAndTypeString: String): Aircraft {
        val adc = aircraftRepository.getAircraftDataCache()
        return getBestHitForPartialRegistration(regAndTypeString, adc)
            ?: Aircraft(registration = regAndTypeString, source = Aircraft.UNKNOWN)
    }

    //expects a string in format REG(TYPE)
    private fun makeRegAndType(regAndTypeString: String): Aircraft{
        val regAndType = regAndTypeString.filter { it != ')' }.split("(")
        return Aircraft(
            registration = regAndType.first(),
            type = AircraftType(shortName = regAndType.last(), multiPilot = editor.multiPilotTime > 0),
        )
    }

    private fun getBestHitForPartialRegistration(r: String, dataCache: AircraftDataCache): Aircraft? =
        dataCache.getAircraftFromRegistration(r)
            ?: dataCache.getAircraftFromRegistration(findBestHitForRegistration(r,dataCache.getRegistrationToAircraftMap().keys))


}