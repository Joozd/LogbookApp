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

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.core.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.background.BackupCenter
import nl.joozd.logbookapp.data.sharedPrefs.CalendarSyncType
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.databinding.ActivitySettingsBinding
import nl.joozd.logbookapp.errors.errorDialog
import nl.joozd.logbookapp.extensions.getStringWithMakeup
import nl.joozd.logbookapp.extensions.makeCsvSharingIntent
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
            setHelpOnClickListeners()

            observeStatus()
            observeSettingsFlows()

            // Set content view
            setContentView(root)
        }
    }


    private fun observeStatus() {
        viewModel.statusFlow.launchCollectWhileLifecycleStateStarted{
            when(it){
                null -> { }

                SettingsActivityStatus.CalendarDialogNeeded -> showFragment<CalendarSyncDialog>(R.id.settingsActivityLayout)

                is SettingsActivityStatus.Error -> errorDialog(getString(it.errorResource))

            }
            if (it != null) viewModel.resetStatus()
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

        viewModel.backupIntervalFlow.launchCollectWhileLifecycleStateStarted{
            backupIntervalButton.text = getBackupIntervalString(it)
        }

        viewModel.sendBackupEmailsFlow.launchCollectWhileLifecycleStateStarted{
            backupFromCloudSwitch.isChecked = it
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
        calendarSyncTypeButton.text = getString(
            when (it) {
                CalendarSyncType.CALENDAR_SYNC_NONE -> R.string.disabled
                CalendarSyncType.CALENDAR_SYNC_DEVICE -> R.string.calendar_this_device
                CalendarSyncType.CALENDAR_SYNC_ICAL -> R.string.ical_link
            }
        )
    }

    private fun getBackupIntervalString(it: Int) = getStringWithMakeup(
        R.string.backup_interval_time, (if (it == 0) getString(
            R.string.never
        ) else getString(R.string.n_days, it.toString()))
    )


    private fun ActivitySettingsBinding.setHelpOnClickListeners() {
        popupTextboxesBackground.setOnClickListener {
            closeAllHintBoxes()
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

        backupIntervalButton.setOnClickListener { showBackupIntervalNumberPicker() }

        backupFromCloudSwitch.setOnClickListener {
            lifecycleScope.launch {
                toggleBackupFromCloudWithDialogIfNeeded()
            }
        }

        backupNowButton.setBackupNowButtonToActive()
    }

    private fun ActivitySettingsBinding.setGroupOpenCloseOnClickListeners() {
        loggingPreferencesSelector.setOnClickListener {
            toggleGeneralPreferencesVisible()
        }

        backupPreferencesSelector.setOnClickListener {
            backupPreferencesLayout.toggleVisibility()
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
                removeFragment(it)
                generalPreferencesContainer.visibility = View.GONE // toggling visibility to GONE makes the activity auto-animate removal of fragment
            }
            else null
        } ?: showGeneralPreferencesFragment()
    }

    private fun ActivitySettingsBinding.showGeneralPreferencesFragment(){
        showFragment<GeneralPreferencesFragment>(R.id.general_preferences_container, tag = GENERAL_PREFERENCES_FRAGMENT_TAG)
        generalPreferencesContainer.visibility = View.VISIBLE // toggling visibility to VISIBLE makes the activity auto-animate insertion of fragment
    }



    private fun Button.shareCsvAndActivateBackupNowButton(
        it: Uri
    ) {
        makeCsvSharingIntent(it)
        setBackupNowButtonToActive()
        viewModel.resetStatus()
    }


    private fun Button.setBackupNowButtonToActive() {
        setOnClickListener {
            setBackupNowButtonToBuildingCsv()
            lifecycleScope.launch {
                shareCsvAndActivateBackupNowButton(BackupCenter.makeBackupUri())
            }
            setText(R.string.backup_now)
        }
    }

    private fun Button.setBackupNowButtonToBuildingCsv() {
        setOnClickListener {  }
        setText(R.string.exporting_csv)
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

    private fun ActivitySettingsBinding.closeAllHintBoxes(){
        createLoginLinkHintCardview.visibility = View.GONE
        popupTextboxesBackground.visibility = View.GONE
    }

    private fun showBackupIntervalNumberPicker(){
        BackupIntervalNumberPicker().apply{
            title = App.instance.getString(R.string.pick_backup_interval)
            wrapSelectorWheel = false
            maxValue = 365

        }.show(supportFragmentManager, null)
    }

    private suspend fun toggleBackupFromCloudWithDialogIfNeeded(){
        when{
            Prefs.sendBackupEmails() -> Prefs.sendBackupEmails(false)
            !viewModel.emailGoodAndVerified() -> showEmailDialog { Prefs.sendBackupEmails(true) }
            else -> Prefs.sendBackupEmails(true)
        }
    }

    /**
     * Show dialog to enter an email address.
     * @param OnSuccess: Extra function to be performed on successfully entering an email address (eg. switching auto backups on after email was entered)
     * NOTE Email backup can be scheduled right away, as it will wait for user to confirm email address.
     */
    private fun showEmailDialog(OnSuccess: () -> Unit = {}) {
        TODO("Show email dialog")
    }


    companion object{
        private const val GENERAL_PREFERENCES_FRAGMENT_TAG = "GENERAL_PREFERENCES_FRAGMENT_TAG"
    }
}
