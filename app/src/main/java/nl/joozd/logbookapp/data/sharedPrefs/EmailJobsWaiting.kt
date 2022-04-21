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

package nl.joozd.logbookapp.data.sharedPrefs

import android.util.Log
import nl.joozd.logbookapp.data.comm.Cloud
import nl.joozd.logbookapp.extensions.mask


/**
 * Keeps track of which email jobs are to be executed once email is confirmed
 * You can iterate over open jobs, this will take care of flagging for success etc
 */
class EmailJobsWaiting(var maskedValues: Int ): Iterable<suspend () -> Unit> {
    var sendLoginLink: Boolean
        get() = maskedValues and SEND_LOGIN_LINK != 0
        set(loginLinkWaiting){
            maskedValues = if (loginLinkWaiting) maskedValues or SEND_LOGIN_LINK
            else maskedValues mask SEND_LOGIN_LINK
            saveToPrefs()
        }

    var sendBackupCsv: Boolean
        get() = maskedValues and SEND_BACKUP_CSV != 0
        set(backupCsvWaiting){
            maskedValues = if (backupCsvWaiting) maskedValues or SEND_BACKUP_CSV
            else maskedValues mask SEND_BACKUP_CSV
            saveToPrefs()
        }

    private fun saveToPrefs(){
        Prefs.emailJobsWaitingInt = maskedValues
    }

    /**
     * Values for use in Iterator
     */
    private val runSendLoginLink = suspend { Cloud.requestLoginLinkMail(); Unit }
    private val runSendBackupCsv = suspend { Cloud.requestBackup(); Unit }
    private val resetIterator = suspend { iteratorAlreadyRunning = false }

    private val jobMarkers = listOf(sendLoginLink, sendBackupCsv, true) // last true is the job that sets [iteratorAlreadyRunning] to false
    private val possibleJobs = listOf(runSendLoginLink, runSendBackupCsv, resetIterator)
    init{
        require (jobMarkers.size == possibleJobs.size) { "DO NOT ADD JOBS WITHOUT MARKERS OR SCREW UP THEIR ORDER!!!!1"}
    }

    private var iteratorAlreadyRunning = false

    companion object{
        const val SEND_LOGIN_LINK: Int = 0x1
        const val SEND_BACKUP_CSV: Int = 0x2
    }

    /**
     * Returns an iterator over the elements of this object.
     */
    override fun iterator(): Iterator<suspend () -> Unit> = if (!iteratorAlreadyRunning) {
        object : Iterator<suspend () -> Unit> {


            private val openJobs = possibleJobs.filterIndexed { index, _ -> jobMarkers[index] }
            private var pointer = 0

            override fun hasNext(): Boolean = pointer < openJobs.size

            override fun next(): suspend () -> Unit =
                openJobs[pointer++].also{
                    Log.d("emailJobs","running job #$pointer")
                }

        }
    } else object: Iterator<suspend () -> Unit>{
        init{
            Log.w("EmailJobsWaiting", "Do not run two EmailJobsWaiting iterators at the same time. Second one is redirected to an empty Iterator.")
        }
        override fun hasNext(): Boolean  = false
        override fun next(): suspend () -> Unit = {}
    }
}