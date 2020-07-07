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
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_settings.*
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.extensions.setSelectionWithArrayAdapter
import nl.joozd.logbookapp.model.viewmodels.activities.SettingsActivityViewModel
import nl.joozd.logbookapp.ui.activities.JoozdlogActivity
import nl.joozd.logbookapp.ui.utils.longToast
import nl.joozd.logbookapp.ui.utils.toast
import nl.joozd.logbookapp.utils.checkPermission

class SettingsActivity : JoozdlogActivity() {
    val viewModel: SettingsActivityViewModel by viewModels()

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            CALENDAR_PERMISSION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED))
                    fillCalendarsList()
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
        setContentView(R.layout.activity_settings)


        setSupportActionBarWithReturn(settings_toolbar)?.apply{
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.settings)
        }


        ArrayAdapter(
            this,
            R.layout.spinner_item,
            arrayListOf<String>()
        ).apply {
            // Specify the layout to use when the list of choices appears
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }.also { adapter ->
            // Apply the adapter to the spinner
            settingsCalendarPickerSpinner.adapter = adapter
        }
        settingsCalendarPickerSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                //mainSearchField.text = mainSearchField.text
                viewModel.calendarPicked(position)
            }
        }


        /****************************************************************************************
         * Populate spinners
         ****************************************************************************************/

        ArrayAdapter.createFromResource(
            this,
            R.array.supported_calendars,
            android.R.layout.simple_spinner_item
        ).apply {
            // Specify the layout to use when the list of choices appears
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }.also { aa ->
            // Apply the adapter to the spinner
            settingsCalendarTypeSpinner.apply{
                adapter = aa
                if (Preferences.calendarType >= 0) setSelection(Preferences.calendarType)
            }
        }
        settingsCalendarTypeSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                //mainSearchField.text = mainSearchField.text
                viewModel.calendarTypePicked(position)
            }
        }



        /****************************************************************************************
         * Logic for setters and other thingies
         ****************************************************************************************/
        settingsUseIataSelector.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setUseIataAirports(isChecked)
        }

        settingsGetFlightsFromCalendarSelector.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setGetFlightsFromCalendar(isChecked)
        }

        temporaryButton.setOnClickListener {
            if (checkPermission(Manifest.permission.READ_CALENDAR))
                viewModel.tempButtonClicked()

        }




        /****************************************************************************************
         * Observers for feedback
         ****************************************************************************************/

        viewModel.settingsUseIataSelectorTextResource.observe(this, Observer {
            settingsUseIataSelector.text = getString(it)
        })
        viewModel.useIataAirports.observe(this, Observer {
            settingsUseIataSelector.isChecked = it
        })

        viewModel.getFlightsFromCalendar.observe(this, Observer {
            settingsGetFlightsFromCalendarSelector.isChecked = it
            if (it)
                fillCalendarsList()
            else {
                settingsCalendarPickerSpinner.visibility = View.GONE
                settingsCalendarTypeSpinner.visibility = View.GONE
            }
        })

        viewModel.foundCalendars.observe(this, Observer {
            @Suppress("UNCHECKED_CAST")
            (settingsCalendarPickerSpinner.adapter as ArrayAdapter<String>).apply{
                clear()
                addAll(it)
            }
            settingsCalendarPickerSpinner.setSelection(it.indexOf(Preferences.selectedCalendar))
        })

        viewModel.selectedCalendar.observe(this, Observer {
            if (it != null) settingsCalendarPickerSpinner.setSelectionWithArrayAdapter(it.name)
            Log.d(this::class.simpleName, "triggered AAAA, $it")
        })

        viewModel.pickedCalendarType.observe(this, Observer {
            settingsCalendarTypeSpinner.setSelection(it)
        })

    }

    private fun fillCalendarsList(){
        Log.d(this::class.simpleName, "started fillCalendarsList()")
        if (!checkPermission(Manifest.permission.READ_CALENDAR)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CALENDAR), CALENDAR_PERMISSION_REQUEST_CODE)
            settingsCalendarPickerSpinner.visibility = View.GONE
            settingsCalendarTypeSpinner.visibility = View.GONE
        }
        else {
            settingsCalendarPickerSpinner.visibility = View.VISIBLE
            // Not used at the moment but it's there when we need it
            // TAG: #SETTHISIFNEEDED1
            // settingsCalendarTypeSpinner.visibility = View.VISIBLE
            viewModel.fillCalendarsList()
        }
    }

    companion object{
        private const val CALENDAR_PERMISSION_REQUEST_CODE = 1
        private const val CALENDAR_PERMISSION_TEMP_CODE = 2
    }
}
