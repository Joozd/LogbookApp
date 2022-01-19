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

package nl.joozd.logbookapp.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

object DispatcherProvider {
    private val getMain = { Dispatchers.Main }
    private val getIO = { Dispatchers.IO }
    private val getDefault = { Dispatchers.Default }
    private val getUnconfined = { Dispatchers.Unconfined }

    var main: () -> CoroutineDispatcher = getMain; private set
    var io: () -> CoroutineDispatcher = getIO; private set
    var default: () -> CoroutineDispatcher = getDefault; private set
    var unconfined: () -> CoroutineDispatcher = getUnconfined; private set

    fun switchToTestDispatchers(testDispatcher: CoroutineDispatcher){
        main = { testDispatcher }
        io = { testDispatcher }
        default = { testDispatcher }
        unconfined = { testDispatcher }
    }

    fun switchToNormalDispatchers() {
        main = getMain
        io = getIO
        default = getDefault
        unconfined = getUnconfined
    }
}