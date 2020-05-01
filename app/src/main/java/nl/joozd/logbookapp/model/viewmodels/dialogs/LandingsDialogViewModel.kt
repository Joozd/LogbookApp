package nl.joozd.logbookapp.model.viewmodels.dialogs

import androidx.lifecycle.Transformations
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.lifecycle.ViewModel
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.data.repository.FlightRepository
import nl.joozd.logbookapp.extensions.minusOneWithFloor
import nl.joozd.logbookapp.utils.InitialSetFlight

class LandingsDialogViewModel: ViewModel() {
    private val flightRepository = FlightRepository.getInstance()

    private val undoFlight = InitialSetFlight()

    init{
        undoFlight.flight = flightRepository.workingFlight.value
    }
    fun undo(){
        undoFlight.flight?.let {flightRepository.updateWorkingFlight(it)}
    }

    val flight = distinctUntilChanged(flightRepository.workingFlight)
    fun updateFlight(f: Flight?) = f?.let { flightRepository.updateWorkingFlight(it) }
    val workingFlight: Flight?
        get() = flightRepository.workingFlight.value

    val toDay = Transformations.map(flight){ it.takeOffDay }
    val toNight = Transformations.map(flight){ it.takeOffNight }
    val ldgDay = Transformations.map(flight){ it.landingDay }
    val ldgNight = Transformations.map(flight){ it.landingNight }
    val autoland = Transformations.map(flight){ it.autoLand }



    fun toDayUpButtonClick() { workingFlight?.let { updateFlight(it.copy(takeOffDay = it.takeOffDay + 1)) }}
    fun toNightUpButtonClick() { workingFlight?.let { updateFlight(it.copy(takeOffNight = it.takeOffNight + 1)) }}
    fun ldgDayUpButtonClick() { workingFlight?.let { updateFlight(it.copy(landingDay = it.landingDay + 1)) }}
    fun ldgNightUpButtonClick() { workingFlight?.let { updateFlight(it.copy(landingNight = it.landingNight + 1)) }}
    fun autolandUpButtonClick() { workingFlight?.let { updateFlight(it.copy(autoLand = it.autoLand + 1)) }}

    fun toDayDownButtonClick() { workingFlight?.let { updateFlight(it.copy(takeOffDay = it.takeOffDay.minusOneWithFloor(0))) }}
    fun toNightDownButtonClick() { workingFlight?.let { updateFlight(it.copy(takeOffNight = it.takeOffNight.minusOneWithFloor(0))) }}
    fun ldgDayDownButtonClick() { workingFlight?.let { updateFlight(it.copy(landingDay = it.landingDay.minusOneWithFloor(0))) }}
    fun ldgNightDownButtonClick() { workingFlight?.let { updateFlight(it.copy(landingNight = it.landingNight.minusOneWithFloor(0))) }}
    fun autolandDownButtonClick() { workingFlight?.let { updateFlight(it.copy(autoLand = it.autoLand.minusOneWithFloor(0))) }}
}