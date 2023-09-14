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

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import nl.joozd.logbookapp.R
import nl.joozd.joozdlogcommon.AugmentedCrew
import nl.joozd.logbookapp.extensions.getColorFromAttr
import nl.joozd.logbookapp.model.ModelFlight

abstract class FlightsListItemViewHolder(containerView: View): RecyclerView.ViewHolder(containerView) {
    abstract fun bindItem(
        flight: ModelFlight,
        useIata: Boolean = false,
        picNameMustBeSet: Boolean = false,
        onClick: (ModelFlight) -> Unit,
        onDelete: (ModelFlight) -> Unit
    )

    protected fun ModelFlight.checkIfIncomplete(picNameMustBeSet: Boolean): Boolean =
        orig.ident.isBlank()
                || dest.ident.isBlank()
                || aircraft.type == null
                || aircraft.registration.isBlank()
                || (picNameMustBeSet && name.isBlank())

    protected fun ModelFlight.namesString(): String{
        val names = (listOf(name) + name2).filter { it.isNotBlank() }
        return if (names.size <=2) names.joinToString(", ")
        else names.take(2).joinToString(", ") + " + ${names.size - 2}"
    }

    protected fun ModelFlight.isAugmented() =
        AugmentedCrew.fromInt(augmentedCrew).isAugmented

    protected fun ModelFlight.isIfr() =
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