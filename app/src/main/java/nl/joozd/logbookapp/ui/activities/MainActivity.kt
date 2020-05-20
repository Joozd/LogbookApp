/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020 Joost Welle
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU Affero General Public License as
 *      published by the Free Software Foundation, either version 3 of the
 *      License, or (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU Affero General Public License for more details.
 *
 *      You should have received a copy of the GNU Affero General Public License
 *      along with this program.  If not, see https://www.gnu.org/licenses
 *
 */


package nl.joozd.logbookapp.ui.activities


import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main_new.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.SettingsActivity
import nl.joozd.logbookapp.extensions.*
import nl.joozd.logbookapp.model.helpers.FeedbackEvents
import nl.joozd.logbookapp.model.helpers.FeedbackEvents.MainActivityEvents
import nl.joozd.logbookapp.model.viewmodels.activities.MainActivityViewModel
import nl.joozd.logbookapp.ui.adapters.flightsadapter.FlightsAdapter
import nl.joozd.logbookapp.ui.fragments.EditFlightFragment
import nl.joozd.logbookapp.ui.utils.customs.CustomSnackbar
import nl.joozd.logbookapp.ui.utils.customs.JoozdlogProgressBar
import nl.joozd.logbookapp.ui.utils.longToast
import nl.joozd.logbookapp.ui.utils.toast


class MainActivity : JoozdlogActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    private var snackbarShowing: CustomSnackbar? = null
    private var airportSyncProgressBar: JoozdlogProgressBar? = null
    private var flightsSyncProgressBar: JoozdlogProgressBar? = null

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menu_settings -> {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }
        R.id.menu_add_flight -> {
            viewModel.menuSelectedAddFlight()
            true
        }
        R.id.menu_total_times -> {
            val intent = Intent(this, TotalTimesActivity::class.java)
            startActivity(intent)
            true
        }
        R.id.menu_balance_forward -> {
            viewModel.menuSelectedBalanceForward()
            true
        }
        R.id.app_bar_search -> {
            viewModel.menuSelectedSearch()
            true
        }
        R.id.menu_edit_aircraft -> {
            viewModel.menuSelectedEditAircraft()
            true
        }
        R.id.menu_export_pdf -> {
            viewModel.menuSelectedExportPDF()
            //TODO decide if this becomes a new activity or a fragment
            true
        }
        R.id.menu_do_something -> {
            viewModel.menuSelectedDoSomething()
            true

        }
        R.id.menu_login -> {
            //TODO decide if this becomes a new activity or a fragment
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

        setSupportActionBarWithReturn(main_toolbar as Toolbar)?.apply{
            // can do stuff with toolbar here
        }

        //RecyclerView thingies
        val flightsAdapter = FlightsAdapter().apply {
            onDelete = { id -> viewModel.deleteFlight(id) }
            itemClick = { id -> viewModel.showFlight(id) }
        }.also {
                flightsList.layoutManager = LinearLayoutManager(this)
                flightsList.adapter = it
            }

        ArrayAdapter.createFromResource(
            this,
            R.array.search_options,
            android.R.layout.simple_spinner_item
        ).apply {
            // Specify the layout to use when the list of choices appears
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }.also { adapter ->
            // Apply the adapter to the spinner
            searchTypeSpinner.adapter = adapter
        }
        searchTypeSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                //mainSearchField.text = mainSearchField.text
                viewModel.setSpinnerSelection(position)
            }

        }

        /**
         * addButton wil show a null flight (and showing a null flight will create a new empty flight)
         */
        addButton.setOnClickListener{
            viewModel.addFlight()
        }

        /**
         * Search field and -spinner functions
         */

        mainSearchField.onTextChanged {
            viewModel.setSearchString(it)
        }

        /**
         * Obesrvers below here
         */

        viewModel.displayFlightsList.observe(this, Observer { flightsAdapter.updateList(it) })

        viewModel.searchFieldHint.observe(this, Observer {
            mainSearchBoxLayout.hint = it ?: getString(R.string.search)
        })

        viewModel.feedbackEvent.observe(this, Observer {
            when(it.getEvent()){
                MainActivityEvents.NOT_IMPLEMENTED -> toast("Not Implemented")
                MainActivityEvents.SHOW_FLIGHT -> showFlight()
                MainActivityEvents.OPEN_SEARCH_FIELD -> searchField.visibility = View.VISIBLE
                MainActivityEvents.CLOSE_SEARCH_FIELD -> {
                    mainSearchField.setText("")
                    searchTypeSpinner.setSelection(0)
                    //reset search types spinner//
                    searchField.visibility = View.GONE
                }
                MainActivityEvents.FLIGHT_SAVED -> snackbarShowing = CustomSnackbar.make(this@MainActivity.mainActivityLayout).apply {
                    setMessage("Saved Flight")
                    setOnAction { //onAction = UNDO
                        viewModel.undoSaveWorkingFlight()
                        dismiss()
                    }
                    setOnActionBarShown { this@MainActivity.addButton.hide() }
                    setOnActionBarGone {
                        snackbarShowing = null
                        this@MainActivity.addButton.show()
                    }
                    show()
                }
                MainActivityEvents.FLIGHT_NOT_FOUND -> longToast("ERROR: Flight ID not found!")
                MainActivityEvents.TRYING_TO_DELETE_COMPLETED_FLIGHT -> {
                    AlertDialog.Builder(this).apply {
                        setTitle("WARNING")
                        setMessage("You are trying to delete a COMPLETED flight. This might be illegal. Delete anyway??")
                        setPositiveButton("Continue") {_, _ -> viewModel.deleteNotPlannedFlight(it.extraData.getInt("ID"))}
                        setNegativeButton("Cancel", null)
                    }.create().show()
                }
                MainActivityEvents.DONE -> longToast(" Yay fixed!")
                MainActivityEvents.ERROR -> longToast("An error occurred :(")
            }
        })

        viewModel.flightOpenEvents.observe(this, Observer{
            if (it.getEvent() == FeedbackEvents.FlightEditorOpenOrClosed.CLOSED) {
                main_toolbar.showAnimated()
                if (snackbarShowing == null) addButton.show()
            }
        })

        viewModel.airportSyncProgress.observe(this, Observer {p ->
            when {
                p == -1 -> airportSyncProgressBar?.remove()
                airportSyncProgressBar != null -> airportSyncProgressBar?.progress = p
                else -> airportSyncProgressBar = makeProgressBar(R.string.loadingAirports, p).show()
            }
        })
        viewModel.flightSyncProgress.observe(this, Observer {p ->
            when {
                p == -1 -> flightsSyncProgressBar?.remove()
                flightsSyncProgressBar != null -> flightsSyncProgressBar?.progress = p
                else -> flightsSyncProgressBar = makeProgressBar(R.string.loadingFlights, p).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.notifyActivityResumed()

    }


    /**
     * Shows a flight in EditFlightFragment.
     * If Flight is entered, that will be loaded, if left blank or null, a new flight will be made.
     */
    private fun showFlight(){
        viewModel.closeSearchField()
        snackbarShowing?.dismiss()
        addButton.fadeOut()
        // main_toolbar.hideAnimated()
        main_toolbar.visibility = View.GONE
        val flightEditor = EditFlightFragment()
        supportFragmentManager.commit {
            add(R.id.mainActivityLayout, flightEditor)
            addToBackStack(null)
        }
    }

    private fun makeProgressBar(textResource: Int, initialProgress: Int = 0): JoozdlogProgressBar {
        return JoozdlogProgressBar(progressBarField).apply{
            text = getString(textResource)
            backgroundColor = getColorFromAttr(android.R.attr.colorPrimary)
            progress = initialProgress
        }
    }

    /**
     * snackbar:
     *


     */


    companion object{
        const val TAG = "MainActivity"

    }
}
