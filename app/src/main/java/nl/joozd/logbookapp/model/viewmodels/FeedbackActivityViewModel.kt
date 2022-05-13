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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.core.App
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.GeneralEvents
import nl.joozd.logbookapp.core.JoozdlogWorkersHubOld

class FeedbackActivityViewModel: JoozdlogActivityViewModel() {
    /**
     * Text entered in feedback text field
     */
    private var _feedbackText = Prefs.feedbackWaiting
    // + exposed getter
    val feedbackText
        get() = _feedbackText

    /**
     * Text entered in contact field
     */
    private var _contactInfo = Prefs.emailAddress
    // + exposed getter
    val contactInfo
        get() = _contactInfo

    private val _knownIssuesLiveData = MutableLiveData("")
    val knownIssuesLiveData: LiveData<String>
        get() = _knownIssuesLiveData
    fun loadKnownIssuesLiveData(source: Int) = viewModelScope.launch(Dispatchers.IO) {
        _knownIssuesLiveData.postValue(App.instance.resources.openRawResource(source).use{
            it.reader().readText()
        })
    }

    fun updateFeedbackText(it: String){ _feedbackText = it }
    fun updateContactText(it: String){ _contactInfo = it }

    /**
     * When "submit" is clicked, save [_feedbackText] to [Prefs] and start a worker with [_contactInfo]
     */
    fun submitClicked(){
        Prefs.feedbackWaiting = _feedbackText
        JoozdlogWorkersHubOld.sendFeedback(_contactInfo)
        feedback(GeneralEvents.DONE)
    }

    companion object{
        const val EMPTY_FEEDBACK = 1
        const val NO_INTERNET = 2
        const val CONNECTION_ERROR = 3
    }
}