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

package nl.joozd.logbookapp.model.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.data.repository.*
import nl.joozd.logbookapp.data.repository.aircraftrepository.AircraftRepository
import nl.joozd.logbookapp.data.repository.airportrepository.AirportRepository
import nl.joozd.logbookapp.data.repository.flightRepository.FlightRepositoryImpl
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvent
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents

open class JoozdlogViewModel: ViewModel() {
    protected val flightRepository = FlightRepositoryImpl.getInstance()
    protected val aircraftRepository = AircraftRepository.getInstance()
    protected val airportRepository = AirportRepository.getInstance()
    protected val balanceForwardRepository = BalanceForwardRepository.getInstance()

    protected val applicationScope
        get() = App.instance.applicationScope

    private val _feedbackEvent = MutableLiveData<FeedbackEvent>()
    val feedbackEvent: LiveData<FeedbackEvent>
        get() = _feedbackEvent

    /**
     * App context. Can be used in viewModels for application-wide context, do not use for anything UI related.
     */
    protected val context: Context
        get() = App.instance.ctx

    /**
     * Gives feedback to activity.
     * @param event: type of event
     * @param feedbackEvent: livedata to send feedback to
     * @return: The event that si being fed back
     * The [FeedbackEvent] that is being returned can be edited (ie. extraData can be filled)
     * with an [apply] statement. This is faster than the filling of the livedata so it works.
     */
    protected fun feedback(event: FeedbackEvents.Event): FeedbackEvent =
        FeedbackEvent(event).also{
            Log.d("Feedback", "event: $event, feedbackEvent: $feedbackEvent")
            viewModelScope.launch(Dispatchers.Main) {_feedbackEvent.value = it }
        }

    protected fun feedback(event: FeedbackEvents.Event, feedbackEvent: MutableLiveData<FeedbackEvent>): FeedbackEvent =
        FeedbackEvent(event).also{
            Log.d("Feedback2", "event: $event, feedbackEvent: $feedbackEvent")
            viewModelScope.launch(Dispatchers.Main) { feedbackEvent.value = it }
        }

    protected fun getString(resID: Int) = context.getString(resID)
}