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

package nl.joozd.logbookapp.ui.dialogs

import android.Manifest
import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.app.ActivityCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joozd.joozdcalendarapi.CalendarDescriptor
import nl.joozd.joozdcalendarapi.getCalendars
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.sharedPrefs.CalendarSyncType
import nl.joozd.logbookapp.databinding.DialogCalendarSyncBinding
import nl.joozd.logbookapp.extensions.getColorFromAttr
import nl.joozd.logbookapp.extensions.setSelectionWithArrayAdapter
import nl.joozd.logbookapp.model.viewmodels.dialogs.CalendarSyncDialogViewModel
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment
import nl.joozd.logbookapp.ui.utils.toast
import nl.joozd.logbookapp.model.viewmodels.status.CalendarDialogStatus

/*
When a button is selected, check for permissions and do stuff
see fillCalendarsList for how to check
 */

class CalendarSyncDialog: JoozdlogFragment() {
    private val viewModel: CalendarSyncDialogViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        DialogCalendarSyncBinding.bind(inflater.inflate(R.layout.dialog_calendar_sync, container, false)).apply {
            /*************************************
             * Initialization
             *************************************/
            if (checkClipboardForIcalLink()){
                showIcalLinkFoundDialog()
            }

            initializeCalendarPickerSpinner()
            launchFlowCollectors()
            setOnClickListeners()

            calendarPickerSpinner.onItemSelectedListener = calendarSelectedListener
        }.root

    private fun DialogCalendarSyncBinding.setOnClickListeners() {
        calendarScraperRadioButton.isChecked =
            viewModel.calendarSyncType == CalendarSyncType.CALENDAR_SYNC_DEVICE
        calendarScraperRadioButton.setOnClickListener {
            lifecycleScope.launch {
                fillCalendarsList()
                viewModel.calendarScraperRadioButtonClicked()
            }
        }

        icalSubscriptionRadioButton.setOnClickListener {
            icalSubscriptionRadioButton.isChecked = viewModel.calendarSyncType == CalendarSyncType.CALENDAR_SYNC_ICAL
            viewModel.icalSubscriptionRadioButtonClicked(getLinkFromClipboard())
        }

        cancelButton.setOnClickListener {
            closeFragment()
        }
    }


    private suspend fun fillCalendarsList(){
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
            return
        }
        viewModel.fillCalendarsList( withContext(Dispatchers.IO) { requireActivity().getCalendars() })
    }

    private fun DialogCalendarSyncBinding.initializeCalendarPickerSpinner(){
        calendarPickerSpinner.adapter = ArrayAdapter(
            requireActivity(),
            android.R.layout.simple_spinner_item,
            arrayListOf<String>()
        ).apply {
            // Specify the layout to use when the list of choices appears
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }
    }

    private fun DialogCalendarSyncBinding.launchFlowCollectors(){
        viewModel.foundCalendarsFlow.launchCollectWhileLifecycleStateStarted{ list ->
            if (list != null) {
                getAdapter().apply {
                    clear()
                    addAll(list.map { c -> c.displayName })
                    selectSelectedCalendar(list)
                }
            }
        }

        viewModel.selectedCalendarFlow.launchCollectWhileLifecycleStateStarted{ c->
            c?.let{
                calendarPickerSpinner.setSelectionWithArrayAdapter(c.displayName)
            }
        }

        viewModel.okButtonEnabledFlow.launchCollectWhileLifecycleStateStarted{
            enableOkButton(okButton, it)
        }

        viewModel.calendarSyncTypeFlow.launchCollectWhileLifecycleStateStarted{
            calendarScraperRadioButton.isChecked = it == CalendarSyncType.CALENDAR_SYNC_DEVICE
            icalSubscriptionRadioButton.isChecked = it == CalendarSyncType.CALENDAR_SYNC_ICAL
            makeLayoutForSyncType(it)
        }

        viewModel.statusFlow.launchCollectWhileLifecycleStateStarted{
            when(it){
                null -> { }
                CalendarDialogStatus.NO_ICAL_LINK_FOUND -> showNoIcalLinkFoundDialog()
                CalendarDialogStatus.DONE -> closeFragment()
                is CalendarDialogStatus.Error -> {
                    toast(it.error)
                }
            }
        }
    }


    private fun DialogCalendarSyncBinding.makeLayoutForSyncType(syncType: CalendarSyncType){
        when(syncType){
            CalendarSyncType.CALENDAR_SYNC_DEVICE -> {
                icalAddressLayout.visibility = View.INVISIBLE
                calendarPickerSpinnerLayout.visibility = View.VISIBLE
                lifecycleScope.launch {
                    fillCalendarsList()
                }
            }
            CalendarSyncType.CALENDAR_SYNC_ICAL -> {
                icalAddressLayout.visibility = View.VISIBLE
                calendarPickerSpinnerLayout.visibility = View.GONE
                icalAddressEditText.setText(viewModel.foundLink)
            }
            else -> {
                icalAddressLayout.visibility = View.INVISIBLE
                calendarPickerSpinnerLayout.visibility = View.GONE
            }
        }
    }


    private fun DialogCalendarSyncBinding.selectSelectedCalendar(list: List<CalendarDescriptor>) {
        viewModel.selectedCalendar?.let { c ->
            calendarPickerSpinner.setSelection(list.indexOf(c))
        }
    }


    private val requestPermissionLauncher =
        registerForActivityResult(RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                lifecycleScope.launch {
                    fillCalendarsList()
                }
            } else {
                showNeedPermissionDialog()
            }
        }


    private fun showNeedPermissionDialog() {
        JoozdlogAlertDialog().show(requireActivity()) {
            titleResource = R.string.need_permission
            messageResource = R.string.need_permission_calendar
            setPositiveButton(android.R.string.ok) {
                requestPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
            }
            setNegativeButton(android.R.string.cancel)
        }
    }


    private fun showIcalLinkFoundDialog(){
        AlertDialog.Builder(requireActivity()).apply{
            setTitle(R.string.ical_link_found)
            setMessage(R.string.ical_link_found_long)
            setPositiveButton(android.R.string.ok){ _, _ ->
                viewModel.icalSubscriptionRadioButtonClicked(getLinkFromClipboard())
            }
            setNegativeButton(android.R.string.cancel) {
                _, _ -> // intentionally left blank
            }
        }.create().show()
    }


    private fun showNoIcalLinkFoundDialog(){
        AlertDialog.Builder(requireActivity()).apply{
            setTitle(R.string.no_ical_link_found)
            setMessage(R.string.no_ical_link_found_long)
            setNegativeButton(android.R.string.cancel){
                _, _ -> // intentionally left blank
                viewModel.resetStatus()
            }
            setPositiveButton(android.R.string.ok){ _, _ ->
                viewModel.resetStatus()
                viewModel.icalSubscriptionRadioButtonClicked(getLinkFromClipboard())
            }
        }.create().show()
    }

    /**
     * Only use for OK button
     */
    private fun enableOkButton(okButton: TextView, enabled: Boolean) = with (okButton){
        if (!enabled){
            setOnClickListener {
            }
            setTextColor(requireActivity().getColorFromAttr(android.R.attr.textColorTertiary))
        }
        else{
            setOnClickListener {
                viewModel.okClicked()
            }
            setTextColor(requireActivity().getColorFromAttr(android.R.attr.colorAccent))
        }
    }

    /**
     * This checks clipboard for an iCalendar link, and if found, places it in foundLink and returns true
     * @return true if an iCal link was found on clipboard, false if not
     */
    private fun checkClipboardForIcalLink(): Boolean =
        getLinkFromClipboard().let{
            it != null && it != viewModel.calendarSyncIcalAddress
        }


    /**
     * Grab iCalendar link from cliboard
     * @return link as String if found, null if not.
     */
    private fun getLinkFromClipboard(): String?{
        val klmIcalRegex = """^https://calendar\.klm\.com/crew/.+""".toRegex()

        with (getClipboardManager()){
            if (!hasPrimaryClip()) return null
            return primaryClip?.getItemAt(0)?.text?.let {
                klmIcalRegex.find(it)?.value
            }
        }
    }

    private fun getClipboardManager() =
        requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    @Suppress("UNCHECKED_CAST")
    private fun DialogCalendarSyncBinding.getAdapter() =
        (calendarPickerSpinner.adapter as ArrayAdapter<String>)


    private val calendarSelectedListener = object : AdapterView.OnItemSelectedListener {
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
}