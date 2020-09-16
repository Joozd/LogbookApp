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

package nl.joozd.logbookapp.ui.activities.newUserActivity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter

import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer

import nl.joozd.logbookapp.R

import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.databinding.ActivityNewUserPage3Binding
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents
import nl.joozd.logbookapp.model.viewmodels.activities.NewUserActivityViewModel
import nl.joozd.logbookapp.ui.fragments.JoozdlogFragment
import nl.joozd.logbookapp.ui.utils.longToast
import nl.joozd.logbookapp.ui.utils.toast
import nl.joozd.logbookapp.utils.checkPermission

class NewUserActivityPage3: JoozdlogFragment() {
    val viewModel: NewUserActivityViewModel by activityViewModels()

    private var mBinding: ActivityNewUserPage3Binding? = null

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        Log.d("requestPermissionsReslt", "code: $requestCode, permissions: $permissions, grantResults: $grantResults")
        when (requestCode) {
            CALENDAR_PERMISSION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) && checkPermission(Manifest.permission.READ_CALENDAR)) {
                    showCalendarPickerSpinner(mBinding)
                    viewModel.fillCalendarsList()
                }

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = ActivityNewUserPage3Binding.bind(layoutInflater.inflate(R.layout.activity_new_user_page_3, container, false))
        mBinding = binding



        /****************************************************************************************
         * initialize spinner
         ****************************************************************************************/

        ArrayAdapter(requireContext(), R.layout.spinner_item, arrayListOf<String>()).apply {
            // Specify the layout to use when the list of choices appears
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }.also { adapter ->
            // Apply the adapter to the spinner
            binding.calendarPickerSpinner.adapter = adapter
        }
        binding.calendarPickerSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                //mainSearchField.text = mainSearchField.text
                viewModel.calendarPicked(position)
            }
        }




        /*******************************************************************************************
         * OnClickedListeners
         *******************************************************************************************/

        with(binding){
            continueTextView.setOnClickListener {
                viewModel.nextPage(PAGE_NUMBER)
            }

            skipTextView.setOnClickListener {
                viewModel.noCalendarSelected()
                viewModel.nextPage(PAGE_NUMBER)
            }

            cancelTextView.setOnClickListener {
                viewModel.noCalendarSelected()
                viewModel.nextPage(PAGE_NUMBER)
            }

            useCalendarImportTextView.setOnClickListener {
                if (!checkPermission(Manifest.permission.READ_CALENDAR))
                    requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR), CALENDAR_PERMISSION_REQUEST_CODE)
                else {
                    showCalendarPickerSpinner(this)
                    viewModel.fillCalendarsList()
                }
            }
        }



        /*******************************************************************************************
         * Observers
         *******************************************************************************************/

        viewModel.foundCalendars.observe(viewLifecycleOwner, Observer {
            @Suppress("UNCHECKED_CAST")
            (binding.calendarPickerSpinner.adapter as ArrayAdapter<String>).apply{
                clear()
                addAll(it)
            }
            binding.calendarPickerSpinner.setSelection(it.indexOf(Preferences.selectedCalendar))
        })

        viewModel.page3Feedback.observe(viewLifecycleOwner, Observer {
            when (it.getEvent()){
                FeedbackEvents.NewUserActivityEvents.CALENDAR_PICKED -> binding.continueTextView.visibility = View.VISIBLE
            }
        })

        return binding.root
    }

    private fun showCalendarPickerSpinner(binding: ActivityNewUserPage3Binding?){
        binding?.apply{
            useCalendarImportTextView.visibility = View.GONE
            skipTextView.visibility = View.GONE

            calendarPickerSpinner.visibility = View.VISIBLE
            cancelTextView.visibility = View.VISIBLE

        }
    }

    companion object{
        private const val PAGE_NUMBER = 3
        private const val CALENDAR_PERMISSION_REQUEST_CODE = 1
    }
}