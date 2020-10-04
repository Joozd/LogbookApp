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


import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModelWithWorkingFlight

@Deprecated("Switch to NewEditFlightFragmentViewModel")
class EditFlightFragmentViewModel: JoozdlogDialogViewModelWithWorkingFlight(){
    /*

    /**********************************************************************************************
     * Private parts
     *********************************************************************************************/



    val flightNumber: LiveData<String> = workingFlight.flightNumber
    val orig: LiveData<String> = Transformations.map(workingFlight.origin) { (if (Preferences.useIataAirports) it?.iata_code?.nullIfEmpty() else it?.ident) ?: "(...)" }
    val dest: LiveData<String> = Transformations.map(workingFlight.destination) { (if (Preferences.useIataAirports) it?.iata_code?.nullIfEmpty() else it?.ident) ?: "(...)" }
    val timeOut: LiveData<String> = Transformations.map(workingFlight.timeOut) { it.toTimeString()}
    val timeIn: LiveData<String> = Transformations.map(workingFlight.timeIn) { it.toTimeString()}



    /**********************************************************************************************
     * logic for EditText fields:
     * - observables
     * - setters
     *********************************************************************************************/

    val date: LiveData<String> = Transformations.map(workingFlight.date) { it.toDateString() }
    fun setDate(date: LocalDate){
        workingFlight.setDate(date)
    }

    /**
     * Sets flightnumber if it is not only the first letters of old flightNumber.
     * (eg of it was "KL123", setting "KL" will be ignored and will result in old flightNumber being set to reset EditText field through LiveData
     *
     */
    fun setFlightNumber(flightNumber: String){
            if (flightNumber != workingFlight.flightNumber.value!!.removeTrailingDigits())
                workingFlight.setFlightNumber(flightNumber)
            else {
                workingFlight.setFlightNumber(workingFlight.flightNumber.value!!)
            }

    }

    /**
     * Set origin. workingFlight will take care of heavy lifting.
     */
    fun setOrig(enteredData: String){
        workingFlight.setOrig(enteredData)
    }

    /**
     * Set destination. workingFlight will take care of heavy lifting.
     */
    fun setDest(enteredData: String){
        workingFlight.setDest(enteredData)
    }

    val origChecked = Transformations.map(workingFlight.origin) { it?.checkIfValidCoordinates() ?: false }
    val destChecked = Transformations.map(workingFlight.destination) { it?.checkIfValidCoordinates() ?: false }

    /**
     * set Time Out.
     * Take first 4 digits (padded with 0's at start) and convert it into a LocalTime and set that.
     */
    fun setTimeOut(enteredData: Editable){


    }

    fun setSimTime(enteredData: Editable)

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
                            putString(enteredData).also{
                                workingFlight = f.copy(
                                    registration = enteredData,
                                    aircraftType= ""
                                )
                            }
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
                val orig = airportRepository.getAirportByIcaoIdentOrNull(f.orig)
                val dest = airportRepository.getAirportByIcaoIdentOrNull(f.dest)
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

    */

}


