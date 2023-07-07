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

package nl.joozd.logbookapp.extensions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.children
import com.google.android.material.textfield.TextInputEditText

var TextView.textColor: Long
    get() = currentTextColor.toLong()
    set(value) {
        this.setTextColor(value.toInt())
    }

fun View.setBackgroundColor(c: Long) = this.setBackgroundColor(c.toInt())

fun TextInputEditText.onTextChanged(text: (String) -> Unit) = (this as EditText).onTextChanged(text)

fun EditText.onTextChanged(text: (String) -> Unit) {
    val v = this // for logging. Can't reference this@onTextChanged because override function also has that name.
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            //intentionally left blank
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            p0?.let { text(it.toString()) }
                ?: run { Log.w("onTextChanged", "text changed to null for $v")}
        }

        override fun afterTextChanged(editable: Editable?) {
            //intentionally left blank
        }
    })
}

//Find the Activity this View is a part of
fun View.findActivity(): Activity? {
    var ctx: Context? = context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) {
            return ctx
        }
        ctx = ctx.baseContext
    }
    return null
}


/**
 * Finds a TextView in a view that is either a TextView itself, or a (nested) ViewGroup with at least one textview in it.
 * Null if none found.
 */
fun View.findTextView(): TextView?{
    if (this is TextView) return this
    if (this is ViewGroup)
        return children.firstNotNullOfOrNull { child -> child.findTextView() }
    return null
}

fun EditText.setOnFocusLostListener(l: OnFocusLostListener){
    setOnFocusChangeListener { _, hasFocus ->
        if (!hasFocus) l.onFocusLost(this)
    }
}

fun interface OnFocusLostListener{
    fun onFocusLost(editText: EditText)
}