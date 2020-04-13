package nl.joozd.logbookapp.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import nl.joozd.logbookapp.data.dataclasses.AircraftRegistrationWithType
import nl.joozd.logbookapp.data.dataclasses.AircraftTypeConsensus
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.room.dao.*
import nl.joozd.logbookapp.data.room.model.AircraftTypeData
import nl.joozd.logbookapp.data.room.model.FlightData

@Database(entities = [FlightData::class, Airport::class, AircraftTypeData::class, AircraftRegistrationWithType::class, AircraftTypeConsensus::class], version = 3)
abstract class JoozdlogDatabase: RoomDatabase() {
    abstract fun flightDao(): FlightDao
    abstract fun airportDao(): AirportDao
    abstract fun aircraftTypeDao(): AircraftTypeDao
    abstract fun registrationDao(): RegistrationDao
    abstract fun aircraftTypeConsensusDao(): AircraftTypeConsensusDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: JoozdlogDatabase? = null

        fun getDatabase(context: Context): JoozdlogDatabase {
            INSTANCE?.let {return it}
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JoozdlogDatabase::class.java,
                    "flights_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}