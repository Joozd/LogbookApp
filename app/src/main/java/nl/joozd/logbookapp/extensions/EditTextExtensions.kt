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

package nl.joozd.logbookapp.extensions

import android.util.Log
import android.widget.EditText

/**
 * Selects all digits that any text entered in this EditText ends with.
 * eg. abc123de456 will select "456"
 */
fun EditText.removeTrailingDigits(){
    val currentText = text.toString()
    if (currentText.isEmpty()) return
    var trailingDigits: Int = 0
    while (currentText.dropLast(trailingDigits).last().isDigit()) trailingDigits++
    //We now know how many digits this EditText's text ends with (spoiler: it's [trailingDigits]

    setText(text.toString().dropLast(trailingDigits))
    //text.delete(text.length-trailingDigits, text.length)
}

fun EditText.setTextIfNotFocused(text: CharSequence?){
    if(!isFocused) setText(text ?: "")
}

fun EditText.setTextIfNotFocused(resource: Int){
    if(!isFocused) setText(resource)
}