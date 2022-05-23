package nl.joozd.logbookapp.utils

import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.messages.MessageCenter
import nl.joozd.logbookapp.core.usermanagement.UserManagement
import nl.joozd.logbookapp.data.sharedPrefs.Prefs

class IntentHandler(private val intent: Intent) {
    private val uri: Uri? = intent.data

    // Handling a login link means just saving the stored data.
    // Any problems with the data (i.e. bad login/pass combo) will arise when anything is done with it
    // so any problems will be known to user when he needs to know.
    suspend fun handleLoginLink() {
        getLoginLinkFromIntent()?.let {
            showLoginDataSavedMessage()
            storeNewLoginData(makeLoginPassPair(it))
        }
    }

    private fun getLoginLinkFromIntent() =
        uri?.lastPathSegment

    private fun makeLoginPassPair(loginPassString: String): Pair<String, String> =
        loginPassString.replace('-', '/').split(":").let { lp ->
            lp.first() to lp.last()
        }

    private suspend fun storeNewLoginData(lpPair: Pair<String, String>)= withContext(DispatcherProvider.io()) {
        Prefs.username = lpPair.first
        Prefs.keyString = lpPair.second
        Prefs.lastUpdateTime = -1
        Prefs.useCloud = true
        UserManagement().requestEmailVerificationMail()
    }

    private fun showLoginDataSavedMessage(){
        MessageCenter.commitMessage {
            titleResource = R.string.logged_in
            descriptionResource = R.string.no_internet_login
            setPositiveButton(android.R.string.ok){ }
        }
    }
}