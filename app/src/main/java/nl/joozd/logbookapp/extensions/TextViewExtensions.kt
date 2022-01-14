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

package nl.joozd.logbookapp.extensions

import android.graphics.Typeface
import android.widget.TextView
import nl.joozd.logbookapp.R

fun TextView.showAsActiveIf(active: Boolean){
    if (active) this.showAsActive()
    else this.showAsInactive()
}

fun TextView.showAsActive(){
    this.alpha=ACTIVE_ALPHA
    this.setTypeface(null, Typeface.BOLD)
    this.setBackgroundResource(R.drawable.rounded_corners_primarybackground)
}

fun TextView.showAsInactive(){
    this.alpha=INACTIVE_ALPHA
    this.setTypeface(null, Typeface.NORMAL)
    this.setBackgroundResource(0)
}

private const val ACTIVE_ALPHA = 1.0F
private const val INACTIVE_ALPHA = 0.5F