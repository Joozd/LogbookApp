/*
 *  JoozdLog Pilot's Logbook
 *  Copyright (c) 2021 Joost Welle
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
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.sharedPrefs.CalendarSyncTypes
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.databinding.DialogCalendarSyncBinding
import nl.joozd.logbookapp.extensions.getColorFromAttr
import nl.joozd.logbookapp.extensions.setSelectionWithArrayAdapter
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.CalendarSyncDialogEvents
import nl.joozd.logbookapp.model.viewmodels.dialogs.CalendarSyncDialogViewModel
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment
import nl.joozd.logbookapp.ui.utils.toast
import nl.joozd.logbookapp.utils.ErrorCodes
import nl.joozd.logbookapp.utils.errorDialog

/*
When a button is selected, check for permissions and do stuff
see fillCalendarsList for how to check
 */

class CalendarSyncDialog : JoozdlogFragment() {
    private val viewModel: CalendarSyncDialogViewModel by viewModels()
    private var mCancelButton: TextView? = null // to get focus onResume

    /**
     * OK button when enabled will get focus (so onFocusChangeListeners will trigger) and tell viewModel it has been clicked.
     * If not enabled, [okButtonListener] shall not be called
     */
    private val okButtonListener = View.OnClickListener{
        activity?.currentFocus?.clearFocus()
        it.requestFocus()
        viewModel.okClicked()
    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        DialogCalendarSyncBinding.bind(inflater.inflate(R.layout.dialog_calendar_sync, container, false)).apply {
            /*************************************
             * Initialization
             *************************************/

            if (viewModel.checkClipboardForIcalLink()){
                showIcalLinkFoundDialog()
            }

            /**
             * Populate Calendar picker spinner
             */
            ArrayAdapter(
                requireActivity(),
                android.R.layout.simple_spinner_item,
                arrayListOf<String>()
            ).apply {
                // Specify the layout to use when the list of choices appears
                setDropDownViewResource(R.layout.spinner_dropdown_item)
            }.also { a ->
                // Apply the adapter to the spinner
                calendarPickerSpinner.apply {
                    adapter = a
                }
            }


            /*************************************
             * Listeners
             *************************************/

            calendarScraperRadioButton.setOnClickListener {
                clearButtons()
                fillCalendarsList(true)
                viewModel.calendarScraperRadioButtonClicked()
            }

            icalSubscriptionRadioButton.setOnClickListener {
                clearButtons()
                viewModel.icalSubscriptionRadioButtonClicked()
            }

            cancelButton.setOnClickListener {
                closeFragment()
            }

            mCancelButton = cancelButton


            okButton.setOnClickListener(okButtonListener)


            calendarPickerSpinner.onItemSelectedListener =
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

            /*************************************
             * Observers:
             *************************************/

            viewModel.foundCalendars.observe(viewLifecycleOwner) {
                @Suppress("UNCHECKED_CAST")
                (calendarPickerSpinner.adapter as ArrayAdapter<String>).apply {
                    clear()
                    addAll(it)
                }
                calendarPickerSpinner.setSelection(it.indexOf(Preferences.selectedCalendar))
            }

            viewModel.calendarSyncType.observe(viewLifecycleOwner){
                println("TEST123 $it")
                calendarScraperRadioButton.isChecked = it == CalendarSyncTypes.CALENDAR_SYNC_DEVICE
                icalSubscriptionRadioButton.isChecked = it == CalendarSyncTypes.CALENDAR_SYNC_ICAL

                when(it){
                    CalendarSyncTypes.CALENDAR_SYNC_DEVICE -> {
                        icalAddressLayout.visibility = View.INVISIBLE
                        calendarPickerSpinnerLayout.visibility = View.VISIBLE
                        fillCalendarsList()
                    }
                    CalendarSyncTypes.CALENDAR_SYNC_ICAL -> {
                        icalAddressLayout.visibility = View.VISIBLE
                        calendarPickerSpinnerLayout.visibility = View.GONE
                        icalAddressEditText.setText(viewModel.foundLink)
                    }

                }
            }

            // NOTE when using iCalendar link, [it] should always be null
            viewModel.selectedCalendar.observe(viewLifecycleOwner) {
                if (it != null) calendarPickerSpinner.setSelectionWithArrayAdapter(it.name)
            }

            viewModel.okButtonEnabled.observe(viewLifecycleOwner){
                enableOkButton(okButton, it)
            }

            /**
             * Observer for feedback
             */
            viewModel.feedbackEvent.observe(viewLifecycleOwner) {
                when (it.getEvent()){
                    CalendarSyncDialogEvents.DONE -> closeFragment()
                    CalendarSyncDialogEvents.ERROR -> errorDialog(ErrorCodes.FOUNDLINK_IS_NULL)
                    CalendarSyncDialogEvents.NO_ICAL_LINK_FOUND -> {
                        noIcalLinkFoundDialog()
                    }

                    else -> toast("UNHANDLED EVENT: $it")
                }
            }

        }.root


    val requestPermissionLauncher =
        registerForActivityResult(RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                fillCalendarsList()
            } else {
                showNeedPermissionDialog()

            }
        }

    /**
     * Sets both radio buttons to off. Observers will turn on any that should be turned on.
     */
    private fun DialogCalendarSyncBinding.clearButtons(){
        icalSubscriptionRadioButton.isChecked = false
        calendarScraperRadioButton.isChecked = false
    }


    private fun fillCalendarsList(forceCalendarScrape: Boolean? = null) {
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
            return
        }
        viewModel.fillCalendarsList(forceCalendarScrape)
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
        JoozdlogAlertDialog().show(requireActivity()) {
            titleResource = R.string.ical_link_found
            messageResource = R.string.ical_link_found_long
            setPositiveButton(android.R.string.ok){
                viewModel.useIcalLink()
            }
            setNegativeButton(android.R.string.cancel)
        }
    }

    private fun noIcalLinkFoundDialog(){
        JoozdlogAlertDialog().show(requireActivity(), tag = NO_ICAL_LINK_DIALOG_TAG){
            titleResource = R.string.no_ical_link_found
            messageResource = R.string.no_ical_link_found_long
            setNegativeButton(android.R.string.cancel)
            setPositiveButton(android.R.string.ok){
                if (viewModel.checkClipboardForIcalLink()){
                    viewModel.useIcalLink()
                }
            }
        }
    }

    /**
     * Only use for OK button
     */
    private fun enableOkButton(okButton: TextView, enabled: Boolean) = with (okButton){
        if (!enabled){
            setOnClickListener {
                activity?.currentFocus?.clearFocus()
                it.requestFocus()
            }
            setTextColor(requireActivity().getColorFromAttr(android.R.attr.textColorTertiary))
        }
        else{
            setOnClickListener(okButtonListener)
            setTextColor(requireActivity().getColorFromAttr(android.R.attr.colorAccent))
        }
    }

    companion object{
        private const val NO_ICAL_LINK_DIALOG_TAG = "NO_ICAL_LINK_DIALOG"
    }

}