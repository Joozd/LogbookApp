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

package nl.joozd.logbookapp.ui.dialogs.popups

import android.app.Activity
import android.app.AlertDialog
import kotlinx.android.synthetic.main.alert_edit_text.view.*
import nl.joozd.logbookapp.R

/**
 * Will make a view with an editTextbox called "editText"
 */
class SimpleEditTextAlert(activity: Activity): AlertDialog.Builder(activity){
    val inflater = activity.layoutInflater
    private val editTextView = inflater.inflate(R.layout.alert_edit_text, null)
    val editText = editTextView.textInputEditText

    fun setHint(hint: String){
        editTextView.textInputLayout.hint = hint
    }

    override fun show(): AlertDialog {
        setView(editTextView)
        return super.show()
    }

}