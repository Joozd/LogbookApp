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

package nl.joozd.logbookapp.ui.utils

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.snackbar.ContentViewCallback
import nl.joozd.logbookapp.R

class CustomSnackbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), ContentViewCallback {

    init {
        View.inflate(context, R.layout.view_snackbar_custom, this)
    }
    class OnBarShown (private val f: () -> Unit){
        fun actionOnBarShown(){
            f()
        }
    }
    class OnBarGone (private val f: () -> Unit){
        fun actionOnBarGone(){
            f()
        }
    }

    var onBarShown: OnBarShown? = null
    var onBarGone: OnBarGone? = null


    override fun animateContentIn(delay: Int, duration: Int) {
        onBarShown?.actionOnBarShown()
    }

    override fun animateContentOut(delay: Int, duration: Int) {
        onBarGone?.actionOnBarGone()
        Log.d("iets anders", "nog iets")

    }

}