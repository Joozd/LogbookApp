package nl.joozd.logbookapp.model.viewmodels.activities



import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.toBoolean
import nl.joozd.logbookapp.model.dataclasses.DisplayFlight
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.helpers.FeedbackEvents.MainActivityEvents
import nl.joozd.logbookapp.model.viewmodels.fragments.JoozdlogActivityViewModel

class MainActivityViewModel: JoozdlogActivityViewModel() {
    /**
     * Menu functions
     */
    fun menuSelectedDoSomething(){
        Log.d("useIataAirports", "before: ${Preferences.useIataAirports}")
        Preferences.useIataAirports = !Preferences.useIataAirports
        Log.d("useIataAirports", "after: ${Preferences.useIataAirports}")
        Log.d("displayFlightsList.value", "after: ${displayFlightsList.value}")
    }

    fun menuSelectedRebuild(){
        feedback(MainActivityEvents.NOT_IMPLEMENTED)
        /*
val ENABLE_REBUILD = true
if (ENABLE_REBUILD) {
    launch { MenuFunctions.rebuildFlightsFromServer(this@MainActivity) }
}
else{
    longToast("Disabled!")            }

 */
    }

    fun menuSelectedAddFlight() = addFlight()

    fun menuSelectedTotalTimes(){
        feedback(MainActivityEvents.NOT_IMPLEMENTED)
    }

    fun menuSelectedBalanceForward(){
        feedback(MainActivityEvents.NOT_IMPLEMENTED)
    }

    fun menuSelectedSearch(){
        feedback(MainActivityEvents.NOT_IMPLEMENTED)
    }

    fun menuSelectedEditAircraft(){
        feedback(MainActivityEvents.NOT_IMPLEMENTED)
        /*
        launch {
            progressBarField?.let { pbf ->
                val progBar = JoozdlogProgressBar(
                    pbf
                ).apply {
                    backgroundColor = getColorFromAttr(android.R.attr.colorPrimary)
                    text = getString(R.string.loadingAirports)
                }.show()
                Cloud.getAircraftTypes{progBar.progress = it}?.let { result ->
                    launch(NonCancellable) {
                        aircraftRepository.saveAircraftTypes(result)
                        Log.d(this::class.simpleName,"repo now has ${aircraftRepository.liveAircraftTypes.value?.size} types")
                    }
                }
                progBar.remove()
            }
        }
         */
    }

    fun menuSelectedExportPDF(){
        feedback(MainActivityEvents.NOT_IMPLEMENTED)
    }

    /**
     * Observable data:
     */

    private val _displayFlightsList2= MediatorLiveData<List<DisplayFlight>>()
    init{
        _displayFlightsList2.addSource(flightRepository.liveFlights) { fff ->
            Log.d("DEBUGGGGGG111111", "list has ${fff.size} items")
            _displayFlightsList2.value = fff.map{DisplayFlight.of(it, airportRepository.icaoIataMap.value ?: emptyMap(), Preferences.useIataAirports) }
        }
        _displayFlightsList2.addSource(airportRepository.icaoIataMap) {map ->
            Log.d("DEBUGGGGG222222", "list has ${flightRepository.liveFlights.value?.size} items")
            _displayFlightsList2.value = flightRepository.liveFlights.value?.map{DisplayFlight.of(it, map ?: emptyMap(), Preferences.useIataAirports) } ?: emptyList()
        }
        _displayFlightsList2.addSource(airportRepository.useIataAirports){useIataAirports ->
            Log.d("DEBUGGGGG3333333", "list has ${flightRepository.liveFlights.value?.size} items")
            _displayFlightsList2.value = flightRepository.liveFlights.value?.map{DisplayFlight.of(it,airportRepository.icaoIataMap.value ?: emptyMap(), useIataAirports) }
        }
    }
    val displayFlightsList: LiveData<List<DisplayFlight>>
        get() = _displayFlightsList2



    /**
     * Handler for clickety thingies
     */
    fun showFlight(flightID: Int){
        viewModelScope.launch(Dispatchers.IO) {
            flightRepository.fetchFlightByIdToWorkingFlight(flightID)
            feedback(MainActivityEvents.SHOW_FLIGHT)
        }

    }

    fun addFlight(){
        viewModelScope.launch(Dispatchers.IO) {
            flightRepository.createNewWorkingFlight()
            feedback(MainActivityEvents.SHOW_FLIGHT)
        }
        feedback(MainActivityEvents.NOT_IMPLEMENTED)
    }
    fun saveWorkingFlight() {
        flightRepository.saveWorkingFlight()
        feedback(MainActivityEvents.FLIGHT_SAVED)
    }

    fun undoSaveWorkingFlight() = flightRepository.undoSaveWorkingFlight()

    /**
     * Does one of three things:
     * - if the flight with flightID [id] is:
     *  * nonexistent:      feedback(FLIGHT_NOT_FOUND)
     *  * planned:          Delete flight
     *  * Not planned:      feedback(TRYING_TO_DELETE_COMPLETED_FLIGHT) with its id in ExtraData as "ID"
     */
    fun deleteFlight(id: Int){
        viewModelScope.launch {
            withContext (Dispatchers.IO) { flightRepository.fetchFlightByID(id)}.let{
                when(it?.isPlanned?.toBoolean()){
                    null -> feedback(MainActivityEvents.FLIGHT_NOT_FOUND)
                    true -> {
                        flightRepository.delete(it)
                        feedback(MainActivityEvents.DELETED_FLIGHT)
                    }
                    false -> feedback(MainActivityEvents.TRYING_TO_DELETE_COMPLETED_FLIGHT).apply{
                        extraData.putInt("ID", id)
                    }
                }
            }
        }
    }

    fun deleteNotPlannedFlight(id: Int) = flightRepository.delete(id)


    /**
     * Internal functions:
     */

}