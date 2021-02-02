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

package nl.joozd.logbookapp.ui.adapters.flightsadapter

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.extensions.getColorFromAttr

fun ViewGroup.setColorAccordingstatus(planned: Boolean, warning: Boolean = false){
    val normalColor = context.getColorFromAttr(android.R.attr.textColorSecondary)
    val plannedColor = context.getColorFromAttr(android.R.attr.textColorHighlight)
    val warningColor = context.getColorFromAttr(R.attr.textColorWarning)
    val colorToSet =  when{
        planned -> plannedColor
        warning -> warningColor
        else -> normalColor
    }
    for (c in 0 until this.childCount) {
        val v: View = getChildAt(c)
        if (v is TextView) {
            v.setTextColor(colorToSet)
        }
    }
}

fun View.shouldBeVisible(vis: Boolean){
    this.visibility = if (vis) View.VISIBLE else View.GONE
}




