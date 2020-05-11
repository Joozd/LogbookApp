package nl.joozd.logbookapp.model.viewmodels.activities

import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joozd.klcrosterparser.Activities
import nl.joozd.klcrosterparser.KlcRosterParser
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.extensions.toBoolean
import nl.joozd.logbookapp.model.dataclasses.Flight
import nl.joozd.logbookapp.model.helpers.FeedbackEvents.PdfParserActivityEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import nl.joozd.logbookapp.ui.activities.helpers.PdfRosterFunctions.makeFlightsList
import nl.joozd.logbookapp.utils.reversed
import java.io.FileNotFoundException

class PdfParserActivityViewModel: JoozdlogActivityViewModel() {
    private val _progressText = MutableLiveData<String>()
    val progressText: LiveData<String>
        get() = _progressText
    //no public setter
    private fun setProgressText(it: String){
        _progressText.value = it
    }

    private val _progress = MutableLiveData<Int>()
    val progress: LiveData<Int>
        get() = _progress
    //no public setter
    private fun setProgress(progress: Int){
        _progress.value = progress
    }

    private fun updateProgress(percentage: Int, text: String){
        setProgress(percentage)
        setProgressText(text)
    }

    private var intent: Intent? = null
    fun runOnce(newIntent: Intent){
        if (intent == null){
            intent = newIntent
            run()
        }
    }

    private fun run(){
        //TODO this is where the magic happens
        intent?.let {
            updateProgress(0, "AAAAAAAA")
            viewModelScope.launch {
                val getMostRecentFlight = flightRepository.getMostRecentFlight()
                val getHighestID = flightRepository.getHighestId()
                (it.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let { uri ->
                    val inputStream = try {
                        /*
                 * Get the content resolver instance for this context, and use it
                 * to get a ParcelFileDescriptor for the file.
                 */
                        App.instance.contentResolver.openInputStream(uri)
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                        feedback(PdfParserActivityEvents.FILE_NOT_FOUND)
                        return@launch
                    }
                    if (inputStream == null) {
                        feedback(PdfParserActivityEvents.ERROR)
                        return@launch
                    }
                    //TODO do something with progress(received data)
                    updateProgress(20, "BBBBBBBBBBB")

                    val roster = withContext(Dispatchers.IO) { KlcRosterParser(inputStream) }
                    if (!roster.seemsValid) {
                        feedback(PdfParserActivityEvents.NOT_A_KNOWN_ROSTER)
                        return@launch
                    }

                    //TODO do something with progress (file read)
                    updateProgress(40, "CCCCCCCCCC")
                    val cutoffTime = getMostRecentFlight.await()?.timeIn ?: 0
                    val alreadyPlannedFlights = flightRepository.getAllFlights().filter{it.isPlanned.toBoolean()}

                    val flightsToPlan = roster.days.map { it.events }.flatten()
                        .filter { it.type == Activities.FLIGHT }
                        .filter { it.startEpochSecond > cutoffTime }
                    // simsToPlan are all actual sim times in roster after most recent completed flight on-blocks time
                    val simsToPlan = roster.days.map { it.events }.flatten()
                        .filter { it.type == Activities.ACTUALSIM }
                        .filter { it.startEpochSecond > cutoffTime }

                    //days is a list of pairs (start of day, end of day)
                    val days = roster.days.map { it.startOfDayEpochSecond to it.endOfDayEpochSecond }
                    flightRepository.delete(alreadyPlannedFlights.filter { pf -> days.any { day -> pf.timeIn in (day.first..day.second) } })

                    //TODO do something with progress (old flights removed)
                    updateProgress(60, "DDDDDDDDD")

                    val nextFlightId = getHighestID.await() + 1
                    val iataIcaoMap = (airportRepository.icaoIataMap.value ?: emptyMap()).reversed()

                    val newFlights: List<Flight> = makeFlightsList(flightsToPlan, nextFlightId, iataIcaoMap)

                    //TODO do something with progress (flights are created from roster)
                    updateProgress(80, "EEEEEEEEE")

                    flightRepository.saveFlights(newFlights)

                    //TODO do something with progress (done)
                    updateProgress(100, "FFFFFFFFFFFF")

                    feedback(PdfParserActivityEvents.ROSTER_SUCCESSFULLY_ADDED)

                }
            }
        } ?: feedback(PdfParserActivityEvents.FILE_NOT_FOUND)

        //Now, we have an inputstream containing (hopefully) a roster
    }


}