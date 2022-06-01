package nl.joozd.logbookapp.comm

import android.util.Log
import kotlinx.coroutines.withContext
import nl.joozd.joozdlogcommon.DataFilesMetaData
import nl.joozd.joozdlogcommon.Protocol
import nl.joozd.logbookapp.utils.DispatcherProvider
import java.io.IOException
import java.net.URL

// HTTP requests cannot be rejected only fail due to connection problems, so they will either return requested data or null.
// For testing I can just inject a local url (e.g. file:///c|/temp/)
class HTTPServer(private val urlPrefix: String = Protocol.DATAFILES_URL_PREFIX){
    suspend fun getDataFilesMetaData(): DataFilesMetaData? =
        readMetaDataJsonFileToString()?.let { DataFilesMetaData.fromJSON(it) }


    // Does not check for proper data, null on connection failure.
    private suspend fun readMetaDataJsonFileToString(): String? =
        try {
            withContext(DispatcherProvider.io()) {
                @Suppress("BlockingMethodInNonBlockingContext")
                URL(urlPrefix + Protocol.DATAFILES_METADATA_FILENAME).readText()
            }
        } catch (ioe: IOException){
            Log.w("readMetaDataJsonString", "IO error when trying to read url")
            null
        }

}