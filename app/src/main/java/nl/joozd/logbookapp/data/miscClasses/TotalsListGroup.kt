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

package nl.joozd.logbookapp.data.miscClasses

import android.os.Parcel
import android.os.Parcelable

// The idea is that the ExpandableListAdapter for total times wil get a list of TotalsListGroups, each containing a header name, and a list of items.
// Each item consists (for now) of a name (eg. ME-Piston) and a value in minutes (eg. 523)

class TotalsListItem(val valueName: String, val totalTime: Long)

class TotalsListGroup(val title: String, var items: List<TotalsListItem>)