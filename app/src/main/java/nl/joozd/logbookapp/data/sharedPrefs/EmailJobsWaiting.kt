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

package nl.joozd.logbookapp.data.sharedPrefs

import nl.joozd.logbookapp.extensions.mask

class EmailJobsWaiting(var maskedValues: Int ) {
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
        Preferences._emailJobsWaiting = maskedValues
    }

    companion object{
        const val SEND_LOGIN_LINK: Int = 0x1
        const val SEND_BACKUP_CSV: Int = 0x2
    }
}