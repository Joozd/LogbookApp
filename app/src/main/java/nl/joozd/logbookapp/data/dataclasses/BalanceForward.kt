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

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class BalanceForward (
    @PrimaryKey val id: Int = -1,
    val logbookName: String,
    val multiPilotTime: Int,
    val aircraftTime: Int,
    val landingDay: Int,
    val landingNight: Int,
    val nightTime: Int,
    val ifrTime: Int,
    val picTime: Int,
    val copilotTime: Int,
    val dualTime: Int,
    val instructortime: Int,
    val simTime: Int
){
    val grandTotal: Int
        get() = aircraftTime + simTime

    companion object{
        val EMPTY get() = BalanceForward(-1, "", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    }
}