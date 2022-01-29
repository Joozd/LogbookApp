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

import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SupportSQLiteOpenHelper
import nl.joozd.logbookapp.data.room.dao.*

class MockDatabase: JoozdlogDatabase() {
    override fun flightDao(): FlightDao = MockFlightDao()

    override fun airportDao(): AirportDao = MockAirportDao()

    override fun aircraftTypeDao(): AircraftTypeDao = MockAircraftTypeDao()

    override fun registrationDao(): RegistrationDao = MockRegstrationDao()

    override fun aircraftTypeConsensusDao(): AircraftTypeConsensusDao {
        TODO("Not used at the moment") // not used at the moment but don't want to run updates on DB
    }

    override fun preloadedRegistrationsDao(): PreloadedRegistrationsDao = MockPreloadedRegistrationsDao()

    override fun balanceForwardDao(): BalanceForwardDao {
        TODO("Not testable yet")
    }

    override fun createOpenHelper(config: DatabaseConfiguration?): SupportSQLiteOpenHelper {
        TODO("Not implemented") // not used in mock
    }

    override fun createInvalidationTracker(): InvalidationTracker {
        return InvalidationTracker(this) // not used in mock
    }

    override fun clearAllTables() {
        TODO("Not implemented") // not used in mock
    }
}