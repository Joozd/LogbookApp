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

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.comm.Cloud
import nl.joozd.logbookapp.data.comm.InternetStatus
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.GeneralEvents

class FeedbackActivityViewModel: JoozdlogActivityViewModel() {
    /**
     * Text entered in feedback text field
     */
    private var _feedbackText = ""
    // + exposed getter
    val feedbackText
        get() = _feedbackText

    /**
     * Text entered in contact field
     */
    private var _contactInfo = Preferences.emailAddress
    // + exposed getter
    val contactInfo
        get() = _contactInfo

    fun updateFeedbackText(it: String){ _feedbackText = it }
    fun updateContactText(it: String){ _contactInfo = it }

    // TODO make this be handled by a worker
    fun submitClicked(){
        when {
            InternetStatus.internetAvailable != true -> feedback(GeneralEvents.ERROR).putInt(NO_INTERNET)
            _feedbackText.isBlank() -> feedback(GeneralEvents.ERROR).putInt(EMPTY_FEEDBACK) // don't feel like making a while FeedbackEvents class
            else -> viewModelScope.launch{
                if (Cloud.sendFeedback(_feedbackText, _contactInfo)) {
                    Preferences.feedbackWaiting = ""
                    feedback(GeneralEvents.DONE)
                }
                else {
                    Preferences.feedbackWaiting = _feedbackText
                    feedback(GeneralEvents.ERROR).putInt(CONNECTION_ERROR)
                }
            }
        }
    }

    companion object{
        const val EMPTY_FEEDBACK = 1
        const val NO_INTERNET = 2
        const val CONNECTION_ERROR = 3
    }
}