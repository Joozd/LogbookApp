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

package nl.joozd.logbookapp.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.NumberPicker
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.fragment.app.commit
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.comm.UserManagement
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.databinding.ActivitySettingsBinding
import nl.joozd.logbookapp.extensions.getStringWithMakeup
import nl.joozd.logbookapp.extensions.setSelectionWithArrayAdapter
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.SettingsActivityEvents
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.model.viewmodels.activities.SettingsActivityViewModel
import nl.joozd.logbookapp.ui.dialogs.*
import nl.joozd.logbookapp.ui.utils.JoozdlogActivity
import nl.joozd.logbookapp.ui.utils.longToast
import nl.joozd.logbookapp.ui.utils.toast
import nl.joozd.logbookapp.utils.checkPermission

//TODO make standard sim time picker
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

            /****************************************************************************************
             * Populate spinners
             ****************************************************************************************/

            /**
             * Dark mode spinner
             */
            ArrayAdapter.createFromResource(
                activity,
                R.array.dark_mode_choices,
                android.R.layout.simple_spinner_item
            ).apply {
                // Specify the layout to use when the list of choices appears
                setDropDownViewResource(R.layout.spinner_dropdown_item)
            }.also { a ->
                // Apply the adapter to the spinner
                darkModePickerSpinner.apply{
                    adapter = a
                    setSelection(viewModel.defaultNightMode)
                }
            }
            darkModePickerSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        //mainSearchField.text = mainSearchField.text
                        viewModel.darkmodePicked(position)
                    }
                }

            /****************************************************************************************
             * Expand or collapse groups of settings
             ****************************************************************************************/

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


            /****************************************************************************************
             * Logic for setters and other thingies
             ****************************************************************************************/
            settingsUseIataSelector.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setUseIataAirports(isChecked)
            }

            settingsUseConsensusOptIn.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setConsensusOptIn(isChecked)
            }

            settingsMarkInclompleteWithoutPicSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setMarkIncompleteWithoutPIC(isChecked)
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


            autoPostponeCalendarSyncSelector.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setAutoPostponeCalendarSync(isChecked)
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
                startActivity(UserManagement.generateLoginLinkIntent())
            }

            loginLinkExplanationImageView.setOnClickListener {
                showLoginLinkHint()
            }

            useWifiForLargeFilesSwitch.setOnCheckedChangeListener { _, _ ->
                viewModel.useWifiForLargeFilesToggled()
            }

            addNamesFromRosterSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setAddNamesFromRoster(isChecked)
            }

            /*
            //NOT IMPLEMENTED
            addRemarksToChronoUpdatesSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setShowOldTimesOnChronoUpdate(isChecked)
            }
            */

            augmentedCrewButton.setOnClickListener { showAugmentedTimesNumberPicker() }

            backupIntervalButton.setOnClickListener { showBackupIntervalNumberPicker() }

            backupFromCloudSwitch.setOnClickListener {
                toggleBackupFromCloudWithDialogIfNeeded()
                backupFromCloudSwitch.isChecked = Preferences.backupFromCloud
            }

            backupNowButton.setOnClickListener{ viewModel.backUpNow() }

            augmentedTakeoffTimeHintButton.setOnClickListener {
                showAugmentedStartLandingTimesHint()
            }

            popupTextboxesBackground.setOnClickListener {
                closeAllHintBoxes()
            }


            /****************************************************************************************
             * Observers for feedback
             ****************************************************************************************/

            viewModel.feedbackEvent.observe(activity) {
                when (it.getEvent()){
                    SettingsActivityEvents.SIGNED_OUT ->
                        setLoggedInInfo(Preferences.username)
                    SettingsActivityEvents.LOGIN_LINK_COPIED ->
                        toast(R.string.login_link_created)
                    SettingsActivityEvents.NOT_LOGGED_IN ->
                        toast(R.string.error)
                    SettingsActivityEvents.WANT_TO_CREATE_NEW_ACCOUNT_QMK -> showNewAccountDialog()

                    SettingsActivityEvents.CALENDAR_DIALOG_NEEDED -> showCalendarDialog()
                }
            }

            /****************************************************************************************
             * Other observers
             ****************************************************************************************/

            viewModel.csvUriToShare.observe(activity){
                startActivity(Intent.createChooser(Intent().apply{
                    action = Intent.ACTION_SEND
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setDataAndType(it, CSV_MIME_TYPE)
                    putExtra(Intent.EXTRA_STREAM, it)
                }, "Gooi maar ergens heen aub"))
            }



            viewModel.settingsUseIataSelectorTextResource.observe(activity) {
                settingsUseIataSelector.text = getString(it)
            }
            viewModel.useIataAirports.observe(activity) {
                settingsUseIataSelector.isChecked = it
            }

            viewModel.consensusOptIn.observe(activity){
                settingsUseConsensusOptIn.isChecked = it
                consensusDescription.visibility = if (it) View.VISIBLE else View.GONE
            }

            viewModel.picNameNeedsToBeSet.observe(activity){
                settingsMarkInclompleteWithoutPicSwitch.isChecked = it
                markInclompleteWithoutPicText.visibility = if (it) View.VISIBLE else View.GONE
            }

            viewModel.getFlightsFromCalendar.observe(activity){
                settingsGetFlightsFromCalendarSelector.isChecked = it
                showAllCalendarSyncViews(it)
            }

            viewModel.calendarSyncType.observe(activity){
                if (it == 0) // this is when calendar sync is not used or when no selection is made
                    showAllCalendarSyncViews(false)
                else {
                    calendarSyncTypeButton.text = getString(it)
                    showAllCalendarSyncViews(true)
                }

            }

            viewModel.alwaysPostponeCalendarSync.observe(activity){
                autoPostponeCalendarSyncSelector.isChecked = it
            }

            viewModel.useCloudSync.observe(activity) {
                useCloudSyncSwitch.isChecked = it
                if (it) showCloudSyncItems() else hideCloudSyncItems()
            }

            viewModel.lastUpdateTime.observe(activity){
                lastSynchedTimeTextView.text = getStringWithMakeup(R.string.last_synched_at, it)
            }

            viewModel.getNamesFromRosters.observe(activity) {
                addNamesFromRosterSwitch.isChecked = it
            }

            viewModel.username.observe(activity) {
                setLoggedInInfo(it)
            }

            viewModel.emailData.observe(activity) {
                setEmailInfo(it.first, it.second)
            }

            viewModel.calendarDisabled.observe(activity) {
                if (it)
                    showCalendarDisabled()
                else
                    hideCalendarDisabled()
            }

            viewModel.standardTakeoffLandingTimes.observe(activity) {
                augmentedCrewButton.text = getStringWithMakeup(R.string.standard_augmented_time, it.toString())
            }

            viewModel.backupInterval.observe(activity){
                backupIntervalButton.text = getStringWithMakeup(
                    R.string.backup_interval_time, (if (it == 0) getString (
                        R.string.never
                    ) else getString(R.string.n_days, it.toString())))
            }

            viewModel.backupFromCloud.observe(activity){
                backupFromCloudSwitch.isChecked = it
            }

            viewModel.updateLargerFilesOverWifiOnly.observe(activity){
                useWifiForLargeFilesSwitch.isChecked = it
            }

            // Set content view
            setContentView(root)
        }

    }


    //TODO make this happen in viewModel through observer and get rid of [mBinding]
    override fun onResume() {
        super.onResume()
        mBinding.setLoggedInInfo(Preferences.username)
    }

    /**
     * This dialog will ask viewModel to make a new account.
     * If [Preferences.acceptedCloudSyncTerms] it will enable [Preferences.useCloud]
     * if not, it will open a dialog that will allow user to accept terms and if so, sets those both to true.
     */
    private fun showNewAccountDialog(){
        JoozdlogAlertDialog().show(activity){
            titleResource = R.string.cloud_sync_title
            messageResource = R.string.make_new_account_question
            setNegativeButton(android.R.string.cancel)
            setPositiveButton(android.R.string.ok){
                UserManagement.newUser()
                if (Preferences.acceptedCloudSyncTerms)
                    viewModel.forceUseCloud()
                else supportFragmentManager.commit {
                    add(R.id.settingsActivityLayout, CloudSyncTermsDialog(sync = true)) // secondary constructor used, works on recreate
                    addToBackStack(null)
                }
            }
        }
    }

    /**
     * This dialog will ask all info for calendar sync (ical + address, scraper + calendar)
     * If [Preferences.useCalendarSync] is false it will set it to true on OK
     * if not, it will open a dialog that will allow user to accept terms and if so, sets those both to true.
     */
    private fun showCalendarDialog(){
        supportFragmentManager.commit {
            add(R.id.settingsActivityLayout, CalendarSyncDialog())
            addToBackStack(null)
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
    private fun ActivitySettingsBinding.showAllCalendarSyncViews(visible: Boolean){
        val show = if (visible) View.VISIBLE else View.GONE
        calendarSyncTypeButton.visibility = show
        autoPostponeCalendarSyncSelector.visibility = show
        calendarSyncPostponedTextView.visibility = if (viewModel.calendarDisabled.value == true) show else View.GONE
        dontPostponeTextView.visibility = if (viewModel.calendarDisabled.value == true) show else View.GONE
    }


    /**
     * hide "you are logged in" line
     */
    private fun ActivitySettingsBinding.hideCloudSyncItems(){
        youAreSignedInAsButton.visibility=View.GONE
        emailAddressButton.visibility=View.GONE
        lastSynchedTimeTextView.visibility=View.GONE
        changePasswordButton.visibility=View.GONE
        loginLinkButton.visibility = View.GONE
        loginLinkExplanationImageView.visibility = View.GONE
        backupFromCloudSwitch.visibility = View.GONE
    }
    private fun ActivitySettingsBinding.showCloudSyncItems(){
        youAreSignedInAsButton.visibility=View.VISIBLE
        emailAddressButton.visibility=View.VISIBLE
        lastSynchedTimeTextView.visibility=View.VISIBLE
        changePasswordButton.visibility=View.VISIBLE
        val showLoginLinkButton = if (Preferences.username == null) View.GONE else View.VISIBLE
        loginLinkButton.visibility = showLoginLinkButton
        loginLinkExplanationImageView.visibility = showLoginLinkButton
        backupFromCloudSwitch.visibility = View.VISIBLE
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
            setValue(Preferences.standardTakeoffLandingTimes)
        }.show(supportFragmentManager, null)
    }

    private fun showBackupIntervalNumberPicker(){
        BackupIntervalNumberPicker().apply{
            title = App.instance.getString(R.string.pick_backup_interval)
            wrapSelectorWheel = false
            maxValue = 365
            selectedValue = Preferences.backupInterval
        }.show(supportFragmentManager, null)
    }

    private fun toggleBackupFromCloudWithDialogIfNeeded(){
        when{
            Preferences.backupFromCloud -> Preferences.backupFromCloud = false
            !viewModel.emailGoodAndVerified -> showEmailDialog { Preferences.backupFromCloud = true }
            else -> Preferences.backupFromCloud = true
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


    class AugmentedNumberPicker: NumberPickerDialog() {
        fun setValue(value: Int) {
            selectedValue = when (value) {
                in (0..THIRTY) -> value
                in (THIRTY..NINETY) -> THIRTY + (value - 30) / 5
                else -> maxOf(NINETY + (value - 90) / 15, 0)
            }
        }

        /**
         * Override this to actually do something with the picked number
         */
        override fun onNumberPicked(pickedNumber: Int) {
            Preferences.standardTakeoffLandingTimes = unFormat(pickedNumber)
        }

        override val formatter = NumberPicker.Formatter{
            format(it)
        }

        private fun format(value: Int): String = when (value) {
            in (0..THIRTY) -> minutesToHoursAndMinutesString(value)
            in (THIRTY..NINETY) -> minutesToHoursAndMinutesString(30 + (value- THIRTY)*5)
            else -> minutesToHoursAndMinutesString(maxOf(90 + (value- NINETY)*15, 0))
        }

        private fun unFormat(value: Int) = when (value) {
            in (0..THIRTY) -> value
            in (THIRTY..NINETY) -> 30 + (value- THIRTY)*5
            else -> 90 + (value- NINETY) * 15
        }

        companion object {
            private const val THIRTY: Int = 30
            private const val NINETY: Int = 30 + (90-30) / 5
            const val EIGHT_HOURS = NINETY + (8*60 - 90) / 15
        }

    }

    class BackupIntervalNumberPicker: NumberPickerDialog() {
        /**
         * Override this to actually do something with the picked number
         */
        override fun onNumberPicked(pickedNumber: Int) {
            Preferences.backupInterval = pickedNumber
        }

        override val formatter = NumberPicker.Formatter{
            when (it) {
                0 -> App.instance.getString(R.string.never)
                1 -> App.instance.getString(R.string.day)
                else -> App.instance.getStringWithMakeup(R.string.n_days, it.toString()).toString()
            }
        }
    }

    companion object{
        private const val CSV_MIME_TYPE = "text/csv"
    }
}
