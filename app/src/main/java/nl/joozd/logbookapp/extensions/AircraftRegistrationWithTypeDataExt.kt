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

package nl.joozd.logbookapp.extensions

import nl.joozd.joozdlogcommon.AircraftType
import nl.joozd.joozdlogcommon.ConsensusData
import nl.joozd.logbookapp.data.room.model.AircraftRegistrationWithTypeData
import nl.joozd.logbookapp.utils.emptyMutableList

/**
 * Returns a list of [ConsensusData] to be sent to server
 * If no previous type, it will only contain the new type to add
 * if previous type, it will also contain previous type with [ConsensusData.subtract] flag set
 */
fun AircraftRegistrationWithTypeData.toConsensusDataList(): List<ConsensusData>{
    val results = emptyMutableList<ConsensusData>()
    if (serializedPreviousType.isNotEmpty()) results.add(ConsensusData(registration, serializedPreviousType, subtract = true))
    results.add(ConsensusData(registration, serializedType))
    return results
}