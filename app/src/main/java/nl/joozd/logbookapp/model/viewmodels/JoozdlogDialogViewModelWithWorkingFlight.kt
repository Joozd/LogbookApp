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

package nl.joozd.logbookapp.model.viewmodels

/**
 * This gets to be used for when a workingFlight must be set.
 * If it is not set, it will cause a NullPointerException.
 * For non-WorkingFlight viewModel use JoozdlogDialogViewModel()
 */
abstract class JoozdlogDialogViewModelWithWorkingFlight: JoozdlogDialogViewModel() {
    protected val workingFlight = flightRepository.wf
    /**
     * Set undo values on initial construction
     * If multiple Dialogs are opened at the same time, this will get overwritten
     */
    protected val snapshot = workingFlight.toFlight()

    /**
     * Undo all changes made after starting this dialog (including changes made in other dialogs or EditFlightFragment)
     * Can be overridden by a custom undo function
     */
    open fun undo() = workingFlight.setFromFlight(snapshot)
}