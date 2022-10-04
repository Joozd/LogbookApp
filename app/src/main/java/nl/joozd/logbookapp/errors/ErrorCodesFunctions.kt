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
    showErrorDialog(requireActivity(), error)
}

/**
 * Show an errorDialog with a predefined error from a [FragmentActivity]
 * @param error: Predefined error
 * @see Errors
 */
fun FragmentActivity.errorDialog(error: Error) {
    showErrorDialog(this, error)
}

private fun showErrorDialog(activity: FragmentActivity, error: Error){
    JoozdlogAlertDialog().show(activity) {
        titleResource = R.string.error
        message = activity.getString(R.string.error_dialog_message, error.code, error.message)
        setPositiveButton(android.R.string.ok)
    }
}


/**
 * Show an error with a custom code and message from a [FragmentActivity]
 * @param message: Error Message
 * @param code: Error code
 * @param extraData: Extra data
 */
fun FragmentActivity.errorDialog(message: String, code: Int = Errors.UNDEFINED_ERROR.code, extraData: String? = null) = errorDialog(Error(code, message, extraData))