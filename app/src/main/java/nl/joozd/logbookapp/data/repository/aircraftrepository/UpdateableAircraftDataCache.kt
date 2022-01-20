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
import nl.joozd.logbookapp.data.repository.helpers.formatRegistration

/**
 * Use this for read-only scenarios, or update manually after writing to aircraft DB
 */
class UpdateableAircraftDataCache(
    private var types: List<AircraftType>,
    private var registrationToAircraftMap: Map<String, Aircraft>
    ) : AircraftDataCache {
    constructor(aircraftDataCache: AircraftDataCache):
            this(aircraftDataCache.getAircraftTypes(), aircraftDataCache.getRegistrationToAircraftMap())

    fun updateTypes(types: List<AircraftType>) {
        this.types = types
    }

    fun updateAircraftMap(map: Map<String, Aircraft>) {
        registrationToAircraftMap = map
    }

    override fun getRegistrationToAircraftMap(): Map<String, Aircraft> = registrationToAircraftMap

    override fun getAircraftTypes(): List<AircraftType> = types

    override fun getAircraftFromRegistration(registration: String?): Aircraft? =
        registration?.let {
            registrationToAircraftMap[formatRegistration(it)]
        }

    override fun getAircraftTypeByShortName(shortName: String): AircraftType? =
        types.firstOrNull { it.shortName == shortName }

}