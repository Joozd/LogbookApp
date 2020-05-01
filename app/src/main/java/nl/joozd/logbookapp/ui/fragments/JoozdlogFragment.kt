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

package nl.joozd.logbookapp.ui.fragments

import android.R
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import nl.joozd.logbookapp.extensions.getColorFromAttr

/**
 * Contains boilerplate used in all my fragments, including
 * coroutinescope,
 * viewModel,
 * repository, and
 * supportFragmentManager
 * Also, this takes care of keeping track of an undo-function that can be accessed by calling
 * undoChanges() or undoAndClose()
 */
//TODO remove things and replace with viewModel per fragment
open class JoozdlogFragment: Fragment(),  CoroutineScope by MainScope() {
    protected val supportFragmentManager: FragmentManager by lazy { requireActivity().supportFragmentManager }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    protected fun View.joozdLogSetBackgroundColor(color: Int = requireActivity().getColorFromAttr(R.attr.colorPrimary)){
        (this.background as GradientDrawable).colorFilter = PorterDuffColorFilter( color, PorterDuff.Mode.SRC_IN)
    }

    protected fun closeFragment() = supportFragmentManager.popBackStack()
}