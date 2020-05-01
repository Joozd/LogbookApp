package nl.joozd.logbookapp.model.viewmodels

import androidx.lifecycle.*
import androidx.lifecycle.Transformations.distinctUntilChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.toggleZeroAndOne
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.withDate
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.withRegAndType
import nl.joozd.logbookapp.model.helpers.FlightDataEntryFunctions.withTakeoffLandings
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions.getDateStringFromEpochSeconds
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions.getTimestringFromEpochSeconds
import nl.joozd.logbookapp.extensions.toInt
import nl.joozd.logbookapp.model.helpers.FeedbackEvents.EditFlightFragmentEvents.AIRPORT_NOT_FOUND_FOR_LANDINGS
import nl.joozd.logbookapp.model.helpers.FeedbackEvents.EditFlightFragmentEvents.INVALID_REG_TYPE_STRING
import nl.joozd.logbookapp.model.helpers.FeedbackEvents.EditFlightFragmentEvents.NOT_IMPLEMENTED
import java.time.LocalDate

class EditFlightFragmentViewModel: JoozdlogDialogViewModel(){

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
        flight.value?.let {
            feedback(NOT_IMPLEMENTED)
            // TODO: Try to find an airport from this string.
            // Perhaps take a look at deprecated EditFlightNew class.
            // flightRepository.setWorkingFlight(it.copy(orig = XXXXXXXXXX))
        }
    }

    //TODO IATA or ICAO display
    private val _dest: LiveData<String> = Transformations.map(flight) { it.dest }
    val dest: LiveData<String> = distinctUntilChanged(_dest)
    fun setDest(enteredData: String){
        flight.value?.let {
            feedback(NOT_IMPLEMENTED)
            // TODO: Try to find an airport from this string.
            // Perhaps take a look at deprecated EditFlightNew class.
            // flightRepository.setWorkingFlight(it.copy(orig = XXXXXXXXXX))
        }
    }

    private val _timeOut: LiveData<String> = Transformations.map(flight) { getTimestringFromEpochSeconds(it.timeOut) }
    val timeOut: LiveData<String> = distinctUntilChanged(_timeOut)
    fun setTimeOut(enteredData: String){
        flight.value?.let {
            feedback(NOT_IMPLEMENTED)
            // TODO: Try to build a time from this string.
            // Perhaps take a look at deprecated EditFlightNew class.
            // flightRepository.setWorkingFlight(it.copy(orig = XXXXXXXXXX))
            //flightRepository.setWorkingFlight(it.copy(timeOut = XXXXXXXXXX))
        }
    }

    private val _timeIn: LiveData<String> = Transformations.map(flight) { getTimestringFromEpochSeconds(it.timeIn) }
    val timeIn: LiveData<String> = distinctUntilChanged(_timeIn)
    fun setTimeIn(enteredData: String){
        flight.value?.let {
            feedback(NOT_IMPLEMENTED)
            // TODO: Try to build a time from this string.
            // Perhaps take a look at deprecated EditFlightNew class.
            // flightRepository.setWorkingFlight(it.copy(orig = XXXXXXXXXX))
            //flightRepository.setWorkingFlight(it.copy(timeOut = XXXXXXXXXX))
        }
    }

    val regAndType: LiveData<String> = distinctUntilChanged(Transformations.map(flight) { it.regAndType() })
    fun setRegAndType(enteredData: String){
        workingFlight?.let{
            if (!("(" in enteredData && ")" in enteredData)){
                //TODO: Search for aircraft/type from this
                // for now: send INVALID_REG_TYPE_STRING event to empty on false entry:
                feedback(INVALID_REG_TYPE_STRING)
            }
            workingFlight = it.withRegAndType(enteredData)
        }
    }

    val takeoffLandings: LiveData<String> = distinctUntilChanged(Transformations.map(flight) { it.takeoffLanding })
    fun settakeoffLandings(enteredData: String){
        require(enteredData.all { it in "0123456789"}) { "$enteredData did not consist of only numbers"}
        workingFlight?.let {f ->
            viewModelScope.launch(Dispatchers.IO) {
                val landings = enteredData.toInt()
                val orig = airportRepository.getAirportOnce(f.orig)
                val dest = airportRepository.getAirportOnce(f.dest)
                if (orig == null || dest == null) feedback(AIRPORT_NOT_FOUND_FOR_LANDINGS)
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
    fun setname2(name2: String){
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
}
