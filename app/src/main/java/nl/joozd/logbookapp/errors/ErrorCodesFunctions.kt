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

package nl.joozd.logbookapp.errors

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.ui.dialogs.JoozdlogAlertDialog

/**
 * Show an errorDialog with a predefined error from a [Fragment]
 * @param error: Predefined error
 * @see Errors
 */
fun Fragment.errorDialog(error: Error) {
    val ctx = this
    JoozdlogAlertDialog().show(requireActivity()){
        titleResource = R.string.error
        message = ctx.getString(R.string.error_dialog_message, error.code, error.message)
        setPositiveButton(android.R.string.ok)
    }
}

/**
 * Show an errorDialog with a predefined error from a [FragmentActivity]
 * @param error: Predefined error
 * @see Errors
 */
fun FragmentActivity.errorDialog(error: Error) {
    val ctx = this
    JoozdlogAlertDialog().show(this) {
        titleResource = R.string.error
        message = ctx.getString(R.string.error_dialog_message, error.code, error.message)
        setPositiveButton(android.R.string.ok)
    }
}

/**
 * Show an error with a custom code and message from a [Fragment]
 * @param message: Error Message
 * @param code: Error code
 * @param extraData: Extra data
 * Good practice is to always use error code -1 for this, otherwise make an [Errors] entry
 */
fun Fragment.errorDialog(message: String, code: Int = -1, extraData: String? = null) = errorDialog(Error(code, message, extraData))

/**
 * Show an error with a custom code and message from a [FragmentActivity]
 * @param message: Error Message
 * @param code: Error code
 * @param extraData: Extra data
 * Good practice is to always use error code -1 for this, otherwise make an [Errors] entry
 */
fun FragmentActivity.errorDialog(message: String, code: Int = -1, extraData: String? = null) = errorDialog(Error(code, message, extraData))