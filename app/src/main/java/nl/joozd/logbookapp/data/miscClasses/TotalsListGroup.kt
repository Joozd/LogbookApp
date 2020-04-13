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

import android.os.Parcel
import android.os.Parcelable

// The idea is that the ExpandableListAdapter for total times wil get a list of TotalsListGroups, each containing a header name, and a list of items.
// Each item consists (for now) of a name (eg. ME-Piston) and a value in minutes (eg. 523)

class TotalsListItem(val valueName: String, val totalTime: Long) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readLong()
    )


    constructor(valueName: String, totalTime: Int): this(valueName, totalTime.toLong())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(valueName)
        parcel.writeLong(totalTime)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TotalsListItem> {
        override fun createFromParcel(parcel: Parcel): TotalsListItem {
            return TotalsListItem(parcel)
        }

        override fun newArray(size: Int): Array<TotalsListItem?> {
            return arrayOfNulls(size)
        }
    }
}

class TotalsListGroup(val title: String, var items: List<TotalsListItem>) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.createTypedArrayList(TotalsListItem) ?: emptyList()
    )


    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeTypedList(items)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TotalsListGroup> {
        override fun createFromParcel(parcel: Parcel): TotalsListGroup {
            return TotalsListGroup(parcel)
        }

        override fun newArray(size: Int): Array<TotalsListGroup?> {
            return arrayOfNulls(size)
        }
    }
}