/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2021 Joost Welle
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

package nl.joozd.logbookapp.ui.utils.customs.viewpagernavigatorbar

import android.util.TypedValue
import android.view.View
import androidx.annotation.AttrRes

fun View.getAttribute(@AttrRes attribute: Int) = with(TypedValue()){
    context.theme.resolveAttribute(attribute, this, true)
    data
}

/**
 * Convert DP to pixels and vice versa
 */
fun View.dpToPixels(dp: Int) = (dp * resources.displayMetrics.density).toInt()