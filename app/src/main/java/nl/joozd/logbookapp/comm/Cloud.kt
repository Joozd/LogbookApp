package nl.joozd.logbookapp.comm

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nl.joozd.comms.Client
import nl.joozd.joozdlogcommon.*
import nl.joozd.joozdlogcommon.comms.Protocol
import nl.joozd.joozdlogcommon.comms.JoozdlogCommsKeywords
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.core.emailFunctions.UsernameWithKey
import nl.joozd.logbookapp.exceptions.CloudException
import nl.joozd.serializing.*

/**
 * Cloud does the communication with the server.
 * Server refusals (bad login data, wrong email address, etc) in communication wil be handled here,
 * rescheduling of events or resetting of flags must be done in the calling function.
 * Functions can return either
 *  - a [CloudFunctionResult] for server commands like "request confirmation email"
 *  - the requested data, or null if failed for any reason.
 */
class Cloud(
    private val server: String = Protocol.SERVER_URL,
    private val port: Int = Protocol.SERVER_PORT
) {
    private val basicFlightVersion get() = BasicFlight.VERSION.version
    /**
     * @return true if data accepted, false if username and/or key are rejected by server.
     */
    suspend fun createNewUser(username: String, key: ByteArray): CloudFunctionResult {
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
        return resultForRequest(JoozdlogCommsKeywords.SET_EMAIL, data)
    }

    /**
     * Change password. Will return true if success, false if failed due to no connection or server refused.
     * Server refusal will be handled by [handleResponse].
     *
     */
    suspend fun changeLoginKey(username: String, currentKey: ByteArray, newKey: ByteArray): CloudFunctionResult =
        withClient {
            require(newKey.size == Protocol.KEY_SIZE) // maybe handle this a bit more gracefully? Should not happen anyway.
            val r = login(UsernameWithKey(username, currentKey))
            if (r != CloudFunctionResult.OK) return r
            return changePasswordOnServer(newKey, ServerPrefs.emailAddress())
        }

    /**
     * Send email confirmation string (from confirmation email) to server for checking.
     */
    suspend fun confirmEmail(confirmationString: String): CloudFunctionResult =
        resultForRequest(JoozdlogCommsKeywords.CONFIRM_EMAIL, wrap(confirmationString))

    /**
     * Request an email with a login link from the server
     */
    suspend fun requestLoginLinkMail(username: String, key: ByteArray, emailAddress: String): CloudFunctionResult =
        resultForRequest(JoozdlogCommsKeywords.REQUEST_LOGIN_LINK_MAIL, makeLoginDataWithEmailPayload(username, key, emailAddress))

    suspend fun sendBackupMailThroughServer(backupEmailData: BackupEmailData){
        resultForRequestOrException(JoozdlogCommsKeywords.SENDING_BACKUP_EMAIL_DATA, backupEmailData.serialize())
    }

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

    suspend fun sendFeedback(feedbackData: FeedbackData): CloudFunctionResult = withClient {
        sendRequest(JoozdlogCommsKeywords.SENDING_FEEDBACK, feedbackData.serialize())
        handleResponse()!!
    }

    /*
     * Log in to server until Client is closed again
     * This will handle responses from server like no username / bad key / bad data etc.
     */
    suspend fun Client.login(usernameWithKey: UsernameWithKey): CloudFunctionResult {
        //payLoad is LoginData.serialize()
        val payLoad = LoginData(usernameWithKey.username, usernameWithKey.key, BasicFlight.VERSION.version)
            .serialize()

        sendRequest(JoozdlogCommsKeywords.LOGIN, payLoad)
        return handleResponse()!!
    }

    suspend fun sendP2PData(data: ByteArray): Long {
        println("SendP2PData (${data.size})")
        return unwrapLong(resultForRequestOrException(JoozdlogCommsKeywords.SENDING_P2P_DATA, data))
    }

    suspend fun receiveP2PData(sessionID: Long): ByteArray {
        println("receiveP2PData")
        val payload = wrap(sessionID)
        println("wrapped session ID")
        return resultForRequestOrException(JoozdlogCommsKeywords.REQUEST_P2P_DATA, payload).also{
            println("receiveP2PData returns ${it.size} bytes")
        }
    }


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

    // all client usage should use this function so locking is properly taken care of.
    private suspend inline fun <T> withClient(block: Client.() -> T): T = clientMutex.withLock {
        client().use {
            block(it)
        }
    }

    private suspend fun resultForRequest(request: String, extraData: ByteArray?): CloudFunctionResult = withClient {
        sendRequest(request, extraData)
        handleResponse()!!
    }

    private suspend fun resultForRequestOrException(request: String, extraData: ByteArray?, progressListener: ProgressListener? = null): ByteArray = withClient {
        sendRequest(request, extraData)
        val response = if (progressListener != null) readFromServer(progressListener::progress) else readFromServer()
        handleServerResultAndThrowExceptionOnError(response)
        return response!! // null gets caught by handleServerResultAndThrowExceptionOnError()
    }


    private suspend fun Client.changePasswordOnServer(newPassword: ByteArray, email: String): CloudFunctionResult {
        val payload = LoginDataWithEmail("", newPassword, 0, email).serialize() // username and basicFlightVersion are unused in this function
        sendRequest(JoozdlogCommsKeywords.UPDATE_PASSWORD, payload)
        return handleResponse()!!
    }

    private suspend fun Client.handleResponse() = handleServerResult(readServerResponse())

    private suspend fun Client.handleResponseAndThrowExceptionOnError() = handleServerResultAndThrowExceptionOnError(readServerResponse())


    fun interface ProgressListener{
        fun progress(progress: Int)
    }

    // Pass this to functions giving a result to get returned data.
    // The return of the function will only be a CloudFunctionResult, so if you want anything else you can drop it in here.
    data class Result<T>(var value: T? = null)


    companion object {
        private val clientMutex = Mutex() // Only one client connection at a time. More can cause problems when changing password or stuff like that.
    }


}