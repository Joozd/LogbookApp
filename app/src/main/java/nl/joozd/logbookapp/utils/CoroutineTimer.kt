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

package nl.joozd.logbookapp.utils

import kotlinx.coroutines.*
import java.time.Duration
import java.time.Instant

/**
 * Runs something after [delayMillis] milliseconds.
 * if [repeating] it will keep doing that untill [cancel] is called.
 * Can be constructed with an [Instant] which will result in running once at approximately that instant and won't repeat.
 * Can be constructed with a [Duration], can be repeated then.
 *
 * A [CoroutineTimerTask] can be (re)started with [run] and stopped with [cancel] at any time.
 */
class CoroutineTimerTask(val delayMillis: Long, val repeating: Boolean = false) {
    constructor(runAtInstant: Instant): this(delayMillis = (Instant.now().toEpochMilli() - runAtInstant.toEpochMilli()).abs(), repeating = false)
    constructor(duration: Duration, repeating: Boolean): this(duration.toMillis(), repeating)

    private var _job: Job = Job()

    val isActive = _job.isActive

    fun cancel(){
        _job.cancel()
    }

    fun run(scope: CoroutineScope, action: () -> Unit) {
        _job.cancel()
        _job = scope.launch{
            do {
                delay(delayMillis)
                if (isActive)
                    action()
            } while (isActive && repeating)
        }
    }

    companion object{
        private fun Long.abs() = if (this > 0) this else this * -1
    }
}