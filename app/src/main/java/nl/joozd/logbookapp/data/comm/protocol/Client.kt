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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Observer
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import nl.joozd.joozdlogcommon.ProtocolVersion
import nl.joozd.joozdlogcommon.comms.JoozdlogCommsKeywords
import nl.joozd.joozdlogcommon.comms.Packet
import nl.joozd.joozdlogcommon.serializing.intFromBytes
import nl.joozd.joozdlogcommon.serializing.toByteArray
import nl.joozd.joozdlogcommon.serializing.wrap
import nl.joozd.joozdlogcommon.utils.LzwCompressor
import nl.joozd.logbookapp.data.comm.InternetStatus

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
     * By convention, should contain an instance of a [job][Job] to enforce structured concurrency.
     */
    override val coroutineContext: CoroutineContext = Dispatchers.IO + Job()

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
            sendRequest(JoozdlogCommsKeywords.HELLO, ProtocolVersion.CURRENTVERSION.toByteArray())
        }
    }

    /**
     * Send data to server. On success, returns the amount of bytes sent, on failure returns a negative Integer
     * TODO define return codes in [CloudFunctionResults]
     */
    fun sendToServer(packet: Packet): Int {
        // Log.d("SendToServer:", packet.message.take(40).toByteArray().toString(Charsets.UTF_8))
        try {
            socket?.let {
                val output = BufferedOutputStream(it.getOutputStream())
                output.write(packet.content)
                output.flush()
                return packet.message.size
            }
            Log.e(TAG, "Error 0005: Socket is null")
            return -5
        } catch (he: UnknownHostException) {
            val exceptionString = "An exception 0001 occurred:\n ${he.printStackTrace()}"
            Log.e(TAG, exceptionString, he)
            return -1
        } catch (ioe: IOException) {
            val exceptionString = "An exception 0002 occurred:\n ${ioe.printStackTrace()}"
            Log.e(TAG, exceptionString, ioe)
            return -2
        } catch (ce: ConnectException) {
            val exceptionString = "An exception 0003 occurred:\n ${ce.printStackTrace()}"
            Log.e(TAG, exceptionString, ce)
            return -3
        } catch (se: SocketException) {
            val exceptionString = "An exception 0004 occurred:\n ${se.printStackTrace()}"
            Log.e(TAG, exceptionString, se)
            return -4
        }
    }
    /**
     * sendCompressed with send a Packet with uncompressed header and
     * will send it's message compressed with 16-bits LZW compression
     */
    fun sendCompressed(packet: Packet): Int{
        Log.d("sendCompressed:", packet.message.take(40).toByteArray().toString(Charsets.UTF_8))
        val compressedMessage = LzwCompressor.compress(packet.message)
        val newPacket = Packet(compressedMessage)
        try {
            socket?.let {
                val output = BufferedOutputStream(it.getOutputStream())
                output.write(newPacket.content)
                output.flush()
                return newPacket.message.size
            }
            Log.e(TAG, "Error 0005: Socket is null")
            return -5
        } catch (he: UnknownHostException) {
            val exceptionString = "An exception 0001 occurred:\n ${he.printStackTrace()}"
            Log.e(TAG, exceptionString, he)
            return -1
        } catch (ioe: IOException) {
            val exceptionString = "An exception 0002 occurred:\n ${ioe.printStackTrace()}"
            Log.e(TAG, exceptionString, ioe)
            return -2
        } catch (ce: ConnectException) {
            val exceptionString = "An exception 0003 occurred:\n ${ce.printStackTrace()}"
            Log.e(TAG, exceptionString, ce)
            return -3
        } catch (se: SocketException) {
            val exceptionString = "An exception 0004 occurred:\n ${se.printStackTrace()}"
            Log.e(TAG, exceptionString, se)
            return -4
        }
    }

    /**
     * Runs listsner f with a 0-100 percentage completed value
     */
    fun readFromServer(f: (Int) -> Unit = {}): ByteArray? {
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

    /**
     * Runs listener f with a 0-100 percentage completed value
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
     */
    fun sendRequest(request: String, extraData: ByteArray? = null): Int =
        sendToServer(Packet(wrap(request) + (extraData ?: ByteArray(0)))).also{
            Log.d("request", request)
        }


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
        Log.d("Client()", "Starting $timeOutMillis millis timeout")
        delay(timeOutMillis)
        Log.d("Client()", "$timeOutMillis millis passed")
        if (isActive) {
            Log.d("Client()", "Closing Client")
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
                Log.d(TAG, "sending EOS, closing socket")
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
            try{ // in a try loop so it won't spam Log with exceptions
                timeOut.cancel()
            } finally{

                return instance?.also{Log.d("Client()", "Reusing instance")}
                    ?: Client().also{ instance = it }.also{Log.d("Client()", "Creating new instance")}
            }
        }
    }


}