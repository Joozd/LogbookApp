/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020-2022 Joost Welle
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

package nl.joozd.logbookapp.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.data.dataclasses.BalanceForward
import nl.joozd.logbookapp.data.room.model.AircraftRegistrationWithTypeData
import nl.joozd.logbookapp.data.room.model.AircraftTypeConsensusData
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.room.dao.*
import nl.joozd.logbookapp.data.room.model.AircraftTypeData
import nl.joozd.logbookapp.data.dataclasses.FlightData
import nl.joozd.logbookapp.data.room.model.PreloadedRegistration

/**
 * Version 8: added multiPilotTime to FlightData
 * Version 9: Changed AircraftRegistrationWithTypeData
 */
@Database(entities = [FlightData::class, Airport::class, AircraftTypeData::class, AircraftRegistrationWithTypeData::class, AircraftTypeConsensusData::class, PreloadedRegistration::class, BalanceForward::class], version = 9, exportSchema = true)
abstract class JoozdlogDatabase protected constructor(): RoomDatabase() { // protected constructor instead of private due to room needing access to it
    abstract fun flightDao(): FlightDao
    abstract fun airportDao(): AirportDao
    abstract fun aircraftTypeDao(): AircraftTypeDao
    abstract fun registrationDao(): RegistrationDao
    abstract fun aircraftTypeConsensusDao(): AircraftTypeConsensusDao
    abstract fun preloadedRegistrationsDao(): PreloadedRegistrationsDao
    abstract fun balanceForwardDao(): BalanceForwardDao

    companion object {
        private var INSTANCE: JoozdlogDatabase? = null

        @Synchronized
        fun getInstance(): JoozdlogDatabase {
            INSTANCE?.let { return it }
            val instance = Room.databaseBuilder(
                App.instance.applicationContext,
                JoozdlogDatabase::class.java,
                "flights_database"
            ).addMigrations(UPDATE_8_9)
                .build()
            INSTANCE = instance
            return instance
        }

        //obsolete migrations are kept here to remind me of how to do that

        /*
        private val UPDATE_4_5 = object: Migration(4,5){
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE AircraftRegistrationWithTypeData ADD COLUMN timestamp INTEGER NOT NULL DEFAULT -1")
            }
        }
        private val UPDATE_5_6 = object: Migration(5,6){
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE PreloadedRegistration (registration TEXT NOT NULL, type TEXT NOT NULL, PRIMARY KEY(registration))")
            }
        }

 */
        private val UPDATE_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE FlightData ADD COLUMN multiPilotTime INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val UPDATE_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE AircraftRegistrationWithTypeData")
                database.execSQL("CREATE TABLE IF NOT EXISTS `AircraftRegistrationWithTypeData` (`registration` TEXT NOT NULL, `serializedType` BLOB NOT NULL, `knownToServer` INTEGER NOT NULL, `serializedPreviousType` BLOB NOT NULL, PRIMARY KEY(`registration`))")
            }
        }
    }

}