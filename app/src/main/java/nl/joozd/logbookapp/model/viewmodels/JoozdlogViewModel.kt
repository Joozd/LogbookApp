package nl.joozd.logbookapp.model.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.repository.AircraftRepository
import nl.joozd.logbookapp.data.repository.AirportRepository
import nl.joozd.logbookapp.data.repository.FlightRepository
import nl.joozd.logbookapp.model.helpers.FeedbackEvent
import nl.joozd.logbookapp.model.helpers.FeedbackEvents

open class JoozdlogViewModel: ViewModel() {
    protected val flightRepository = FlightRepository.getInstance()
    protected val aircraftRepository = AircraftRepository.getInstance()
    protected val airportRepository = AirportRepository.getInstance()

    private val _feedbackEvent = MutableLiveData<FeedbackEvent>()
    val feedbackEvent: LiveData<FeedbackEvent>
        get() = _feedbackEvent

    protected fun feedback(event: FeedbackEvents.Event): FeedbackEvent =
        FeedbackEvent(event).also{
            viewModelScope.launch(Dispatchers.Main) {_feedbackEvent.value = it }
        }
}