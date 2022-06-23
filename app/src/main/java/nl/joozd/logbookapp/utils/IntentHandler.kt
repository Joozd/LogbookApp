package nl.joozd.logbookapp.utils

import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.withContext
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.messages.MessageCenter
import nl.joozd.logbookapp.core.usermanagement.UserManagement
import nl.joozd.logbookapp.data.sharedPrefs.Prefs

class IntentHandler(intent: Intent) {
    private val uri: Uri? = intent.data

    /**
     * Returns true if IntentHandler could handle this, of false if not
     */
    suspend fun handle(): Boolean = when(IntentType.ofUri(uri)){
        IntentType.INVALID -> false
        IntentType.LOGIN_LINK -> {
            handleLoginLink()
            true
        }
        IntentType.EMAIL_VERIFICATION_LINK -> {
            handleEmailCodeConfirmation()
            true
        }

    }

    // Handling a login link means just saving the stored data.
    // Any problems with the data (i.e. bad login/pass combo) will arise when anything is done with it
    // so any problems will be known to user when he needs to know.
    private suspend fun handleLoginLink() {
        getLoginLinkFromIntent()?.let {
            showLoginDataSavedMessage()
            storeNewLoginData(makeLoginPassPair(it))
        }
    }

    private suspend fun handleEmailCodeConfirmation(){
        getEmailConfirmationCodeFromIntent()?.let{
            showEmailConfirmationSheduledMessage()
            UserManagement().confirmEmail(it)
        }
    }

    private fun getLoginLinkFromIntent() =
        uri?.lastPathSegment

    private fun getEmailConfirmationCodeFromIntent() =
        uri?.lastPathSegment

    private fun makeLoginPassPair(loginPassString: String): Pair<String, String> =
        loginPassString.replace('-', '/').split(":").let { lp ->
            lp.first() to lp.last()
        }


    private suspend fun storeNewLoginData(lpPair: Pair<String, String>)= withContext(DispatcherProvider.io()) {
        UserManagement().storeNewLoginData(lpPair.first, lpPair.second)
        Prefs.useCloud(true)
        Prefs.acceptedCloudSyncTerms(true) // I guess they must have accepted it sometime in the past or they wouldn't have login data
        UserManagement().requestEmailVerificationMail()
    }



    private fun showLoginDataSavedMessage(){
        MessageCenter.commitMessage {
            titleResource = R.string.logged_in
            descriptionResource = R.string.no_internet_login
            setPositiveButton(android.R.string.ok){ }
        }
    }

    private fun showEmailConfirmationSheduledMessage(){
        MessageCenter.commitMessage {
            titleResource = R.string.email_verification_scheduled_title
            descriptionResource = R.string.email_verification_scheduled_message
            setPositiveButton(android.R.string.ok){ }
        }
    }

    private enum class IntentType {
        LOGIN_LINK,
        EMAIL_VERIFICATION_LINK,
        INVALID;

        companion object {
            fun ofUri(uri: Uri?): IntentType = when (uri?.pathSegments?.firstOrNull()) {
                INTENT_LOGIN_LINK_PATH -> LOGIN_LINK
                INTENT_VERIFY_EMAIL_PATH -> EMAIL_VERIFICATION_LINK
                else -> INVALID
            }
        }
    }


    companion object{
        private const val INTENT_VERIFY_EMAIL_PATH = "verify-email"
        private const val INTENT_LOGIN_LINK_PATH ="inject-key"
    }
}