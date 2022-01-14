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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.SettingsActivityEvents
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.databinding.ActivityNewUserPageCalendarBinding
import nl.joozd.logbookapp.model.viewmodels.activities.NewUserActivityViewModel
import nl.joozd.logbookapp.ui.dialogs.CalendarSyncDialog
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment

/**
 * Calendar import
 */
class NewUserActivityCalendarPage: JoozdlogFragment() {
    val pageNumber = NewUserActivityViewModel.PAGE_CALENDAR

    val viewModel: NewUserActivityViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ActivityNewUserPageCalendarBinding.bind(layoutInflater.inflate(R.layout.activity_new_user_page_calendar, container, false)).apply {


            /*******************************************************************************************
             * OnClickedListeners
             *******************************************************************************************/

            /**
             * When useCalendarImportSwitch is clicked, viewModel will open UseCalendarDialog if switching to on,
             * or switch [Preferences.useCalendarSync] when switching to off.
             * This will intitially also set switch state to [Preferences.useCalendarSync],
             *      which will update later when that gets updated through viewModel.getFlightsFromCalendar.observe
             */
            useCalendarImportSwitch.setOnClickListener {
                viewModel.setGetFlightsFromCalendarClicked()
                useCalendarImportSwitch.isChecked = Preferences.useCalendarSync
            }


            /*******************************************************************************************
             * Observers
             *******************************************************************************************/

            // set useCalendarImportSwitch and "Continue" button enabled to it
            viewModel.getFlightsFromCalendar.observe(viewLifecycleOwner){
                useCalendarImportSwitch.isChecked = it
                viewModel.setNextButtonEnabled(pageNumber, it)
            }

            /**
             * Observe feedback from viewModel
             */
            viewModel.getFeedbackChannel(pageNumber).observe(viewLifecycleOwner){
                when (it.getEvent()){
                    SettingsActivityEvents.CALENDAR_DIALOG_NEEDED -> showCalendarDialogNoSync()
                }
            }

        }.root

    /**
     * This dialog will ask all info for calendar sync (ical + address, scraper + calendar)
     * If [Preferences.useCalendarSync] is false it will set it to true on OK
     * if not, it will open a dialog that will allow user to accept terms and if so, sets those both to true.
     */
    private fun showCalendarDialogNoSync(){
        supportFragmentManager.commit {
            add(R.id.newUserActivityLayout, CalendarSyncDialog(syncAfter = false))
            addToBackStack(null)
        }
    }
}