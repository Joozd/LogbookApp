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
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.fragment.app.commit
import androidx.recyclerview.widget.LinearLayoutManager
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.data.sharedPrefs.errors.Errors
import nl.joozd.logbookapp.databinding.ActivityMainNewBinding
import nl.joozd.logbookapp.extensions.*
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.MainActivityEvents
import nl.joozd.logbookapp.model.viewmodels.activities.mainActivity.MainActivityFeedbackExtraData
import nl.joozd.logbookapp.model.viewmodels.activities.mainActivity.MainActivityViewModel
import nl.joozd.logbookapp.ui.activities.newUserActivity.NewUserActivity
import nl.joozd.logbookapp.ui.activities.totalTimesActivity.TotalTimesActivity
import nl.joozd.logbookapp.ui.adapters.flightsadapter.FlightsAdapter
import nl.joozd.logbookapp.ui.dialogs.AboutDialog
import nl.joozd.logbookapp.ui.dialogs.JoozdlogAlertDialog
import nl.joozd.logbookapp.ui.dialogs.EditFlightFragment
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity
import nl.joozd.logbookapp.ui.utils.customs.JoozdlogProgressBar
import nl.joozd.logbookapp.ui.utils.longToast
import nl.joozd.logbookapp.ui.utils.toast
import nl.joozd.logbookapp.workmanager.JoozdlogWorkersHub

//TODO: Handle Scheduled Errors from ScheduledErrors
class MainActivity : JoozdlogActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    private var airportSyncProgressBar: JoozdlogProgressBar? = null
    private var flightsSyncProgressBar: JoozdlogProgressBar? = null

    private var undoMenuItem: MenuItem? = null
    private var redoMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        with (ActivityMainNewBinding.inflate(layoutInflater)) {
            setSupportActionBarWithReturn(mainToolbar)?.apply {
                // intentionally blank
            }

            /**
             * Flights List (RecyclerView) adapter initialization and onClick/onDelete
             */
            val flightsAdapter = FlightsAdapter().apply {
                onDelete = { id -> viewModel.deleteFlight(id) }
                itemClick = { id -> viewModel.showFlight(id) }
            }.also {
                flightsList.layoutManager = LinearLayoutManager(activity)
                flightsList.adapter = it
            }

            /**
             * Fill spinner and set onSelected
             * for search field
             */
            ArrayAdapter.createFromResource(
                activity,
                R.array.search_options,
                android.R.layout.simple_spinner_item
            ).apply {
                // Specify the layout to use when the list of choices appears
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }.also { adapter ->
                // Apply the adapter to the spinner
                searchTypeSpinner.adapter = adapter
            }
            searchTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {}
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    //mainSearchField.text = mainSearchField.text
                    viewModel.setSpinnerSelection(position)
                }
            }

            /**
             * addButton wil show a null flight (and showing a null flight will create a new empty flight)
             */
            addButton.setOnClickListener {
                viewModel.addFlight()
            }

            /**
             * Search field and -spinner functions
             */
            mainSearchField.onTextChanged {
                viewModel.setSearchString(it)
            }

            /**
             * Hide backup mnessage until end of day or untill next creation of viewModel
             */
            dontBackupButton.setOnClickListener { viewModel.dismissBackup() }

            backupButton.setOnClickListener { viewModel.backUpNow() }

            /**
             * Set dynamic text fields:
             */
            backupMessage.text = getString(R.string.backup_message_main_activity, viewModel.daysSinceLastBackup)

            /*******************************************************************************************
             * Observers below here
             *******************************************************************************************/

            viewModel.showBackupNotice.observe(activity){
                backupReminderLayout.visibility = if (it) View.VISIBLE else View.GONE
            }

            /**
             * Redraw flights list when requirements for PIC name being filled changes
             */
            viewModel.picNameNeedsToBeSet.observe(activity){
                flightsAdapter.notifyDataSetChanged()
            }

            viewModel.backupUri.observe(activity){
                startActivity(Intent.createChooser(Intent().apply{
                    action = Intent.ACTION_SEND
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setDataAndType(it, CSV_MIME_TYPE)
                    putExtra(Intent.EXTRA_STREAM, it)
                }, "Gooi maar ergens heen aub"))
            }

            /**
             * Checks for bad login data
             */
            viewModel.notLoggedIn.observe(activity) {
                if (it && Preferences.useCloud){
                    if (Preferences.newPassword.isNotEmpty()) // app got killed while changing password
                        viewModel.tryToFixLogin()
                    else
                        showLoginDialog()
                }
                else hideLoginDialog()
            }

            viewModel.displayFlightsList.observe(activity,){ fff ->
                flightsAdapter.updateList(fff)
                if (!flightsList.canScrollVertically(-1)){ // if at top
                    val plannedFlightsInSight = findAmountOfPlannedFlightsToShow()
                    val positionToScrollTo = maxOf (fff.firstOrNull{!it.planned}?.let { f -> fff.indexOf(f)} ?: 0, plannedFlightsInSight) - plannedFlightsInSight
                    flightsList.scrollToPosition(positionToScrollTo)

                    // scroll by half the size of the top item in the list (actual item may not be drawn yet so we'll hav to make do with

                    if (positionToScrollTo != 0) flightsList.scrollBy(0, -30.dpToPixels().toInt())
                }
            }

            viewModel.displayedFlightsAmount.observe(activity){
                mainToolbar.subtitle = getString(R.string.number_of_flights, it)
            }

            viewModel.errorToShow.observe(activity) {
                when (it) {
                    Errors.LOGIN_DATA_REJECTED_BY_SERVER -> showBadLoginDataDialog()
                    Errors.EMAIL_CONFIRMATION_FAILED -> showBadEmailConfirmationDialog()
                    Errors.SERVER_ERROR -> showRandomServerErrorDialog()
                    Errors.BAD_EMAIL_SAVED -> showBadEmailSavedDialog()
                    // null -> do nothing
                }
            }

            viewModel.undoAvailableLiveData.observe(activity){
                println("BOERENMETWORST!!!!!1 $it")
                undoMenuItem?.let{ item ->
                    println("BERNAAN")
                    item.isVisible = it
                    //invalidateOptionsMenu()
                }

            }

            viewModel.redoAvailableLiveData.observe(activity){
                println("BOERENZONDERWORST!!!!!1 $it")
                redoMenuItem?.let{ item ->
                    println("BERNAAN2")
                    item.isVisible = it
                    //invalidateOptionsMenu()
                }
            }

            viewModel.feedbackEvent.observe(activity) {
                when (it.getEvent()) {
                    MainActivityEvents.NOT_IMPLEMENTED -> toast("Not Implemented")
                    // MainActivityEvents.SHOW_FLIGHT -> showFlight() this is now through liveData
                    MainActivityEvents.OPEN_SEARCH_FIELD -> {
                        searchField.visibility = View.VISIBLE
                        mainSearchField.requestFocus()
                        showKeyboard()
                    }
                    MainActivityEvents.CLOSE_SEARCH_FIELD -> {
                        mainSearchField.setText("")
                        mainSearchField.clearFocus()
                        hideKeyboard(mainSearchField)
                        searchTypeSpinner.setSelection(0)
                        //reset search types spinner//
                        searchField.visibility = View.GONE
                    }
                    MainActivityEvents.FLIGHT_NOT_FOUND -> longToast("ERROR: Flight not found!")
                    MainActivityEvents.TRYING_TO_DELETE_CALENDAR_FLIGHT ->
                        showDeletePlannedCalendarFlightDialog(it.extraData.getInt(MainActivityFeedbackExtraData.FLIGHT_ID))
                    MainActivityEvents.TRYING_TO_DELETE_COMPLETED_FLIGHT ->
                        showDeleteCompletedFlightDialog(it.extraData.getInt(MainActivityFeedbackExtraData.FLIGHT_ID))
                    MainActivityEvents.CALENDAR_SYNC_PAUSED -> showCalendarSyncRestartInfo()
                    MainActivityEvents.EMAIL_VERIFIED -> {
                        Log.d("MainActivity", "Email Verified!")
                        longToast(R.string.email_verified)
                    }
                    MainActivityEvents.LOGGED_IN -> longToast(R.string.logged_in)
                    MainActivityEvents.LOGIN_DELAYED_DUE_NO_SERVER -> longToast(R.string.login_delayed)

                    MainActivityEvents.BACKUP_NEEDED -> JoozdlogWorkersHub.periodicBackupFromServer(true) // If a backup should be made, try to do that through server
                    MainActivityEvents.SHOW_ABOUT_DIALOG -> showAboutDialog()
                    MainActivityEvents.DONE -> longToast(" Yay fixed!")
                    MainActivityEvents.ERROR -> longToast("An error occurred :(")
                }
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

                    else -> flightsSyncProgressBar = makeProgressBar(this, R.string.loadingFlights, p).show()
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

            setContentView(root)
        }
    }


    /**
     * Create menu
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        undoMenuItem = menu.findItem(R.id.menu_undo).also{ item ->
            item.isVisible = viewModel.undoAvailable
        }
        redoMenuItem = menu.findItem(R.id.menu_redo).also{ item ->
            item.isVisible = viewModel.redoAvailable
        }
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
        R.id.menu_undo -> {
            viewModel.undo()
        }

        R.id.menu_redo -> {
            viewModel.redo()
        }


        R.id.menu_total_times -> {
            startActivity(Intent(this, TotalTimesActivity::class.java))
            true
        }
        R.id.menu_balance_forward -> {
            startActivity(Intent(this, BalanceForwardActivity::class.java))
            true
        }
        R.id.app_bar_search -> {
            viewModel.menuSelectedSearch()
            true
        }
        R.id.menu_feedback -> {
            startActivity(Intent(this, FeedbackActivity::class.java))
            true
        }
        R.id.menu_about -> {
            viewModel.menuSelectedAboutDialog()
            true
        }
        /*
        R.id.menu_edit_aircraft -> {
            viewModel.menuSelectedEditAircraft()
            true
        }
        */
        R.id.menu_export_pdf -> {
            //viewModel.menuSelectedExportPDF()
            startActivity(Intent(this, MakePdfActivity::class.java))
            //TODO decide if this becomes a new activity or a fragment
            true
        }
/*
        R.id.menu_do_something -> {
            viewModel.menuSelectedDoSomething()

            true

        }

        R.id.menu_login -> {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
            true
        }
        */
        else -> false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()

        if (!Preferences.newUserActivityFinished)
            startActivity(Intent(this, NewUserActivity::class.java))
        viewModel.notifyActivityResumed()

    }

    override fun onBackPressed() {
        moveTaskToBack(true)
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
        addButton.fadeIn()
    }

    /**
     * hide toolbar and "add" button
     */
    private fun ActivityMainNewBinding.hideToolbar(){
        mainToolbar.visibility = View.GONE
        addButton.fadeOut()
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

    private fun showAboutDialog(){
        supportFragmentManager.commit{
            add(R.id.mainActivityLayout, AboutDialog())
            addToBackStack(null)
        }

    }

    /**
     * Shows dialog stating that login data have been rejected by server
     * Will notify viewModel that OK has been clicked
     */
    private fun showBadLoginDataDialog(){
        JoozdlogAlertDialog().show(activity){
            titleResource = R.string.error
            messageResource = R.string.wrong_username_password
            setPositiveButton(android.R.string.ok){ viewModel.loginErrorSeen() }
        }
    }

    /**
     * Shows dialog stating that login data have been rejected by server
     * Will notify viewModel that OK has been clicked
     */
    private fun showBadEmailConfirmationDialog(){
        JoozdlogAlertDialog().show(activity){
            titleResource = R.string.error
            messageResource = R.string.email_data_rejected
            setPositiveButton(android.R.string.ok){ viewModel.emailConfirmationErrorSeen() }
        }
    }

    /**
     * Shows dialog stating that login data have been rejected by server
     * Will notify viewModel that OK has been clicked
     */
    private fun showBadEmailSavedDialog(){
        JoozdlogAlertDialog().show(activity){
            titleResource = R.string.error
            messageResource = R.string.email_address_rejected
            setPositiveButton(android.R.string.ok){ viewModel.badEmailErrorSeen() }
        }
    }


    /**
     * Shows dialog stating that server did something wierd
     * Will notify viewModel that OK has been clicked
     */
    private fun showRandomServerErrorDialog(){
        JoozdlogAlertDialog().show(activity){
            titleResource = R.string.error
            messageResource = R.string.server_random_error
            setPositiveButton(android.R.string.ok){ viewModel.serverErrorSeen() }
        }
    }

    private fun showKeyboard(){
        val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    private fun hideKeyboard(view: View){
        val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * Returns the amount of planned flights to show when opening app (constant now, might change for something dynamic depending on screen size or somwething.
     * It's an nl.joozd.joozdlogpdfdetector.interface dependant function, thats why this is here and not in ViewModel
     */
    private fun findAmountOfPlannedFlightsToShow(): Int{
        return PLANNED_FLIGHTS_IN_SIGHT
    }




    companion object{
        const val EDIT_FLIGHT_TAG = "EDIT_FLIGHT_TAG"
        const val PLANNED_FLIGHTS_IN_SIGHT = 3

        const val LOGIN_DIALOG_TAG = "LOGIN_DIALOG"

        private const val CSV_MIME_TYPE = "text/csv"

    }
}
