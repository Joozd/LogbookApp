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

interface AircraftDataCache {
    /**
     * Return a map with all currently loaded data
     */
    fun getRegistrationToAircraftMap(): Map<String, Aircraft>

    /**
     * Return a list of all aircraft types, or an empty list if cache not loaded yet
     */
    fun getAircraftTypes(): List<AircraftType>

    /**
     * Get an aircraft from its registration.
     * Should call [nl.joozd.logbookapp.data.repository.helpers.formatRegistration] on [registration]
     */
    fun getAircraftFromRegistration(registration: String?): Aircraft?

    /**
     * Get an aircraft type from its short name
     */
    fun getAircraftTypeByShortName(shortName: String): AircraftType?

    companion object{
        fun make(types: List<AircraftType>, aircraftMap: Map<String, Aircraft>): AircraftDataCache =
            AircraftDataCacheImpl(types, aircraftMap)

        fun empty(): AircraftDataCache = make(emptyList(), emptyMap())
    }

}