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
data class FlightData(
    @PrimaryKey val flightID: Int,
    val orig: String,
    val dest: String,
    val timeOut: Long,              // timeOut and timeIn are seconds since epoch
    val timeIn: Long,               // timeOut and timeIn are seconds since epoch
    val correctedTotalTime: Int,
    val multiPilotTime: Int,
    val nightTime: Int,
    val ifrTime:Int,
    val simTime: Int,
    val aircraft: String,
    val registration: String,
    val name: String,
    val name2: String,
    val takeOffDay: Int,
    val takeOffNight: Int,
    val landingDay: Int,
    val landingNight: Int,
    val autoLand: Int,
    val flightNumber: String,
    val remarks: String,
    val isPIC: Boolean,
    val isPICUS: Boolean,
    val isCoPilot: Boolean,
    val isDual: Boolean,
    val isInstructor: Boolean,
    val isSim: Boolean,
    val isPF: Boolean,
    val isPlanned: Boolean,
    val autoFill: Boolean,
    val augmentedCrew: Int,
    val signature: String = ""
)