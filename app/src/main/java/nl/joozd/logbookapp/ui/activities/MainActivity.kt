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

package nl.joozd.logbookapp.ui.activities


import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main_new.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.extensions.fadeOut
import nl.joozd.logbookapp.extensions.showAnimated
import nl.joozd.logbookapp.model.helpers.FeedbackEvents.MainActivityEvents
import nl.joozd.logbookapp.model.viewmodels.activities.MainActivityViewModel
import nl.joozd.logbookapp.ui.adapters.flightsadapter.FlightsAdapter
import nl.joozd.logbookapp.ui.fragments.EditFlightFragment
import nl.joozd.logbookapp.ui.utils.customs.CustomSnackbar
import nl.joozd.logbookapp.ui.utils.longToast
import nl.joozd.logbookapp.ui.utils.toast


class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    private val viewModel: MainActivityViewModel by viewModels()

    private var snackbarShowing: CustomSnackbar? = null

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menu_rebuild -> {
            viewModel.menuSelectedRebuild()

            true
        }
        R.id.menu_add_flight -> {
            viewModel.menuSelectedAddFlight()
            true
        }
        R.id.menu_total_times -> {
            viewModel.menuSelectedTotalTimes()
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

        setSupportActionBar(my_toolbar as Toolbar)

        //RecyclerView thingies
        val flightsAdapter = FlightsAdapter().apply {
            onDelete = { id -> viewModel.deleteFlight(id) }
            itemClick = { id -> viewModel.showFlight(id) }
        }.also {
                flightsList.layoutManager = LinearLayoutManager(this)
                flightsList.adapter = it
            }

        /**
         * addButton wil show a null flight (and showing a null flight will create a new empty flight)
         */
        addButton.setOnClickListener{
            viewModel.addFlight()
        }

        /**
         * Obesrvers below here
         */

        viewModel.displayFlightsList.observe(this, Observer { flightsAdapter.updateList(it) })

        viewModel.feedbackEvent.observe(this, Observer {
            when(it.getEvent()){
                MainActivityEvents.NOT_IMPLEMENTED -> toast("Not Implemented")
                MainActivityEvents.SHOW_FLIGHT -> showFlight()
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
    }

    override fun onResume() {
        super.onResume()
        viewModel.notifyActivityResumed()

    }

    override fun onDestroy() {
        cancel()
        super.onDestroy()
    }


    /**
     * Shows a flight in EditFlightFragment.
     * If Flight is entered, that will be loaded, if left blank or null, a new flight will be made.
     */
    private fun showFlight(){
        snackbarShowing?.dismiss()
        addButton.fadeOut()
        my_toolbar.visibility = View.GONE
        val flightEditor = EditFlightFragment().apply {
            setOnSaveListener {
                viewModel.saveWorkingFlight()
            }
            setOnCloseListener {
                // 3. Show Toolbar, and addButton if no snackbar
                this@MainActivity.my_toolbar.showAnimated()
                if (snackbarShowing == null) this@MainActivity.addButton.show()
            }
        }

        supportFragmentManager.commit {
            add(R.id.mainActivityLayout, flightEditor)
            addToBackStack(null)
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
