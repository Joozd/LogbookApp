package nl.joozd.logbookapp.comm

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nl.joozd.comms.Client
import nl.joozd.joozdlogcommon.*
import nl.joozd.joozdlogcommon.comms.Protocol
import nl.joozd.joozdlogcommon.comms.JoozdlogCommsRequests
import nl.joozd.joozdlogcommon.comms.JoozdlogCommsResponses
import nl.joozd.logbookapp.core.TaskFlags
import nl.joozd.logbookapp.core.messages.MessageCenter
import nl.joozd.logbookapp.core.messages.Messages
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
     * If no success, sets [TaskFlags.updateEmailWithServer] to true
     */
    suspend fun sendNewEmailAddress(emailAddress: String): Long? {
        val data = EmailData(EmailData.EMAIL_ID_NOT_SET, emailAddress, ByteArray(0))
        val result = try{
            responseForRequestOrException(JoozdlogCommsRequests.SET_EMAIL, data)
        } catch(e: CloudException){
            Log.d("sendNewEmailAddress", "server replied ${e.cloudFunctionResult}")
            throw(e)
        }
        TaskFlags.updateEmailWithServer(false)
        return unwrap(result)
    }

    /**
     * Migrate email data on server to new system (Long ID instead of String Username)
     */
    suspend fun migrateEmailData(username: String, emailAddress: String): Long? {
        val data = LoginDataWithEmail(username, ByteArray(0), -1, emailAddress).serialize()
        val result = try{
            responseForRequestOrException(JoozdlogCommsRequests.SET_EMAIL, data)
        } catch(e: CloudException){
            Log.d("sendNewEmailAddress", "server replied ${e.cloudFunctionResult}")
            return null
        }
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
            MessageCenter.pushMessage(Messages.unknownOrUnverifiedEmailMessage)
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
        println("SendP2PData (${data.size})")
        return unwrapLong(responseForRequestOrException(JoozdlogCommsRequests.SENDING_P2P_DATA, data))
    }

    suspend fun receiveP2PData(sessionID: Long): ByteArray {
        println("receiveP2PData")
        val payload = wrap(sessionID)
        println("wrapped session ID")
        return responseForRequestOrException(JoozdlogCommsRequests.REQUEST_P2P_DATA, payload).also{
            println("receiveP2PData returns ${it.size} bytes")
        }
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

    private suspend fun resultForRequest(request: JoozdlogCommsRequests, extraData: ByteArray?): CloudFunctionResult = withClient {
        sendRequest(request, extraData)
        handleResponse()!!
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

    private suspend fun Client.handleResponseAndThrowExceptionOnError() = handleServerResultAndThrowExceptionOnError(readServerResponse())


    fun interface ProgressListener{
        fun progress(progress: Int)
    }

    // Pass this to functions giving a result to get returned data.
    // The return of the function will only be a CloudFunctionResult, so if you want anything else you can drop it in here.
    data class Result<T>(var value: T? = null)

    private suspend fun Client.sendRequest(request: JoozdlogCommsRequests, extraData: ByteArray?) =
        sendRequest(request.keyword, extraData)


    companion object {
        private val clientMutex = Mutex() // Only one client connection at a time. More can cause problems when changing password or stuff like that.
    }
}