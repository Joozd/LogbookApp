package nl.joozd.logbookapp.comm

import android.util.Log
import kotlinx.coroutines.withContext
import nl.joozd.joozdlogcommon.DataFilesMetaData
import nl.joozd.joozdlogcommon.comms.Protocol
import nl.joozd.logbookapp.utils.DispatcherProvider
import java.io.IOException
import java.net.URL

// HTTP requests cannot be rejected only fail due to connection problems, so they will either return requested data or null.
// For testing I can just inject a local url (e.g. file:///c|/temp/)
class HTTPServer(private val server: String = Protocol.SERVER_URL){
    suspend fun getDataFilesMetaData(): DataFilesMetaData? =
        readMetaDataJsonFileToString()?.let { DataFilesMetaData.fromJSON(it) }

    // Does not check for proper data, null on connection failure.
    private suspend fun readMetaDataJsonFileToString(): String? =
        try {
            withContext(DispatcherProvider.io()) {
                @Suppress("BlockingMethodInNonBlockingContext")
                URL(server + Protocol.DATAFILES_URL_PREFIX + Protocol.DATAFILES_METADATA_FILENAME).readText()
            }
        } catch (ioe: IOException){
            Log.w("readMetaDataJsonString", "IO error when trying to read url")
            null
        }

}