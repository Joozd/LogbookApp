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

package nl.joozd.logbookapp.data.repository.workingFlightRepository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import nl.joozd.logbookapp.data.dataclasses.Aircraft
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.repository.AircraftRepository
import nl.joozd.logbookapp.data.repository.AirportRepository
import nl.joozd.logbookapp.model.dataclasses.Flight
import kotlin.coroutines.CoroutineContext


/**
 * Worker class to do things pertaining Origin, Destination and Aircraft of a flight such as:
 * - get an [Airport] from an ident
 * - get an [Aircraft] from a registration
 * and put those in observable values
 */
class OrigDestAircraftWorker: CoroutineScope {
    override val coroutineContext: CoroutineContext = Job() + Dispatchers.Main
    private val airportRepository = AirportRepository.getInstance()
    private val aircraftRepository = AircraftRepository.getInstance()

    /**********************************************************************************************
     * Private parts
     **********************************************************************************************/


    private var setOrigJob: Job = Job()
    private var setDestJob: Job = Job()
    private var setAircraftJob: Job = Job()


    //holds last known orig
    private val _origAirport = MutableLiveData<Airport?>()
    //holds last known dest
    private val _destAirport = MutableLiveData<Airport?>()
    //holds last known Aircraft
    private val _aircraft = MediatorLiveData<Aircraft?>()
    init{
        /**
         * If aircraftDatabase gets updated, update aircraft
         */
        _aircraft.addSource(aircraftRepository.liveAircraftTypes){
            _aircraft.value?.let{
                setAircraft(it.registration)
            }
        }
    }

    private fun setOrigAirport(ident: String?): Job {
        return when {
            ident == null -> launch {
                ensureActive()
                _origAirport.value = null
            }
            _origAirport.value?.ident == ident -> Job() // empty job

            else -> launch {
                val foundAirport = airportRepository.getAirportOnce(ident).also{ Log.d("XXXXXXXXXXXXX", "88888888888888 $ident - $it")}
                ensureActive()
                _origAirport.value = foundAirport
            }
        }
    }

    private fun setDestAirport(ident: String?): Job = when {
        ident == null -> launch {
            ensureActive()
            _destAirport.value = null
        }
        _destAirport.value?.ident == ident -> Job() // empty job

        else -> launch {
            val foundAirport = airportRepository.getAirportOnce(ident)
            ensureActive()
            _destAirport.value = foundAirport
        }
    }

    private fun setAircraft(registration: String?): Job = when {
        registration == null -> launch {
            ensureActive()
            _aircraft.value = null
        }
        _aircraft.value?.registration == registration -> Job() //  empty job
        else -> launch {
            val result = aircraftRepository.getAircraftFromRegistration(registration) ?: Aircraft(registration)
            ensureActive()
            _aircraft.value = result
        }
    }



    /**********************************************************************************************
     * Public parts
     *********************************************************************************************/


    var orig: String? = null
        set(it) {
            field = it
            setOrigJob.cancel()
            setOrigJob = setOrigAirport(it)
        }

    var dest: String? = null
        set(it) {
            field = it
            setDestJob.cancel()
            setDestJob = setDestAirport(it)
        }

    var registration: String? = null
        set(it){
            field = it
            setAircraftJob.cancel()
            setAircraftJob = setAircraft(it)
        }

    val origAirport: LiveData<Airport?>
        get() = _origAirport

    val destAirport: LiveData<Airport?>
        get() = _destAirport

    val aircraft: LiveData<Aircraft?>
        get() = _aircraft

    var flight: Flight? = null
        set(it){
            field = it
            if (orig != it?.orig) orig = it?.orig
            if (dest != it?.dest) dest = it?.dest
            if (registration != it?.registration) registration = it?.registration
        }

    /**
     * Reset flight, making sure all fields fire again
     * @param f: Flight to set (optional)
     */
    fun reset(f: Flight? = null){
        orig = null
        dest = null
        registration = null
        f?.let{
            flight = it
        }
    }


}