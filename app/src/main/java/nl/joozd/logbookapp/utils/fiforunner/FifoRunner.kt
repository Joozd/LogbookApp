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

package nl.joozd.logbookapp.utils.fiforunner

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch

/**
 * Makes an Actor in [coroutineScope] for executing [RunnableAction]s one by one.
 */
class FifoRunner(private val coroutineScope: CoroutineScope) {
    private val channel = Channel<RunnableAction>()
    private val runner = makeFifoRunner(channel)

    suspend fun runInTurn(action: RunnableAction){
        channel.send(action)
    }

    suspend fun runInTurn(action: suspend () -> Unit){
        channel.send(BasicRunnableAction(action))
    }

    private fun makeFifoRunner(channel: ReceiveChannel<RunnableAction>) =
        coroutineScope.launch {
            channel.consumeEach { action ->
                action()
            }
        }
}