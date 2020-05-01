package nl.joozd.logbookapp.model.viewmodels.dialogs

import androidx.lifecycle.*
import androidx.lifecycle.Transformations.distinctUntilChanged
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.repository.helpers.Aircraft
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel

class AircraftPickerViewModel: JoozdlogDialogViewModel(){

    val selectedAircraft: LiveData<Aircraft> = distinctUntilChanged(aircraftRepository.activeAircraft)

    val selectedAircraftString: LiveData<String> = Transformations.map(aircraftRepository.activeAircraft) { it.type?.name}

    fun selectAircraftByString(typeString: String){
        viewModelScope.launch {
            val newType = aircraftRepository.getAircraftType(typeString)
            aircraftRepository.updateActiveAircraft(type = newType)
        }

        //TODO set AircraftRegistrationWithTypeData
    }

    val registration: LiveData<String> = Transformations.map(aircraftRepository.activeAircraft) { it.registration }
    fun updateRegistration(reg: String){
        aircraftRepository.updateActiveAircraft(reg = reg, flight = flightRepository.workingFlight.value)
        //TODO search for known type with that reg, if null, seacrh from flight, if null search for consensus
        //TODO set found type as [_selectedAircraftString] and [_selectedAircraftType]
    }
    fun start(){
        aircraftRepository.updateActiveAircraft(flight = flightRepository.workingFlight.value)
    }



    fun updateSearchString(query: String){
        //TODO
    }

}