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

package nl.joozd.logbookapp.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment


/**
 * Use this to show some text. Clicking anywhere or rotating screen will close this fragment
 */
class MessageDialog: DialogFragment() {
    var title: String? = null
    var titleRes: Int? = null

    var message: String? = null
    var messageRes: Int? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(activity).apply{
        message?.let{ setMessage(it) }
        messageRes?.let { setMessage(it) }

        title?.let{ setTitle(it) }
        titleRes?.let{ setTitle(it) }

        setPositiveButton(android.R.string.ok){ _, _ ->
            // close the dialog, do nothing
        }
    }.create()
    companion object{
        fun make(title: String, msg: String) = MessageDialog().apply{
            this.title = title
            message = msg
        }

        fun make(titleResource: Int, msgResource: Int) = MessageDialog().apply {
            titleRes = titleResource
            messageRes = msgResource
        }

        fun make(msgResource: Int) = MessageDialog().apply {
            messageRes = msgResource
        }

        fun make(msg: String) = MessageDialog().apply{
            message = msg
        }
    }
}