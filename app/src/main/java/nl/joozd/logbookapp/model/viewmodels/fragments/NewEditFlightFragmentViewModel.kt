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

package nl.joozd.logbookapp.model.viewmodels.fragments

import android.text.Editable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.*
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.EditFlightFragmentEvents
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.hoursAndMinutesStringToInt
import nl.joozd.logbookapp.model.viewmodels.JoozdlogViewModel
import nl.joozd.logbookapp.model.workingFlight.WorkingFlight
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter


class NewEditFlightFragmentViewModel: JoozdlogViewModel() {
    private val wf = flightRepository.workingFlight.value!!

    // If this is true, no more windows should be opened
    private var closing: Boolean = false


    /**
     * MediatorLiveData
     */

    val _aircraft = MediatorLiveData<String>().apply{
        addSource(wf.aircraft) {ac -> value = (if (sim) ac?.type?.shortName else ac?.toString()) ?: NO_DATA_STRING}
        addSource (wf.isSim) { sim -> value = (if (sim) wf.aircraft.value?.type?.shortName else wf.aircraft.value?.toString()) ?: NO_DATA_STRING}
    }

    //Transformations.map(wf.aircraft)
    /**
     * Observables
     */
    // this will cause nullpointerexception if not set
    // However, fragment should only be
    val date = Transformations.map(wf.date){ it.toDateString() ?: NO_DATA_STRING }
    val localDate
            get() = wf.date.value
    val flightNumber
            get() = wf.flightNumber
    val origin = Transformations.map(wf.origin) { getAirportString(it)}
    val destination = Transformations.map(wf.destination) { getAirportString(it)}
    val originIsValid = Transformations.map(wf.origin){ it != null && it.latitude_deg != 0.0 && it.longitude_deg != 0.0}
    val destinationIsValid = Transformations.map(wf.destination){ it != null && it.latitude_deg != 0.0 && it.longitude_deg != 0.0}
    val timeOut = Transformations.map(wf.timeOut) { it.toTimeString()}
    val timeIn = Transformations.map(wf.timeIn) { it.toTimeString()}
    val landings = Transformations.map(wf.takeoffLandings){it.toString()}
    val aircraft: LiveData<String>
        get() = _aircraft
    val name
        get() = wf.name
    val name2
        get() = wf.name2
    val allNames
        get() = flightRepository.allNames
    val remarks
        get() = wf.remarks
    val ifrTime
        get() = wf.ifrTime
    val simTime
        get() = wf.simTime
    val nightTime
        get() = wf.nightTime
    val multiPilotTime
        get() = wf.multiPilotTime
    val isSim
        get() = wf.isSim
    val sim: Boolean    // val to check if flight is sim
        get()= isSim.value ?: false

    val isSigned
        get() = wf.isSigned
    val signature: String
        get() = wf.signature.value ?: ""
    val isDual
        get() = wf.isDual
    val isInstructor
        get() = wf.isInstructor
    val isIfr
        get() = wf.isIfr
    val isPic
        get() = wf.isPic
    val isPF
        get() = wf.isPF
    val isAutoValues
        get() = wf.isAutoValues
    val knownRegistrations
        get() = flightRepository.usedRegistrations


    /**
     * Data entry functions
     */

    /**
     * Set date
     * @param newDate: Date as [LocalDate]
     */
    fun setDate(newDate: LocalDate){
        wf.setDate(newDate)
    }

    /**
     * Set FlightNumber
     */
    fun setFlightNumber(newFlightNumber: Editable?){
        newFlightNumber?.toString()?.let{
            if (it != (wf.flightNumber.value ?: "").removeTrailingDigits())
                wf.setFlightNumber(it)
            else wf.setFlightNumber(wf.flightNumber.value ?: "")
        }
    }

    /**
     * Set origin.
     * Checks if entered data is found in airportRepository.
     * If it is not found, it will enter it "as is" as identifier (ICAO code)
     */
    fun setOrig(origString: String) = viewModelScope.launch{
        val ident = airportRepository.searchAirportOnce(origString)?.ident ?: origString
        wf.setOrig(ident)
    }

    /**
     * Set destination.
     * Checks if entered data is found in airportRepository.
     * If it is not found, it will enter it "as is" as identifier (ICAO code)
     */
    fun setDest(destString: String) = viewModelScope.launch{
        val ident = airportRepository.searchAirportOnce(destString)?.ident ?: destString
        wf.setDest(ident)
    }


    /**
     * Set departure time
     * If flight is sim, this is used for entering simTime because of reasons.
     * TODO I might want to change that and make a dedicated box for that in EditFlightFragmentLayout
     * Works for now.
     */
    fun setTimeOut(timeString: Editable?){
        wf.setTimeOut(makeTimeFromTimeString(timeString.toString()))
    }

    /**
     * Set arrival time
     */
    fun setTimeIn(timeString: Editable?){
        wf.setTimeIn(makeTimeFromTimeString(timeString.toString()))
    }

    fun setSimTime(simTimeString: Editable?){
        wf.setSimTime(hoursAndMinutesStringToInt(simTimeString.toString()) ?: return)
    }

    /**
     * Set registration and type from regAndTypeString.
     * - If [sim] it saves the whole thing as type
     * - if no '(' in [regAndTypeString] it assumes all is registration.
     * Else, it will save exactly [reg] and [type] in [reg]([type]).
     * Closing bracket is ignored. If no opening bracket, it is all reg.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun setRegAndType(regAndTypeString: String){
        if (sim) wf.setAircraft(type = regAndTypeString)
        else when{
            regAndTypeString.isBlank() -> wf.setAircraft(Aircraft("")) // no reg and type if field is empty

            "(" !in regAndTypeString -> viewModelScope.launch { // only registration entered
                aircraftRepository.getBestHitForPartialRegistration(regAndTypeString)?.let{
                    wf.setAircraft(it)
                } ?: feedback(EditFlightFragmentEvents.AIRCRAFT_NOT_FOUND).apply{
                    putString(regAndTypeString)
                    }.also {
                    wf.setAircraft(regAndTypeString)
                }
            }

            else -> { // If a ( or ) in [regAndTypeString] it will save exactly [reg] and [type] in [reg]([type]). Closing bracket is ignored. If no opening bracket, it is all reg.
                val reg: String?
                val type: String?
                regAndTypeString.filter{ it != ')'}.split('(').let{
                    reg = it.firstOrNull()
                    type = it.getOrNull(1)
                }
                wf.setAircraft(reg, type)
            }


        }
    }

    fun setRegAndType(regAndTypeEditable: Editable) = setRegAndType(regAndTypeEditable.toString())

    /**
     * Set takeoff/landings from a string.
     * If '/' in string, it takes [takeoff]/[landing]
     * else it sets both takeoff and landing to that value.
     * [WorkingFlight] takes care of day/night
     * @param tlString: Takeoff/landing string. Can only consist of digits or '/'
     */
    fun setTakeoffLandings(tlString: String){
        require (tlString.all{ it in "1234567890/"})
        if ('/' in tlString) tlString.split('/').let{
            wf.takeoff = it[0].toInt()
            wf.landing = it[1].toInt()
        } else {
            wf.takeoff = tlString.toInt()
            wf.landing = tlString.toInt()
        }
    }

    /**
     * Set name. Will auto-complete names if it doesn't end with ';'
     * @see [String.anyWordStartsWith]
     */
    fun setName(name: String){
        if (';' in name) wf.setName(name.dropLast(1))
        else wf.setName(if (name.isEmpty()) "" else allNames.value?.firstOrNull{it.anyWordStartsWith(name, ignoreCase = true)} ?: name)
    }

    /**
     * Set name2. Will auto-complete names.
     * Names can be separated by ';', in which case they will be trimmed and not autocompleted
     * (you can also use this to enter an exact name if a longer one exists ie. "jan"jans" might becom "jan janssen" where "jan jans;" will be "jan jans")
     * @see [String.anyWordStartsWith]
     */
    fun setName2(name2: String){
        if (';' in name2) wf.setName2(name2.split(';').joinToString(";") { it.trim() })
        else wf.setName2(if (name2.isEmpty()) "" else allNames.value?.firstOrNull{it.anyWordStartsWith(name2, ignoreCase = true)} ?: name2)

    }


    /**
     * Set remarks
     */
    fun setRemarks(remarks: String) = wf.setRemarks(remarks)

    /**
     * Set sim
     * @param force: Can be used to force sim on or off. If not given or null, it sets it to what it currently is not
     */
    fun toggleSim(force: Boolean? = null) = wf.setIsSim ( force ?: !sim) // [sim] comes straight from [wf] so it is a true toggle

    /**
     * Set signature
     */
    fun setSignature(signature: String) = wf.setSignature(signature)

    /**
     * Set dual
     * @param force: Can be used to force sim on or off. If not given or null, it sets it to what it currently is not (or to true if it is null for some reason)
     */
    fun toggleDual(force: Boolean? = null) = wf.setIsDual ( force ?: wf.isDual.value == false)

    /**
     * Set instructor
     * @param force: Can be used to force sim on or off. If not given or null, it sets it to what it currently is not (or to true if it is null for some reason)
     */
    fun toggleInstructor(force: Boolean? = null) = wf.setIsInstructor ( force ?: wf.isInstructor.value == false)

    /**
     * Set IFR
     * workingFlight will take care of also adjusting ifrTime (if autovalues)
     * @param force: Can be used to force sim on or off. If not given or null, it sets it to what it currently is not (or to true if it is null for some reason)
     */
    fun toggleIfr(force: Boolean? = null){
        wf.setIsIfr(force ?: wf.isIfr.value == false)
    }

    /**
     * Toggle PIC
     * @param force: Can be used to force sim on or off. If not given or null, it sets it to what it currently is not (or to true if it is null for some reason)
     */
    fun togglePic(force: Boolean? = null){
        wf.setIsPic(force ?: wf.isPic.value == false)
    }

    /**
     * Toggle PF.
     * If augmentedCrew.crewSize > 2, WorkingFlight will recalculate times (if autovalues)
     * @param force: Can be used to force sim on or off. If not given or null, it sets it to what it currently is not (or to true if it is null for some reason)
     */
    fun togglePF(force: Boolean? = null){
        val newValue = force ?: wf.isPF.value == false
        wf.setIsPF(newValue)
        if (wf.isAutoValues.value == true) {
            wf.takeoff = newValue.toInt() // Boolean.toInt() is 1 if true or 0 if false
            wf.landing = newValue.toInt()
        }
    }

    fun toggleAutoValues(force: Boolean? = null){
        wf.setAutoValues(force ?: wf.isAutoValues.value == false)
    }

    /**
     * Save WorkingFlight and send Close message to fragment
     */
    fun saveAndClose() {
        wf.saveAndClose()
        feedback(EditFlightFragmentEvents.CLOSE_EDIT_FLIGHT_FRAGMENT)
    }

    /**
     * Let the viewModel know the Fragment is about to close itself
     */
    fun notifyClosing(){
        closing = true
    }


    /**
     * Check if
     */
    fun checkIfStillOpen(): Boolean = !closing



    /**
     * Helper functions
     */

    /**
     * Displays the airport as ICAO or IATA, depending on settings in Preferences.
     * If no IATA found, defaults to ICAO
     */
    private fun getAirportString(a: Airport?): String = when{
        a == null -> NO_DATA_STRING
        !Preferences.useIataAirports -> a.ident
        else -> a.iata_code.nullIfBlank() ?: a.ident
    }

    /**
     * Make a Local Time from a string with format "1234"
     * Any non-number characters are ignored (so you can use 12+34, 12:34, etc)
     * "34" will be 00:34
     * 12345 will be 12:34, same for 12:34:56
     * @param s: String to parse
     * @return Local Time parsed from [s]
     */
    private fun makeTimeFromTimeString(s: String): LocalTime =
        LocalTime.parse(
            s.filter{it.isDigit()}.padStart(4, '0').take(4),
            DateTimeFormatter.ofPattern("HHmm")
        )


    companion object{
        const val NO_DATA_STRING = "…"
    }

}