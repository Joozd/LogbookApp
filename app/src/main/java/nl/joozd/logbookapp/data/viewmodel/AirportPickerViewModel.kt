package nl.joozd.logbookapp.data.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import nl.joozd.logbookapp.data.dataclasses.Airport
import java.util.*

class AirportPickerViewModel: ViewModel(){
    private var _airports = MutableLiveData<List<Airport>>()
    var filteredAirports = MutableLiveData<List<Airport>>()
    private var currentJob: Job = Job()

    var airports: List<Airport>
        get() = _airports.value ?: emptyList()
        set(a) {_airports.value = a}


    val liveAirports = distinctUntilChanged(_airports)
    val foundAirports = distinctUntilChanged(filteredAirports)



}