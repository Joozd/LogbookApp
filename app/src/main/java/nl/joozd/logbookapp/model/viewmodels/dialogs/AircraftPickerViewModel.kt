package nl.joozd.logbookapp.model.viewmodels.dialogs

import android.util.Log
import androidx.lifecycle.*
import androidx.lifecycle.Transformations.distinctUntilChanged
import kotlinx.android.synthetic.main.item_sim.*
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.repository.helpers.Aircraft
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel

class AircraftPickerViewModel: JoozdlogDialogViewModel(){
    private val _typesSearchString = MutableLiveData<String>()
    private val typesSearchString
        get() = _typesSearchString.value ?: ""

    val aircraftTypes = MediatorLiveData<List<String>>()
    init{
        aircraftTypes.addSource(aircraftRepository.liveAircraftTypes){
            aircraftTypes.value = it.map{ac -> ac.name}.filter{typesSearchString in it}
        }
        aircraftTypes.addSource(_typesSearchString){
            aircraftTypes.value = (aircraftRepository.liveAircraftTypes.value ?: emptyList()).map{ac -> ac.name}.filter{typesSearchString in it}
        }
    }

    val selectedAircraft: LiveData<Aircraft> = distinctUntilChanged(aircraftRepository.activeAircraft)

    val selectedAircraftString: LiveData<String> = Transformations.map(aircraftRepository.activeAircraft) { it.type?.name ?: "UNKNOWN"}

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

    fun saveAircraft() {
        selectedAircraft.value?.run{
            if (type != null){
                aircraftRepository.updateAircraftRegistrationWithType(registration, type)
            }
        }
        workingFlight?.let {
            Log.d(this::class.simpleName, "selectedAircraft = ${selectedAircraft.value}")
            workingFlight = it.copy(
                registration = selectedAircraft.value?.registration ?: "".also{Log.d(this::class.simpleName, "selectedAircraft.value?.regustration is null")},
                aircraft = selectedAircraft.value?.type?.shortName ?: ""
            ).also{Log.d(this@AircraftPickerViewModel::class.simpleName,"saved $it")}
            //TODO: Save reg+type to repo
        }
        Log.d(this::class.simpleName, "SAVED FLIGHT $workingFlight")
    }

    fun updateSearchString(query: String){
        _typesSearchString.value = query
    }

}