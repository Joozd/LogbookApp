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

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.databinding.ActivityMainNewBinding
import nl.joozd.logbookapp.model.viewmodels.activities.mainActivity.MainActivityViewModelNew
import nl.joozd.logbookapp.ui.activities.newUserActivity.NewUserActivity
import nl.joozd.logbookapp.ui.activities.totalTimesActivity.TotalTimesActivity
import nl.joozd.logbookapp.ui.adapters.flightsadapter.FlightsAdapter
import nl.joozd.logbookapp.ui.dialogs.AboutDialog
import nl.joozd.logbookapp.ui.dialogs.EditFlightFragment
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity
import nl.joozd.logbookapp.workmanager.JoozdlogWorkersHub
import nl.joozd.logbookapp.extensions.onTextChanged
import nl.joozd.logbookapp.model.ModelFlight

//TODO: Handle Scheduled Errors from ScheduledErrors
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
            R.id.menu_export_pdf -> startMakePdfActivity()
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
        val binding = (ActivityMainNewBinding.inflate(layoutInflater)).apply {
            setSupportActionBar(mainToolbar)
            makeFlightsList()
            startCollectors()
            initializeSearchFieldViews()
            setOnClickListeners()
        }
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()
        JoozdlogWorkersHub.syncTimeAndFlightsIfEnoughTimePassed()
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
        collectSearchFieldOpenAndOpenSearchFieldIfNeeded()
        collectFlightsListFlow()
        collectUndoRedoChangedFlow()
    }

    private fun collectWorkingFlightAndLaunchEditFlightFragmentWhenNeeded(){
        viewModel.flightEditorFlow.launchCollectWhileLifecycleStateStarted{
            println("Collected flightEditorFlow $it")
            killWorkingFlightEditingFragments()
            if (it != null)
                launchEditFlightFragment()
        }
    }

    private fun ActivityMainNewBinding.collectSearchFieldOpenAndOpenSearchFieldIfNeeded(){
        viewModel.searchFieldOpenFlow.launchCollectWhileLifecycleStateStarted{ open ->
            if (open)
                openSearchField()
            else
                closeSearchField()
        }
    }

    private fun ActivityMainNewBinding.collectFlightsListFlow(){
        viewModel.flightsFlow.launchCollectWhileLifecycleStateStarted{ flights ->
            (flightsList.adapter as FlightsAdapter).submitList(flights)
        }
    }

    //Whenever undo/redo icons state changes, redraw menu
    private fun collectUndoRedoChangedFlow(){
        viewModel.undoRedoStatusChangedFlow.launchCollectWhileLifecycleStateStarted{
            invalidateOptionsMenu()
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

    private fun killWorkingFlightEditingFragments(){
        supportFragmentManager.popBackStack(EDIT_FLIGHT_FRAGMENT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    private fun ActivityMainNewBinding.showBackupReminderLayoutIf(it: Boolean) {
        backupReminderLayout.visibility = if (it) View.VISIBLE else View.GONE
    }

    private fun makeFlightsListAdapter() =
        FlightsAdapter(
            onDelete = { flight -> viewModel.deleteFlight(flight) },
            itemClick = { flight -> viewModel.showEditFlightDialog(flight) }
        )

    private fun ActivityMainNewBinding.closeSearchField() {
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

    private fun ActivityMainNewBinding.scrollToFirstCompletedFlight(
        flights: List<ModelFlight>
    ) {
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



    private fun View.showKeyboard(){
        val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(this,0)
    }

    private fun View.hideKeyboard(){
        val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun launchEditFlightFragment() {
        val eff = EditFlightFragment()
        println("Launching EFF $eff")
        supportFragmentManager.commit {
            add(R.id.mainActivityLayout, eff, EDIT_FLIGHT_FRAGMENT_TAG)
            addToBackStack(EDIT_FLIGHT_FRAGMENT_TAG)
        }
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


    private fun startNewUserActivityIfNewInstall() {
        if (!Preferences.newUserActivityFinished)
            startActivity(Intent(this, NewUserActivity::class.java))
    }

    private fun showAboutDialog() {
        supportFragmentManager.commit{
            add(R.id.mainActivityLayout, AboutDialog())
            addToBackStack(null)
        }
    }



    companion object{
        const val EDIT_FLIGHT_FRAGMENT_TAG = "EDIT_FLIGHT_FRAGMENT_TAG"
        const val PLANNED_FLIGHTS_IN_SIGHT_WHEN_SCROLLING_TO_FIRST_COMPLETED = 3
    }

    /*

    private var airportSyncProgressBar: JoozdlogProgressBar? = null
    private var flightsSyncProgressBar: JoozdlogProgressBar? = null

    private var undoMenuItem: MenuItem? = null
    private var redoMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = (ActivityMainNewBinding.inflate(layoutInflater)).apply {
            setSupportActionBar(mainToolbar)

            addAdapterAndManagerToFlightsList(makeFlightsListAdapter())

            initializeSearchTypeSpinner()

            setOnClickListeners()

            addOnTextChangedListenerToMainSearchField()

            setBackupMessageText()

            observeFeedback()

            startObservers()
        }

        setContentView(binding.root)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        undoMenuItem = getUndoMenuItem(menu).apply{
            isVisible = viewModel.undoAvailable
        }
        redoMenuItem = getRedoMenuItem(menu).apply{
            isVisible = viewModel.redoAvailable
        }
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menu_settings -> startSettingsActivity()
        R.id.menu_add_flight -> viewModel.menuSelectedAddFlight()
        R.id.menu_undo -> viewModel.undo()
        R.id.menu_redo -> viewModel.redo()
        R.id.menu_total_times -> startTotalTimesActivity()
        R.id.menu_balance_forward -> startBalanceForwardActivity()
        R.id.app_bar_search -> viewModel.menuSelectedSearch()
        R.id.menu_feedback -> startFeedbackActivity()
        R.id.menu_about -> viewModel.menuSelectedAboutDialog()
        R.id.menu_export_pdf -> startMakePdfActivity()
        else -> false
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        startNewUserActivityIfNewInstall()
        viewModel.syncWithServer()
    }


    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    private fun ActivityMainNewBinding.startObservers() {
        viewModel.showBackupNotice.observe(activity) {
            showBackupReminderLayoutIf(it)
        }

        // It doesn;t matter if pic name actually needs to be set or not,
        // if this value changes, the list needs to be redrawn.
        viewModel.picNameNeedsToBeSet.observe(activity) {
            redrawFlightsList(getFlightsListAdapter())
        }

        viewModel.backupUri.observe(activity) {
            shareBackupFile(it)
        }

        viewModel.loginFailed.observe(activity) {
            handleLoginFailedSituation(it)
        }

        viewModel.displayFlightsList.observe(activity) { fff ->
            updateFlightsListAdapter(fff)
        }

        viewModel.displayedFlightsAmount.observe(activity) {
            mainToolbar.subtitle = getString(R.string.number_of_flights, it)
        }

        viewModel.undoAvailableLiveData.observe(activity) {
            showUndoMenuItemIf(it)
        }

        viewModel.redoAvailableLiveData.observe(activity) {
            showRedoMenuItemIf(it)
        }


        viewModel.airportSyncProgress.observe(activity) { p ->
            when {
                p == -1 -> airportSyncProgressBar?.remove()
                airportSyncProgressBar != null -> airportSyncProgressBar?.progress = p
                else -> airportSyncProgressBar =
                    makeProgressBar(this, R.string.loadingAirports, p).show()
            }
        }

        viewModel.flightSyncProgress.observe(activity) { p ->
            when {
                p == -1 -> flightsSyncProgressBar?.remove()

                flightsSyncProgressBar != null -> flightsSyncProgressBar?.progress = p

                else -> flightsSyncProgressBar =
                    makeProgressBar(this, R.string.loadingFlights, p).show()
            }
        }

        /**
         * If workingFlight gives a WorkingFlight, show it in an EditFlightFragment.
         * If it is null, it means a workingFlight is closed. Check for conflicts and show undo-bar if a flight was saved.
         */
        viewModel.workingFlight.observe(activity) {
            it?.let {
                with(supportFragmentManager) {
                    findFragmentByTag(EDIT_FLIGHT_TAG)?.let {
                        commit { remove(it) }
                    }
                }
                showFlight()
            } ?: showToolbar()
        }
    }

    private fun showRedoMenuItemIf(it: Boolean) {
        redoMenuItem?.let { item ->
            item.isVisible = it
            //invalidateOptionsMenu()
        }
    }

    private fun showUndoMenuItemIf(it: Boolean) {
        undoMenuItem?.let { item ->
            item.isVisible = it
        }
    }

    private fun ActivityMainNewBinding.getFlightsListAdapter() =
        flightsList.adapter as FlightsAdapter

    private fun ActivityMainNewBinding.updateFlightsListAdapter(newFLightsList: List<DisplayFlight>) {
        getFlightsListAdapter().updateList(newFLightsList)
        scrollToFirstCompletedFlight(newFLightsList)
    }

    private fun ActivityMainNewBinding.scrollToFirstCompletedFlight(
        newFLightsList: List<DisplayFlight>
    ) {
        if (!flightsList.canScrollVertically(-1)) { // if at top
            val positionToScrollTo = findIndexOfTopFlightOnScreenAfterScrolling(newFLightsList)
            flightsList.scrollToPosition(positionToScrollTo)

            // scroll by another 30 dp
            if (positionToScrollTo != 0) flightsList.scrollBy(0, -30.dpToPixels().toInt())
        }
    }

    private fun findIndexOfTopFlightOnScreenAfterScrolling(
        newFLightsList: List<DisplayFlight>
    ) = maxOf(
        newFLightsList.firstOrNull { !it.planned }
            ?.let { f -> newFLightsList.indexOf(f) } ?: 0,
        PLANNED_FLIGHTS_IN_SIGHT_WHEN_SCROLLING_TO_FIRST_COMPLETED
    ) - PLANNED_FLIGHTS_IN_SIGHT_WHEN_SCROLLING_TO_FIRST_COMPLETED

    private fun handleLoginFailedSituation(loginFailed: Boolean) {
        if (loginFailed && Preferences.useCloud) {
            if (Preferences.newPassword.isNotEmpty()) // app got killed while changing password
                viewModel.tryToFixLogin()
            else
                showLoginDialog()
        } else hideLoginDialog()
    }

    private fun shareBackupFile(it: Uri?) {
        startActivity(Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setDataAndType(it, CSV_MIME_TYPE)
            putExtra(Intent.EXTRA_STREAM, it)
        }, "Gooi maar ergens heen aub"))
    }

    @SuppressLint("NotifyDataSetChanged")
    // I am aware this is not an efficient way to do this, but it is a very specific event.
    // (picNameNeedsToBeSet changed in settings and then navigating back to mainActivity)
    // which is a screen change anyway so one or two dropped frames won't be noticed.
    private fun redrawFlightsList(flightsListAdapter: FlightsAdapter) {
        flightsListAdapter.notifyDataSetChanged()
    }

    private fun ActivityMainNewBinding.showBackupReminderLayoutIf(it: Boolean) {
        backupReminderLayout.visibility = if (it) View.VISIBLE else View.GONE
    }

    private fun ActivityMainNewBinding.observeFeedback() {
        viewModel.feedbackEvent.observe(activity) {
            handleFeedbackEvent(it)
        }

        viewModel.errorToShow.observe(activity) {
            handleErrorEvent(it)
        }
    }

    private fun handleErrorEvent(error: Errors?) {
        when (error) {
            Errors.LOGIN_DATA_REJECTED_BY_SERVER -> showBadLoginDataDialog()
            Errors.EMAIL_CONFIRMATION_FAILED -> showBadEmailConfirmationReceivedFromServerDialog()
            Errors.SERVER_ERROR -> showRandomServerErrorDialog()
            Errors.BAD_EMAIL_SAVED -> showBadEmailAddressSavedDialog()
            null -> { /* do nothing */
            }
        }
    }

    private fun ActivityMainNewBinding.handleFeedbackEvent(feedbackevent: FeedbackEvent) {
        when (feedbackevent.getEvent()) {
            MainActivityEvents.NOT_IMPLEMENTED -> toast("Not Implemented")
            MainActivityEvents.OPEN_SEARCH_FIELD -> openSearchField()
            MainActivityEvents.CLOSE_SEARCH_FIELD -> closeSearchField()
            MainActivityEvents.FLIGHT_NOT_FOUND -> longToast("ERROR: Flight not found!")
            MainActivityEvents.TRYING_TO_DELETE_CALENDAR_FLIGHT ->
                showDeletePlannedFLightDialogWithFlightData(feedbackevent)
            MainActivityEvents.TRYING_TO_DELETE_COMPLETED_FLIGHT ->
                showDeleteCompletedFlightDialog(
                    feedbackevent.extraData.getInt(
                        MainActivityFeedbackExtraData.FLIGHT_ID
                    )
                )
            MainActivityEvents.CALENDAR_SYNC_PAUSED -> showCalendarSyncRestartInfo()
            MainActivityEvents.EMAIL_VERIFIED -> {
                longToast(R.string.email_verified)
            }
            MainActivityEvents.LOGGED_IN -> longToast(R.string.logged_in)
            MainActivityEvents.LOGIN_DELAYED_DUE_NO_SERVER -> longToast(R.string.login_delayed)

            MainActivityEvents.BACKUP_NEEDED -> JoozdlogWorkersHub.periodicBackupFromServer(true) // If a backup should be made, try to do that through server
            MainActivityEvents.SHOW_ABOUT_DIALOG -> showAboutDialog()
            MainActivityEvents.DONE -> longToast(" Yay fixed!")
            MainActivityEvents.ERROR -> longToast("An error ${feedbackevent.getInt()} occurred :(") // currently not used
        }
    }

    private fun showDeletePlannedFLightDialogWithFlightData(it: FeedbackEvent) {
        val flightID = it.extraData.getInt(MainActivityFeedbackExtraData.FLIGHT_ID)
        showDeletePlannedCalendarFlightDialog(flightID)
    }



    private fun ActivityMainNewBinding.setBackupMessageText() {
        backupMessage.text =
            getString(R.string.backup_message_main_activity, viewModel.daysSinceLastBackup)
    }

    private fun ActivityMainNewBinding.addOnTextChangedListenerToMainSearchField() {
        mainSearchField.onTextChanged { viewModel.setSearchString(it) }
    }

    private fun ActivityMainNewBinding.setOnClickListeners() {
        /*
         * addButton wil make viewModel add a null flight,
         * adding a flight will trigger EditFlightFragment to show.
         */
        addButton.setOnClickListener { viewModel.addFlight() }

        dontBackupButton.setOnClickListener { viewModel.dismissBackupUntilEndOfDay() }

        backupButton.setOnClickListener { viewModel.backUpNow() }
    }

    private fun ActivityMainNewBinding.addAdapterAndManagerToFlightsList(
        flightsListAdapter: FlightsAdapter
    ) {
        flightsList.layoutManager = LinearLayoutManager(activity)
        flightsList.adapter = flightsListAdapter
    }

    private fun makeFlightsListAdapter() =
        FlightsAdapter().apply {
            onDelete = { id -> viewModel.deleteFlight(id) }
            itemClick = { id -> viewModel.showFlight(id) }
        }


    private fun ActivityMainNewBinding.initializeSearchTypeSpinner() {
        addAdapterToSearchTypeSpinner()
        addOnItemSelectedListenerToSearchTypeSpinner()
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
                viewModel.setSpinnerSelection(position)
            }
        }
    }




    /**
     * Shows a flight in EditFlightFragment.
     * If Flight is entered, that will be loaded, if left blank or null, a new flight will be made.
     */
    private fun ActivityMainNewBinding.showFlight(){
        viewModel.closeSearchField()
        hideToolbar()
        val flightEditor = EditFlightFragment()
        supportFragmentManager.commit {
            add(R.id.mainActivityLayout, flightEditor, EDIT_FLIGHT_TAG)
            addToBackStack(null)
        }
    }

    /**
     * Show toolbar and "add" button
     */
    private fun ActivityMainNewBinding.showToolbar(){
        mainToolbar.visibility = View.VISIBLE
        addButton.setVisibilityVisibleAndFadeIn()
    }

    /**
     * hide toolbar and "add" button
     */
    private fun ActivityMainNewBinding.hideToolbar(){
        mainToolbar.visibility = View.GONE
        addButton.visibility = View.GONE
    }

    //TODO make this something else. Password is no longer known to users.
    private fun showLoginDialog(){
        JoozdlogAlertDialog().show(activity){
            title = "Your login is farked"
            message = "This will be something else one day. For now, use a working login link or create a new cloud login and resync to server"
            setPositiveButton(android.R.string.ok)
        }
    }

    private fun hideLoginDialog(){
        supportFragmentManager.findFragmentByTag(LOGIN_DIALOG_TAG)?.let {
            supportFragmentManager.commit {
                remove(it)
            }
        }
    }

    private fun makeProgressBar(binding: ActivityMainNewBinding, textResource: Int, initialProgress: Int = 0): JoozdlogProgressBar  = with (binding) {
        return JoozdlogProgressBar(progressBarField).apply{
            text = getString(textResource)
            backgroundColor = getColorFromAttr(android.R.attr.colorPrimary)
            progress = initialProgress
        }
    }

    private fun showDeletePlannedCalendarFlightDialog(id: Int){
        JoozdlogAlertDialog().show(this) {
            messageResource = R.string.delete_calendar_flight
            setPositiveButton(android.R.string.ok){
                viewModel.disableCalendarImport()
                viewModel.deleteFlight(id)
            }
            setNegativeButton(R.string.delete_calendar_flight_until_end){
                viewModel.deleteAndDisableCalendarImportUntillAfterThisFlight(id)
            }
            setNeutralButton(android.R.string.cancel)
        }
    }

    private fun showDeleteCompletedFlightDialog(id: Int){
        JoozdlogAlertDialog().show(this) {
            title = "WARNING"
            messageResource = R.string.delete_completed_flight
            setPositiveButton("Continue") {
                viewModel.deleteNotPlannedFlight(id)}
            setNegativeButton("Cancel")
        }
    }

    private fun showCalendarSyncRestartInfo(){
        JoozdlogAlertDialog().show(this) {
            messageResource = R.string.you_can_start_calendar_sync_again
            setPositiveButton(android.R.string.ok)
        }
    }



    private fun showBadLoginDataDialog(){
        JoozdlogAlertDialog().show(activity){
            titleResource = R.string.error
            messageResource = R.string.wrong_username_password
            setPositiveButton(android.R.string.ok){ viewModel.loginErrorSeen() }
        }
    }

    private fun showBadEmailConfirmationReceivedFromServerDialog(){
        JoozdlogAlertDialog().show(activity){
            titleResource = R.string.error
            messageResource = R.string.email_data_rejected
            setPositiveButton(android.R.string.ok){ viewModel.emailConfirmationErrorSeen() }
        }
    }

    private fun showBadEmailAddressSavedDialog(){
        JoozdlogAlertDialog().show(activity){
            titleResource = R.string.error
            messageResource = R.string.email_address_rejected
            setPositiveButton(android.R.string.ok){ viewModel.badEmailErrorSeen() }
        }
    }

    private fun showRandomServerErrorDialog(){
        JoozdlogAlertDialog().show(activity){
            titleResource = R.string.error
            messageResource = R.string.server_random_error
            setPositiveButton(android.R.string.ok){ viewModel.serverErrorSeen() }
        }
    }





    companion object{
        const val EDIT_FLIGHT_TAG = "EDIT_FLIGHT_TAG"
        const val PLANNED_FLIGHTS_IN_SIGHT_WHEN_SCROLLING_TO_FIRST_COMPLETED = 3
        const val LOGIN_DIALOG_TAG = "LOGIN_DIALOG"
        private const val CSV_MIME_TYPE = "text/csv"
    }

     */
}
