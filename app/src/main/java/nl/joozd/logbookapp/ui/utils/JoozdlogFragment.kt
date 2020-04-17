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
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import nl.joozd.logbookapp.data.dataclasses.Flight
import nl.joozd.logbookapp.data.room.Repository
import nl.joozd.logbookapp.data.viewmodel.DialogViewmodel
import nl.joozd.logbookapp.data.viewmodel.JoozdlogViewModel

/**
 * Contains boilerplate used in all my fragments, including
 * coroutinescope,
 * viewModel,
 * repository, and
 * supportFragmentManager
 * Also, this takes care of keeping track of an undo-function that can be accessed by calling
 * undoChanges() or undoAndClose()
 */

open class JoozdlogFragment: Fragment(),  CoroutineScope by MainScope() {
    protected val viewModel: JoozdlogViewModel by activityViewModels()
    protected val dialogViewModel: DialogViewmodel by viewModels()
    protected val repository = Repository.getInstance()
    protected val supportFragmentManager: FragmentManager by lazy { requireActivity().supportFragmentManager }

    /**
     * Reference to the flight being worked on (viewModel.workingFlight)
     */
    protected var flight: Flight
        get() = viewModel.workingFlight.value!!
        set(f) { viewModel.workingFlight.value = f }

    /**
     * Reference to a backup-flight for dialogs.
     * Supports only one dialog at a time.
     */
    protected var unchangedFlight
        get() = dialogViewModel.unchangedFlight
        set(f) { dialogViewModel.unchangedFlight = f }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    protected fun closeFragment() = supportFragmentManager.popBackStack()

    /**
     * Override this to trigger upon changing of [flight]
     * 2 versions available; with or without updated flight as parameter
     *
     */
    protected open fun setViews(v: View?){
        // intentionally left blank
    }
    protected open fun setViews(v: View?, f: Flight){
        // intentionally left blank
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)


        // set "unchangedFlight" to flight if this is the first run (to prevent this re-happening on screen rotations etc)
        val firstRun = savedInstanceState?.getInt(ALREADY_STARTED) != 1
        if (firstRun) unchangedFlight = flight
        viewModel.distinctWorkingFlight.observe(viewLifecycleOwner, Observer {
            setViews(this.view)
            setViews(this.view, it)
        })
    }

    /**
     * reverts all changes since first creation of the JoozdlogFragment.
     * Will throw and error if unchangedFlight == null
     */
    protected fun undoChanges() = unchangedFlight.let {
        require (it != null)
        flight = it
    }

    protected fun undoAndClose(){
        unchangedFlight?.let { flight = it }
        supportFragmentManager.popBackStack()
    }

    protected fun saveAndClose(){
        supportFragmentManager.popBackStack()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(ALREADY_STARTED, 1)
        super.onSaveInstanceState(outState)
    }

    companion object{
        const val ALREADY_STARTED = "joozdLogFragmentAlreadyStarted"
    }

}