package nl.joozd.logbookapp.comm

import android.util.Base64
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nl.joozd.comms.Client
import nl.joozd.joozdlogcommon.BasicFlight
import nl.joozd.joozdlogcommon.LoginData
import nl.joozd.joozdlogcommon.LoginDataWithEmail
import nl.joozd.joozdlogcommon.Protocol
import nl.joozd.joozdlogcommon.comms.JoozdlogCommsKeywords
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.data.sharedPrefs.EmailPrefs
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.serializing.longFromBytes
import nl.joozd.serializing.unwrapInt
import nl.joozd.serializing.wrap

/**
 * Cloud does the communication with the server.
 * Server refusals (bad login data, wrong email address, etc) in communication wil be handled here,
 * rescheduling of events or resetting of flags must be done in the calling function.
 * Functions can return either
 *  - a [CloudFunctionResult] for server commands like "request confirmation email"
 *  - the requested data, or null if failed for any reason.
 */
class Cloud(private val server: String = Protocol.SERVER_URL, private val port: Int = Protocol.SERVER_PORT) {
    private val basicFlightVersion get() = BasicFlight.VERSION.version
    /**
     * @return true if data accepted, false if username and/or key are rejected by server.
     */
    suspend fun createNewUser(username: String, key: ByteArray): CloudFunctionResult{
        val payLoad = LoginData(username, key, BasicFlight.VERSION.version).serialize()
        return resultForRequest(JoozdlogCommsKeywords.NEW_ACCOUNT, payLoad)
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
    suspend fun sendNewEmailAddress(username: String, key: ByteArray, emailToSend: String): CloudFunctionResult {
        val data = LoginDataWithEmail(username, key, basicFlightVersion, emailToSend).serialize()
        return resultForRequest (JoozdlogCommsKeywords.SET_EMAIL, data)
    }

    /**
     * check username and password with server
     * @param username: username to check
     * @param password: password to check. This expects an already hashed password.
     * @return
     *  - CloudFunctionResult.OK if server accepted,
     *  - CloudFunctionResult.SERVER_REFUSED if server rejected,
     *  - CloudFunctionResult.CONNECTION_ERROR if no connection made or client or server error.
     *  This does nothing to handle bad login data, as it is only made to check if login data is OK or not.
     */
    suspend fun checkLoginDataWithServer(username: String, password: String): CloudFunctionResult {
        val payload = LoginData(username, Base64.decode(password, Base64.DEFAULT), basicFlightVersion).serialize()
        return withClient {
            sendRequest(JoozdlogCommsKeywords.LOGIN, payload)
            readServerResponse()?.let {
                if (it == JoozdlogCommsKeywords.OK) CloudFunctionResult.OK
                else CloudFunctionResult.SERVER_REFUSED
            } ?: CloudFunctionResult.CONNECTION_ERROR
        }
    }

    /**
     * Change password. Will return true if success, false if failed due to no connection or server refused.
     * Server refusal will be handled by [handleResponse].
     *
     */
    suspend fun changeLoginKey(username: String, currentKey: ByteArray, newKey: ByteArray): CloudFunctionResult =
        withClient {
            require(newKey.size == Protocol.KEY_SIZE) // maybe handle this a bit more gracefully? Should not happen anyway.
            val r = login(username, currentKey)
            if(r != CloudFunctionResult.OK) return r
            return changePasswordOnServer(newKey, EmailPrefs.emailAddress())
        }

    /**
     * Send email confirmation string (from confirmation email) to server for checking.
     */
    suspend fun confirmEmail(confirmationString: String): CloudFunctionResult =
        resultForRequest(JoozdlogCommsKeywords.CONFIRM_EMAIL, wrap(confirmationString))

    /**
     * Request an email with a login link from the server
     */
    //TODO pass login detaiuls. CLoud does not need to know where to find those.
    suspend fun requestLoginLinkMail(username: String, key: ByteArray, emailAddress: String): CloudFunctionResult =
        resultForRequest(JoozdlogCommsKeywords.REQUEST_LOGIN_LINK_MAIL, makeLoginDataWithEmailPayload(username, key, emailAddress))

    /**
     * This will send a mail with all flights currently in cloud.
     * Flights should be synced first just to be sure things are correct.
     */
    suspend fun requestBackupEmail(username: String, key: ByteArray, emailAddress: String): CloudFunctionResult =
        resultForRequest(JoozdlogCommsKeywords.REQUEST_BACKUP_MAIL, makeLoginDataWithEmailPayload(username, key, emailAddress))

    /**
     * sends a REQUEST TIMESTAMP to server
     * Expects server to reply with a single Long (8 Bytes)
     * @return the Timestamp from server as a Long (epochSeconds) or -1 if error
     */
    suspend fun getTime(): Long? =
        withClient {
            sendRequest(JoozdlogCommsKeywords.REQUEST_TIMESTAMP)
            readFromServer()?.let {
                longFromBytes(it)
            }
        }

    /*
      TODO: Make a sync protocol taht works better than this one:
       - NOTE this must be done in a separate Sync class/module/whatever. Not here. Here is only for communication.
       - Roll all timestamps into a hash, make class Class(amountOfFlights: Int, combinedTimes, Long): JoozdLogSerializable in JoozdLogCommon.
       - Compare this hash with the server's timestamp hash (requesy hash from server)
       - If not the same, request a list of all FlightID's with timestamps from server
       - Request all flights with a higher timestamp from server
       - Send a list with all flights with a more recent local timestamp + all IDs which are not yet on server
       -
       - Keep session alive and pass Client around, or login with every step? Last would be much more server load.
       -
       - Change server storage to keep a hash-only reference for speed improvement? This can be done later as well.
     */


    //Doesn't handle bad login data, but server will refuse empty usernames etc.
    //However, it is better to check this (e.g. with with UserManagement.checkIfLoginDataSet()) before making bad data.
    private fun makeLoginDataWithEmailPayload(username: String, key: ByteArray, emailAddress: String): ByteArray =
        LoginDataWithEmail(
            username,
            key,
            basicFlightVersion,
            emailAddress
        ).serialize()


    private suspend fun client() = Client.getInstance(server, port)

    private suspend fun Client.readServerResponse() =
        readFromServer()?.toString(Charsets.UTF_8)

    private fun generateLoginDataWithEmail(username: String? = null, key: ByteArray? = null, email: String? = null): LoginDataWithEmail? {
        return LoginDataWithEmail(username ?: Prefs.username ?: return null,
            key ?: Prefs.key ?: return null,
            BasicFlight.VERSION.version, email ?: EmailPrefs.emailAddress)
    }

    // all client usage should use this function so locking is properly taken care of.
    private suspend inline fun <T> withClient(block: Client.() -> T): T = clientMutex.withLock {
        client().use {
            block(it)
        }
    }

    private suspend fun resultForRequest(request: String, extraData: ByteArray? ): CloudFunctionResult = withClient {
        sendRequest(request, extraData)
        handleResponse()
    }

    /*
     * Log in to server until Client is closed again
     */
    private suspend fun Client.login(username: String, key: ByteArray): CloudFunctionResult {
        //payLoad is LoginData.serialize()
        val payLoad = LoginData(username, key , BasicFlight.VERSION.version)
            .serialize()

        sendRequest(JoozdlogCommsKeywords.LOGIN, payLoad)
        return handleResponse()
    }

    private suspend fun Client.changePasswordOnServer(newPassword: ByteArray, email: String): CloudFunctionResult {
        val payload = LoginDataWithEmail("", newPassword, 0, email).serialize() // username and basicFlightVersion are unused in this function
        sendRequest(JoozdlogCommsKeywords.UPDATE_PASSWORD, payload)
        return handleResponse()
    }

    private suspend fun Client.handleResponse() = handleServerResult(readServerResponse())


    companion object {
        private val clientMutex = Mutex() // Only one client connection at a time. More can cause problems when changing password or stuff like that.
    }


}