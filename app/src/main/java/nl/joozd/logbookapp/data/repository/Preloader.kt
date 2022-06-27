package nl.joozd.logbookapp.data.repository

import android.content.Context
import kotlinx.coroutines.withContext
import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.joozdlogcommon.BasicAirport
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.App
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.utils.DispatcherProvider
import nl.joozd.serializing.unpackSerialized

class Preloader(private val context: Context = App.instance) {
    suspend fun getPreloadedAirports(): List<Airport> = withContext(DispatcherProvider.default()) {
        unpackSerialized(withContext(DispatcherProvider.io()) { context.resources.openRawResource(R.raw.airports_0).readBytes() }).map {
            Airport(BasicAirport.deserialize(it))
        }
    }

    suspend fun getPreloadedAircraftTypes(): List<AircraftType> =
        unpackSerialized(withContext(DispatcherProvider.io()) {context.resources.openRawResource(R.raw.aircrafttypes_0).readBytes() }).map{
            AircraftType.deserialize(it)
        }
}