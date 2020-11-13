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

package nl.joozd.logbookapp.model.viewmodels.dialogs

import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.GeneralEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel

class CloudBackupDialogViewModel: JoozdlogDialogViewModel() {
    var emailAddress = Preferences.emailAddress

    fun okClicked(){
        Preferences.emailAddress = emailAddress
        Preferences.backupFromCloud = true
        feedback(GeneralEvents.DONE)
    }

    fun updateEmail(it: String){
        when {
            !android.util.Patterns.EMAIL_ADDRESS.matcher(it.trim()).matches() ->
                feedback(GeneralEvents.ERROR).putString("Not an email address") // TODO make other errors as well
            else -> {
                emailAddress = it.trim()
            }
        }
    }
}