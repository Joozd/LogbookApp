package nl.joozd.logbookapp.model.viewmodels

import androidx.lifecycle.Transformations
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.utils.InitialSetFlight

open class JoozdlogDialogViewModel: JoozdlogViewModel() {
    private val undoFlight = InitialSetFlight()

    init{
        undoFlight.flight = flightRepository.workingFlight.value
    }
    fun undo(){
        undoFlight.flight?.let {flightRepository.updateWorkingFlight(it)}
    }

    val flight = Transformations.distinctUntilChanged(flightRepository.workingFlight)
    protected var workingFlight: Flight?
        get() = flightRepository.workingFlight.value
        set(f){
            f?.let {
                flightRepository.updateWorkingFlight(it)
            }
        }
}