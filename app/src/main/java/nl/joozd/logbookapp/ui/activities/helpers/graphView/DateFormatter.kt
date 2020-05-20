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

package nl.joozd.logbookapp.ui.activities.helpers.graphView

import com.jjoe64.graphview.DefaultLabelFormatter
import com.jjoe64.graphview.LabelFormatter
import com.jjoe64.graphview.Viewport
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class DateFormatter: DefaultLabelFormatter(){
    override fun formatLabel(value: Double, isValueX: Boolean): String {
        val date = LocalDateTime.ofInstant(Instant.ofEpochSecond(value.toLong()), ZoneOffset.UTC).plusMinutes(1) // add a minutes fo rrounding errors
        return if (isValueX) {
            if (date.month.value == 1)
            "${date.month.value.toString().padStart(2, '0')}.${date.year.toString().takeLast(2)}" else date.month.value.toString().padStart(2, '0')
        } else "${(value/60).toInt()}"
    }


}