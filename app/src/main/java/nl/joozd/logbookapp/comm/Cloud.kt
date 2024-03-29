package nl.joozd.logbookapp.comm

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nl.joozd.comms.Client
import nl.joozd.joozdlogcommon.*
import nl.joozd.joozdlogcommon.comms.Protocol
import nl.joozd.joozdlogcommon.comms.JoozdlogCommsRequests
import nl.joozd.joozdlogcommon.comms.JoozdlogCommsResponses
import nl.joozd.logbookapp.core.MessageCenter
import nl.joozd.logbookapp.core.messages.MessageBarMessage
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
    /**
     * Send new email address to server
     * Server will send a confirmation mail if it worked.
     * Throws CloudException on failure.
     */
    suspend fun sendNewEmailAddress(emailAddress: String): Long {
        val data = EmailData(EmailData.EMAIL_ID_NOT_SET, emailAddress, ByteArray(0))
        val result = responseForRequestOrException(JoozdlogCommsRequests.SET_EMAIL, data)
        return unwrapLong(result)
    }

    /**
     * Migrate email data on server to new system (Long ID instead of String Username)
     * Throws CloudException on failure.
     */
    suspend fun migrateEmailData(username: String, emailAddress: String): Long {
        @Suppress("DEPRECATION") val data = LoginDataWithEmail(username, ByteArray(0), -1, emailAddress).serialize() // deprecated but here for migration
        val result = responseForRequestOrException(JoozdlogCommsRequests.MIGRATE_EMAIL_DATA, data)
        return unwrap(result)
    }


    /**
     * Send email confirmation string (from confirmation email) to server for checking.
     */
    suspend fun confirmEmail(confirmationString: String): CloudFunctionResult {
        val result = try {
            JoozdlogCommsResponses.from(responseForRequestOrException(JoozdlogCommsRequests.CONFIRM_EMAIL, wrap(confirmationString)))
        } catch(e: CloudException){
            return e.cloudFunctionResult
        }
        if(result == JoozdlogCommsResponses.ID_NOT_FOUND) {
            MessageCenter.scheduleMessage(MessageBarMessage.UNKNOWN_OR_UNVERIFIED_EMAIL)
            return CloudFunctionResult.SERVER_REFUSED
        }
        return CloudFunctionResult.OK
    }

    suspend fun sendBackupMailThroughServer(backupEmailData: EmailData){
        responseForRequestOrException(JoozdlogCommsRequests.SENDING_BACKUP_EMAIL_DATA, backupEmailData.serialize())
    }

    suspend fun sendFeedback(feedbackData: FeedbackData): CloudFunctionResult = withClient {
        sendRequest(JoozdlogCommsRequests.SENDING_FEEDBACK, feedbackData.serialize())
        handleResponse()!!
    }

    suspend fun sendP2PData(data: ByteArray): Long {
        return unwrapLong(responseForRequestOrException(JoozdlogCommsRequests.SENDING_P2P_DATA, data))
    }

    suspend fun receiveP2PData(sessionID: Long): ByteArray {
        val payload = wrap(sessionID)
        return responseForRequestOrException(JoozdlogCommsRequests.REQUEST_P2P_DATA, payload)
    }

    private suspend fun client() = Client.getInstance(server, port)

    private suspend fun Client.readServerResponse() =
        readFromServer()?.toString(Charsets.UTF_8)

    // all client usage should use this function so locking is properly taken care of.
    private suspend inline fun <T> withClient(block: Client.() -> T): T = clientMutex.withLock {
        client().use {
            block(it)
        }
    }

    private suspend fun responseForRequestOrException(request: JoozdlogCommsRequests, extraData: ByteArray?, progressListener: ProgressListener? = null): ByteArray = withClient {
        sendRequest(request, extraData)
        val response = if (progressListener != null) readFromServer(progressListener::progress) else readFromServer()
        handleServerResultAndThrowExceptionOnError(response)
        return response!! // null gets caught by handleServerResultAndThrowExceptionOnError()
    }

    private suspend fun responseForRequestOrException(
        request: JoozdlogCommsRequests,
        extraData: JoozdSerializable,
        progressListener: ProgressListener? = null
    ): ByteArray =
        responseForRequestOrException(request, extraData.serialize(), progressListener)


    private suspend fun Client.handleResponse() = handleServerResult(readServerResponse())

    private suspend fun Client.sendRequest(request: JoozdlogCommsRequests, extraData: ByteArray?) =
        sendRequest(request.keyword, extraData)


    fun interface ProgressListener{
        fun progress(progress: Int)
    }

    companion object {
        private val clientMutex = Mutex() // Only one client connection at a time. More can cause problems when changing password or stuff like that.
    }
}