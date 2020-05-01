package nl.joozd.logbookapp.model.viewmodels.dialogs

import androidx.lifecycle.*
import androidx.lifecycle.Transformations.distinctUntilChanged
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.helpers.FeedbackEvents.AirportPickerEvents.ORIG_OR_DEST_NOT_SELECTED
import nl.joozd.logbookapp.model.helpers.FeedbackEvents.AirportPickerEvents.NOT_IMPLEMENTED
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel

//TODO make sure list gets filled straight away?
//TODO sort airportsList based on ICAO/IATA prefs?
class AirportPickerViewModel: JoozdlogDialogViewModel(){
    private var currentSearchJob: Job = Job()

    /**
     * this MUST be set in onActivityCreated in Fragment so feedback event will be observed
     * also, feedbackEvent must be observed. If [workingOnOrig] == null, things won't work.
     */
    private var workingOnOrig: Boolean? = null
    fun setWorkingOnOrig(orig: Boolean?){
        workingOnOrig = orig
        if (orig == null) feedback(ORIG_OR_DEST_NOT_SELECTED)
    }

    private val _airportsList = MutableLiveData<List<Airport>>()
    val airportsList: LiveData<List<Airport>> = distinctUntilChanged(_airportsList)

    private val _pickedAirport = MutableLiveData<Airport>()
    val pickedAirport: LiveData<Airport>
        get() = distinctUntilChanged(_pickedAirport)

    fun pickAirport(airport: Airport){
        //TODO
        feedback(NOT_IMPLEMENTED)
    }

    fun setCustomAirport(airport: String){
        //TODO
        feedback(NOT_IMPLEMENTED)
    }

    @ExperimentalCoroutinesApi
    fun updateSearch(query: String){
        currentSearchJob.cancel()
        currentSearchJob = viewModelScope.launch{
            collectAirports(airportRepository.getQueryFlow(query))
        }
        //feedback(NOT_IMPLEMENTED)
    }

    @ExperimentalCoroutinesApi
    suspend fun collectAirports(flow: Flow<List<Airport>>){
        flow.conflate().collect {
            _airportsList.value = it
            delay(200)
        }
    }
}
