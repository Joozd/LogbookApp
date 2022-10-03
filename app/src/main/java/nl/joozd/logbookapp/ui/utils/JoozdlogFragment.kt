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
import android.text.Editable
import android.view.View
import android.widget.CompoundButton
import android.widget.EditText
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.first
import nl.joozd.logbookapp.core.App
import kotlin.coroutines.CoroutineContext

/**
 * Contains boilerplate used in all my fragments, including
 * viewModel,
 * repository, and
 * supportFragmentManager
 */
//TODO remove things and replace with viewModel per fragment
abstract class JoozdlogFragment: Fragment() {
    protected val supportFragmentManager: FragmentManager get() = requireActivity().supportFragmentManager
    protected val fragment: JoozdlogFragment
        get() = this
    protected val ctx: Context
        get() = App.instance

    /**
     * dp to pixels and reverse
     */

    protected fun Float.pixelsToDp() = this / resources.displayMetrics.density
    protected fun Float.dpToPixels() = this * resources.displayMetrics.density
    protected fun Int.pixelsToDp() = this.toFloat() / resources.displayMetrics.density
    protected fun Int.dpToPixels() = this.toFloat() * resources.displayMetrics.density

    protected fun removeFragment() = supportFragmentManager.commit { remove(fragment) }

    protected open fun closeFragment() {
        if (getTopFragment() == this)
            supportFragmentManager.popBackStack()
        else removeFragment()
    }

    /**
     * Disable the "back" button so you cannot close a fragment by pressing it
     */
    protected fun disableBackPressed() = requireActivity().onBackPressedDispatcher.addCallback(this) {
            // fragment BackPressed will be disabled because nothing happens here.
    }

    protected fun recreate(){
        supportFragmentManager.commit{ detach(fragment) }
        supportFragmentManager.commit{ attach(fragment) }
    }

    fun <T> Flow<T>.launchCollectWhileLifecycleStateStarted(collector: FlowCollector<T>){
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                collect(collector)
            }
        }
    }

    protected fun <T> Flow<T>.launchCollectWhileLifecycleStateStarted(coroutineContext: CoroutineContext, collector: FlowCollector<T>){
        lifecycleScope.launch(coroutineContext) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                collect(collector)
            }
        }
    }

    private var textToBeReplacedWhenNoDataEntered: Editable? = null
    /*
     * When an editText gains focus, save its contents in here.
     * When it loses focus, replace it again.
     * The idea is that data in editTexts will only be changed from viewModel,
     * and entering anything is meant as data to be sent to viewModel, not for displaying.
     * If any data is entered, do [action] on it.
     * This is to be placed in an [EditText.onFocusChanged] block.
     * NOTE: No text entered means no action, so don't use this for fields that can be made empty.
     * @param hasFocus -> true if View gained focus, false it lost focus
     * @param action -> action to be performed when any text is entered.
     */
    private inline fun EditText.separateDataDisplayAndEntry(
        hasFocus: Boolean,
        action: (Editable?) -> Unit
    ) {
        if (!hasFocus) {
            val newText = text
            textToBeReplacedWhenNoDataEntered?.let { text = it }
            if (!newText.isNullOrBlank())
                action(newText)
        }
        else{
            textToBeReplacedWhenNoDataEntered = text
            setText("")
        }
    }

    /**
     * This separates ENTRY and DISPLAY functions of an edittext.
     * Any text that is in the field when it gains focus will be placed in the field again after focus is lost.
     * Any text that is entered in this wield will sent to [action]
     * @param action: The action to be performed on the entered data
     */
    protected fun EditText.separateDataDisplayAndEntry(action: (Editable?) -> Unit){
        onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            separateDataDisplayAndEntry(hasFocus, action)
        }
    }


    /**
     * Bind a CompoundButton (e.g. a Switch) to a Flow<Boolean>.
     * The only way to change this buttons state after calling this function is to change the output of [flow]
     */
    protected fun CompoundButton.bindToFlow(flow: Flow<Boolean>){
        setOnCheckedChangeListener { _, _ ->
            lifecycleScope.launch {
                flow.first().let{
                    if (isChecked != it)
                        isChecked = it
                }
            }
        }
        flow.launchCollectWhileLifecycleStateStarted{
            println("COLLECTED: $it from $flow")
            isChecked = it
        }
    }

    private fun getTopFragment(): Fragment? =
        with (supportFragmentManager) {
            return when (backStackEntryCount) {
                0 -> null
                else -> findFragmentById(getBackStackEntryAt(backStackEntryCount - 1).id)
            }
        }

}