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

@file:OptIn(DelicateCoroutinesApi::class)

package nl.joozd.logbookapp.model.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.core.App
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvent
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents

open class JoozdlogViewModel: ViewModel(){
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
    protected fun feedback(event: FeedbackEvents.Event, feedbackEvent: MutableLiveData<FeedbackEvent> = _feedbackEvent): FeedbackEvent =
        FeedbackEvent(event).also{
            Log.d("Feedback", "event: $event, feedbackEvent: $feedbackEvent")
            viewModelScope.launch(Dispatchers.Main) {_feedbackEvent.value = it }
        }

    protected fun getString(resID: Int) = context.getString(resID)
    protected fun getString(resID: Int, vararg formatArgs: Any) = context.getString(resID, formatArgs)

    /**
     * Launch a job that needs to complete no matter what.
     * NOTE: Do not use this for permanent running tasks as it will keep running until device is rebooted or app is killed.
     */
    protected fun launchNonCancelable(block: suspend CoroutineScope.() -> Unit){
        viewModelScope.launch(NonCancellable) {
            block()
        }
    }
}