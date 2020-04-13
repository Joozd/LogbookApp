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

import android.animation.ValueAnimator
import android.view.View
import androidx.appcompat.widget.Toolbar

fun Toolbar.showAnimated(){
    if (this.visibility == View.GONE) {
        this.visibility= View.VISIBLE
        val animator = ValueAnimator.ofInt(0, this.height)
        animator.addUpdateListener {
            this.translationY = 1.0f * it.animatedValue as Int - height
            this.alpha = (it.animatedValue as Int / height.toFloat())

        }
        animator.apply{
            duration=500
            start()
        }
    }

}

fun Toolbar.hideAnimated() {
    if (this.visibility == View.VISIBLE) {
        val animator = ValueAnimator.ofInt(0, this.height)
        animator.addUpdateListener {
            this.translationY = -1.0f * it.animatedValue as Int
            this.alpha = 1.0f - (it.animatedValue as Int / height.toFloat())
            if (it.animatedValue as Int == this.height) this.visibility = View.GONE
        }
        animator.apply{
            duration=500
            start()
        }
    }
}
