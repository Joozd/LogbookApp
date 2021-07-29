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

package nl.joozd.logbookapp.data.comm.protocol

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import nl.joozd.joozdlogcommon.ProtocolVersion
import nl.joozd.joozdlogcommon.comms.JoozdlogCommsKeywords
import nl.joozd.joozdlogcommon.comms.Packet
import nl.joozd.joozdlogcommon.serializing.intFromBytes
import nl.joozd.joozdlogcommon.serializing.toByteArray
import nl.joozd.joozdlogcommon.serializing.wrap
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLSocketFactory
import kotlin.coroutines.CoroutineContext

class Client: Closeable, CoroutineScope {
    /**
     * The context of this scope.
     * Context is encapsulated by the scope and used for implementation of coroutine builders that are extensions on the scope.
     * Accessing this property in general code is not recommended for any purposes except accessing the [Job] instance for advanced usages.
     *
     * By convention, should contain an instance of a [Job] to enforce structured concurrency.
     */
    override val coroutineContext: CoroutineContext = Dispatchers.IO + Job()

    /**
     * If socket died, this is set to false.
     * Set to true on creation of socket.
     * Set to false on receiving a socket exception
     */
    var alive = false
        private set

    private val socket = try {
        SSLSocketFactory.getDefault().createSocket(
            SERVER_URL,
            SERVER_PORT
        )
    } catch (ioe: IOException){
        Log.w(TAG, "Error creating SSLSocket:")
        Log.w(TAG, ioe.stackTrace.toString())
        null
    }
    init{
        socket?.let{
            sendRequest(JoozdlogCommsKeywords.HELLO, ProtocolVersion.CURRENTVERSION.toByteArray(), initializing = true).let{
                alive = it.isOK()
                println("ALIVE! - $it")
            }


        }
        println("WWWWWWWWWWW socket alive: $alive")
    }

    /**
     * Send data to server. On success, returns the amount of bytes sent, on failure returns a negative Integer
     * @return [CloudFunctionResults]:
     *  [CloudFunctionResults.OK] if all OK
     *  [CloudFunctionResults.SOCKET_IS_NULL] if socket is not created
     *  [CloudFunctionResults.UNKNOWN_HOST] if host was not found
     *  [CloudFunctionResults.IO_ERROR] if a generic IO error occurred
     *  [CloudFunctionResults.CONNECTION_REFUSED] if connection could not be established by remote host
     *  [CloudFunctionResults.SOCKET_ERROR] if there is an error creating or accessing a Socket
     */
    private fun sendToServer(packet: Packet): CloudFunctionResults {
        // Log.d("SendToServer:", packet.message.take(40).toByteArray().toString(Charsets.UTF_8))
        try {
            socket?.let {
                val output = BufferedOutputStream(it.getOutputStream())
                output.write(packet.content)
                output.flush()
                return CloudFunctionResults.OK
            }
            Log.e(TAG, "Error 0005: Socket is null")
            return CloudFunctionResults.SOCKET_IS_NULL.also{
                instance = null
                alive = false
            }
        } catch (he: UnknownHostException) {
            return CloudFunctionResults.UNKNOWN_HOST.also{
                instance = null
                alive = false
            }
        } catch (ioe: IOException) {
            return CloudFunctionResults.IO_ERROR.also{
                instance = null
                alive = false
            }
        } catch (ce: ConnectException) {
            return CloudFunctionResults.CONNECTION_REFUSED.also{
                instance = null
                alive = false
            }
        } catch (se: SocketException) {
            return CloudFunctionResults.SOCKET_ERROR.also{
                instance = null
                alive = false
            }
        }
    }

    /**
     * Reads data from server
     * @param f: listener with a 0-100 percentage completed value
     */
    fun readFromServer(f: (Int) -> Unit = {}): ByteArray? {
        if (alive) {
            try {
                socket?.let {
                    return try {
                        getInput(BufferedInputStream(it.getInputStream()), f)
                    } catch (e: IOException) {
                        Log.e(TAG, "Error: $e, ${e.printStackTrace()}")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error: $e, ${e.printStackTrace()}")
            }
            return null
        }
        else return null
    }

    /**
     * Gets input from a BufferedInputStream
     * @param inputStream: The InputStream. This must be a complete [Packet]
     * @param f: listener with a 0-100 percentage completed value
     */
    private fun getInput(inputStream: BufferedInputStream, f: (Int) -> Unit = {}): ByteArray {
        val buffer = ByteArray(8192)
        val header = ByteArray(Packet.HEADER.length+4)

        //Read the header as it comes in, or fail trying.
        repeat (header.size){
            val r = inputStream.read()
            if (r<0) throw IOException("Stream too short: ${it-1} bytes")
            header[it] = r.toByte()
        }
        val expectedSize =
            intFromBytes(header.takeLast(4))
        if (expectedSize > MAX_MESSAGE_SIZE) throw IOException("size bigger than $MAX_MESSAGE_SIZE")
        val message = mutableListOf<Byte>()

        //read buffers until correct amount of bytes reached or fail trying
        while (message.size < expectedSize){
            f(100*message.size/expectedSize)
            val b = inputStream.read(buffer)
            if (b<0) throw IOException("Stream too short: expect $expectedSize, got ${message.size}")
            message.addAll(buffer.take(b))
        }
        f(100)
        return message.toByteArray()
    }

    /**
     * Tries to send an END_OF_SESSION message, then tries to close the socket no matter what.
     */


    /**
     * Send a request to server
     * @param request: A string as defined in nl.joozd.joozdlogcommon.comms.JoozdlogCommsKeywords
     * @param extraData: A bytearray with extra data to be sent as part of this request
     * @return [CloudFunctionResults]:
     *  [CloudFunctionResults.OK] if all OK
     *  [CloudFunctionResults.CLIENT_NOT_ALIVE] if Client died
     *  Or any of the results that [sendToServer] can return:
     *  [CloudFunctionResults.SOCKET_IS_NULL] if socket is not created
     *  [CloudFunctionResults.UNKNOWN_HOST] if host was not found
     *  [CloudFunctionResults.IO_ERROR] if a generic IO error occurred
     *  [CloudFunctionResults.CONNECTION_REFUSED] if connection could not be established by remote host
     *  [CloudFunctionResults.SOCKET_ERROR] if there is an error creating or accessing a Socket
     */
    fun sendRequest(request: String, extraData: ByteArray? = null, initializing: Boolean = false): CloudFunctionResults = if (alive || initializing)
        sendToServer(Packet(wrap(request) + (extraData ?: ByteArray(0))))
    else CloudFunctionResults.CLIENT_NOT_ALIVE
            //.also{ Log.d("request", request) }


    /**
     * Will call timeOut, which will close the connection after 10 seconds.
     * If a new instance if asked in the mean time, the closing is cancelled
     */
    override fun close(){
        timeOut = timeOut()
        try{
            mutex.unlock()
        } catch(e: java.lang.Exception){
            Log.w("Client", "already unlocked, probably closed before opening again? ${e.stackTraceToString()}")
        }

    }

    private fun timeOut(timeOutMillis: Long = 10000): Job = launch{
        delay(timeOutMillis)
        if (isActive) {
            Log.i("Client()", "Closing Client")
            instance = null
            finalClose()
        }
    }

    /**
     * Will send END_OF_SESSION and close socket
     * Will set lifeCycle to DESTROYED
     * Will cancel all running jobs
     */
    private fun finalClose(){
        try{
            socket.use {
                Log.v(TAG, "sending EOS, closing socket")
                this.sendRequest(JoozdlogCommsKeywords.END_OF_SESSION)
            }
        } finally {
            coroutineContext.cancel()
        }
    }

    companion object{
        private var instance: Client? = null
        private var timeOut: Job = Job()
        private val mutex = Mutex()

        const val SERVER_URL = "joozd.nl"
        const val SERVER_PORT = 1337
        const val TAG = "comm.protocol.Client"
        const val MAX_MESSAGE_SIZE = Int.MAX_VALUE-1

        /**
         * Returns an open instance if it is available
         * Client will be locked untill starting timeOut()
         */
        suspend fun getInstance(): Client {
            mutex.lock()
            timeOut.cancel()
            return instance?.also{Log.v("Client()", "Reusing instance")}
                    ?: Client().also{ instance = it }.also{Log.v("Client()", "Creating new instance")}

        }
    }


}