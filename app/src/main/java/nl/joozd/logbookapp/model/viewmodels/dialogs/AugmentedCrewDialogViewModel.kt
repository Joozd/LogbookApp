package nl.joozd.logbookapp.model.viewmodels.dialogs

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import nl.joozd.logbookapp.data.miscClasses.Crew
import nl.joozd.logbookapp.data.repository.FlightRepository
import nl.joozd.logbookapp.utils.InitialSetFlight

class AugmentedCrewDialogViewModel: ViewModel() {
    private val flightRepository = FlightRepository.getInstance()

    private val undoFlight = InitialSetFlight()

    init{
        undoFlight.flight = flightRepository.workingFlight.value
    }
    fun undo(){
        undoFlight.flight?.let {flightRepository.updateWorkingFlight(it)}
    }


    val augmentedCrewData: LiveData<Crew> = Transformations.map(flightRepository.workingFlight) { Crew.of(it.augmentedCrew)}

    fun crewDown(){
        TODO("Not Implenmented")
    }
    fun crewUp(){
        TODO("Not Implenmented")
    }
    fun setTakeoff(takeoff: Boolean){
        TODO("Not Implenmented")
    }
    fun setLanding(landing: Boolean){
        TODO("Not Implenmented")
    }
    fun setTakeoffLandingTime(time: Int){
        TODO("Not Implenmented")
    }


}
