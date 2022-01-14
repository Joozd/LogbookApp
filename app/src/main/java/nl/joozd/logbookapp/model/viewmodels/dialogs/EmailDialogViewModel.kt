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

package nl.joozd.logbookapp.model.viewmodels.dialogs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.comm.UserManagement
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.GeneralEvents
import nl.joozd.logbookapp.model.viewmodels.JoozdlogDialogViewModel
import java.util.*

class EmailDialogViewModel: JoozdlogDialogViewModel() {
    var email1 = Preferences.emailAddress.lowercase(Locale.ROOT)
        private set
    var email2 = Preferences.emailAddress.lowercase(Locale.ROOT)
        private set

    //intentionally public
    var onComplete: () -> Unit = {}

    /**
     * Text to be shown as "OK" button in email dialog
     * Will witch to "VERIFY" if clicking it will lead to a verification mail being sent
     * Initially set to OK if email verified or empty, or VERIFY otherwise
     */
    private val _okButtonText = MutableLiveData(if (Preferences.emailAddress.isBlank() || Preferences.emailVerified) android.R.string.ok else R.string.verify)
    val okButtonText: LiveData<Int>
        get() = _okButtonText

    /**
     * When OK clicked:
     * - This checks for correct data (should always be the case but never hurts to doublecheck)
     * - If email has been changed, will set Preferences.emailVerified to false
     * - If Preferences.emailVerified was false or has just been set to that, call UserManagement.changeEmailAddress()
     * - feeds back DONE to fragment if email changed, OK if nothing happened, so it will close itself
     */
    fun okClicked(){
        if (email1 != email2) feedback(GeneralEvents.ERROR).putInt(3)
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email1).matches() && email1.isNotBlank()) feedback(GeneralEvents.ERROR).putInt(4)

        if (Preferences.emailAddress.lowercase(Locale.ROOT) != email1)
            Preferences.emailVerified = false

        if (!Preferences.emailVerified) {
            if (email1 != Preferences.emailAddress.lowercase(Locale.ROOT))
                Preferences.emailAddress = email1

            if (email1.isNotBlank()) {
                viewModelScope.launch {
                    UserManagement.changeEmailAddress()
                    // Fire and forget, UserManagement takes care of any errors that arise
                }
            }
            feedback(GeneralEvents.DONE)
        }
        else feedback(GeneralEvents.OK)



    }

    fun updateEmail(it: String){
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(it.trim()).matches())
                feedback(GeneralEvents.ERROR).putString("Not an email address").putInt(1)
                email1 = it.lowercase(Locale.ROOT).trim()
    }

    fun updateEmail2(it: String){
        when {
            it.lowercase(Locale.ROOT).trim() != email1 ->
                feedback(GeneralEvents.ERROR).putString("Does not match").putInt(2)
            else -> {
                email2 = it.lowercase(Locale.ROOT).trim()

            }
        }
    }

    /**
     * Checks if current text is the same as email2 and they both are an email address or empty
     * Will also update text for okButton
     */
    fun checkSame1(s: String): Boolean =
        (s.trim() == email2 && (s.isBlank() || android.util.Patterns.EMAIL_ADDRESS.matcher(email2).matches())).also{
            updateOKButtonText(s.trim())
        }

    /**
     * Checks if current text is the same as email1 and they both are an email address
     */
    fun checkSame2(s: String): Boolean =
        (s.trim() == email1 && (s.isBlank() || android.util.Patterns.EMAIL_ADDRESS.matcher(email1).matches()))

    /**
     * True if ok button should be enabled, false if not
     * Basically just checks if email1 is an email address and email1 and 2 match
     */
    fun okButtonShouldBeEnabled(): Boolean = android.util.Patterns.EMAIL_ADDRESS.matcher(email1).matches() && email1 == email2

    /**
     * Checks what text OK button should have (OK or Verify) and sets that
     * It should say OK if
     *      - Entered email is same as saved in Preferences OR empty
     *      - Preferences.emailVerified OR email is empty
     * It should say VERIFY if
     *      - entered email is not the same as saved in Preferences
     *      OR !Preferences.emailVerifies
     * In other cases it should stay the way it was
     * @param forceEmail1: Force email1, ued when [email1] isn't updated yet (during typing)
     */
    fun updateOKButtonText(forceEmail1: String? = null){
        val e1 = forceEmail1 ?: email1
        when{
            e1.isBlank() || Preferences.emailVerified && e1 == Preferences.emailAddress.lowercase(Locale.ROOT) -> _okButtonText.value = android.R.string.ok
            e1 != Preferences.emailAddress || !Preferences.emailVerified -> _okButtonText.value = R.string.verify
        }
    }
}