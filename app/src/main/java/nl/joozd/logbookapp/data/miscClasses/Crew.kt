/*
 * JoozdLog Pilot's Logbook
 * Copyright (C) 2020 Joost Welle
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see https://www.gnu.org/licenses
 */

package nl.joozd.logbookapp.data.miscClasses

import nl.joozd.logbookapp.extensions.getBit
import nl.joozd.logbookapp.extensions.setBit
import nl.joozd.logbookapp.extensions.toInt

/************************************************************************************
 * CrewValue will store info on augmented crews:                                    *
 * bits 0-3: amount of crew (no crews >15 ppl :) )                                  *
 * bit 4: in seat on takeoff                                                        *
 * bit 5: in seat on landing                                                        *
 * bit 6-31: amount of time reserved for takeoff/landing (standard in settings)     *
 ************************************************************************************/
data class Crew(var crewSize: Int = 2,
                val didTakeoff: Boolean = true,
                val didLanding: Boolean = true,
                val takeoffLandingTimes: Int = 0)
{
    companion object {
        fun of(value: Int) = if (value == 0) Crew() else Crew(
            crewSize = 15.and(value),
            didTakeoff = value.getBit(4),
            didLanding = value.getBit(5),
            takeoffLandingTimes = value.ushr(6)
        )
        fun of(crewSize: Int, didTakeoff: Boolean, didLanding: Boolean, nonStandardTimes: Int) = Crew(crewSize,didTakeoff,didLanding,nonStandardTimes)
    }

    fun toInt():Int {
        var value = if (crewSize > 15) 15 else crewSize
        value = value.setBit(4, didTakeoff).setBit(5, didLanding)
        value += takeoffLandingTimes.shl(6)
        return value
    }
    fun getLogTime(totalTime: Int, pic: Boolean = false): Int{
        if (pic) return totalTime
        return ((totalTime-2*takeoffLandingTimes)/crewSize) * 2 + takeoffLandingTimes * (didTakeoff.toInt() + didLanding.toInt())
    }
}