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

package nl.joozd.logbookapp.ui.activities.mainactivity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import androidx.recyclerview.widget.LinearLayoutManager
import android.util.Log
import android.view.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_main_new.*
import kotlinx.android.synthetic.main.item_progressbar.view.*
import kotlinx.coroutines.*

import nl.joozd.logbookapp.comm.Cloud

import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.joozdlogcommon.BalanceForward
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.dataclasses.Flight

import nl.joozd.logbookapp.data.room.Repository

import nl.joozd.logbookapp.ui.adapters.FlightsAdapter

import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.data.utils.*
import nl.joozd.logbookapp.data.viewmodel.JoozdlogViewModel
import nl.joozd.logbookapp.extensions.*
import nl.joozd.logbookapp.ui.activities.*
import nl.joozd.logbookapp.ui.fragments.EditFlightFragment


import nl.joozd.logbookapp.ui.utils.CustomSnackbar
import nl.joozd.logbookapp.ui.utils.longToast
import nl.joozd.logbookapp.ui.utils.toast


//deprecated

import kotlin.coroutines.CoroutineContext


class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    private val viewModel: JoozdlogViewModel by viewModels()
    private val repository = Repository.getInstance()

    class ShowMenuListener(private val f: () -> Unit){
        fun go(){
            f()
        }
    }


    val fragmentManager = supportFragmentManager

    // private val airportDb = AirportDb()
    // private val aircraftDb = AircraftDb()
    // private val balanceForwardDb = BalanceForwardDb()

    // private var allFlights: List<Flight> = emptyList() // dont use this anymore go with viewModel data
    /* set(value){
        //met dank aan LuukLuuk!
        field=value
        supportActionBar?.title = "${value.filter{it.DELETEFLAG == 0}.size} Flights"
    } */
    var allAircraft: List<Aircraft> = emptyList()

    //TODO switch this to viewModel
    //private val flightSearcher = FlightSearcher(FlightDb(), AirportDb(), aircraftDb)

    // private var namesWorker:NamesWorker = NamesWorker()
    private var showTotalTimesMenuListener: ShowMenuListener? = null
    private var showBalanceForwardListener: ShowMenuListener? = null
    private var balancesForward: List<BalanceForward> = emptyList()
    private var foundMissingAircraft: List<Aircraft> = emptyList()

    private var snackbarShowing: CustomSnackbar? = null

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_rebuild -> {
            val ENABLE_REBUILD = true
            if (ENABLE_REBUILD) {
                launch { MenuFunctions.rebuildFlightsFromServer(this@MainActivity) }
            }
            else{
                longToast("Disabled!")            }
            true
        }
        R.id.menu_add_flight -> {
            showFlight(null)
            true
        }
        R.id.menu_total_times -> {
            showTotalTimesMenuListener?.go()
            true
        }
        R.id.menu_balance_forward -> {
            showBalanceForwardListener?.go()
            true
        }
        R.id.app_bar_search -> {
            if (searchField.visibility == View.VISIBLE){
                searchField.visibility = View.GONE
                pilotNameSearch.setText("")
                airportSearch.setText("")
                aircraftSearch.setText("")
                addButton.show()
            }
            else {
                if (false) {
                    searchField.visibility = View.VISIBLE
                    addButton.hide()
                }
                else toast("disabled")
            }
            true
        }
        R.id.menu_edit_aircraft -> {
            val intent = Intent(this, EditAircraftActivity::class.java)
            intent.putExtra("missingAircraft", foundMissingAircraft.toTypedArray())
            intent.putExtra("allAircraft", allAircraft.toTypedArray())
            startActivity(intent)

            true
        }
        R.id.menu_export_pdf -> {
            val intent = Intent(this, ExportPdfActivity::class.java)
            startActivity(intent)

            true
        }
        R.id.menu_do_something -> {
            launch { MenuFunctions.sendAllFlightsAndToastResult() }
            /*
            doAsync{
                when(Cloud.justSendFlights(allFlights)){
                    0 -> runOnUiThread { longToast("Flights sent ok!") }
                    1 -> runOnUiThread { longToast("login error") }
                    2 -> runOnUiThread { longToast("sending error") }
                }
            }*/

            true
        }
        R.id.menu_login -> {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)

            true
        }

        else -> false

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_main_new)
        Log.d(TAG, "Started!")
        viewModel.allNames


        // job = Job()
        // allFlights = (flightDb.requestAllFlights()).sortedBy { it.tOut }.asReversed()
        // balancesForward=balanceForwardDb.requestAllBalanceForwards()
        // allAircraft = (aircraftDb.requestAllAircraft())

        // start initializing search functionality
        //TODO integrate this into viewModel


        Log.d(TAG, "Checkpoint 1")

         // .activity_main_with_search
        setSupportActionBar(my_toolbar as Toolbar)


        val layoutManager = LinearLayoutManager(this)
        val flightsAdapter = FlightsAdapter(repository.liveFlights.value?: emptyList(), deleteListener) { showFlight(it) }
        flightsList.layoutManager = layoutManager
        flightsList.adapter = flightsAdapter
        Log.d(TAG, "Checkpoint 2")


        repository.liveFlights.observe(this, Observer { flights ->
            flights?.let {newFlights ->
                supportActionBar?.title="${newFlights.size} Flights"
                flightsAdapter.allFlights = newFlights
                flightsAdapter.notifyDataSetChanged()
                if (newFlights.isNotEmpty() && flightsAdapter.scrollToLastPlanned) {
                    flightsAdapter.scrollToNotPlanned()
                    flightsAdapter.scrollToLastPlanned = false
                }
            }
            Log.d(TAG, "Just updated FlightsAdapter with ${flights.size} flights")
        })


        Log.d(TAG, "Checkpoint 3")
        toast("halp aub ik zit hier vast")
        //flightsAdapter.flights=allFlights

        // start synching with server

        /**
         * fill list of missing aircraft (async)
         * a "missing aircraft" is one where registratin is in flights, but not in allAircraft
         */
        launch(Dispatchers.IO) {
            foundMissingAircraft = findMissingaircraftInFlights(repository.requestAllFlights())
        }

        /**
         * Sync with server
         */
        launch {
            Functions.update(this@MainActivity, viewModel)
        }


            // TODO remove this commented-out section if stuff works

            // flightsAdapter.flights = allFlights
            /*
            val progBarParent = layoutInflater.inflate(R.layout.item_progressbar, progressBarField, false)
            uiThread {
                progBarParent.setBackgroundColor(getColorFromAttr(android.R.attr.colorPrimary))
                progressBarField.setBackgroundColor(getColorFromAttr(android.R.attr.colorPrimary))
            }
            val progBar = progBarParent.progressBar
            uiThread {
                progBarParent.progressBarText.text = getString(R.string.synchronizing)
                progressBarField.addView(progBarParent)
            }
            Cloud.runFullUpdate {p ->
                uiThread {
                    progBar.progress = p
                }
            }
                allFlights = flightDb.requestAllFlights().sortedByDescending { it.timeOut }
            uiThread {
                flightsAdapter.flights = allFlights
                // progBarParent.visibility = View.GONE
                progressBarField.removeView(progBarParent)
            }



        }

             */


        /**
         * update airportDb if needed
         * TODO move this to Functions
         */
        launch {
            val serverVersion = Cloud.getAirportDbVersion()
            if (Preferences.airportDbVersion < serverVersion){
                //initialize progressbar and add it to UI
                val progBarParent = layoutInflater.inflate(R.layout.item_progressbar, progressBarField, false)
                progBarParent.setBackgroundColor(getColorFromAttr(android.R.attr.colorPrimary))
                progressBarField.setBackgroundColor(getColorFromAttr(android.R.attr.colorPrimary))
                val progBar = progBarParent.progressBar
                progBarParent.progressBarText.text = getString(R.string.loadingAirports)
                progressBarField.addView(progBarParent)

                if (Cloud.updateAirportsDb {p -> progBar.progress = p})
                    Preferences.airportDbVersion = serverVersion // update if sync successful
                progressBarField.removeView(progBarParent)
            }
        }

        addButton.setOnClickListener{
            showFlight(null)
        }


        /**
         * TODO Change this into a filter inside ViewModel so LiveData gets properly updated

        var nameSearchString: String? = null
        var airportSearchString: String? = null
        var aircraftSearchString: String? = null


        pilotNameSearch.onTextChanged {
            nameSearchString = if (it.isNotEmpty()) it else null
            flightsAdapter.flights=flightSearcher.search(nameSearchString, aircraftSearchString, airportSearchString)
            supportActionBar?.title="${flightsAdapter.flights.size} Flights"
        }
        airportSearch.onTextChanged {
            airportSearchString = if (it.isNotEmpty()) it else null
            flightsAdapter.flights=flightSearcher.search(nameSearchString, aircraftSearchString, airportSearchString)
            supportActionBar?.title="${flightsAdapter.flights.size} Flights"
        }
        aircraftSearch.onTextChanged {
            aircraftSearchString = if (it.isNotEmpty()) it else null
            flightsAdapter.flights=flightSearcher.search(nameSearchString, aircraftSearchString, airportSearchString)
            supportActionBar?.title="${flightsAdapter.flights.size} Flights"
        }
        */


        //TODO This should change into a fragment
        showTotalTimesMenuListener =
            ShowMenuListener {
                val intent = Intent(
                    this,
                    TotalTimesActivity::class.java
                )
                val totalTimesCalculator = TotalTimesCalculator()
                totalTimesCalculator.balancesForward = balancesForward
                totalTimesCalculator.flightsList = repository.liveFlights.value?: emptyList()
                intent.putExtra("totalTimes", totalTimesCalculator.totalsData.toTypedArray())
                startActivity(intent)
            }
        showBalanceForwardListener =
            ShowMenuListener {
                val intent = Intent(
                    this,
                    BalanceForwardActivity::class.java
                )
                startActivity(intent)
            }





    }



    override fun onResume() {
        super.onResume()
        // balancesForward=balanceForwardDb.requestAllBalanceForwards()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    /*

    This function is no longer the way to go. Use new EditFlightFragment.
    private fun showFlight(flight: Flight?, newFlight: Boolean = false) { // new flight gets (null, true)
        if (!alreadyEditingFlight) {
            alreadyEditingFlight = true
            //hide snackbar if showing:
            snackbarShowing?.dismiss()

            //hide toolbar:
            my_toolbar.visibility = View.GONE
            launch {
                val flightEditor = EditFlightNew()

                Log.d("DEBUG", "1")

                //set actions when closing dialog with SAVE:
                flightEditor.onSave =
                    EditFlightNew.OnSave { flightToSave, oldFlight ->
                        alreadyEditingFlight = false
                        launch { repository.saveFlight(flightToSave.copy (timeStamp = Instant.now().epochSecond + Preferences.serverTimeOffset)) }
                        flightSearcher.addFlight(flightToSave)
                        namesWorker.addFlight(flightToSave)
                        Log.d("DEBUG", "2")
                        // flightDb.updateCachedFlights(allFlights)
                        Log.d("DEBUG", "3")

                        //define snackbar to be displayed when dialog is closed with SAVE
                        val snackBar = CustomSnackbar.make(mainActivityLayout).apply{
                            setMessage("Saved Flight")
                            if (newFlight)
                                setOnAction {
                                    Log.d("onAction", "ding 1")
                                    // flightDb.deleteFlight(flightToSave)
                                    viewModel.deleteFlight(flightToSave)
                                    dismiss()
                                }
                            else
                                setOnAction {//onAction = UNDO
                                    Log.d("onAction", "ding 2")
                                    viewModel.saveFlight(oldFlight)
                                    dismiss()
                                }
                            setOnActionBarShown { addButton.hide() }
                            setOnActionBarGone {
                                snackbarShowing = null
                                if (!alreadyEditingFlight) addButton.show()
                            }
                        }
                        snackbarShowing = snackBar
                        snackBar.show()

                        my_toolbar.showAnimated()

                        //make sure search field is closed and emptied
                        if (searchField.visibility != View.GONE) {
                            pilotNameSearch.text = pilotNameSearch.text
                            airportSearch.setText("")
                            aircraftSearch.setText("")
                        }
                    }
                Log.d("DEBUG", "4")

                flightEditor.onCancel = EditFlightNew.OnCancel { canceledFlight, _ ->
                    alreadyEditingFlight = false

                    val snackBar = CustomSnackbar.make(mainActivityLayout).apply {
                        setMessage("Cancelled")
                        setActionText("Continue")
                        setOnAction {
                            showFlight(canceledFlight)
                            dismiss()
                        }
                        setOnActionBarShown { addButton.hide() }
                        setOnActionBarGone {
                            snackbarShowing = null
                            if (!alreadyEditingFlight) addButton.show()
                        }
                    }
                    snackbarShowing = snackBar
                    snackBar.show()

                    my_toolbar.showAnimated()


                }
                Log.d("DEBUG", "5")

                //set flight if not null, otherwise it will stay at default in fragment constructor
                flightEditor.flight = flight
                    ?: reverseFlight(mostRecentCompleteFlight(viewModel.liveFlights.value ?: emptyList()), viewModel.highestFlightId() + 1)

                if (namesWorker.isInitialized) flightEditor.namesWorker = namesWorker
                else {
                    namesWorker.initializationListener = NamesWorker.InitializationListener {
                        flightEditor.namesWorker = namesWorker
                    } // if (or when) namesWorker is done making its names, put it into  EditFlight fragment
                }
                Log.d("DEBUG", "6")


                supportFragmentManager.beginTransaction()
                    .add(R.id.mainActivityLayout, flightEditor)
                    .addToBackStack(null)
                    .commit()
                Log.d("DEBUG", "7")
                addButton.fadeOut()
                Log.d("DEBUG", "8")
            }
        }

    }
    */

    /**
     * Shows a flight in EditFlightFragment.
     * If Flight is entered, that will be loaded, if left blank or null, a new flight will be made.
     */
    private fun showFlight(flight: Flight? = null){
        Log.d("DEBUG", "1")
        addButton.fadeOut()
        launch {
            //hide snackbar if showing:
            snackbarShowing?.dismiss()
            Log.d("DEBUG", "2")
            viewModel.setWorkingFlight(flight)
            Log.d("DEBUG", "3 - workingFlight = ${viewModel.workingFlight} aka ${viewModel.workingFlight.value}")
            // from here on, if viewModel.undoFlight == null, this is a new flight


            //hide toolbar
            my_toolbar.visibility = View.GONE

            val flightEditor = EditFlightFragment().apply {
                setOnSaveListener {
                    launch {
                        // 1. save flight
                        val flightToSave =
                            viewModel.workingFlight.value!! // cannot be null as it is set before starting fragment (setWorkingFlight takes care of that)
                        repository.saveFlight(flightToSave)

                        // 2. Show UNDO SnackBar
                        snackbarShowing = CustomSnackbar.make(mainActivityLayout).apply {
                            setMessage("Saved Flight")
                            setOnAction {//onAction = UNDO
                                launch {
                                    viewModel.undoFlight?.let { f -> repository.saveFlight(f) }
                                        ?: repository.deleteFlight(flightToSave) // save undoFlight, or delete flight as it was a new one
                                }
                                dismiss()
                            }
                            setOnActionBarShown { addButton.hide() }
                            setOnActionBarGone {
                                snackbarShowing = null
                                addButton.show()
                            }
                            show()
                        }

                        // 3. Show Toolbar
                        my_toolbar.showAnimated()

                        // 4. Make searchField visible.
                        // TODO searchField should be improved (and doesnt work atm)
                        if (searchField.visibility != View.GONE) {
                            pilotNameSearch.text = pilotNameSearch.text
                            airportSearch.setText("")
                            aircraftSearch.setText("")
                        }


                    }
                }

            }
            Log.d("DEBUG", "4")

            supportFragmentManager.commit {
                add(R.id.mainActivityLayout, flightEditor)
                addToBackStack(null)
            }
            Log.d("DEBUG", "5")
        }

        Log.d("DEBUG", "8")
    }

    /**
     * fill list of missing aircraft (async)
     * a "missing aircraft" is one where registration is in flights, but not in allAircraft
     */
    private fun findMissingaircraftInFlights(flights: List<Flight>): List<Aircraft>{
        val foundAircraft = mutableListOf<Aircraft>()
        val knownRegistrations: List<String> = allAircraft.map{ac -> ac.registration}
        val knownModels: List<String> = allAircraft.map{ac -> ac.model}
        var highestID = -1 // aircraftDb.highestId +1
        flights.forEach{f ->
            if (f.registration !in knownRegistrations + foundAircraft.map{ac -> ac.registration}) {
                if (f.aircraft in knownModels)
                    // foundAircraft.add(aircraftDb.searchRegAndType(type=f.aircraft).first().copy(id = highestID, registration = f.registration))
                else
                    foundAircraft.add(
                        Aircraft(
                            highestID,
                            f.registration,
                            "",
                            f.aircraft,
                            "",
                            0,
                            0,
                            0,
                            if (f.name2.isNotEmpty()) 1 else 0,
                            if (f.ifrTime > 0) 1 else 0
                        )
                    )
                highestID += 1
            }
        }
        return foundAircraft
    }

    /**
     * Listeners below here
     */

    /**
     * deleteListener is what will happen if a flight is deleted:
     * -> Save flight with DELETEFLAG set to 1, or just plain delete it if unknown to server (viewModel takes care of that)
     * -> Show a snackbar that give UNDO functionality
     */
    //TODO make this work in new way with viewModel
    private val deleteListener: (Flight) -> Unit = { f ->
        launch {
            Log.d("Deletelistener", "timing check A")
            if (f.planned) {
                repository.deleteFlight(f)

                // show UNDO snackbar
                val snackBar = CustomSnackbar.make(flightsList)
                snackBar.setOnActionBarShown { addButton.hide() }
                snackBar.setOnActionBarGone { addButton.show() }
                snackBar.setOnAction {
                    launch {
                        repository.saveFlight(f)
                        snackBar.dismiss()
                    }
                }
                snackBar.show()
            } else {
                AlertDialog.Builder(this@MainActivity).apply {
                    setMessage("Do you want to delete COMPLETED flight? This might be illegal.")
                    setPositiveButton("DO IT ANYWAY") { _, _ ->
                        launch {
                            repository.deleteFlight(f)
                            // show UNDO snackbar
                            val snackBar = CustomSnackbar.make(flightsList)
                            snackBar.setOnActionBarShown { addButton.hide() }
                            snackBar.setOnActionBarGone { addButton.show() }
                            snackBar.setOnAction {
                                snackBar.dismiss()
                            }
                            snackBar.show()
                        }
                    }


                    //setNegativeButton("CANCEL", DialogInterface.OnClickListener { _, _ -> goAhead = false })
                }.create().apply {
                    setTitle("WARNING")
                    show()
                }
            }
        }
    }


    companion object{
        const val TAG = "MainActivity"

    }

}
