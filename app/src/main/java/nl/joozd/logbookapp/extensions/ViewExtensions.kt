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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.textfield.TextInputEditText


val View.ctx: Context
    get() = context

/*
extends a view (assumes said View's visibility is "gone", quickly fade that in, and
 */

fun View.fadeIn(vararg fadeOutView: View) {
    val shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
    this.apply {
        // Set the content view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.
        alpha = 0f
        visibility = View.VISIBLE

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        animate()
            .alpha(1f)
            .setDuration(shortAnimationDuration)
            .setListener(null)
    }
    for (v in fadeOutView){
        v.animate()
            .alpha(0f)
            .setDuration(shortAnimationDuration)
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

var TextView.textColor: Long
    get() {
        return this.currentTextColor.toLong()
    }
    set(value) {
        this.setTextColor(value.toInt())
    }

fun View.setBackgroundColor(c: Long) = this.setBackgroundColor(c.toInt())


fun TextInputEditText.onTextChanged(text: (String) -> Unit) = (this as EditText).onTextChanged(text)

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

/*
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
*/

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

val View.activity: Activity?
    get() {
    var context: Context? = getContext()
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

fun View.setMargins(left: Int, top: Int, right: Int, bottom: Int) {
    if (layoutParams is MarginLayoutParams) {
        val p = layoutParams as MarginLayoutParams
        p.setMargins(left, top, right, bottom)
        requestLayout()
    }
}

val View.xPositionOnScreen: Int
    get() = IntArray(2).also { getLocationOnScreen(it) }[0]

val View.yPositionOnScreen: Int
    get() = IntArray(2).also { getLocationOnScreen(it) }[1]

/**
 * Constrain top of a View in a ConstraintView to another view.
 * @param anchor: View to connect to
 * @param margin: Margin in px or dp
 */
fun View.constrainTopToBottom(anchor: View, margin: String){
    fun Int.dpToPixels() = this.toFloat() * resources.displayMetrics.density
    val regex = "(\\d+)(px|dp)".toRegex()
    require (regex matches margin) { "Margin must be in px or dp (ie. \"3dp\" )"}
    val (v, t) = regex.find(margin)!!.destructured
    val distance = when(t){
        "px" -> v.toInt()
        "dp" -> v.toInt().dpToPixels().toInt()
        else -> error ("Margin must be in px or dp (ie. \"3dp\" )")
    }
    constrainTopToBottom(anchor, distance)
}

/**
 * Constrain top of a View in a ConstraintView to another view.
 * @param anchor: View to connect to
 * @param margin: Margin in px
 */
fun View.constrainTopToBottom(anchor: View, margin: Int){
    require (parent is ConstraintLayout) { "View Parent must be ConstrainLayout" }
    val parent = parent as ConstraintLayout
    val viewToConstrain = this
    with (ConstraintSet()){
        clone(parent)
        clear(viewToConstrain.id, ConstraintSet.TOP)
        connect(viewToConstrain.id, ConstraintSet.TOP, anchor.id, ConstraintSet.BOTTOM, margin)
        applyTo(parent)
    }
}


/**
 * Contrain top of a view to anchor's top, and bottom to bottom
 */
fun View.constrainToCenterVertical(anchor: View){
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


/*
fun View.setWidth(width: Int){
    layoutParams.width = width
    invalidate()
    requestLayout()
}

 */