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
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import nl.joozd.logbookapp.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.databinding.ActivitySettingsBinding
import nl.joozd.logbookapp.extensions.getStringWithMakeup
import nl.joozd.logbookapp.extensions.setSelectionWithArrayAdapter
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.SettingsActivityEvents
import nl.joozd.logbookapp.model.helpers.FlightDataPresentationFunctions.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.model.viewmodels.activities.SettingsActivityViewModel
import nl.joozd.logbookapp.ui.dialogs.NumberPickerDialog
import nl.joozd.logbookapp.ui.utils.longToast
import nl.joozd.logbookapp.ui.utils.toast
import nl.joozd.logbookapp.utils.checkPermission

class SettingsActivity : JoozdlogActivity() {
    private lateinit var mBinding: ActivitySettingsBinding
    val viewModel: SettingsActivityViewModel by viewModels()


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            CALENDAR_PERMISSION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED))
                    fillCalendarsList(mBinding)
                else {
                    //TODO make this a dialog, use R strings
                    longToast("No permission for calendar access")
                }
                return
            }
            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                toast("DEBUG: warning 1")
                // Ignore all other requests.
            }
        }
    }

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

            ArrayAdapter(
                activity,
                R.layout.spinner_item,
                arrayListOf<String>()
            ).apply {
                // Specify the layout to use when the list of choices appears
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }.also { adapter ->
                // Apply the adapter to the spinner
                settingsCalendarPickerSpinner.adapter = adapter
            }
            settingsCalendarPickerSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        //mainSearchField.text = mainSearchField.text
                        viewModel.calendarPicked(position)
                    }
                }
/*
            ArrayAdapter.createFromResource(
                activity,
                R.array.supported_calendars,
                android.R.layout.simple_spinner_item
            ).apply {
                // Specify the layout to use when the list of choices appears
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }.also { aa ->
                // Apply the adapter to the spinner
                settingsCalendarTypeSpinner.apply {
                    adapter = aa
                    if (Preferences.calendarType >= 0) setSelection(Preferences.calendarType)
                }
            }
            settingsCalendarTypeSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        //mainSearchField.text = mainSearchField.text
                        viewModel.calendarTypePicked(position)
                    }
                }

*/
            /****************************************************************************************
             * Logic for setters and other thingies
             ****************************************************************************************/
            settingsUseIataSelector.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setUseIataAirports(isChecked)
            }

            settingsGetFlightsFromCalendarSelector.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setGetFlightsFromCalendar(isChecked)
            }

            dontPostponeTextView.setOnClickListener {
                viewModel.dontPostponeCalendarSync()
            }

            useCloudSyncSwitch.setOnClickListener {
                viewModel.useCloudSyncToggled()
            }

            youAreSignedInAsButton.setOnClickListener {
                startActivity(Intent(activity, LoginActivity::class.java))
            }

            changePasswordButton.setOnClickListener {
                startActivity(Intent(activity, ChangePasswordActivity::class.java))
            }

            loginLinkButton.setOnClickListener {
                viewModel.copyLoginLinkToClipboard()
            }

            loginLinkExplanationImageView.setOnClickListener {
                showLoginLinkHint()
            }

            addRemarksToChronoUpdatesSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setShowOldTimesOnChronoUpdate(isChecked)
            }

            augmentedCrewButton.setOnClickListener { showAugmentedTimesNumberPicker() }

            backupIntervalButton.setOnClickListener { showBackupIntervalNumberPicker() }

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

            viewModel.getFlightsFromCalendar.observe(activity) {
                settingsGetFlightsFromCalendarSelector.isChecked = it
                if (it)
                    fillCalendarsList(this)
                else {
                    settingsCalendarPickerSpinner.visibility = View.GONE
                    // settingsCalendarTypeSpinner.visibility = View.GONE
                }
            }

            viewModel.useCloudSync.observe(activity) {
                useCloudSyncSwitch.isChecked = it
                if (it) showLoggedInButton() else hideLoggedInButton()
            }

            viewModel.lastUpdateTime.observe(activity){
                lastSynchedTimeTextView.text = getStringWithMakeup(R.string.last_synched_at, it)
            }

            viewModel.showOldTimesOnChronoUpdate.observe(activity) {
                addRemarksToChronoUpdatesSwitch.isChecked = it
            }

            viewModel.foundCalendars.observe(activity) {
                @Suppress("UNCHECKED_CAST")
                (settingsCalendarPickerSpinner.adapter as ArrayAdapter<String>).apply {
                    clear()
                    addAll(it)
                }
                settingsCalendarPickerSpinner.setSelection(it.indexOf(Preferences.selectedCalendar))
            }

            viewModel.selectedCalendar.observe(activity) {
                if (it != null) settingsCalendarPickerSpinner.setSelectionWithArrayAdapter(it.name)
                Log.d(this::class.simpleName, "triggered AAAA, $it")
            }

            // viewModel.pickedCalendarType.observe(this, Observer {
            //     binding.settingsCalendarTypeSpinner.setSelection(it)
            // })

            viewModel.username.observe(activity, Observer {
                setLoggedInInfo(it)
            })

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


            // Set content view
            setContentView(root)
        }

    }

    override fun onResume() {
        super.onResume()
        mBinding.setLoggedInInfo(Preferences.username)
    }

    private fun fillCalendarsList(binding: ActivitySettingsBinding){
        with (binding) {
            Log.d(this::class.simpleName, "started fillCalendarsList()")
            if (!checkPermission(Manifest.permission.READ_CALENDAR)) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.READ_CALENDAR),
                    CALENDAR_PERMISSION_REQUEST_CODE
                )
                settingsCalendarPickerSpinner.visibility = View.GONE
                // settingsCalendarTypeSpinner.visibility = View.GONE
            } else {
                settingsCalendarPickerSpinner.visibility = View.VISIBLE
                // Not used at the moment but it's there when we need it
                // TAG: #SETTHISIFNEEDED1
                // settingsCalendarTypeSpinner.visibility = View.VISIBLE
                viewModel.fillCalendarsList()
            }
        }
    }

    /***********************************************************************************************
     * Private functions for changing layout
     ***********************************************************************************************/

    /**
     * hide "you are logged in" line
     */
    private fun ActivitySettingsBinding.hideLoggedInButton(){
        youAreSignedInAsButton.visibility=View.GONE
        lastSynchedTimeTextView.visibility=View.GONE
        changePasswordButton.visibility=View.GONE
        loginLinkButton.visibility = View.GONE
        loginLinkExplanationImageView.visibility = View.GONE
    }
    private fun ActivitySettingsBinding.showLoggedInButton(){
        youAreSignedInAsButton.visibility=View.VISIBLE
        lastSynchedTimeTextView.visibility=View.VISIBLE
        changePasswordButton.visibility=View.VISIBLE
        val showLoginLinkButton = if (Preferences.username == null) View.GONE else View.VISIBLE
        loginLinkButton.visibility = showLoginLinkButton
        loginLinkExplanationImageView.visibility = showLoginLinkButton
    }

    private fun ActivitySettingsBinding.setLoggedInInfo(name: String?){
        youAreSignedInAsButton.text = getStringWithMakeup(R.string.signed_in_as, name ?: getString(R.string.you_are_not_signed_in))
        val showLoginLinkButton = if (name == null) View.GONE else View.VISIBLE
        loginLinkButton.visibility = showLoginLinkButton
        loginLinkExplanationImageView.visibility = showLoginLinkButton
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
            title="HALLON AUB GRGR"
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

        override val formatter = NumberPicker.Formatter{ when (it) {
            0 -> App.instance.getString(R.string.never)
            1 -> App.instance.getString(R.string.day)
            else -> App.instance.getStringWithMakeup(R.string.n_days, it.toString()).toString()
        }
        }


    }

    companion object{
        private const val CALENDAR_PERMISSION_REQUEST_CODE = 1
        private const val CALENDAR_PERMISSION_TEMP_CODE = 2
        private const val CSV_MIME_TYPE = "text/csv"
    }
}
