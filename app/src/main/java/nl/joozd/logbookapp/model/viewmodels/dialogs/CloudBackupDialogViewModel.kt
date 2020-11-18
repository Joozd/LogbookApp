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
    var email1 = Preferences.emailAddress
    var email2 = Preferences.emailAddress

    fun okClicked(){
        if (email1 != email2) feedback(GeneralEvents.ERROR).putInt(3)
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email1).matches()) feedback(GeneralEvents.ERROR).putInt(4)
        Preferences.emailAddress = email1
        Preferences.backupFromCloud = true
        feedback(GeneralEvents.DONE)
    }

    fun updateEmail(it: String){
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(it.trim()).matches())
                feedback(GeneralEvents.ERROR).putString("Not an email address").putInt(1) // TODO make other errors as well
                email1 = it.trim()
    }

    fun updateEmail2(it: String){
        when {
            it.trim() != email1 ->
                feedback(GeneralEvents.ERROR).putString("Does not match").putInt(2) // TODO make other errors as well
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

}