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

package nl.joozd.logbookapp.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.DialogMessageBinding
import nl.joozd.logbookapp.ui.fragments.JoozdlogFragment


/**
 * Use this to show some text. Clicking anywhere or rotating screen will close this fragment
 */
class MessageDialog: JoozdlogFragment() {
    var message: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        DialogMessageBinding.bind(inflater.inflate(R.layout.dialog_message, container, false)).apply {
            if (message == null) closeFragment() // just kill the fragment if no message set.
            messageView.text = message?: "NO TEXT"
            messageDialogBackground.setOnClickListener { closeFragment() }
        }.root

    companion object{
        fun make(msg: String) = MessageDialog().apply{
            message = msg
        }
        fun make(msgResource: Int) = MessageDialog().apply {
            message = ctx.getString(msgResource)
        }

        private const val MESSAGE = "MESSAGE"
    }
}