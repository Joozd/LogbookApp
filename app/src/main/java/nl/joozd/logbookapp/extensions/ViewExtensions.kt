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

package nl.joozd.logbookapp.extensions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout


val View.ctx: Context
    get() = context

/*
extends a view (assumes said View's visibility is "gone", quickly fade that in, and
 */

fun View.fadeIn(vararg fadeOutView: View) {
    var shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)
    this.apply {
        // Set the content view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.
        alpha = 0f
        visibility = View.VISIBLE

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        animate()
            .alpha(1f)
            .setDuration(shortAnimationDuration.toLong())
            .setListener(null)
    }
    for (v in fadeOutView){
        v.animate()
            .alpha(0f)
            .setDuration(shortAnimationDuration.toLong())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    v.visibility = View.GONE
                }
            })
    }
}

fun View.fadeOut() {
    val shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)
    this.apply {
        // Animate the content view to 0% opacity
        animate()
            .alpha(0f)
            .setDuration(shortAnimationDuration.toLong())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = View.GONE
                }
            })
    }
}

fun View.animateToZeroHeight() {
    val shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)
    val orinigalHeight = height

    val animator = ValueAnimator.ofInt(orinigalHeight, 0)
    animator.addUpdateListener {
        val newHeight = it.animatedValue as Int
        layoutParams.height = newHeight
        layoutParams = layoutParams
    }
    animator.apply {
        duration = 128
        start()
    }

}

fun TextView.setLayoutToOff(){
    val color= (this.textColor % 0x01000000)
    this.textColor=(0x20000000 + color)
    this.setTypeface(null, Typeface.NORMAL)
}

fun TextView.setLayoutToOn(){
    val color= (this.textColor % 0x01000000)
    this.textColor=(0xCC000000 + color)
    this.setTypeface(null, Typeface.BOLD)
}


var TextView.textColor: Long
    get() {
        return this.currentTextColor.toLong()
    }
    set(value: Long) {
        this.setTextColor(value.toInt())
    }

fun View.setBackgroundColor(c: Long) = this.setBackgroundColor(c.toInt())

fun EditText.onFocusChange(v: View, hasFocus: Boolean){
    if (hasFocus){
        this.hint = this.text
        this.setText("")
    }
    else{
        if (this.text.toString() == "") {
            this.setText(this.hint)
        }
    }
}

fun EditText.onTextChanged(text: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            text.invoke(p0.toString())
        }

        override fun afterTextChanged(editable: Editable?) {
        }
    })
}

fun EditText.onTextChanged(before: (String) -> Unit, during: (String) -> Unit, after: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            during.invoke(p0.toString())
        }

        override fun afterTextChanged(editable: Editable?) {
        }
    })
}

internal fun View?.findSuitableParent(): ViewGroup? {
    var view = this
    var fallback: ViewGroup? = null
    do {
        if (view is CoordinatorLayout) {
            // We've found a CoordinatorLayout, use it
            return view
        } else if (view is FrameLayout) {
            if (view.id == android.R.id.content) {
                // If we've hit the decor content view, then we didn't find a CoL in the
                // hierarchy, so use it.
                return view
            } else {
                // It's not the content view but we'll use it as our fallback
                fallback = view
            }
        }

        if (view != null) {
            // Else, we will loop and crawl up the view hierarchy and try to find a parent
            val parent = view.parent
            view = if (parent is View) parent else null
        }
    } while (view != null)

    // If we reach here then we didn't find a CoL or a suitable content view so we'll fallback
    return fallback
}

fun View.getActivity(): Activity? {
    var context: Context? = getContext()
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}