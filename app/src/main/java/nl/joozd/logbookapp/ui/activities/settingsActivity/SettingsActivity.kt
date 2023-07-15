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

package nl.joozd.logbookapp.ui.activities.settingsActivity

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.commit
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.sharedPrefs.CalendarSyncType
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.databinding.ActivitySettingsBinding
import nl.joozd.logbookapp.errors.errorDialog
import nl.joozd.logbookapp.extensions.getStringWithMakeup
import nl.joozd.logbookapp.extensions.showFragment
import nl.joozd.logbookapp.model.viewmodels.activities.settingsActivity.SettingsActivityViewModel
import nl.joozd.logbookapp.model.viewmodels.status.SettingsActivityStatus
import nl.joozd.logbookapp.ui.dialogs.*
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity

class SettingsActivity : JoozdlogActivity() {
    private lateinit var mBinding: ActivitySettingsBinding
    val viewModel: SettingsActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        mBinding = ActivitySettingsBinding.inflate(layoutInflater).apply {

            setSupportActionBarWithReturn(settingsToolbar)?.apply {
                setDisplayShowHomeEnabled(true)
                setDisplayHomeAsUpEnabled(true)
                title = getString(R.string.settings)
            }

            setGroupOpenCloseOnClickListeners()
            setItemOnClickedListeners()

            observeControlFlows()
            observeSettingsFlows()

            // Set content view
            setContentView(root)
        }
    }

    override fun onStop() {
        // Clear focus from anything focussed, so any editTexts that are still open will trigger their onFocusChangedListeners
        currentFocus?.clearFocus()
        super.onStop()
    }


    private fun observeControlFlows() {
        viewModel.statusFlow.launchCollectWhileLifecycleStateStarted{
            when(it){
                null -> { }

                SettingsActivityStatus.CalendarDialogNeeded -> showFragment<CalendarSyncDialog>(R.id.settingsActivityLayout)

                is SettingsActivityStatus.Error -> errorDialog(getString(it.errorResource))
            }
            if (it != null) viewModel.resetStatus()
        }

        viewModel.showHintFlow.launchCollectWhileLifecycleStateStarted{
            it?.let { showHintDialog(it) }
        }
    }

    private fun ActivitySettingsBinding.observeSettingsFlows(){
        viewModel.useCalendarSyncFlow.launchCollectWhileLifecycleStateStarted{
            settingsGetFlightsFromCalendarSelector.isChecked = it
            showAllCalendarSyncViews(it)
        }

        viewModel.alwaysPostponeCalendarSyncFlow.launchCollectWhileLifecycleStateStarted{
            autoPostponeCalendarSyncSelector.isChecked = it
        }


        viewModel.calendarSyncTypeFlow.launchCollectWhileLifecycleStateStarted{
            setCalendarSyncTypeButtonText(it)
            showAllCalendarSyncViews(it != CalendarSyncType.CALENDAR_SYNC_NONE)
        }

        viewModel.calendarDisabledFlow.launchCollectWhileLifecycleStateStarted{
            if (it)
                showCalendarDisabled()
            else
                hideCalendarDisabled()
        }

        viewModel.getNamesFromRostersFlow.launchCollectWhileLifecycleStateStarted{
            addNamesFromRosterSwitch.isChecked = it
        }
    }

    private fun ActivitySettingsBinding.setCalendarSyncTypeButtonText(it: CalendarSyncType) {
        calendarSyncTypeButton.text = when (it) {
            CalendarSyncType.CALENDAR_SYNC_NONE -> getString(R.string.disabled)
            CalendarSyncType.CALENDAR_SYNC_DEVICE -> getStringWithMakeup(R.string.source_with_placeholder, getString(R.string.calendar_this_device)) // placeholder while loading
                .also{// actual text, loaded async but probably instant
                    Prefs.selectedCalendar.flow.launchCollectWhileLifecycleStateStarted{calendarName ->
                        calendarSyncTypeButton.text = getStringWithMakeup(R.string.source_with_placeholder, calendarName)
                    }
            }
            CalendarSyncType.CALENDAR_SYNC_ICAL -> getStringWithMakeup(R.string.source_with_placeholder, getString(R.string.calendar_this_device)) // placeholder while loading
                .also{// actual text, loaded async but probably instant
                    Prefs.calendarSyncIcalAddress.flow.launchCollectWhileLifecycleStateStarted{calendarName ->
                        calendarSyncTypeButton.text = getStringWithMakeup(R.string.source_with_placeholder, calendarName)
                    }
                }
        }
    }

    private fun ActivitySettingsBinding.setItemOnClickedListeners() {
        settingsGetFlightsFromCalendarSelector.setOnClickListener {
            viewModel.setGetFlightsFromCalendarClicked()
        }

        calendarSyncTypeButton.setOnClickListener {
            supportFragmentManager.commit {
                add(R.id.settingsActivityLayout, CalendarSyncDialog())
                addToBackStack(null)
            }
        }

        autoPostponeCalendarSyncSelector.setOnClickListener {
            viewModel.toggleAutoPostponeCalendarSync()
        }

        dontPostponeTextView.setOnClickListener {
            viewModel.dontPostponeCalendarSync()
        }

        addNamesFromRosterSwitch.setOnClickListener {
            viewModel.toggleAddNamesFromRoster()
        }

    }

    private fun ActivitySettingsBinding.setGroupOpenCloseOnClickListeners() {
        loggingPreferencesSelector.setOnClickListener {
            toggleGeneralPreferencesVisible()
        }

        backupPreferencesSelector.setOnClickListener {
            toggleBackupPreferencesVisible()
        }

        syncPreferencesSelector.setOnClickListener {
            syncPreferencesLayout.toggleVisibility()
        }

        pdfPreferencesSelector.setOnClickListener {
            pdfPreferencesLayout.toggleVisibility()
        }
    }

    private fun ActivitySettingsBinding.toggleGeneralPreferencesVisible(){
        supportFragmentManager.findFragmentByTag(GENERAL_PREFERENCES_FRAGMENT_TAG)?.let{
            if (it.isResumed) {
                generalPreferencesContainer.visibility = View.GONE // toggling visibility to GONE makes the activity auto-animate removal of fragment
                removeFragment(it)
            }
            else null
        } ?: showGeneralPreferencesFragment()
    }

    private fun ActivitySettingsBinding.toggleBackupPreferencesVisible(){
        supportFragmentManager.findFragmentByTag(BACKUP_PREFERENCES_FRAGMENT_TAG)?.let{
            if (it.isResumed) {
                backupPreferencesContainer.visibility = View.GONE // toggling visibility to GONE makes the activity auto-animate removal of fragment
                removeFragment(it)
            }
            else null // if fragment is found but not started, do the same as if it wasn't found (findFragmentByTag?.let() returns null)
        } ?: showBackupPreferencesFragment()
    }


    private fun ActivitySettingsBinding.showGeneralPreferencesFragment(){
        showFragment<GeneralPreferencesFragment>(R.id.general_preferences_container, tag = GENERAL_PREFERENCES_FRAGMENT_TAG)
        generalPreferencesContainer.visibility = View.VISIBLE // toggling visibility to VISIBLE makes the activity auto-animate insertion of fragment
    }

    private fun ActivitySettingsBinding.showBackupPreferencesFragment(){
        showFragment<BackupPreferencesFragment>(R.id.backup_preferences_container, tag = BACKUP_PREFERENCES_FRAGMENT_TAG)
        backupPreferencesContainer.visibility = View.VISIBLE // toggling visibility to VISIBLE makes the activity auto-animate insertion of fragment
    }

    // Expects a Resource ID as parameter
    private fun showHintDialog(message: Int){
        AlertDialog.Builder(this).apply{
            setMessage(message)
            setPositiveButton(android.R.string.ok){ _, _ -> viewModel.hintShown() } // Tell viewModel not to provide this hint on recreate
        }
    }



    /***********************************************************************************************
     * Private functions for changing layout
     ***********************************************************************************************/

    /**
     * show or hide a group of Views
     */
    private fun ConstraintLayout.toggleVisibility() {
        visibility = if (this.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    /**
     * Shows/hides Calendar controls if calendar is en/disabled.
     * Has some extra logic for things that should be hidden even if the rest is allowed to be shown again
     */
    private suspend fun ActivitySettingsBinding.showAllCalendarSyncViews(visible: Boolean){
        val show = if (visible) View.VISIBLE else View.GONE
        calendarSyncTypeButton.visibility = show
        autoPostponeCalendarSyncSelector.visibility = show
        calendarSyncPostponedTextView.visibility = if (viewModel.calendarDisabled()) show else View.GONE
        dontPostponeTextView.visibility = if (viewModel.calendarDisabled()) show else View.GONE
    }

    private suspend fun ActivitySettingsBinding.showCalendarDisabled(){
        calendarSyncPostponedTextView.text = getString(R.string.disabled_until, viewModel.calendarDisabledUntilString())
        calendarSyncPostponedTextView.visibility = View.VISIBLE
        dontPostponeTextView.visibility = View.VISIBLE
    }

    private fun ActivitySettingsBinding.hideCalendarDisabled(){
        calendarSyncPostponedTextView.visibility = View.GONE
        dontPostponeTextView.visibility = View.GONE
    }




    companion object{
        private const val GENERAL_PREFERENCES_FRAGMENT_TAG = "GENERAL_PREFERENCES_FRAGMENT_TAG"
        private const val BACKUP_PREFERENCES_FRAGMENT_TAG = "BACKUP_PREFERENCES_FRAGMENT_TAG"
    }
}
