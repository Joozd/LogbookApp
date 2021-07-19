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

package nl.joozd.logbookapp.utils

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.ui.dialogs.JoozdlogAlertDialog

object ErrorCodes {
    val FOUNDLINK_IS_NULL = 1 to "CalendarSyncDialogViewModel.foundLink == null"
}



fun Fragment.errorDialog(code: Pair<Int, String>) {
    val ctx = this
    JoozdlogAlertDialog().show(requireActivity()){
        titleResource = R.string.error
        message = ctx.getString(R.string.error_dialog_message, code.first, code.second)
        setPositiveButton(android.R.string.ok)
    }
}

fun FragmentActivity.errorDialog(code: Pair<Int, String>) {
    val ctx = this
    JoozdlogAlertDialog().show(this) {
        titleResource = R.string.error
        message = ctx.getString(R.string.error_dialog_message, code.first, code.second)
        setPositiveButton(android.R.string.ok)
    }
}