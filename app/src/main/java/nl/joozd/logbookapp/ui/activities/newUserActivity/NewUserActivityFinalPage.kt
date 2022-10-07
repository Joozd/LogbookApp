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

package nl.joozd.logbookapp.ui.activities.newUserActivity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.metadata.Version
import nl.joozd.logbookapp.data.sharedPrefs.BackupPrefs
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.data.sharedPrefs.toggle
import nl.joozd.logbookapp.databinding.ActivityNewUserPageFinalBinding
import java.time.Instant

/**
 * Final puntjes op de i
 */
class NewUserActivityFinalPage: NewUseractivityPage() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ActivityNewUserPageFinalBinding.bind(layoutInflater.inflate(R.layout.activity_new_user_page_final, container, false)).apply {
            icaoIataSwitch.bindToFlow(Prefs.useIataAirports.flow)
            launchCollectUseIataFlow()

            /*******************************************************************************************
             * OnClickListeners
             *******************************************************************************************/

            icaoIataSwitch.setOnClickListener {
                Prefs.useIataAirports.toggle()
            }

            doneButton.setOnClickListener {
                lifecycleScope.launch{
                    markBackupAsDoneNow() // so users won't start with a "you haven't backed up yet" message
                    Prefs.configuredVersion.valueBlocking = Version.currentVersion // make sure this is taken care of before launching MainActivity, so it won't jump straight back to NewUserActivity
                    newUserActivity.finish()
                }
            }
        }.root

    private fun ActivityNewUserPageFinalBinding.launchCollectUseIataFlow(){
        Prefs.useIataAirports.flow.launchCollectWhileLifecycleStateStarted{
            icaoIataSwitch.setText(if (it) R.string.useIataAirports else R.string.useIcaoAirports)
        }
    }

    private fun markBackupAsDoneNow(){
        BackupPrefs.mostRecentBackup(Instant.now().epochSecond)
    }
}