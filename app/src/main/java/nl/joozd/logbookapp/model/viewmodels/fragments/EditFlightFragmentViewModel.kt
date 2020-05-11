package nl.joozd.logbookapp.model.viewmodels.fragments

import android.util.Log
import androidx.lifecycle.*
import androidx.lifecycle.Transformations.distinctUntilChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.extensions.toBoolean
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.toggleZeroAndOne
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.withDate
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.withRegAndType
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.withTakeoffLandings
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions.getDateStringFromEpochSeconds
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions.getTimestringFromEpochSeconds
import nl.joozd.logbookapp.extensions.toInt
import nl.joozd.logbookapp.model.helpers.FeedbackEvents.EditFlightFragmentEvents
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.hoursAndMinutesStringToInt
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.withTimeInStringToTime
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.withTimeOutStringToTime
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel
import java.time.LocalDate

class EditFlightFragmentViewModel: JoozdlogDialogViewModel(){
    /**********************************************************************************************
     * logic for EditText fields:
     * - private mutable fields
     * - observables
     * - setters
     **********************************************************************************************/

    private val _date: LiveData<String> = Transformations.map(flight) { getDateStringFromEpochSeconds(it.timeOut) }
    val date: LiveData<String> = distinctUntilChanged(_date)
    //This sets a LocalDate instead of a string, because that is what the picker returns. Field is not focusable.
    fun setDate(date: LocalDate){
        workingFlight?.let{ workingFlight = it.withDate(date)}
    }

    private val _flightNumber: LiveData<String> = Transformations.map(flight) { it.flightNumber }
    val flightNumber: LiveData<String> = distinctUntilChanged(_flightNumber)
    fun setFlightNumber(flightNumber: String){
        workingFlight?.let{
            workingFlight = it.copy(flightNumber = flightNumber)
        }
    }

    //TODO IATA or ICAO display
    private val _orig: LiveData<String> = Transformations.map(flight) { it.orig }
    val orig: LiveData<String> = distinctUntilChanged(_orig)
    fun setOrig(enteredData: String){
        workingFlight?.let {
            _origChecked.value = null
            viewModelScope.launch {
                airportRepository.searchAirportOnce(enteredData)?.let{foundAirport ->
                    workingFlight?.let{
                        workingFlight = it.copy(orig = foundAirport.ident)
                        _origChecked.value = true
                    }
                } ?: feedback(EditFlightFragmentEvents.AIRPORT_NOT_FOUND).apply {
                    extraData.putString("enteredData", enteredData)
                    extraData.putBoolean("orig", true)
                    _origChecked.value = false
                }
            }

        }
    }
    private val _origChecked = MutableLiveData<Boolean?>()
    val origChecked: LiveData<Boolean?>
        get() = _origChecked

    //TODO IATA or ICAO display
    private val _dest: LiveData<String> = Transformations.map(flight) { it.dest }
    val dest: LiveData<String> = distinctUntilChanged(_dest)
    fun setDest(enteredData: String){
        _destChecked.value = null
        workingFlight?.let {
            viewModelScope.launch {
                airportRepository.searchAirportOnce(enteredData)?.let{foundAirport ->
                    workingFlight?.let{
                        workingFlight = it.copy(dest = foundAirport.ident)
                        _destChecked.value = true
                    }
                } ?: feedback(EditFlightFragmentEvents.AIRPORT_NOT_FOUND).apply {
                    extraData.putString("enteredData", enteredData)
                    extraData.putBoolean("orig", false)
                    _destChecked.value = false
                }
            }
        }
    }
    private val _destChecked = MutableLiveData<Boolean?>()
    val destChecked: LiveData<Boolean?>
        get() = _destChecked



    /**
     * This one also used for sim times as Fragment doesn't know or care if this flight is sim or not
     */
    private val _timeOut: LiveData<String> = Transformations.map(flight) { getTimestringFromEpochSeconds(it.timeOut) }
    val timeOut: LiveData<String> = distinctUntilChanged(_timeOut)
    fun setTimeOut(enteredData: String){
        workingFlight?.let {
            workingFlight = if (it.isSim.toBoolean()) it.copy (simTime = hoursAndMinutesStringToInt(enteredData) ?: 0.also{ feedback(EditFlightFragmentEvents.INVALID_SIM_TIME_STRING)})
            else it.withTimeOutStringToTime(enteredData).also{ f-> // setting null flight does nothing so we can do this
                if (f == null) feedback(EditFlightFragmentEvents.INVALID_TIME_STRING)
            }
        }
    }

    val simTime: LiveData<String> = distinctUntilChanged(Transformations.map(flight) { minutesToHoursAndMinutesString(it.simTime)})

    private val _timeIn: LiveData<String> = Transformations.map(flight) { getTimestringFromEpochSeconds(it.timeIn) }
    val timeIn: LiveData<String> = distinctUntilChanged(_timeIn)
    fun setTimeIn(enteredData: String){
        workingFlight?.let {
            workingFlight = it.withTimeInStringToTime(enteredData)
        }
    }

    val regAndType: LiveData<String> = distinctUntilChanged(Transformations.map(flight) { if (it.isSim.toBoolean()) it.aircraft else it.regAndType() })
    fun setRegAndType(enteredData: String){
        workingFlight?.let{f ->
            if (!("(" in enteredData && ")" in enteredData)){
                viewModelScope.launch {
                    //TODO: Search for aircraft/type from this
                    // for now: send INVALID_REG_TYPE_STRING event to empty on false entry:
                    // -> feedback(EditFlightFragmentEvents.INVALID_REG_TYPE_STRING)
                    aircraftRepository.findAircraft(reg = enteredData)?.let{
                        workingFlight = f.copy(aircraft = it.type?.shortName ?: "UNKNWN", registration = it.registration)
                        return@launch   // work is done
                    }
                    //if we get here, reg not found in database. Now check consensus.
                    val foundConsensus = aircraftRepository.getRegistrationsWithConsensus(enteredData)
                    when{
                        foundConsensus.size == 1 -> {
                            val consensus = aircraftRepository.getConsensusType(foundConsensus.first())
                            workingFlight = f.copy(
                                registration = foundConsensus.first(),
                                aircraft = consensus?.shortName ?: "????".also{feedback(EditFlightFragmentEvents.ERROR)}
                            )
                        }
                        foundConsensus.isEmpty() -> feedback (EditFlightFragmentEvents.REGISTRATION_NOT_FOUND)
                        else -> feedback(EditFlightFragmentEvents.REGISTRATION_HAS_MULTIPLE_CONSENSUS).apply{
                            extraData.putCharSequenceArrayList("foundMatches", ArrayList(foundConsensus))
                        }
                    }
                }
            }
            else {  // TODO check if entry is format [alphanumeric(alphanumeric)], do something with that?
                    // TODO Maybe just get rid of this?
                workingFlight = f.withRegAndType(enteredData)
            }
        }
    }

    val takeoffLandings: LiveData<String> = distinctUntilChanged(Transformations.map(flight) { it.takeoffLanding })
    fun setTakeoffLandings(enteredData: String){
        require(enteredData.all { it in "0123456789"}) { "$enteredData did not consist of only numbers"}
        workingFlight?.let {f ->
            viewModelScope.launch(Dispatchers.IO) {
                val landings = enteredData.toInt()
                val orig = airportRepository.getAirportOnce(f.orig)
                val dest = airportRepository.getAirportOnce(f.dest)
                if (orig == null || dest == null) feedback(EditFlightFragmentEvents.AIRPORT_NOT_FOUND_FOR_LANDINGS)
                // If airports are not found (because, for instance, custom airport was used)
                // it will log day TO and landing
                workingFlight = f.withTakeoffLandings(landings, orig, dest)
            }
        }
    }

    val name: LiveData<String> = distinctUntilChanged(Transformations.map(flight) { it.name })
    fun setName(name: String){
        workingFlight?.let {
            workingFlight = it.copy(name = name)
        }
    }

    val name2: LiveData<String> = distinctUntilChanged(Transformations.map(flight) { it.name2 })
    fun setName2(name2: String){
        workingFlight?.let {
            workingFlight = it.copy(name2 = name2)
        }
    }

    val remarks: LiveData<String> = distinctUntilChanged(Transformations.map(flight) { it.remarks })
    fun setRemarks(remarks: String){
        workingFlight?.let {
            workingFlight = it.copy(remarks = remarks)
        }
    }

    /*********************************************************************************************
     * Toggle switches:
     *********************************************************************************************/

    val sign: LiveData<Boolean> = distinctUntilChanged(Transformations.map(flight) { it.signature.isNotEmpty() })
    //no setter for signature as this always goes through dialog

    val sim: LiveData<Boolean> = distinctUntilChanged(Transformations.map(flight) { it.isSim != 0 })
    fun toggleSim() = workingFlight?.let { workingFlight = it.copy(isSim = toggleZeroAndOne(it.isSim)) }

    val dual: LiveData<Boolean> = distinctUntilChanged(Transformations.map(flight) { it.isDual != 0 })
    fun toggleDual() = workingFlight?.let { workingFlight = it.copy(isDual = toggleZeroAndOne(it.isDual)) }

    val instructor: LiveData<Boolean> = distinctUntilChanged(Transformations.map(flight) { it.isInstructor != 0 })
    fun toggleInstructor() = workingFlight?.let { workingFlight = it.copy(isInstructor = toggleZeroAndOne(it.isInstructor)) }

    val picus: LiveData<Boolean> = distinctUntilChanged(Transformations.map(flight) { it.isPICUS != 0 })
    fun togglePicus() = workingFlight?.let { workingFlight = it.copy(isPICUS = toggleZeroAndOne(it.isPICUS)) }

    val pic: LiveData<Boolean> = distinctUntilChanged(Transformations.map(flight) { it.isPIC != 0 })
    fun togglePic() = workingFlight?.let { workingFlight = it.copy(isPIC = toggleZeroAndOne(it.isPIC)) }

    val pf: LiveData<Boolean> = distinctUntilChanged(Transformations.map(flight) { it.isPF != 0 })
    fun togglePf() = workingFlight?.let { workingFlight = it.copy(isPF = toggleZeroAndOne(it.isPF)) }

    val autoFill: LiveData<Boolean> = distinctUntilChanged(Transformations.map(flight) { it.autoFill != 0 })
    fun setAutoFill(on: Boolean) = workingFlight?.let { workingFlight = it.copy(autoFill = on.toInt()) }

    /*********************************************************************************************
     * Miscellaneous
     *********************************************************************************************/

    val allNames = distinctUntilChanged(flightRepository.allNames)

    val checkSim: Boolean?
        get() = workingFlight?.isSim?.toBoolean()
}
