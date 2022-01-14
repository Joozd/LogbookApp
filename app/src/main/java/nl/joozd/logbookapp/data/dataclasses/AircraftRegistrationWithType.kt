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

package nl.joozd.logbookapp.data.dataclasses

import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.logbookapp.data.room.model.AircraftRegistrationWithTypeData

data class AircraftRegistrationWithType(val registration: String, val type: AircraftType, val knownToServer: Boolean, val previousType: AircraftType? = null){
    constructor(model: AircraftRegistrationWithTypeData): this(
        model.registration,
        AircraftType.deserialize(model.serializedType),
        model.knownToServer,
        if (model.serializedPreviousType.isEmpty()) null else
        AircraftType.deserialize(model.serializedPreviousType)
    )



    fun toModel() = AircraftRegistrationWithTypeData(
        registration = registration,
        serializedType = type.serialize(),
        knownToServer = knownToServer,
        serializedPreviousType = previousType?.serialize() ?: ByteArray(0)
    )
}