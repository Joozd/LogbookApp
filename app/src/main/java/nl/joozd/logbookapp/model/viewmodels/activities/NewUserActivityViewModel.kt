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

import android.content.SharedPreferences
import androidx.lifecycle.*
import kotlinx.coroutines.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.comm.UserManagement
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvent
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.SettingsActivityEvents
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.NewUserActivityEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogActivityViewModel
import java.util.*

class NewUserActivityViewModel: JoozdlogActivityViewModel() {

    /*******************************************************************************************
     * Private parts
     *******************************************************************************************/

    /**
     * Feedback channels, every page should observe it's channel if feedback is needed
     */
    private val _feedbackChannels = Array(FEEDBACK_CHANNELS) { MutableLiveData<FeedbackEvent>() }

    /**
     * List of pages vs if their "continue" button is activated
     */
    private val continueButtonEnabledList = Array(5){
        when(it){
            PAGE_EMAIL, PAGE_CLOUD, PAGE_CALENDAR -> false
            else -> true
        }
    }

    /**
     * Mutable Livedata (private)
     */

    //Use Cloud, used on PAGE_CLOUD
    private val _useCloudCheckboxStatus = MutableLiveData(Preferences.useCloud)

    //Use IATA airports instead of ICAO, used on PAGE_FINAL
    private val _useIataAirports = MutableLiveData(Preferences.useIataAirports)

    private val _getFlightsFromCalendar = MutableLiveData(Preferences.useCalendarSync)



    /**
     * Coroutine Jobs:
     */
    // Job that holds [waitAndCheckMatchingEmails]
    private var checkEmailMatchingJob: Job? = null
        set(it){
            field?.cancel(null)
            field = it
        }

    /**
     * Listen to changes in [Preferences]
     */
    private val onSharedPrefsChangedListener =  SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        // Log.d("AirportRepository", "key = $key")
        when(key) {
            Preferences::useCloud.name -> _useCloudCheckboxStatus.value = Preferences.useCloud
            Preferences::useIataAirports.name -> _useIataAirports.value = Preferences.useIataAirports
            Preferences::useCalendarSync.name -> _getFlightsFromCalendar.value = Preferences.useCalendarSync
        }
    }
    init{
        Preferences.getSharedPreferences().registerOnSharedPreferenceChangeListener (onSharedPrefsChangedListener)
    }

    /*******************************************************************************************
     * Public parts
     *******************************************************************************************/

    /*******************************************************************************************
     * Observables
     *******************************************************************************************/

    //used on PAGE_CLOUD
    val useCloudCheckboxStatus: LiveData<Boolean>
        get() = _useCloudCheckboxStatus

    // used on PAGE_CALENDAR
    val getFlightsFromCalendar: LiveData<Boolean> get() = _getFlightsFromCalendar

    //used on PAGE_FINAL
    val useIataAirports: LiveData<Boolean>
        get() = _useIataAirports


    /*******************************************************************************************
     * NewUserActivity functions / variables
     *******************************************************************************************/
    /**
     * Keeps track of which page is open in case of activity recreations
     */
    var lastOpenPageState: Int? = null

    /*******************************************************************************************
     * Shared functions
     *******************************************************************************************/

    /**
     * Tells Activity to make "Continue" button active
     */
    fun setNextButtonEnabled(pageNumber: Int, isActive: Boolean){
        continueButtonEnabledList[pageNumber] = isActive
        feedback(NewUserActivityEvents.UPDATE_NAVBAR).putBoolean(isActive)
    }

    /**
     * @return true if continue button should be enabled for page [position]
     */
    fun isContinueButtonEnabled(position: Int) = continueButtonEnabledList[position]

    /**
     * @return [R.string.skip] if skip button should be enabled for page [position], empty string (which will remove button) if not.
     */
    fun skipButtonText(position: Int) = if (continueButtonEnabledList[position]) "" else getString(R.string.skip)

    fun continueClicked(position: Int) {
        when(position){
            PAGE_INTRO -> feedback(NewUserActivityEvents.NEXT_PAGE)
            PAGE_CLOUD -> feedback(NewUserActivityEvents.NEXT_PAGE)
            PAGE_EMAIL -> {
                emailPageContinueClicked()
                feedback(NewUserActivityEvents.NEXT_PAGE)
            }
            PAGE_CALENDAR -> feedback(NewUserActivityEvents.NEXT_PAGE)
            PAGE_FINAL -> {
                finalPageDoneClicked()
                feedback(NewUserActivityEvents.FINISHED)
            }
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
     * [PAGE_CLOUD] functions
     *******************************************************************************************/

    /**
     * When useCloudCheckbox is clicked:
     * if terms are already accepted, toggle [Preferences.useCloud]
     * Else, [NewUserActivityEvents.SHOW_TERMS_DIALOG] is sent to NewUserActivityCloudPage
     *  This will show a CloudSyncTermsDialog, which will set [Preferences.useCloud] to true
     */
    fun useCloudCheckboxClicked(){
        if (Preferences.acceptedCloudSyncTerms)
            Preferences.useCloud = !Preferences.useCloud
        else feedback(NewUserActivityEvents.SHOW_TERMS_DIALOG, PAGE_CLOUD)
    }

    /*******************************************************************************************
     * [PAGE_EMAIL] functions / variables
     *******************************************************************************************/

    var email1 = Preferences.emailAddress.also{
        if (it.isNotEmpty()) continueButtonEnabledList[PAGE_EMAIL] = true // if email already set, user can click 'continue' instead of skip.
    }
        private set
    var email2 = Preferences.emailAddress
        private set

    /**
     * Update [email1] and/or [email2]
     */
    fun emailInputChanged(input1: String, input2: String) {
        email1 = input1.lowercase(Locale.ROOT).trim()
        email2 = input2.lowercase(Locale.ROOT).trim()
        checkEmails()
    }

    fun checkEmail2Delayed(){
        checkEmailMatchingJob = viewModelScope.launch {
            waitAndCheckMatchingEmails()
        }
    }

    /**
     * Wait [DELAY_CHECK_EMAIL2] millis and run [checkEmail2]
     */
    private suspend fun waitAndCheckMatchingEmails(){
        println("waitAndCheckMatchingEmails: STARTING")
        delay(DELAY_CHECK_EMAIL2) // delay checks for cancel
        checkEmail2()
        println("waitAndCheckMatchingEmails: DONE")
    }


    /**
     * Check if email1 is a correct email format
     */
    fun checkEmail1(){
        if (email1.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email1).matches())
            feedback(NewUserActivityEvents.BAD_EMAIL, PAGE_EMAIL)
    }

    /**
     * Check if email1 is a correct email format
     */
    // Can be made public if needed
    private fun checkEmail2(){
        if (email1 != email2 && email2.isNotEmpty())
            feedback(NewUserActivityEvents.EMAILS_DO_NOT_MATCH, PAGE_EMAIL)
    }


    /**
     * Check if emails are valid and matching, if so, enable continue button
     */
    private fun checkEmails(): Boolean =
        (email1 == email2 && android.util.Patterns.EMAIL_ADDRESS.matcher(email2).matches()).also{
            if (continueButtonEnabledList[PAGE_EMAIL] != it)
                setNextButtonEnabled(PAGE_EMAIL, it)
    }

    /**
     * When continue is clicked on Email page, email address is saved through UserManagement.
     * This function is called when clicking on continue button. Continue button must only be visible if email is entered (see [checkEmails])
     * If Cloud is used, email address is confirmed with server, if not it is stored for later usage
     */
    private fun emailPageContinueClicked() {
        Preferences.emailAddress = email1
        if (Preferences.useCloud)
            viewModelScope.launch{
                UserManagement.changeEmailAddress()
            }
    }

    /*******************************************************************************************
     * [PAGE_CALENDAR] functions
     *******************************************************************************************/

    /**
     * If [Preferences.useCalendarSync] is true, (switch is on) set it to off
     * else, show dialog (which will switch it on on success)
     */
    fun setGetFlightsFromCalendarClicked() {
        if (!Preferences.useCalendarSync) // if it is switched on from being off
            feedback(SettingsActivityEvents.CALENDAR_DIALOG_NEEDED, PAGE_CALENDAR)
        else
            Preferences.useCalendarSync = false
    }


    /*******************************************************************************************
     * [PAGE_FINAL] functions
     *******************************************************************************************/

    fun setUseIataAirports(useIata: Boolean) {
        Preferences.useIataAirports = useIata
    }

    fun setConsensusOptIn(optIn: Boolean){
        Preferences.consensusOptIn = optIn
    }

    /**
     * Save all stuff that needs saving, and set up everything for first use
     */
    private fun finalPageDoneClicked(){
        Preferences.lastUpdateTime=0                                    // force update upon loading MainActivity if cloud is in use
        Preferences.newUserActivityFinished = true
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

    companion object{
        const val PAGE_INTRO = 0
        const val PAGE_CLOUD = 1
        const val PAGE_EMAIL = 2
        const val PAGE_CALENDAR = 3
        const val PAGE_FINAL = 4

        const val NUMBER_OF_PAGES = 5

        private const val FEEDBACK_CHANNELS = 10

        // PAGE_EMAIL constants
        private const val DELAY_CHECK_EMAIL2 = 1500L // ms

    }
}