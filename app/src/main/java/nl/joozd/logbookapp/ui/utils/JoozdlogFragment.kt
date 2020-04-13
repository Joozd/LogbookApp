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

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import nl.joozd.logbookapp.data.dataclasses.Flight
import nl.joozd.logbookapp.data.room.Repository
import nl.joozd.logbookapp.data.viewmodel.JoozdlogViewModel

/**
 * Contains boilerplate used in all my fragments, including
 * coroutinescope,
 * viewModel,
 * repository, and
 * supportFragmentManager
 */

open class JoozdlogFragment: Fragment(),  CoroutineScope by MainScope() {
    protected val viewModel: JoozdlogViewModel by activityViewModels()
    protected val repository = Repository.getInstance()
    protected val supportFragmentManager: FragmentManager by lazy { requireActivity().supportFragmentManager }

    /**
     * Reference to the flight being worked on (viewModel.workingFlight)
     */
    protected var flight
        get() = viewModel.workingFlight.value!!
        set(f: Flight) { viewModel.workingFlight.value = f }

    /**
     * Reference to a backup-flight for dialogs.
     * Supports only one dialog at a time.
     */
    protected var unchangedFlight
        get() = viewModel.unchangedFlightForUseInDialogs
        set(f: Flight?) { viewModel.unchangedFlightForUseInDialogs = f }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    protected fun closeFragment() = supportFragmentManager.popBackStack()
}