package nl.joozd.logbookapp.comm

import android.util.Log
import kotlinx.coroutines.withContext
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.joozdlogcommon.BasicAirport
import nl.joozd.joozdlogcommon.DataFilesMetaData
import nl.joozd.joozdlogcommon.ForcedTypeData
import nl.joozd.joozdlogcommon.comms.Protocol
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.utils.DispatcherProvider
import nl.joozd.serializing.JoozdSerializable
import nl.joozd.serializing.unpackSerialized
import java.io.IOException
import java.net.URL

// HTTP requests cannot be rejected only fail due to connection problems, so they will either return requested data or null.
// For testing I can just inject a local url (e.g. file:///c|/temp/)
class HTTPServer(private val server: String = Protocol.SERVER_URL) {
    suspend fun getDataFilesMetaData(): DataFilesMetaData? =
        readMetaDataJsonFileToString()?.let { DataFilesMetaData.fromJSON(it) }

    /**
     * Aircraft file is a packed list of serialized AircraftTypes
     */
    suspend fun getAircraftTypes(dataFilesMetaData: DataFilesMetaData): List<AircraftType>? =
        downloadAndDeserializeDataFile(dataFilesMetaData.aircraftTypesLocation){
            AircraftType.deserialize(it)
        }

    suspend fun getForcedTypes(dataFilesMetaData: DataFilesMetaData): List<ForcedTypeData>? =
        downloadAndDeserializeDataFile(dataFilesMetaData.aircraftForcedTypesLocation) {
            ForcedTypeData.deserialize(it)
        }

    suspend fun getAirports(dataFilesMetaData: DataFilesMetaData): List<Airport>? =
        downloadAndDeserializeDataFile(dataFilesMetaData.airportsLocation) {
            BasicAirport.deserialize(it)
        }?.map {Airport(it)}


    private suspend fun <T: JoozdSerializable> downloadAndDeserializeDataFile(location: String, deserializer: (ByteArray) -> T): List<T>? =
        downloadDataFile(location)?.let{ packed ->
            unpackSerialized(packed).map{ serialized ->
                deserializer(serialized)
            }
        }


    private suspend fun downloadDataFile(location: String): ByteArray? =
        try {
            withContext(DispatcherProvider.io()) {
                @Suppress("BlockingMethodInNonBlockingContext")
                URL(server + Protocol.DATAFILES_URL_PREFIX + location).readBytes()
            }
        } catch (ioe: IOException) {
            Log.w("readMetaDataJsonString", "IO error when trying to read url")
            null
        }


    // Does not check for proper data, null on connection failure.
    private suspend fun readMetaDataJsonFileToString(): String? =
        try {
            withContext(DispatcherProvider.io()) {
                @Suppress("BlockingMethodInNonBlockingContext")
                URL(server + Protocol.DATAFILES_URL_PREFIX + Protocol.DATAFILES_METADATA_FILENAME).readText()
            }
        } catch (ioe: IOException){
            null
        }

}