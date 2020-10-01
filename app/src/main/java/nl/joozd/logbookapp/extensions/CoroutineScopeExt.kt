/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2020 Joost Welle
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

package nl.joozd.logbookapp.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlin.coroutines.CoroutineContext

/**
 * Launches a coroutine in the current scope with all Mutex locks in place
 */
fun <T> CoroutineScope.launchWithLocks(vararg mutex: Mutex, block: suspend () -> T ) {
    launch{
        mutex.forEach{
            it.lock()
        }
        try {
            block()
        }
        finally{
            mutex.forEach {
                it.unlock()
            }
        }
    }
}

/**
 * Launches a coroutine with the given CoroutineContext with all Mutex locks in place
 */
fun <T> CoroutineScope.launchWithLocks(context: CoroutineContext, vararg mutex: Mutex, block: suspend () -> T ) =
    launch(context){
        mutex.forEach{
            it.lock()
        }
        try {
            block()
        }
        finally{
            mutex.forEach {
                it.unlock()
            }
        }
    }
