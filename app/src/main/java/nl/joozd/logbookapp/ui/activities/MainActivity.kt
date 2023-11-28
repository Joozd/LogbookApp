/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020-2022 Joost Welle
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

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.calendar.getFlightsFromCalendar
import nl.joozd.logbookapp.data.importing.ImportedFlightsSaver
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.databinding.ActivityMainNewBinding
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.model.viewmodels.activities.mainActivity.MainActivityViewModelNew
import nl.joozd.logbookapp.ui.activities.newUserActivity.NewUserActivity
import nl.joozd.logbookapp.ui.activities.totalTimesActivity.TotalTimesActivity
import nl.joozd.logbookapp.ui.adapters.flightsadapter.FlightsAdapter
import nl.joozd.logbookapp.ui.dialogs.AboutDialog
import nl.joozd.logbookapp.ui.dialogs.editFlightFragment.EditFlightFragment
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity
import nl.joozd.logbookapp.model.ModelFlight
import nl.joozd.logbookapp.model.viewmodels.activities.pdfParserActivity.ImportedLogbookAutoCompleter
import nl.joozd.logbookapp.ui.activities.settingsActivity.SettingsActivity
import nl.joozd.logbookapp.core.MessageCenter
import nl.joozd.logbookapp.core.Migrations
import nl.joozd.logbookapp.core.metadata.Version
import nl.joozd.logbookapp.errors.errorDialog
import nl.joozd.logbookapp.extensions.showFragment
import nl.joozd.logbookapp.ui.dialogs.UpdateMessageDialog
import nl.joozd.logbookapp.utils.IntentHandler

//TODO: Handle Scheduled Errors from ScheduledErrors (use MessageCenter) -- is this still so?

class MainActivity : JoozdlogActivity() {
    private val viewModel: MainActivityViewModelNew by viewModels()

    override fun onOptionsItemSelected(item: MenuItem): Boolean{
        when (item.itemId) {
            R.id.menu_settings -> startSettingsActivity()
            R.id.menu_add_flight -> viewModel.newFlight()
            R.id.menu_undo -> viewModel.undo()
            R.id.menu_redo -> viewModel.redo()
            R.id.menu_total_times -> startTotalTimesActivity()
            R.id.menu_balance_forward -> startBalanceForwardActivity()
            R.id.app_bar_search -> viewModel.menuSelectedSearch()
            R.id.menu_feedback -> startFeedbackActivity()
            R.id.menu_about -> showAboutDialog()
            R.id.menu_transfer -> showTransferActivity()
            R.id.menu_export_pdf -> startMakePdfActivity()
            // R.id.menu_do_something -> performBonusMenuAction()
            else -> return false
        }
        return true
    }

    /*
     * Shows menu, as well as undo and redo icons if needed.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        getUndoMenuItem(menu).apply{
            isVisible = viewModel.undoAvailable
        }
        getRedoMenuItem(menu).apply{
            isVisible = viewModel.redoAvailable
        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkIfNewInstallOrUpdated()
        val binding = (ActivityMainNewBinding.inflate(layoutInflater)).apply {
            setSupportActionBar(mainToolbar)
            makeFlightsList()
            scrollOnNextUpdate()
            startCollectors()
            initializeSearchFieldViews()
            setOnClickListeners()
            collectMessageCenter()
        }
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkIfSearchFieldShouldBeOpen()
    }

    @SuppressLint("MissingSuperCall") // False lint error
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            lifecycleScope.launch {
                IntentHandler(it).handle()
            }
        }
    }

    private fun ActivityMainNewBinding.makeFlightsList(){
        val adapter = makeFlightsListAdapter()

        flightsList.layoutManager = LinearLayoutManager(activity) // won't layout without manager
        flightsList.adapter = adapter

        /*viewModel.flightsToDisplayFlow.launchCollectWhileLifecycleStateStarted {
            adapter.submitList(it)
        }

         */
    }

    private fun ActivityMainNewBinding.startCollectors() {
        collectWorkingFlightAndLaunchEditFlightFragmentWhenNeeded()
        collectSearchFieldOpen()
        collectFlightsListFlow()
        collectUndoRedoChangedFlow()
        collectAmountOfFlightsFlow()

        collectIcaoIataSwappedFlow()
        collectPicNameNeedsToBeSetFlow()

        collectUseCalendarSyncFlow()
    }

    private fun collectWorkingFlightAndLaunchEditFlightFragmentWhenNeeded(){
        viewModel.flightEditorFlow.launchCollectWhileLifecycleStateStarted{
            killWorkingFlightEditingFragments()
            if (it != null)
                launchEditFlightFragment()
        }
    }

    private fun ActivityMainNewBinding.collectSearchFieldOpen(){
        viewModel.searchFieldOpenFlow.launchCollectWhileLifecycleStateStarted{ open ->
            if (open)
                openSearchField()
            else
                closeSearchField()
        }
    }

    private fun ActivityMainNewBinding.collectFlightsListFlow(){
        viewModel.foundFlightsFlow.launchCollectWhileLifecycleStateStarted{ flights ->
            putFlightsInFlightsAdapter(flights)
            mainSearchBoxLayout.hint = getString(R.string.search_n_flights, flights.size)
        }
    }

    private fun ActivityMainNewBinding.putFlightsInFlightsAdapter(
        flights: List<ModelFlight>
    ) {
        (flightsList.adapter as FlightsAdapter).submitList(flights)
    }

    //Whenever undo/redo icons state changes, redraw menu
    private fun collectUndoRedoChangedFlow(){
        viewModel.undoRedoStatusChangedFlow.launchCollectWhileLifecycleStateStarted{
            invalidateOptionsMenu()
        }
    }

    private fun ActivityMainNewBinding.collectAmountOfFlightsFlow(){
        viewModel.amountOfFlightsFlow.launchCollectWhileLifecycleStateStarted{
            mainToolbar.title = getString(R.string.n_flights, it)
        }
    }

    @SuppressLint("NotifyDataSetChanged") // in this case, entire dataset will be changed.
    private fun ActivityMainNewBinding.collectIcaoIataSwappedFlow(){
        Prefs.useIataAirports.flow.launchCollectWhileLifecycleStateStarted{
            (flightsList.adapter as FlightsAdapter).apply{
                useIata = it
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged") // in this case, entire dataset will be changed.
    private fun ActivityMainNewBinding.collectPicNameNeedsToBeSetFlow(){
        Prefs.picNameNeedsToBeSet.flow.launchCollectWhileLifecycleStateStarted{
            (flightsList.adapter as FlightsAdapter).apply{
                picNameMustBeSet = it
            }
        }
    }

    private fun collectUseCalendarSyncFlow() {
        Prefs.useCalendarSync.flow.launchCollectWhileLifecycleStateStarted {
            if (it) updateCalendarEvents()
        }
    }

    private fun ActivityMainNewBinding.initializeSearchFieldViews(){
        initializeSearchTypeSpinner()
        addOnTextChangedListenerToMainSearchField()
    }

    private fun ActivityMainNewBinding.setOnClickListeners(){
        addButton.setOnClickListener {
            viewModel.newFlight()
        }
    }

    private fun ActivityMainNewBinding.collectMessageCenter() {
        MessageCenter.registerActivityForMessageBarDisplay(activity, messageBarTarget)
        MessageCenter.registerActivityForDialogDisplay(activity)
    }

    private fun killWorkingFlightEditingFragments(){
        supportFragmentManager.popBackStack(EDIT_FLIGHT_FRAGMENT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    private fun makeFlightsListAdapter() =
        FlightsAdapter(
            onDelete = { flight -> attemptToDelete(flight) },
            itemClick = { flight -> viewModel.showEditFlightDialog(flight) }
        )

    private fun ActivityMainNewBinding.closeSearchField() {
        if (mainSearchField.text?.isNotBlank() == true)
            scrollOnNextUpdate()
        mainSearchField.setText("")
        mainSearchField.clearFocus()
        mainSearchField.hideKeyboard()
        searchTypeSpinner.setSelection(0)
        searchField.visibility = View.GONE
    }

    private fun ActivityMainNewBinding.openSearchField() {
        searchField.visibility = View.VISIBLE
        mainSearchField.requestFocus()
        mainSearchField.showKeyboard()
    }

    private fun ActivityMainNewBinding.initializeSearchTypeSpinner() {
        addAdapterToSearchTypeSpinner()
        addOnItemSelectedListenerToSearchTypeSpinner()
    }

    private fun ActivityMainNewBinding.addOnTextChangedListenerToMainSearchField() {
        mainSearchField.onTextChanged { viewModel.updateQuery(it) }
    }

    private fun ActivityMainNewBinding.addAdapterToSearchTypeSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            activity, R.array.search_options, android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        searchTypeSpinner.adapter = adapter
    }

    private fun ActivityMainNewBinding.addOnItemSelectedListenerToSearchTypeSpinner() {
        searchTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                viewModel.pickSearchType(position)
            }
        }
    }



    private fun ActivityMainNewBinding.scrollOnNextUpdate(){
        val adapter = flightsList.adapter as FlightsAdapter
        adapter.addListListener(ScrollOnNextUpdateListener(this))
    }

    private fun ActivityMainNewBinding.scrollToFirstCompletedFlight() {
        val flights = (flightsList.adapter as FlightsAdapter).currentList
        val positionToScrollTo = findIndexOfTopFlightOnScreenAfterScrolling(flights)
        flightsList.scrollToPosition(positionToScrollTo)

        // scroll by another 30 dp
        if (positionToScrollTo != 0) flightsList.scrollBy(0, -30.dpToPixels().toInt())
    }

    /*
     * Get index of first completed flight (i), with a minimum of PLANNED_FLIGHTS_IN_SIGHT_WHEN_SCROLLING_TO_FIRST_COMPLETED.
     * Then, subtract PLANNED_FLIGHTS_IN_SIGHT_WHEN_SCROLLING_TO_FIRST_COMPLETED.
     * This way, the top flight on screen is always either 0 if (i)<= 3, or (i)-3 if (i) > 3
     */
    private fun findIndexOfTopFlightOnScreenAfterScrolling(
        flights: List<ModelFlight>
    ): Int {
        val i = flights.firstOrNull { !it.isPlanned }?.let { f -> flights.indexOf(f) }
            ?: 0 // index of first completed flight or 0 if no planned flight found
        val iWithMinValue =  maxOf( i, PLANNED_FLIGHTS_IN_SIGHT_WHEN_SCROLLING_TO_FIRST_COMPLETED)
        return iWithMinValue - PLANNED_FLIGHTS_IN_SIGHT_WHEN_SCROLLING_TO_FIRST_COMPLETED
    }

    private fun attemptToDelete(flight: ModelFlight){
        if (flight.isPlanned)
            viewModel.deleteFlight(flight)
        else (showDeletingCompletedFlightDialog(flight))
    }

/*    private fun makeFlightInfoDialog(flight: ModelFlight): ListDisplayDialog {
        val sunriseSunset = TwilightCalculator().sunrisesSunsets(
            flight.orig.latitude_deg,
            flight.orig.longitude_deg,
            flight.dest.latitude_deg,
            flight.dest.longitude_deg,
            flight.timeOut,
            flight.timeIn)
        val sunrises = sunriseSunset.sunrises.map { "Sunrise" to it } // TODO string value with placeholder
        val sunsets = sunriseSunset.sunsets.map { "Sunset" to it } // TODO string value with placeholder

        val combined = (sunrises + sunsets)
            .sortedBy { it.second }
            .map { "${it.first} ${it.second}" }
            .takeIf { it.isNotEmpty() } ?: listOf ("No sunrise/sunset during this flight") // TODO make string resource

        return ListDisplayDialog().apply {
            title = this@MainActivity.getString(R.string.placeholder)
            valuesToDisplay = combined
        }
    }*/


    private fun View.showKeyboard(){
        val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(this,0)
    }

    private fun View.hideKeyboard(){
        val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun launchEditFlightFragment() {
        showFragment<EditFlightFragment>(tag = EDIT_FLIGHT_FRAGMENT_TAG)
    }

    private fun getRedoMenuItem(menu: Menu) =
        menu.findItem(R.id.menu_redo)

    private fun getUndoMenuItem(menu: Menu) =
        menu.findItem(R.id.menu_undo)



    private fun startMakePdfActivity() {
        startActivity(Intent(this, MakePdfActivity::class.java))
    }

    private fun startFeedbackActivity() {
        startActivity(Intent(this, FeedbackActivity::class.java))
    }

    private fun startBalanceForwardActivity() {
        startActivity(Intent(this, BalanceForwardActivity::class.java))
    }

    private fun startTotalTimesActivity() {
        startActivity(Intent(this, TotalTimesActivity::class.java))
    }

    private fun startSettingsActivity() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    /*
      intentionally blocking. Could collect flow but then mainActivity would load before newUserActivity.
      Normally this is frowned upon, but as this ONLY happens when loading the app it just makes the load time a few millis longer
      and doesn't make the app unresponsive.
     */
    // This checks if this is a new install (launches newUserActivity) or an upgrade (launches appropriate migration steps)
    private fun checkIfNewInstallOrUpdated() {
        updateLegacyVersionTrackingIfNeeded()

        when(Prefs.configuredVersion.valueBlocking) {
            Version.NEW_INSTALL ->
                startActivity(Intent(this, NewUserActivity::class.java))
            Version.ALPACA -> {
                Migrations().migrateToCurrent()
                showUpdateMessageDialog()
            }
            Version.currentVersion ->
                { /* do nothing */ }
            else -> errorDialog("Version is incorrect: ${Prefs.configuredVersion.valueBlocking}")


        }
    }

    /*
     * intentionally blocking. Could collect flow but then mainActivity would load before newUserActivity.
     * As this ONLY happens when loading the app it just makes the load time a few millis longer and doesn't make the app unresponsive.
     */
    // sets version to ALPACA if no configured version saved but newUserActivity has been finished.
    private fun updateLegacyVersionTrackingIfNeeded(){
        if (Prefs.configuredVersion.valueBlocking == Version.NEW_INSTALL)
            if (Prefs.newUserActivityFinished.valueBlocking)
                Prefs.configuredVersion.valueBlocking = Version.ALPACA
    }

    private fun showAboutDialog() {
        showFragment<AboutDialog>()
    }

    private fun showUpdateMessageDialog(){
        showFragment<UpdateMessageDialog>()
    }

    private fun showTransferActivity(){
        startActivity(Intent(this, SharingActivity::class.java))
    }

    private fun showDeletingCompletedFlightDialog(flight: ModelFlight){
        AlertDialog.Builder(this).apply{
            setTitle(R.string.delete)
            setMessage(R.string.delete_completed_flight)
            setPositiveButton(android.R.string.ok){ _, _ ->
                viewModel.deleteFlight(flight)
            }
            setNegativeButton(android.R.string.cancel){ _, _ ->
                // intentionally left blank
            }
        }.create().show()
    }

    private suspend fun updateCalendarEvents(){
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED)
            getFlightsFromCalendar()?.let {
                val ff = ImportedLogbookAutoCompleter().autocomplete(it)
                ImportedFlightsSaver.instance.save(ff, canUndo = false)
            }
        else {
            //granting permission will reset Prefs.useCalendarSync, which will be collected and this function will be called again.
            Prefs.useCalendarSync(false)
            requestCalendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    private val requestCalendarPermissionLauncher =
        registerForActivityResult(RequestPermission()) { hasPermission ->
            Prefs.useCalendarSync(hasPermission)
        }

    private inner class ScrollOnNextUpdateListener(private val binding: ActivityMainNewBinding): FlightsAdapter.ListListener {
        private val adapter = binding.flightsList.adapter as FlightsAdapter
        override fun onListUpdated() {
            adapter.removeListListener(this)
            binding.scrollToFirstCompletedFlight()
        }
    }


    companion object{
        const val EDIT_FLIGHT_FRAGMENT_TAG = "EDIT_FLIGHT_FRAGMENT_TAG"

        const val PLANNED_FLIGHTS_IN_SIGHT_WHEN_SCROLLING_TO_FIRST_COMPLETED = 2
    }
}
