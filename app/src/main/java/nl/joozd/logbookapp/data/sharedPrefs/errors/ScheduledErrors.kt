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

package nl.joozd.logbookapp.data.sharedPrefs.errors

import nl.joozd.logbookapp.data.sharedPrefs.Prefs

/**
 * Errors that need showing at some time but are not really time sensitive
 * [currentErrors] holds a list of all errors that need showing
 */
object ScheduledErrors {
    val currentErrors: List<Errors>
        get() =  Errors.values().filter { Prefs.errorsToBeShown and it.flag != 0L }

    /**
     * Add an error. Use errors in [Errors]
     * If an error already active, this will do nothing
     * @param error: Error to make active
     */
    fun addError(error: Errors){
        Prefs.errorsToBeShown = Prefs.errorsToBeShown or error.flag
    }

    /**
     * Clear an error. Use errors in [Errors]
     * If an error is not present, nothing will happen
     * @param error: Error to make clear
     */
    fun clearError(error: Errors){
        Prefs.errorsToBeShown = Prefs.errorsToBeShown.mask(error.flag)
    }

    /**
     * Bitmask. All bits that are 1 in [mask] will be set to 0 in [this]
     * All bits that are 0 in [mask] will be unaffected in [this]
     * Basically a "nand" operation.
     */
    private fun Long.mask (mask: Long): Long = this and mask.inv()

}