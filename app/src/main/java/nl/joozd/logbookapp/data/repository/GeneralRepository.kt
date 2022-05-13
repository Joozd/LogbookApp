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

package nl.joozd.logbookapp.data.repository

import kotlinx.coroutines.CoroutineScope
import nl.joozd.logbookapp.utils.delegates.dispatchersProviderMainScope
import nl.joozd.logbookapp.core.JoozdlogWorkersHubOld

/**
 * For doing I/O things that are not related to flights, airports, aircraft or balances forward
 */
object GeneralRepository: CoroutineScope by dispatchersProviderMainScope() {
    fun synchTimeWithServer(){
        JoozdlogWorkersHubOld.synchronizeTime()
    }
}