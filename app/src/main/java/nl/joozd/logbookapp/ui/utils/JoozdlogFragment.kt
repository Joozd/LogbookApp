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

package nl.joozd.logbookapp.ui.utils

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.extensions.getColorFromAttr
import nl.joozd.logbookapp.utils.delegates.dispatchersProviderMainScope

/**
 * Contains boilerplate used in all my fragments, including
 * coroutinescope,
 * viewModel,
 * repository, and
 * supportFragmentManager
 */
//TODO remove things and replace with viewModel per fragment
abstract class JoozdlogFragment: Fragment(),  CoroutineScope by dispatchersProviderMainScope() {
    protected val supportFragmentManager: FragmentManager by lazy { requireActivity().supportFragmentManager }
    protected val fragment: JoozdlogFragment
        get() = this
    protected val ctx: Context
        get() = App.instance

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    @Deprecated("Use styled background", ReplaceWith("style=\"@style/DialogBodyLayout\""))
    protected fun View.joozdLogSetBackgroundColor(color: Int = requireActivity().getColorFromAttr(R.attr.colorPrimary)){
        (this.background as GradientDrawable).colorFilter = PorterDuffColorFilter( color, PorterDuff.Mode.SRC_IN)
    }

    /**
     * dp to pixels and reverse
     */

    protected fun Float.pixelsToDp() = this / resources.displayMetrics.density
    protected fun Float.dpToPixels() = this * resources.displayMetrics.density
    protected fun Int.pixelsToDp() = this.toFloat() / resources.displayMetrics.density
    protected fun Int.dpToPixels() = this.toFloat() * resources.displayMetrics.density


    protected fun removeFragment() = supportFragmentManager.commit { remove(fragment) }

    protected open fun closeFragment() = supportFragmentManager.popBackStack()

    /**
     * Disable the "back" button so you cannot close a fragment by pressing it
     */
    protected fun disableBackPressed() = requireActivity().onBackPressedDispatcher.addCallback(this) {
            // With blank your fragment BackPressed will be disabled.
    }

    protected fun recreate(){
        supportFragmentManager.commit{ detach(fragment) }
        supportFragmentManager.commit{ attach(fragment) }
    }

    protected  fun <T> Flow<T>.launchCollectWhileLifecycleStateStarted(collector: FlowCollector<T>){
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                collect(collector)
            }
        }
    }

}