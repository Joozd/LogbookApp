/*
 * JoozdLog Pilot's Logbook
 * Copyright (C) 2020 Joost Welle
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see https://www.gnu.org/licenses
 */

package nl.joozd.logbookapp.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.*
import nl.joozd.logbookapp.R

import nl.joozd.logbookapp.data.room.Repository

//deprecated


import kotlin.coroutines.CoroutineContext

class PdfParserActivity : AppCompatActivity(), CoroutineScope {
    companion object {
        const val TAG = "PdfParserActivity"
    }
    override val coroutineContext: CoroutineContext = Dispatchers.Main + Job()
    private val repository = Repository.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("PdfParserActivity", "started")
        setContentView(R.layout.activity_pdf_parser)

        /*
        launch {

            // val flightDb = FlightDb()
            val allFlights = repository.requestValidFlights()
            Log.d(TAG, "allFlights: ${allFlights.size}")

            loadingFlightsText.visibility = View.VISIBLE

            //val airportDb = AirportDb()
            //map[IATA] = ICAO
            val airportsMap = airportDb.makeIcaoIataPairs().toMap().reversedMap()

            loadingAirportsText.visibility = View.VISIBLE

            //earliestTime is the on-blocks time of latest completed flight in DB. Used as cut-off moment for new flights.
            val earliestTime = mostRecentCompleteFlight(
                allFlights
            ).timeIn
            val alreadyPlannedFlights = allFlights.filter { it.planned }

            readingFileText.visibility = View.VISIBLE

            intent?.let {
                Log.d(TAG, intent.action ?: "No intent.action")
                Log.d(TAG, intent.type ?: "No intent.type")
                (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
                    val inputStream = try {
                        /*
                     * Get the content resolver instance for this context, and use it
                     * to get a ParcelFileDescriptor for the file.
                     */
                        contentResolver.openInputStream(it)
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                        Log.e(TAG, "File not found.")
                        return@launch
                    }
                    if (inputStream == null) {
                        Log.e(TAG, "Inputstream is null")
                        alert("Error: Inputstream is null \n(error $TAG 01").show()
                        return@launch
                    }

                    //Now, we have an inputstream containing (hopefully) a roster

                    val roster = withContext(Dispatchers.IO) { KlcRosterParser(inputStream) }
                    if (!roster.seemsValid) {
                        Log.w(TAG, "roster.seemsValid == false")
                        runOnUiThread {
                            alert("This doesn't seem to be a KLC Roster") { negativeButton("Close") { finish() } }.show()
                        }
                    }
                    readingFileCheck.visibility = View.VISIBLE
                    // flightsToPlan are all flights in roster after most recent completed flight on-blocks time
                    val flightsToPlan = roster.days.map { it.events }.flatten()
                        .filter { it.type == Activities.FLIGHT }
                        .filter { it.startEpochSecond > earliestTime }
                    Log.d(TAG, "flightsToPlan: ${flightsToPlan.size}")
                    // simsToPlan are all actual sim times in roster after most recent completed flight on-blocks time
                    val simsToPlan = roster.days.map { it.events }.flatten()
                        .filter { it.type == Activities.ACTUALSIM }
                        .filter { it.startEpochSecond > earliestTime }
                    Log.d(TAG, "simsToPlan: ${simsToPlan.size}")
                    //days is a list of pairs (start of day, end of day)
                    val days =
                        roster.days.map { it.startOfDayEpochSecond to it.endOfDayEpochSecond }
                    Log.d(TAG, "days: ${roster.days.size}")

                    //remove planned flights from DB that are on a date that new flights are inserted on
                    //viewModel takes care of whether or not it is synced etc.
                    Log.d(TAG, "alreadyPlannedFlights:  ${alreadyPlannedFlights.size}")
                    repository.deleteFlights(alreadyPlannedFlights.filter { pf -> days.any { day -> pf.timeIn in (day.first..day.second) } }.also{
                        Log.d(TAG, "Deleting ${it.size} flights")
                    })


                    val nextFlightId = repository.highestFlightId() + 1
                    savingFlightsText.visibility = View.VISIBLE
                    val newFlights: List<Flight> =
                        flightsToPlan.mapIndexed { index: Int, rf: KlcRosterEvent ->
                            val flightNumOrigArrowDest = rf.description.split(" ").map { it.trim() }
                            val flightNumber = flightNumOrigArrowDest[0]
                            val orig = flightNumOrigArrowDest[1]
                            val dest = flightNumOrigArrowDest[3]

                            Flight(
                                nextFlightId + index,
                                orig = airportsMap[orig] ?: "XXXX",
                                dest = airportsMap[dest] ?: "XXXX",
                                timeOut = rf.startEpochSecond,
                                timeIn = rf.endEpochSecond,
                                flightNumber = flightNumber,
                                timeStamp = Instant.now().epochSecond + Preferences.serverTimeOffset,
                                changed = 1
                            )
                        }
                    // TODO add simsToPlan

                    Log.d(TAG, "Saving ${newFlights.size} flights")
                    repository.saveFlights(newFlights)

                    savingFlightsCheck.visibility = View.VISIBLE
                    startingMainActivityText.visibility = View.VISIBLE

                    //start main activity
                    startMainActivity(App.instance)
                    finish()
                }
            }
        }

         */
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancel()
    }
}



