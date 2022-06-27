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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.data.sharedPrefs.toggle
import nl.joozd.logbookapp.databinding.ActivityNewUserPageCalendarBinding
import nl.joozd.logbookapp.ui.dialogs.CalendarSyncDialog

/**
 * Calendar import
 */
class NewUserActivityCalendarPage: NewUseractivityPage() {
    private lateinit var _binding: ActivityNewUserPageCalendarBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ActivityNewUserPageCalendarBinding.bind(layoutInflater.inflate(R.layout.activity_new_user_page_calendar, container, false)).apply {
            println("at onCreateView: ${Prefs.useCalendarSync.valueBlocking}")
            useCalendarImportSwitch.bindToFlow(Prefs.useCalendarSync.flow)
            collectCalendarSyncFlow()
            useCalendarImportSwitch.setOnClickListener {
                println("Clicked useCalendarImportSwitch")
                println("before action: ${Prefs.useCalendarSync.valueBlocking}")
                disableCalendarSyncOrShowDialog()

            }
        }
        return _binding.root
    }

    private fun ActivityNewUserPageCalendarBinding.collectCalendarSyncFlow(){
        Prefs.useCalendarSync.flow.launchCollectWhileLifecycleStateStarted{
            continueButton.setText(if (it) R.string._continue else R.string.dont_use)
        }
    }

    private fun disableCalendarSyncOrShowDialog(){
        lifecycleScope.launch {
            if (Prefs.useCalendarSync())
                Prefs.useCalendarSync.toggle()
            else
                showCalendarDialog()
        }
    }

    private fun showCalendarDialog(){
        supportFragmentManager.commit {
            add(R.id.newUserActivityLayout, CalendarSyncDialog())
            addToBackStack(null)
        }
    }
}