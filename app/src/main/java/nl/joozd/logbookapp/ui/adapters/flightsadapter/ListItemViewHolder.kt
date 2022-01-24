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

package nl.joozd.logbookapp.ui.adapters.flightsadapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.miscClasses.crew.AugmentedCrew
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.getColorFromAttr
import nl.joozd.logbookapp.model.dataclasses.Flight

abstract class ListItemViewHolder(containerView: View): RecyclerView.ViewHolder(containerView) {
    abstract fun bindItem(flight: Flight, onClick: (Flight) -> Unit, onDelete: (Flight) -> Unit)

    protected fun Flight.checkIfIncomplete(checkNames: Boolean = Preferences.picNameNeedsToBeSet): Boolean =
        orig.isBlank()
                || dest.isBlank()
                || aircraftType.isBlank()
                || registration.isBlank()
                || (checkNames && name.isBlank())

    protected fun Flight.namesString(): String{
        val names = names()
        return if (names.size <=2) names.joinToString(", ")
        else names.take(2).joinToString(", ") + " + ${names.size - 2}"
    }

    protected fun Flight.aircraftString() =
        listOf(registration, aircraftType)
            .filter{it.isNotBlank()}
            .joinToString(" - ")

    protected fun Flight.takeOffLandingString() =
        "${takeoffs()}/${landings()}"

    protected fun Flight.isAugmented() =
        AugmentedCrew.of(augmentedCrew).size > 2

    protected fun Flight.isIfr() =
        ifrTime > 0

    protected fun ViewGroup.setTextViewChildrenColorAccordingstatus(planned: Boolean, warning: Boolean = false){
        val colorToSet =  when{
            planned -> context.getColorFromAttr(android.R.attr.textColorHighlight)
            warning -> context.getColorFromAttr(R.attr.textColorWarning)
            else -> context.getColorFromAttr(android.R.attr.textColorPrimary)
        }
        this.children.forEach{ v->
            if (v is TextView) {
                v.setTextColor(colorToSet)
            }
        }
    }

    protected fun View.shouldBeVisible(vis: Boolean){
        this.visibility = if (vis) View.VISIBLE else View.GONE
    }
}