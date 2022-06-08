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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.core.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.core.background.BackupCenter
import nl.joozd.logbookapp.core.usermanagement.UserManagement
import nl.joozd.logbookapp.data.sharedPrefs.CalendarSyncType
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.databinding.ActivitySettingsBinding
import nl.joozd.logbookapp.errors.errorDialog
import nl.joozd.logbookapp.extensions.getStringWithMakeup
import nl.joozd.logbookapp.extensions.makeCsvSharingIntent
import nl.joozd.logbookapp.extensions.toDateStringLocalized
import nl.joozd.logbookapp.extensions.toTimeStringLocalized
import nl.joozd.logbookapp.model.viewmodels.activities.settingsActivity.SettingsActivityViewModel
import nl.joozd.logbookapp.model.viewmodels.status.SettingsActivityStatus
import nl.joozd.logbookapp.ui.activities.ChangePasswordActivity
import nl.joozd.logbookapp.ui.dialogs.*
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity
import nl.joozd.logbookapp.ui.utils.toast
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

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

            initializeDarkModeSpinner()

            setGroupOpenCloseOnClickListeners()
            setItemOnClickedListeners()
            setHelpOnClickListeners()

            observeStatus()
            observeSettingsFlows()

            // Set content view
            setContentView(root)
        }

    }



    private fun ActivitySettingsBinding.observeStatus() {
        viewModel.statusFlow.launchCollectWhileLifecycleStateStarted{
            when(it){
                null -> { }
                SettingsActivityStatus.SignedOut -> setLoggedInInfo(Prefs.username)
                SettingsActivityStatus.LoginLinkCopied -> toast(R.string.login_link_created)
                SettingsActivityStatus.AskIfNewAccountNeeded -> showNewAccountDialog()
                SettingsActivityStatus.CalendarDialogNeeded -> showCalendarDialog()

                is SettingsActivityStatus.Error -> errorDialog(getString(it.errorResource))

            }
            if (it != null) viewModel.resetStatus()
        }
    }

    private fun ActivitySettingsBinding.observeSettingsFlows(){
        viewModel.picNameRequiredFlow.launchCollectWhileLifecycleStateStarted{
            settingsPicNameRequiredSwitch.isChecked = it
            picNameRequiredText.visibility = if (it) View.VISIBLE else View.GONE
        }

        viewModel.useIataFlow.launchCollectWhileLifecycleStateStarted {
            setSettingsUseIataSelector(it)

        }

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

        viewModel.backupFromCloudFlow.launchCollectWhileLifecycleStateStarted{
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

        viewModel.standardTakeoffLandingTimesFlow.launchCollectWhileLifecycleStateStarted{
            augmentedCrewButton.text = getStringWithMakeup(R.string.standard_augmented_time, it.toString())
        }


        viewModel.useCloudFlow.launchCollectWhileLifecycleStateStarted{
            useCloudSyncSwitch.isChecked = it
            changeCloudSyncItemsVisibility(if (it) View.VISIBLE else View.GONE)
        }

        viewModel.usernameFlow.launchCollectWhileLifecycleStateStarted{
            setLoggedInInfo(it)
        }

        viewModel.emailDataFlow.launchCollectWhileLifecycleStateStarted{
            setEmailInfo(it.first, it.second)
        }

        viewModel.lastUpdateTimeFlow.launchCollectWhileLifecycleStateStarted{
            lastSynchedTimeTextView.text = getStringWithMakeup(R.string.last_synched_at, makeTimeString(it))
        }

        viewModel.getNamesFromRostersFlow.launchCollectWhileLifecycleStateStarted{
            addNamesFromRosterSwitch.isChecked = it
        }
    }

    private fun ActivitySettingsBinding.setSettingsUseIataSelector(it: Boolean) {
        settingsUseIataSelector.setText(if (it) R.string.useIataAirports else R.string.useIcaoAirports)
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
        loginLinkExplanationImageView.setOnClickListener {
            showLoginLinkHint()
        }

        augmentedTakeoffTimeHintButton.setOnClickListener {
            showAugmentedStartLandingTimesHint()
        }

        popupTextboxesBackground.setOnClickListener {
            closeAllHintBoxes()
        }
    }

    private fun ActivitySettingsBinding.setItemOnClickedListeners() {
        settingsUseIataSelector.setOnClickListener{
            viewModel.toggleUseIataAirports()
            setSettingsUseIataSelector(Prefs.useIataAirports)
        }

        settingsPicNameRequiredSwitch.setOnClickListener {
            viewModel.toggleRequirePicName()
            settingsPicNameRequiredSwitch.isChecked = Prefs.picNameNeedsToBeSet
        }

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
            autoPostponeCalendarSyncSelector.isChecked = Prefs.alwaysPostponeCalendarSync
        }

        dontPostponeTextView.setOnClickListener {
            viewModel.dontPostponeCalendarSync()
        }

        useCloudSyncSwitch.setOnClickListener {
            viewModel.useCloudSyncToggled()
        }

        youAreSignedInAsButton.setOnClickListener {
            viewModel.copyLoginLinkToClipboard()
        }

        emailAddressButton.setOnClickListener {
            showEmailDialog() // no extra will default to {}
        }

        changePasswordButton.setOnClickListener {
            startActivity(Intent(activity, ChangePasswordActivity::class.java))
        }

        loginLinkButton.setOnClickListener {
            UserManagement().generateLoginLinkMessage()?.let{
                sendMessageToOtherApp(it, getString(R.string.login_link_title))
            } ?: toast(R.string.not_signed_in_bug_please_tell_joozd)
        }

        addNamesFromRosterSwitch.setOnClickListener {
            viewModel.toggleAddNamesFromRoster()
            addNamesFromRosterSwitch.isChecked = Prefs.getNamesFromRosters
        }

        augmentedCrewButton.setOnClickListener { showAugmentedTimesNumberPicker() }

        backupIntervalButton.setOnClickListener { showBackupIntervalNumberPicker() }

        backupFromCloudSwitch.setOnClickListener {
            toggleBackupFromCloudWithDialogIfNeeded()
            backupFromCloudSwitch.isChecked = Prefs.backupFromCloud
        }

        backupNowButton.setBackupNowButtonToActive()
    }

    private fun ActivitySettingsBinding.setGroupOpenCloseOnClickListeners() {
        loggingPreferencesSelector.setOnClickListener {
            loggingPreferencesLayout.toggleVisibility()
        }

        backupPreferencesSelector.setOnClickListener {
            backupPreferencesLayout.toggleVisibility()
        }

        syncPreferencesSelector.setOnClickListener {
            syncPreferencesLayout.toggleVisibility()
        }

        cloudPreferencesSelector.setOnClickListener {
            cloudPreferencesLayout.toggleVisibility()
        }

        pdfPreferencesSelector.setOnClickListener {
            pdfPreferencesLayout.toggleVisibility()
        }
    }

    private fun ActivitySettingsBinding.initializeDarkModeSpinner() {
        darkModePickerSpinner.adapter = ArrayAdapter.createFromResource(
            activity,
            R.array.dark_mode_choices,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }

        darkModePickerSpinner.setSelection(viewModel.defaultNightMode)

        darkModePickerSpinner.onItemSelectedListener = darkModeSelectedListener
    }

    private val darkModeSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) { }
        override fun onItemSelected(p: AdapterView<*>?, v: View?, position: Int,id: Long) {
            viewModel.darkmodePicked(position)
        }
    }

    /**
     * This dialog will ask viewModel to make a new account.
     * If [Prefs.acceptedCloudSyncTerms] it will enable [Prefs.useCloud]
     * if not, it will open a dialog that will allow user to accept terms and if so, sets those both to true.
     */
    private fun showNewAccountDialog(){
        JoozdlogAlertDialog().show(activity){
            titleResource = R.string.cloud_sync_title
            messageResource = R.string.make_new_account_question
            setNegativeButton(android.R.string.cancel)
            setPositiveButton(android.R.string.ok) {
                lifecycleScope.launch {
                    UserManagement().createNewUser()
                }
                if (Prefs.acceptedCloudSyncTerms)
                    viewModel.forceUseCloud()
                else supportFragmentManager.commit {
                    add(
                        R.id.settingsActivityLayout,
                        CloudSyncTermsDialog(sync = true)
                    ) // secondary constructor used, works on recreate
                    addToBackStack(null)
                }
            }
        }
    }

    /**
     * This dialog will ask all info for calendar sync (ical + address, scraper + calendar)
     * If [Prefs.useCalendarSync] is false it will set it to true on OK
     * if not, it will open a dialog that will allow user to accept terms and if so, sets those both to true.
     */
    private fun showCalendarDialog(){
        supportFragmentManager.commit {
            add(R.id.settingsActivityLayout, CalendarSyncDialog())
            addToBackStack(null)
        }
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
    private fun ActivitySettingsBinding.showAllCalendarSyncViews(visible: Boolean){
        val show = if (visible) View.VISIBLE else View.GONE
        calendarSyncTypeButton.visibility = show
        autoPostponeCalendarSyncSelector.visibility = show
        calendarSyncPostponedTextView.visibility = if (viewModel.calendarDisabled) show else View.GONE
        dontPostponeTextView.visibility = if (viewModel.calendarDisabled) show else View.GONE
    }

    private fun ActivitySettingsBinding.changeCloudSyncItemsVisibility(isVisible: Int){
        youAreSignedInAsButton.visibility=isVisible
        emailAddressButton.visibility=isVisible
        lastSynchedTimeTextView.visibility=isVisible
        changePasswordButton.visibility=isVisible
        val showLoginLinkButton = if (Prefs.username == null) View.GONE else isVisible
        loginLinkButton.visibility = showLoginLinkButton
        loginLinkExplanationImageView.visibility = showLoginLinkButton
        backupFromCloudSwitch.visibility = isVisible
        backupFromCloudDescription.visibility = isVisible
    }

    private fun ActivitySettingsBinding.setLoggedInInfo(name: String?){
        youAreSignedInAsButton.text = getStringWithMakeup(R.string.signed_in_as, name ?: getString(R.string.tbd))
        val showLoginLinkButton = if (name == null) View.GONE else View.VISIBLE
        loginLinkButton.visibility = showLoginLinkButton
        loginLinkExplanationImageView.visibility = showLoginLinkButton
    }

    private fun ActivitySettingsBinding.setEmailInfo(emailAddress: String?, verified: Boolean){
        emailAddressButton.text = if (verified) getStringWithMakeup(R.string.email_button_text_verified, emailAddress ?: getString(R.string.no_email_provided))
            else getStringWithMakeup(R.string.email_button_text_not_verified, emailAddress ?: getString(R.string.no_email_provided))
    }

    private fun ActivitySettingsBinding.showCalendarDisabled(){
        calendarSyncPostponedTextView.text = getString(R.string.disabled_until, viewModel.calendarDisabledUntilString)
        calendarSyncPostponedTextView.visibility = View.VISIBLE
        dontPostponeTextView.visibility = View.VISIBLE
    }

    private fun ActivitySettingsBinding.hideCalendarDisabled(){
        calendarSyncPostponedTextView.visibility = View.GONE
        dontPostponeTextView.visibility = View.GONE
    }

    private fun ActivitySettingsBinding.showLoginLinkHint(){
        createLoginLinkHintCardview.visibility = View.VISIBLE
        popupTextboxesBackground.visibility = View.VISIBLE
    }

    private fun ActivitySettingsBinding.showAugmentedStartLandingTimesHint(){
        augmentedStartLandingTimesHintCardview.visibility = View.VISIBLE
        popupTextboxesBackground.visibility = View.VISIBLE
    }


    private fun ActivitySettingsBinding.closeAllHintBoxes(){
        augmentedStartLandingTimesHintCardview.visibility = View.GONE
        createLoginLinkHintCardview.visibility = View.GONE
        popupTextboxesBackground.visibility = View.GONE
    }

    private fun showAugmentedTimesNumberPicker(){
        AugmentedNumberPicker().apply {
            title= App.instance.getString(R.string.timeForTakeoffLanding)
            wrapSelectorWheel = false
            maxValue = AugmentedNumberPicker.EIGHT_HOURS
            setValue(Prefs.standardTakeoffLandingTimes)
        }.show(supportFragmentManager, null)
    }

    private fun showBackupIntervalNumberPicker(){
        BackupIntervalNumberPicker().apply{
            title = App.instance.getString(R.string.pick_backup_interval)
            wrapSelectorWheel = false
            maxValue = 365
            selectedValue = Prefs.backupInterval
        }.show(supportFragmentManager, null)
    }

    private fun toggleBackupFromCloudWithDialogIfNeeded(){
        when{
            Prefs.backupFromCloud -> Prefs.backupFromCloud = false
            !viewModel.emailGoodAndVerified -> showEmailDialog { Prefs.backupFromCloud = true }
            else -> Prefs.backupFromCloud = true
        }
    }

    /**
     * Show dialog to enter an email address.
     * @param extra: Extra function to be performed on successfully entering an email address (eg. switching auto backups on after email was entered)
     */
    private fun showEmailDialog(extra: () -> Unit = {}) {
        supportFragmentManager.commit {
            add(R.id.settingsActivityLayout, EmailDialog(extra), null)
            addToBackStack(null)
        }
    }

    private fun makeTimeString(instant: Long): String =
        if (instant < 0) "Never"
        else  LocalDateTime.ofInstant(Instant.ofEpochSecond(instant), ZoneOffset.UTC).let{
            "${it.toDateStringLocalized()} ${it.toTimeStringLocalized()}Z"
        }
}
