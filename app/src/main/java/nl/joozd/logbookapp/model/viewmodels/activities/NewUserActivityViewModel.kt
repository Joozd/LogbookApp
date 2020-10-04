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
import nl.joozd.logbookapp.data.calendar.CalendarScraper
import nl.joozd.logbookapp.data.calendar.dataclasses.JoozdCalendar
import nl.joozd.logbookapp.data.comm.InternetStatus
import nl.joozd.logbookapp.data.comm.UserManagement
import nl.joozd.logbookapp.data.repository.GeneralRepository
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvent
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.NewUserActivityEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import nl.joozd.logbookapp.utils.checkPasswordSafety

class NewUserActivityViewModel: JoozdlogActivityViewModel() {
    /*******************************************************************************************
     * Private parts
     *******************************************************************************************/

    val internetAvailable: LiveData<Boolean> = InternetStatus.internetAvailableLiveData

    private val _page2Feedback = MutableLiveData<FeedbackEvent>()
    private val _page3Feedback = MutableLiveData<FeedbackEvent>()

    private val calendarScraper = CalendarScraper(context)

    private val _username = MutableLiveData(Preferences.username)
    private val _acceptTerms = MutableLiveData(Preferences.acceptedCloudSyncTerms)
    private val _useIataAirports = MutableLiveData(Preferences.useIataAirports)

    private val _foundCalendars = MutableLiveData<List<JoozdCalendar>>()

    private fun getString(resID: Int) = App.instance.getString(resID)



    private val onSharedPrefsChangedListener =  SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        // Log.d("AirportRepository", "key = $key")
        if (key == Preferences::usernameResource.name) _username.value = Preferences.username
        if (key == Preferences::acceptedCloudSyncTerms.name) _acceptTerms.value = Preferences.acceptedCloudSyncTerms
        if (key == Preferences::useIataAirports.name) _useIataAirports.value = Preferences.useIataAirports
    }
    init{
        Preferences.getSharedPreferences().registerOnSharedPreferenceChangeListener (onSharedPrefsChangedListener)
    }


    /*******************************************************************************************
     * Observables
     *******************************************************************************************/

    val page2Feedback: LiveData<FeedbackEvent>
        get() = _page2Feedback

    val page3Feedback: LiveData<FeedbackEvent>
        get() = _page3Feedback

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
     * @param pageNumber: Number of the page requesting the next page do be displayed
     */
    fun nextPage(pageNumber: Int){
        feedback(NewUserActivityEvents.NEXT_PAGE).apply{
            putInt(pageNumber)
        }
    }

    fun done(){
        Preferences.newUserActivityFinished = true
        syncFlights()
        feedback(NewUserActivityEvents.FINISHED)
    }

    fun syncGeneralData(){
        GeneralRepository.synchTimeWithServer()
        airportRepository.getAirportsIfNeeded()
        aircraftRepository.checkIfAircraftTypesUpToDate()
    }

    fun syncFlights(){
        flightRepository.syncIfNeeded()
    }

    /*******************************************************************************************
     * Page1 functions
     *******************************************************************************************/


    /*******************************************************************************************
     * Page2 functions
     *******************************************************************************************/

    fun signInClicked(){
        feedback(NewUserActivityEvents.SHOW_SIGN_IN_DIALOG)
    }

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
    fun signUpClicked(username: String, pass1: String, pass2: String){
        Log.d("signUpClicked", "user: $username, pass1: $pass1, pass2: $pass2")
        Preferences.useCloud = true
        when{
            internetAvailable.value != true -> feedback(NewUserActivityEvents.NO_INTERNET, _page2Feedback)
            pass1.isBlank() -> feedback(NewUserActivityEvents.PASSWORD_TOO_SHORT, _page2Feedback)
            username.isBlank() -> feedback(NewUserActivityEvents.USERNAME_TOO_SHORT, _page2Feedback)
            pass1 != pass2 -> feedback(NewUserActivityEvents.PASSWORDS_DO_NOT_MATCH, _page2Feedback)
            !checkPasswordSafety(pass1) -> feedback(NewUserActivityEvents.PASSWORD_DOES_NOT_MEET_STANDARDS, _page2Feedback)
            else -> { // passwords match, are good enough, username not empty and internet looks OK
                feedback(NewUserActivityEvents.WAITING_FOR_SERVER, _page2Feedback)
                viewModelScope.launch{
                    when(UserManagement.createNewUser(username, pass1)){
                        true -> feedback(NewUserActivityEvents.LOGGED_IN_AS, _page2Feedback)
                        null -> feedback(NewUserActivityEvents.SERVER_NOT_RESPONDING, _page2Feedback)
                        false -> { // username taken, check to see if password correct
                            when(UserManagement.login(username, pass1)){
                                true -> feedback(NewUserActivityEvents.USER_EXISTS_PASSWORD_CORRECT, _page2Feedback)
                                false -> feedback(NewUserActivityEvents.USER_EXISTS_PASSWORD_INCORRECT, _page2Feedback)
                                null -> feedback(NewUserActivityEvents.SERVER_NOT_RESPONDING, _page2Feedback)
                            }
                        }
                    }
                }
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
    var password1State: Editable? = null
    var password2State: Editable? = null

    var openPagesState: Int = INITIAL_OPEN_PAGES
    var lastOpenPageState: Int? = null

    companion object{
        private const val INITIAL_OPEN_PAGES = 4
    }
}