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

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.core.App
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.data.sharedPrefs.TaskPayloads
import nl.joozd.logbookapp.utils.CastFlowToMutableFlowShortcut
import nl.joozd.logbookapp.utils.DispatcherProvider

class FeedbackActivityViewModel: JoozdlogActivityViewModel() {
    var enteredFeedback: String? = null
    var enteredContactInfo: String? = null

    val finishedFlow: Flow<Boolean> = MutableStateFlow(false)
    private var finished by CastFlowToMutableFlowShortcut(finishedFlow)

    val knownIssuesFlow: Flow<String> = MutableStateFlow("")
    private var knownIssues by CastFlowToMutableFlowShortcut(knownIssuesFlow)


    fun loadKnownIssuesLiveData(source: Int) = viewModelScope.launch {
        knownIssues = App.instance.resources.openRawResource(source).use{
            withContext(DispatcherProvider.io()) { it.reader().readText() }
        }
    }

    fun submitClicked() = viewModelScope.launch{
        enteredFeedback?.let { TaskPayloads.feedbackWaiting.setValue(it) } // suspending, data is needed for proper feedback sending
        enteredContactInfo?.let { TaskPayloads.feedbackContactInfo.setValue(it) } // suspending, data is needed for proper feedback sending
        TaskFlags.feedbackWaiting(true)
        finished = true
    }
}