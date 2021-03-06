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
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import nl.joozd.joozdutils.JoozdlogAlertDialog
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.databinding.ActivityNewUserPageCalendarBinding
import nl.joozd.logbookapp.model.viewmodels.activities.NewUserActivityViewModel
import nl.joozd.logbookapp.ui.fragments.JoozdlogFragment

/**
 * Calendar import
 */
class NewUserActivityCalendarPage: JoozdlogFragment() {
    val pageNumber = NewUserActivityViewModel.PAGE_CALENDAR

    val viewModel: NewUserActivityViewModel by activityViewModels()

    @SuppressLint("MissingPermission") // false positive
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED){
            viewModel.fillCalendarsList()
        }
        else JoozdlogAlertDialog().show(requireActivity()){
            titleResource = R.string.need_permission
            messageResource = R.string.need_permission_calendar
            setPositiveButton(android.R.string.ok){
                viewModel.disableCalendarSync()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        ActivityNewUserPageCalendarBinding.bind(layoutInflater.inflate(R.layout.activity_new_user_page_calendar, container, false)).apply {



            /****************************************************************************************
             * initialize spinner
             ****************************************************************************************/
            val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, arrayListOf<String>()).apply {
                // Specify the layout to use when the list of choices appears
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }.also { adapter ->
                // Apply the adapter to the spinner
                calendarPickerSpinner.adapter = adapter
            }
            calendarPickerSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {}
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    //mainSearchField.text = mainSearchField.text
                    viewModel.calendarPicked(position)
                }
            }


            /*******************************************************************************************
             * OnClickedListeners
             *******************************************************************************************/

            useCalendarImportSwitch.setOnCheckedChangeListener { v, isChecked ->
                if (!isChecked){
                    viewModel.disableCalendarSync()
                    return@setOnCheckedChangeListener
                }
                fillCalendarsList()
            }


            /*******************************************************************************************
             * Observers
             *******************************************************************************************/

            viewModel.foundCalendars.observe(viewLifecycleOwner) { items ->
                useCalendarImportSwitch.isChecked = items != null
                calendarPickerSpinnerLayout.visibility = items?.let {
                    spinnerAdapter.clear()
                    spinnerAdapter.addAll(it)
                    calendarPickerSpinner.setSelection(it.indexOf(Preferences.selectedCalendar).takeIf {i -> i != -1} ?: 0)

                    View.VISIBLE
                } ?: View.INVISIBLE


            }
            return root
        }
    }

    private fun fillCalendarsList(){
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED)
            viewModel.fillCalendarsList()
        else requestPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
    }
}