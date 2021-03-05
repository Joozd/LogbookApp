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

package nl.joozd.logbookapp.model.viewmodels.activities

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.text.Editable
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.calendar.CalendarScraper
import nl.joozd.logbookapp.data.calendar.dataclasses.JoozdCalendar
import nl.joozd.logbookapp.data.comm.InternetStatus
import nl.joozd.logbookapp.data.comm.UserManagement
import nl.joozd.logbookapp.data.comm.protocol.CloudFunctionResults
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvent
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.NewUserActivityEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import nl.joozd.logbookapp.ui.utils.toast
import nl.joozd.logbookapp.utils.generatePassword
import java.util.*

class NewUserActivityViewModel: JoozdlogActivityViewModel() {

    /*******************************************************************************************
     * Private parts
     *******************************************************************************************/

    private val internetAvailable: LiveData<Boolean> = InternetStatus.internetAvailableLiveData

    private val _feedbackChannels = Array(FEEDBACK_CHANNELS) { MutableLiveData<FeedbackEvent>() }

    /**
     * List of pages corresponging to if their "continue" button is activated
     */
    private val nextButtonEnabledList = Array(5){
        when(it){
            0, 4 -> true
            else -> false
        }
    }

    /*
    private val _page1Feedback = MutableLiveData<FeedbackEvent>()
    private val _page2Feedback = MutableLiveData<FeedbackEvent>()
    private val _page3Feedback = MutableLiveData<FeedbackEvent>()
    */

    private val calendarScraper = CalendarScraper(context)


    /**
     * Mutable Livedata (private)
     */

    //page 1:
    private val _emailsMatch = MutableLiveData(false)


    private val _username = MutableLiveData(Preferences.username)





    private val _acceptTerms = MutableLiveData(Preferences.acceptedCloudSyncTerms)
    private val _useIataAirports = MutableLiveData(Preferences.useIataAirports)

    private val _foundCalendars = MutableLiveData<List<JoozdCalendar>>()

    private fun getString(resID: Int) = App.instance.getString(resID)



    private val onSharedPrefsChangedListener =  SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        // Log.d("AirportRepository", "key = $key")
        when(key) {
            Preferences::usernameResource.name -> _username.value = Preferences.username
            Preferences::acceptedCloudSyncTerms.name -> _acceptTerms . value = Preferences . acceptedCloudSyncTerms
            Preferences::useIataAirports.name -> _useIataAirports.value = Preferences.useIataAirports
        }
    }
    init{
        Preferences.getSharedPreferences().registerOnSharedPreferenceChangeListener (onSharedPrefsChangedListener)
    }


    /*******************************************************************************************
     * Observables
     *******************************************************************************************/

    //page 1:
    val emailsMatch: LiveData<Boolean>
        get() = Transformations.distinctUntilChanged(_emailsMatch)

    val username: LiveData<String?>
        get() = _username

    val acceptTerms: LiveData<Boolean>
        get() = _acceptTerms

    val useIataAirports: LiveData<Boolean>
        get() = _useIataAirports


    val foundCalendars = Transformations.map(_foundCalendars) {it.map{ c -> c.name} }


    /*******************************************************************************************
     * Public functions
     *******************************************************************************************/

    /*******************************************************************************************
     * Shared functions
     *******************************************************************************************/

    /**
     * Tells Activity to make "Continue" button active
     */
    fun makeContinueActive(pageNumber: Int, isActive: Boolean){
        nextButtonEnabledList[pageNumber] = isActive
        feedback(NewUserActivityEvents.UPDATE_NAVBAR).putBoolean(isActive)
    }

    fun nextButtonEnabled(position: Int) = nextButtonEnabledList[position]

    fun continueClicked(position: Int) {
        when(position){
            0 -> feedback(NewUserActivityEvents.NEXT_PAGE)
            1 -> toast ("Continue clicked  for email page")
            2 -> toast ("Continue clicked  for cloud page")
            3 -> toast ("Continue clicked  for calendar page")
            4 -> toast ("Done clicked. This should save all data.")
        }

    }

    fun skipClicked(position: Int){
        when(position){
            1 -> { // skip email page -> make email1 and 2 empty
                email1 = ""
                email2 = ""
                feedback(NewUserActivityEvents.CLEAR_PAGE, PAGE_EMAIL)
                feedback(NewUserActivityEvents.NEXT_PAGE)
            }
            else -> feedback(NewUserActivityEvents.NEXT_PAGE)
        }
    }

    fun getFeedbackChannel(channel: Int): LiveData<FeedbackEvent>{
        require (channel in 0 until FEEDBACK_CHANNELS) { "channel must be between 0 and $FEEDBACK_CHANNELS (NewUserActivityViewModel.Companion.FEEDBACK_CHANNELS)"}
        return _feedbackChannels[channel]
    }

    /**
     * @param pageNumber: Number of the page requesting the next page do be displayed
     */
    fun nextPage(pageNumber: Int){
        feedback(NewUserActivityEvents.NEXT_PAGE).apply{
            putInt(pageNumber + 1)
        }
    }

    fun done(){
        Preferences.newUserActivityFinished = true
        syncFlights()
        feedback(NewUserActivityEvents.FINISHED)
    }

    fun syncFlights(){
        flightRepository.syncIfNeeded()
    }

    /**
     * Send feedback to specific channel
     * @param event: a [FeedbackEvents.Event]
     * @param channel: channel to send to, must be in (0 until [FEEDBACK_CHANNELS])
     */
    fun feedback(event: FeedbackEvents.Event, channel: Int): FeedbackEvent {
        require (channel in 0 until FEEDBACK_CHANNELS) { "channel must be between 0 and $FEEDBACK_CHANNELS (NewUserActivityViewModel.Companion.FEEDBACK_CHANNELS)"}
        return feedback(event, _feedbackChannels[channel])
    }

    /*******************************************************************************************
     * Page1 functions (PAGE_EMAIL)
     *******************************************************************************************/


    var email1 = Preferences.emailAddress
        private set
    var email2 = Preferences.emailAddress
        private set

    fun page1InputChanged(input1: String, input2: String){
        email1 = input1.toLowerCase(Locale.ROOT).trim()
        email2 = input2.toLowerCase(Locale.ROOT).trim()
        checkEmails()
    }

    fun checkEmail1(){
        if (email1.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email1).matches())
            feedback(NewUserActivityEvents.BAD_EMAIL, PAGE_EMAIL)
    }

    /**
     * Check if emails are valid and matching, if so, enable continue button
     */
    private fun checkEmails(): Boolean =
        (email1 == email2 && android.util.Patterns.EMAIL_ADDRESS.matcher(email2).matches()).also{
            if (nextButtonEnabledList[PAGE_EMAIL] != it)
                makeContinueActive(PAGE_EMAIL, it)
    }




    /*******************************************************************************************
     * Page2 functions
     *******************************************************************************************/

    /**
     * Sign out user, send that as feedback
     */
    fun signOutClicked(){
        Log.d("NewUserActivity", "Sign out clicked!")
        Preferences.username = null
        Preferences.password = ""
        _username.value = null
    }

    //TODO document this
    // TODO finish this in Fragment
    // TODO needs a way to handle time when server is busy (in Fragment)
    fun signUpClicked(usernameEditable: Editable?) {
        usernameEditable?.toString()?.let { username ->
            Log.d("signUpClicked", "user: $username")
            Preferences.useCloud = true
            when {
                internetAvailable.value != true -> feedback(NewUserActivityEvents.NO_INTERNET, 2)
                username.isBlank() -> feedback(NewUserActivityEvents.USERNAME_TOO_SHORT, 2)
                else -> { // username not empty and internet looks OK
                    val email = Preferences.emailAddress.takeIf { it.isNotEmpty() }
                    feedback(NewUserActivityEvents.WAITING_FOR_SERVER, 2)
                    viewModelScope.launch {
                        val pass1 = generatePassword(16)
                        when (UserManagement.createNewUser(username, pass1, email)) { // UserManagement will call the correct Cloud function
                            CloudFunctionResults.OK -> {
                                feedback(NewUserActivityEvents.LOGGED_IN_AS, 2)
                                Preferences.emailJobsWaiting.sendLoginLink = true
                            }
                            CloudFunctionResults.CLIENT_ERROR -> feedback(NewUserActivityEvents.SERVER_NOT_RESPONDING, 2)
                            CloudFunctionResults.SERVER_ERROR -> feedback(NewUserActivityEvents.SERVER_NOT_RESPONDING, 2)
                            CloudFunctionResults.NOT_A_VALID_EMAIL_ADDRESS -> feedback(NewUserActivityEvents.BAD_EMAIL, 2)
                            CloudFunctionResults.UNKNOWN_REPLY_FROM_SERVER -> feedback(NewUserActivityEvents.SERVER_NOT_RESPONDING, 2)
                            CloudFunctionResults.USER_ALREADY_EXISTS -> feedback(NewUserActivityEvents.USER_EXISTS, 2)
                            else -> feedback(NewUserActivityEvents.UNKNOWN_ERROR, 2)
                        }
                    }
                }
            }
        }
    }

    /**
     * Copy a login link to clipboard (assuming logged in, else do nothing)
     */
    fun copyLoginLinkToClipboard(){
        UserManagement.generateLoginLink()?.let { loginLink ->
            with(App.instance) {
                (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                    ClipData.newPlainText(getString(R.string.login_link), loginLink)
                )
            }
        }
    }


    fun dontUseCloud(){
        Preferences.useCloud = false
    }

    /*******************************************************************************************
     * Page3 functions
     *******************************************************************************************/

    @RequiresPermission(Manifest.permission.READ_CALENDAR)
    fun fillCalendarsList() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { calendarScraper.getCalendarsList() }.let {
                _foundCalendars.value = it
            }
        }
    }

    fun calendarPicked(index: Int) {
        _foundCalendars.value?.get(index)?.let {
            Preferences.selectedCalendar = it.name
            Preferences.useCalendarSync = true
            feedback(NewUserActivityEvents.CALENDAR_PICKED, 1)
        }
    }

    fun noCalendarSelected(){
        Preferences.useCalendarSync = false
    }

    /*******************************************************************************************
     * Page 4 functions
     *******************************************************************************************/

    fun icaoIataToggle(){
        Preferences.useIataAirports = !Preferences.useIataAirports
    }


    /*******************************************************************************************
     * misc functions
     *******************************************************************************************/

    override fun onCleared() {
        super.onCleared()
        Preferences.getSharedPreferences().unregisterOnSharedPreferenceChangeListener (onSharedPrefsChangedListener)
    }

    /*******************************************************************************************
     * Instance states
     *******************************************************************************************/

    //page 2 editTexts
    var userNameState: Editable? = null

    var openPagesState: Int = INITIAL_OPEN_PAGES
    var lastOpenPageState: Int? = null

    companion object{
        const val PAGE_INTRO = 0
        const val PAGE_EMAIL = 1
        const val PAGE_CLOUD = 2

        private const val FEEDBACK_CHANNELS = 10
        private const val INITIAL_OPEN_PAGES = 5
    }
}