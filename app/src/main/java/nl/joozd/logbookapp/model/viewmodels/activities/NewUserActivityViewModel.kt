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
import nl.joozd.logbookapp.data.comm.ServerFunctions
import nl.joozd.logbookapp.data.comm.UserManagement
import nl.joozd.logbookapp.data.comm.protocol.CloudFunctionResults
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvent
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.NewUserActivityEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import nl.joozd.logbookapp.utils.generatePassword

class NewUserActivityViewModel: JoozdlogActivityViewModel() {

    /*******************************************************************************************
     * Private parts
     *******************************************************************************************/

    private val internetAvailable: LiveData<Boolean> = InternetStatus.internetAvailableLiveData

    private val _page1Feedback = MutableLiveData<FeedbackEvent>()
    private val _page2Feedback = MutableLiveData<FeedbackEvent>()
    private val _page3Feedback = MutableLiveData<FeedbackEvent>()

    private val calendarScraper = CalendarScraper(context)

    private val _username = MutableLiveData(Preferences.username)
    private val _emailAddress = MutableLiveData(Preferences.emailAddress)
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
            Preferences::emailAddress.name -> _emailAddress.value = Preferences.emailAddress
        }
    }
    init{
        Preferences.getSharedPreferences().registerOnSharedPreferenceChangeListener (onSharedPrefsChangedListener)
    }


    /*******************************************************************************************
     * Observables
     *******************************************************************************************/

    val page1Feedback: LiveData<FeedbackEvent>
        get() = _page1Feedback

    val page2Feedback: LiveData<FeedbackEvent>
        get() = _page2Feedback

    val page3Feedback: LiveData<FeedbackEvent>
        get() = _page3Feedback

    val username: LiveData<String?>
        get() = _username

    val emailAddress: LiveData<String>
        get() = _emailAddress

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

    /*******************************************************************************************
     * Page1 functions
     *******************************************************************************************/


    var email1 = Preferences.emailAddress
    var email2 = Preferences.emailAddress

    fun okClickedPage1(){
        Log.d("okClickedPage1()", "email1: $email1, email2: $email2")
        if (email1 != email2) feedback(FeedbackEvents.GeneralEvents.ERROR, _page1Feedback).putInt(3)
        else {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email1).matches()) feedback(FeedbackEvents.GeneralEvents.ERROR, _page1Feedback).putInt(4)
            Preferences.emailAddress = email1
            Preferences.backupFromCloud = true
            feedback(FeedbackEvents.GeneralEvents.DONE, _page1Feedback)
        }
    }

    fun updateEmail(it: String){
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(it.trim()).matches())
            feedback(FeedbackEvents.GeneralEvents.ERROR, _page1Feedback).putString("Not an email address").putInt(1) // TODO make other errors as well
        email1 = it.trim()
    }

    fun updateEmail2(it: String){
        when {
            it.trim() != email1 ->
                feedback(FeedbackEvents.GeneralEvents.ERROR, _page1Feedback).putString("Does not match").putInt(2) // TODO make other errors as well
            else -> {
                email2 = it.trim()

            }
        }
    }

    fun checkSame1(s: String): Boolean =
        (s.trim() == email2 && android.util.Patterns.EMAIL_ADDRESS.matcher(email2).matches())

    fun checkSame2(s: String): Boolean =
        (s.trim() == email1 && android.util.Patterns.EMAIL_ADDRESS.matcher(email1).matches()).also{
            email2 = s
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
                internetAvailable.value != true -> feedback(NewUserActivityEvents.NO_INTERNET, _page2Feedback)
                username.isBlank() -> feedback(NewUserActivityEvents.USERNAME_TOO_SHORT, _page2Feedback)
                else -> { // username not empty and internet looks OK
                    val email = Preferences.emailAddress.takeIf { it.isNotEmpty() }
                    feedback(NewUserActivityEvents.WAITING_FOR_SERVER, _page2Feedback)
                    viewModelScope.launch {
                        val pass1 = generatePassword(16)
                        when (UserManagement.createNewUser(username, pass1, email)) { // UserManagement will call the correct Cloud function
                            CloudFunctionResults.OK -> {
                                feedback(NewUserActivityEvents.LOGGED_IN_AS, _page2Feedback)
                                Preferences.emailJobsWaiting.sendLoginLink = true
                            }
                            CloudFunctionResults.CLIENT_ERROR -> feedback(NewUserActivityEvents.SERVER_NOT_RESPONDING, _page2Feedback)
                            CloudFunctionResults.SERVER_ERROR -> feedback(NewUserActivityEvents.SERVER_NOT_RESPONDING, _page2Feedback)
                            CloudFunctionResults.NOT_A_VALID_EMAIL_ADDRESS -> feedback(NewUserActivityEvents.BAD_EMAIL, _page2Feedback)
                            CloudFunctionResults.UNKNOWN_REPLY_FROM_SERVER -> feedback(NewUserActivityEvents.SERVER_NOT_RESPONDING, _page2Feedback)
                            CloudFunctionResults.USER_ALREADY_EXISTS -> feedback(NewUserActivityEvents.USER_EXISTS, _page2Feedback)
                            else -> feedback(NewUserActivityEvents.UNKNOWN_ERROR, _page2Feedback)
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
            _foundCalendars.value =
                withContext(Dispatchers.IO) { calendarScraper.getCalendarsList() }
        }
    }

    fun calendarPicked(index: Int) {
        _foundCalendars.value?.get(index)?.let {
            Preferences.selectedCalendar = it.name
            Preferences.getFlightsFromCalendar = true
            feedback(NewUserActivityEvents.CALENDAR_PICKED, _page3Feedback)
        }
    }

    fun noCalendarSelected(){
        Preferences.getFlightsFromCalendar = false
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
        private const val INITIAL_OPEN_PAGES = 4
    }
}