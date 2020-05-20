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

package nl.joozd.logbookapp.model.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.repository.*
import nl.joozd.logbookapp.model.helpers.FeedbackEvent
import nl.joozd.logbookapp.model.helpers.FeedbackEvents

open class JoozdlogViewModel: ViewModel() {
    protected val flightRepository = FlightRepository.getInstance()
    protected val aircraftRepository = AircraftRepository.getInstance()
    protected val airportRepository = AirportRepository.getInstance()
    protected val workingFlightRepository = WorkingFlightRepository.getInstance()

    private val _feedbackEvent = MutableLiveData<FeedbackEvent>()
    val feedbackEvent: LiveData<FeedbackEvent>
        get() = _feedbackEvent

    protected fun feedback(event: FeedbackEvents.Event): FeedbackEvent =
        FeedbackEvent(event).also{
            viewModelScope.launch(Dispatchers.Main) {_feedbackEvent.value = it }
        }
}