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

package nl.joozd.logbookapp.data.repository.aircraftrepository

import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.logbookapp.data.dataclasses.Aircraft

class UpdateableAircraftDataCache(
    private var types: List<AircraftType>,
    private var registrationToAircraftMap: Map<String, Aircraft>
    ) : AircraftDataCache {
    constructor(aircraftDataCache: AircraftDataCache):
            this(aircraftDataCache.getAircraftTypes(), aircraftDataCache.getRegistrationToAircraftMap())

    /**
     * Update cached aircraft types
     */
    fun updateTypes(types: List<AircraftType>) {
        this.types = types
    }

    /**
     * Update cached aircraft types
     */
    fun updateAircraftMap(map: Map<String, Aircraft>) {
        TODO("Not yet implemented")
    }

    /**
     * Return a map with all currently loaded data
     */
    override fun getRegistrationToAircraftMap(): Map<String, Aircraft> {
        TODO("Not yet implemented")
    }

    /**
     * Return a list of all aircraft types, or an empty list if cache not loaded yet
     */
    override fun getAircraftTypes(): List<AircraftType> {
        TODO("Not yet implemented")
    }

    /**
     * Get an aircraft from its registration.
     * Should call [nl.joozd.logbookapp.data.repository.helpers.formatRegistration] on [registration]
     */
    override fun getAircraftFromRegistration(registration: String?): Aircraft? {
        TODO("Not yet implemented")
    }

}