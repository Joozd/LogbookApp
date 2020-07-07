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

package nl.joozd.logbookapp.data.room


import kotlinx.coroutines.*
import nl.joozd.logbookapp.data.room.dao.*
import nl.joozd.logbookapp.App


/**
 * This is the handle for accessing the Database
 * It will take care of caching as well as saving/loading to/from DB
 * Initialise:
 * @param flightDao = Dao to access flightsDatabase
 * @param dispatcher = dispatcher for coroutines, standard uses Dispatchers.IO
 *
 * Get a singleton instance with Repository.getInstance()
 */
@Deprecated ("Use separate repositories for separate data types")
class Repository(
    private val flightDao: FlightDao,
    private val airportDao: AirportDao,
    private val aircraftTypeDao: AircraftTypeDao,
    private val registrationDao: RegistrationDao,
    private val aircraftTypeConsensusDao: AircraftTypeConsensusDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
): CoroutineScope by MainScope() {



    /********************************************************************************************
     * Companion Object
     ********************************************************************************************/

    companion object{
        private var singletonInstance: Repository? = null
        fun getInstance(): Repository = synchronized(this) {
            singletonInstance
                ?: run {
                    val dataBase = JoozdlogDatabase.getDatabase(App.instance)
                    val flightsDao = dataBase.flightDao()
                    val airportDao = dataBase.airportDao()
                    val aircraftTypeDao = dataBase.aircraftTypeDao()
                    val registrationDao = dataBase.registrationDao()
                    val aircraftTypeConsensusDao = dataBase.aircraftTypeConsensusDao()

                    singletonInstance = Repository(flightsDao, airportDao, aircraftTypeDao, registrationDao, aircraftTypeConsensusDao)
                    singletonInstance!!
                }
        }
    }


}