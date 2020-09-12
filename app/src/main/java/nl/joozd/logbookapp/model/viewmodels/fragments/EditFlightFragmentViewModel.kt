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

import android.util.Log
import androidx.lifecycle.*
import androidx.lifecycle.Transformations.distinctUntilChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.anyWordStartsWith
import nl.joozd.logbookapp.extensions.nullIfEmpty
import nl.joozd.logbookapp.extensions.removeTrailingDigits
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.toggleTrueAndFalse
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.withDate
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.withRegAndType
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.withTakeoffLandings
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions.getDateStringFromEpochSeconds
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions.getTimestringFromEpochSeconds
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.EditFlightFragmentEvents
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.hoursAndMinutesStringToInt
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.withTimeInStringToTime
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.withTimeOutStringToTime
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel
import java.time.LocalDate

class EditFlightFragmentViewModel: JoozdlogDialogViewModel(){

    /**********************************************************************************************
     * Private parts
     *********************************************************************************************/



    val flightNumber: LiveData<String> = distinctUntilChanged(Transformations.map(flight) { it.flightNumber })
    val orig: LiveData<String> = distinctUntilChanged(Transformations.map(workingFlightRepository.origin) { (if (Preferences.useIataAirports) it?.iata_code?.nullIfEmpty() else it?.ident) ?: workingFlight?.orig })
    val dest: LiveData<String> = distinctUntilChanged(Transformations.map(workingFlightRepository.destination) { (if (Preferences.useIataAirports) it?.iata_code?.nullIfEmpty() else it?.ident) ?: workingFlight?.orig })
    val timeOut: LiveData<String> = distinctUntilChanged(Transformations.map(flight) { getTimestringFromEpochSeconds(it.timeOut) })
    val timeIn: LiveData<String> = distinctUntilChanged(Transformations.map(flight) { getTimestringFromEpochSeconds(it.timeIn) })



    /**********************************************************************************************
     * logic for EditText fields:
     * - observables
     * - setters
     *********************************************************************************************/

    val flightID = distinctUntilChanged(Transformations.map(flight) { it.flightID })

    val date: LiveData<String> = distinctUntilChanged(Transformations.map(flight) { getDateStringFromEpochSeconds(it.timeOut) })
    fun setDate(date: LocalDate){
        workingFlight?.let{ workingFlight = it.withDate(date)}
    }

    fun getLocalDate(): LocalDate? = flight.value?.tOut()?.toLocalDate()

    fun setFlightNumber(flightNumber: String){
        workingFlight?.let{
            if (flightNumber != workingFlight?.flightNumber?.removeTrailingDigits())
                workingFlight = it.copy(flightNumber = flightNumber)
            else {
                val oldFN = it.flightNumber
                workingFlight = it.copy (flightNumber = flightNumber)
                workingFlight = it.copy (flightNumber = oldFN)

            }
        }
    }

    fun setOrig(enteredData: String){
        workingFlight?.let {
            // initially set entered data
            workingFlight = it.copy(orig = enteredData)

            // Check if entered data is a known airport, and if so, set that airport as [orig]
            viewModelScope.launch {
                airportRepository.searchAirportOnce(enteredData)?.let{foundAirport ->
                    //make sure we have most recent workingFlight in case search was taking a while
                    workingFlight?.let{f ->
                        workingFlight = f.copy(orig = foundAirport.ident)
                    }
                } ?: feedback(EditFlightFragmentEvents.AIRPORT_NOT_FOUND).apply {
                    extraData.putString("enteredData", enteredData)
                    extraData.putBoolean("orig", true)
                }
            }

        }
    }

    fun setDest(enteredData: String){
        workingFlight?.let {
            // initially set entered data
            workingFlight = it.copy(dest = enteredData)

            // Check if entered data is a known airport, and if so, set that airport as [dest]
            viewModelScope.launch {
                airportRepository.searchAirportOnce(enteredData)?.let{foundAirport ->
                    //make sure we have most recent workingFlight in case search was taking a while
                    workingFlight?.let{ f ->
                        workingFlight = f.copy(dest = foundAirport.ident)
                    }
                } ?: feedback(EditFlightFragmentEvents.AIRPORT_NOT_FOUND).apply {
                    extraData.putString("enteredData", enteredData)
                    extraData.putBoolean("orig", false)
                }
            }
        }
    }

    val origChecked = workingFlightRepository.origInDatabase
    val destChecked = workingFlightRepository.destInDatabase

    /**
     * This one also used for sim times as Fragment doesn't know or care if this flight is sim or not
     */
    fun setTimeOut(enteredData: String){
        workingFlight?.let {
            workingFlight = if (it.isSim) it.copy (simTime = hoursAndMinutesStringToInt(enteredData) ?: 0.also{
                feedback(EditFlightFragmentEvents.INVALID_SIM_TIME_STRING)
                Log.e("EditFlightViewModel", "INVALID_SIM_TIME_STRING")
            })
            else it.withTimeOutStringToTime(enteredData).also{ f-> // setting null flight does nothing so we can do this
                if (f == null) feedback(EditFlightFragmentEvents.INVALID_TIME_STRING).also{
                    Log.e("EditFlightViewModel", "INVALID_SIM_TIME_STRING")
                }
            }
        }
    }

    val simTime: LiveData<String>
        get() = distinctUntilChanged(Transformations.map(flight) { minutesToHoursAndMinutesString(it.simTime)})

    fun setTimeIn(enteredData: String){
        workingFlight?.let {
            workingFlight = it.withTimeInStringToTime(enteredData)
        }
    }

    val regAndType: LiveData<String>
        get() = distinctUntilChanged(Transformations.map(flight) { if (it.isSim) it.aircraftType else it.regAndType() })
    fun setRegAndType(enteredData: String){
        workingFlight?.let{f ->
            if (checkSim){
                workingFlight?.let{
                    workingFlight = it.copy (aircraftType = enteredData)
                }
            }
            else {
                when {
                    enteredData.isEmpty() -> workingFlight =
                        f.copy(aircraftType = "", registration = "")

                    "(" !in enteredData && ")" !in enteredData -> viewModelScope.launch {
                        aircraftRepository.getBestHitForPartialRegistration(enteredData)?.let {
                            workingFlight = f.copy(
                                aircraftType = it.type?.shortName ?: "UNKNWN",
                                registration = it.registration
                            )
                        } ?: feedback(EditFlightFragmentEvents.AIRCRAFT_NOT_FOUND).apply{
                            putString(enteredData)
                        }
                    }

                    else -> workingFlight = f.withRegAndType(enteredData)
                    // TODO check if entry is format [alphanumeric(alphanumeric)], do something with that?
                    // TODO Maybe just get rid of this?
                }
            }
        }
    }

    val takeoffLandings: LiveData<String>
        get() = distinctUntilChanged(Transformations.map(flight) { it.takeoffLanding })
    fun setTakeoffLandings(enteredData: String){
        if ('/' in enteredData) return // do nothing since that should not be possible to change
        require(enteredData.all { it in "0123456789"}) { "$enteredData did not consist of only numbers"}
        workingFlight?.let {f ->
            viewModelScope.launch(Dispatchers.IO) {
                val landings = enteredData.toInt()
                val orig = airportRepository.getAirportOnce(f.orig)
                val dest = airportRepository.getAirportOnce(f.dest)
                if (orig == null || dest == null) feedback(EditFlightFragmentEvents.AIRPORT_NOT_FOUND_FOR_LANDINGS)
                // If airports are not found (because, for instance, custom airport was used)
                // it will log day TO and landing
                workingFlight = f.withTakeoffLandings(landings, orig, dest, disableAutoFill = true)
            }
        }
    }

    val name: LiveData<String>
        get() = distinctUntilChanged(Transformations.map(flight) { it.name })
    fun setName(name: String){
        workingFlight?.let {f ->
            workingFlight = f.copy(name = if (name.isEmpty()) "" else allNames.value?.firstOrNull{it.anyWordStartsWith(name, ignoreCase = true)} ?: name)
        }
    }

    val name2: LiveData<String>
        get() = distinctUntilChanged(Transformations.map(flight) { it.name2 })
    fun setName2(name2: String){
        // If a ";" in entered value, split it and enter those names. Otherwise, enter first hit in already known names. If none known, enter "as is"
        workingFlight?.let { f ->
            workingFlight = if (';' in name2){
                f.copy(name2 = name2.split(';').joinToString(";") { it.trim() })
            } else
                f.copy(name2 = if (name2.isEmpty()) "" else allNames.value?.firstOrNull{it.anyWordStartsWith(name2, ignoreCase = true)} ?: name2)
        }
    }

    val remarks: LiveData<String>
        get() = distinctUntilChanged(Transformations.map(flight) { it.remarks })
    fun setRemarks(remarks: String){
        workingFlight?.let {
            workingFlight = it.copy(remarks = remarks)
        }
    }

    /*********************************************************************************************
     * Toggle switches:
     *********************************************************************************************/

    val sign: LiveData<Boolean>
        get() = distinctUntilChanged(Transformations.map(flight) { it.signature.isNotEmpty() })
    //no setter for signature as this always goes through dialog

    val sim: LiveData<Boolean>
        get() = distinctUntilChanged(Transformations.map(flight) { it.isSim })
    fun toggleSim() = workingFlight?.let { workingFlight = it.copy(isSim = !it.isSim, simTime = if (it.simTime == 0 && !it.isSim) Preferences.standardSimDuration else it.simTime) } // also set sim time if it is set to isSim = true

    val dual: LiveData<Boolean>
        get() = distinctUntilChanged(Transformations.map(flight) { it.isDual })
    fun toggleDual() = workingFlight?.let { workingFlight = it.copy(isDual = toggleTrueAndFalse(it.isDual)) }

    val instructor: LiveData<Boolean>
        get() = distinctUntilChanged(Transformations.map(flight) { it.isInstructor })
    fun toggleInstructor() = workingFlight?.let { workingFlight = it.copy(isInstructor = toggleTrueAndFalse(it.isInstructor)) }

    val ifr: LiveData<Boolean>
        get() = distinctUntilChanged(Transformations.map(flight) { workingFlightRepository.isIfr })

    fun toggleIFR() = workingFlight?.let{
        workingFlightRepository.isIfr = !workingFlightRepository.isIfr
        if (workingFlightRepository.isIfr) workingFlight?.let{f ->
            workingFlight = f.copy(ifrTime = f.duration())
        }
    }

    val pic: LiveData<Boolean> = distinctUntilChanged(Transformations.map(flight) { it.isPIC })
    fun togglePic() = workingFlight?.let { workingFlight = it.copy(isPIC = toggleTrueAndFalse(it.isPIC)) }

    val pf: LiveData<Boolean>
        get() = distinctUntilChanged(Transformations.map(flight) { it.isPF })
    fun togglePf() = workingFlight?.let { workingFlight = it.copy(isPF = toggleTrueAndFalse(it.isPF)) }

    val autoFill: LiveData<Boolean>
        get() = distinctUntilChanged(Transformations.map(flight) { it.autoFill })
    fun setAutoFill(on: Boolean) = workingFlight?.let { workingFlight = it.copy(autoFill = on) }

    /*********************************************************************************************
     * Miscellaneous
     *********************************************************************************************/

    val allNames = distinctUntilChanged(flightRepository.allNames)
    val knownRegistrations = distinctUntilChanged(flightRepository.usedRegistrations)

    fun save(){
        workingFlightRepository.saveWorkingFlight()
    }

    fun saveOnClose(){
        workingFlightRepository.notifyFinalSave()
        save()
    }

    fun onStart(){
        workingFlightRepository.setOpenInEditor(isOpen = true)
    }

    fun onClosingFragment(){
        workingFlightRepository.setOpenInEditor(isOpen = false)
    }

    val checkSim: Boolean
        get() = workingFlight?.isSim == true
}
