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
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.google.android.material.textfield.TextInputEditText


val View.ctx: Context
    get() = context

fun View.setVisibilityVisibleAndFadeIn() {
    val duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
    visibility = View.VISIBLE
    animateFadeIn(duration)
}

private fun View.animateFadeIn(duration: Long) {
    animate()
        .alpha(1f)
        .setDuration(duration)
        .setListener(null)
}

var TextView.textColor: Long
    get() = currentTextColor.toLong()
    set(value) {
        this.setTextColor(value.toInt())
    }

fun View.setBackgroundColor(c: Long) = this.setBackgroundColor(c.toInt())

fun TextInputEditText.onTextChanged(text: (String) -> Unit) = (this as EditText).onTextChanged(text)

fun EditText.onTextChanged(text: (String) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            //intentionally left blank
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            text.invoke(p0.toString())
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

fun View.constrainTopToBottomOf(anchor: View, marginInPixels: Int){
    require (parent is ConstraintLayout) { "View Parent must be ConstrainLayout" }
    val constraintLayout = parent as ConstraintLayout
    val viewToConstrain = this
    with (ConstraintSet()){
        clone(constraintLayout)
        clear(viewToConstrain.id, ConstraintSet.TOP)
        connect(viewToConstrain.id, ConstraintSet.TOP, anchor.id, ConstraintSet.BOTTOM, marginInPixels)
        applyTo(constraintLayout)
    }
}

fun View.constrainTopToTopAndBottomToBottomOf(anchor: View){
    require (parent is ConstraintLayout) { "View Parent must be ConstrainLayout" }
    val parent = parent as ConstraintLayout
    val viewToConstrain = this
    with (ConstraintSet()){
        clone(parent)
        clear(viewToConstrain.id, ConstraintSet.TOP)
        clear(viewToConstrain.id, ConstraintSet.BOTTOM)
        connect(viewToConstrain.id, ConstraintSet.TOP, anchor.id, ConstraintSet.TOP)
        connect(viewToConstrain.id, ConstraintSet.BOTTOM, anchor.id, ConstraintSet.BOTTOM)
        applyTo(parent)
    }
}