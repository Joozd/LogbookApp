package nl.joozd.logbookapp.data.comm

import android.util.Base64
import android.util.Log
import nl.joozd.comms.Client
import nl.joozd.comms.isOK
import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.joozdlogcommon.LoginData
import nl.joozd.joozdlogcommon.LoginDataWithEmail
import nl.joozd.joozdlogcommon.comms.JoozdlogCommsKeywords
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.data.sharedPrefs.EmailPrefs
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.extensions.toCloudFunctionResults

class Cloud(private val server: String = SERVER_URL, private val port: Int = SERVER_PORT) {
    /**
     * @return true if data accepted, false if username and/or key are rejected by server.
     */
    suspend fun createNewUser(username: String, key: ByteArray): Boolean{
        if (username.isEmpty() || key.isEmpty()) return false
        val payLoad = LoginData(username, key, BasicFlight.VERSION.version).serialize()
        return withClient {
            sendRequest(JoozdlogCommsKeywords.NEW_ACCOUNT, payLoad)
            handleResponse()
        } != CloudFunctionResult.CONNECTION_ERROR // so true on both OK and server refusal. Only reason for refusal is USER_ALREADY_EXISTS, which is handled by handleResponse()
    }

    /**
     * Ask server to make us an unused username
     * @return generated username, or null if connection failed.
     */
    suspend fun requestUsername(): String? =
        withClient {
            sendRequest(JoozdlogCommsKeywords.REQUEST_NEW_USERNAME)
            return readServerResponse()
        }

    /**
     * Send new email address to server
     * Server will send a confirmation mail if it worked.
     * If no success, sets [TaskFlags.updateEmailWithServer] to true
     */
    suspend fun sendNewEmailAddress(username: String, key: ByteArray, emailToSend: String) {
        val data =
            LoginDataWithEmail(username, key, BasicFlight.VERSION.version, emailToSend).serialize()
        withClient {
            if (sendRequest(JoozdlogCommsKeywords.SET_EMAIL, data).isOK()) {

                // this will handle any errors so we can reschedule if not OK
                if (handleResponse() == CloudFunctionResult.OK)
                    return
            }

            // this happens when setting email failed for whatever reason.
            TaskFlags.pushUpdateEmailWithServer(true)
        }
    }

    /**
     * check username and password with server
     * @param username: username to check
     * @param password: password to check. This expects an already hashed password.
     * @return
     *  - true if server accepted,
     *  - false if server rejected,
     *  - null if no connection made or client or server error.
     */
    suspend fun checkLoginDataWithServer(username: String, password: String): Boolean? {
        val payload = LoginData(username, Base64.decode(password, Base64.DEFAULT) ,BasicFlight.VERSION.version).serialize()
        withClient {
            if(!sendRequest(JoozdlogCommsKeywords.LOGIN, payload).isOK())
                return null
            return when(readServerResponse()){
                JoozdlogCommsKeywords.OK -> true
                JoozdlogCommsKeywords.UNKNOWN_USER_OR_PASS -> false
                else -> null
            }
        }
    }





    private suspend fun client() = Client.getInstance(server, port)

    private suspend fun Client.readServerResponse() =
        readFromServer()?.toString(Charsets.UTF_8)

    private fun generateLoginDataWithEmail(username: String? = null, key: ByteArray? = null, email: String? = null): LoginDataWithEmail? {
        return LoginDataWithEmail(username ?: Prefs.username ?: return null,
            key ?: Prefs.key ?: return null,
            BasicFlight.VERSION.version, email ?: EmailPrefs.emailAddress)
    }

    private suspend inline fun <T> withClient(block: Client.() -> T): T =
        client().use {
            block(it)
        }



    private suspend fun Client.handleResponse() = handleServerResult(readServerResponse())


    companion object {
        private const val SERVER_URL = "joozd.nl"
        private const val SERVER_PORT = 1337

    }


}