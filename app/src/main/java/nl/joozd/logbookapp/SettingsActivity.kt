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

package nl.joozd.logbookapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.databinding.ActivitySettingsBinding
import nl.joozd.logbookapp.extensions.getStringWithMakeup
import nl.joozd.logbookapp.extensions.setSelectionWithArrayAdapter
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.SettingsActivityEvents
import nl.joozd.logbookapp.model.viewmodels.activities.SettingsActivityViewModel
import nl.joozd.logbookapp.ui.activities.JoozdlogActivity
import nl.joozd.logbookapp.ui.activities.LoginActivity
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

            addRemarksToChronoUpdatesSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setShowOldTimesOnChronoUpdate(isChecked)
            }

            useCloudSyncSwitch.setOnClickListener {
                viewModel.useCloudSyncToggled()
            }

            dontPostponeTextView.setOnClickListener {
                viewModel.dontPostponeCalendarSync()
            }

            userSignInOutTextview.setOnClickListener {
                viewModel.signInOut()
            }


            /****************************************************************************************
             * Observers for feedback
             ****************************************************************************************/

            viewModel.feedbackEvent.observe(activity) {
                when (it.getEvent()){
                    SettingsActivityEvents.SHOW_LOGIN_ACTIVITY ->
                        startActivity(Intent(activity, LoginActivity::class.java))

                    SettingsActivityEvents.SIGNED_OUT ->
                        setLoggedInInfo(Preferences.username)
                }
            }

            /****************************************************************************************
             * Other observers
             ****************************************************************************************/

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
                if (it) showLoggedInInfo() else hideLoggedInInfo()
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
    private fun ActivitySettingsBinding.hideLoggedInInfo(){
        youAreSignedInAsTextView.visibility=View.GONE
        userSignInOutTextview.visibility = View.GONE
    }
    private fun ActivitySettingsBinding.showLoggedInInfo(){
        youAreSignedInAsTextView.visibility=View.VISIBLE
        userSignInOutTextview.visibility = View.VISIBLE
    }

    private fun ActivitySettingsBinding.setLoggedInInfo(name: String?){
        youAreSignedInAsTextView.text = name?.let{
            getStringWithMakeup(R.string.you_are_signed_in_as, name)
        } ?: getString(R.string.you_are_not_signed_in)
        userSignInOutTextview.text = getString(if (name == null) R.string.signIn else R.string.signOut)

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

    companion object{
        private const val CALENDAR_PERMISSION_REQUEST_CODE = 1
        private const val CALENDAR_PERMISSION_TEMP_CODE = 2
    }
}
