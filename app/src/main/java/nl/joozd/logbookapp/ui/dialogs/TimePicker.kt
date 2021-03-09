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

package nl.joozd.logbookapp.ui.dialogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import nl.joozd.logbookapp.R

import nl.joozd.logbookapp.databinding.DialogTimesInOutBinding
import nl.joozd.logbookapp.extensions.ctx
import nl.joozd.logbookapp.extensions.showIfActive
import nl.joozd.logbookapp.model.feedbackEvents.FeedbackEvents.TimePickerEvents
import nl.joozd.logbookapp.model.viewmodels.dialogs.TimePickerViewModel
import nl.joozd.logbookapp.ui.fragments.JoozdlogFragment
import nl.joozd.logbookapp.ui.utils.toast

open class TimePicker: JoozdlogFragment() {
    private val viewModel: TimePickerViewModel by viewModels()

    // variable to store previous text on any selected field

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DialogTimesInOutBinding.bind(inflater.inflate(R.layout.dialog_times_in_out, container, false)).apply{

            /***************************************************************************************
             * onFocusChangedListeners for EditTexts
             ***************************************************************************************/

            ttofText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    viewModel.setTotalTimeOfFlight(ttofText.text.toString())
                }
            }

            nightTimeText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    viewModel.setNightTime(nightTimeText.text.toString())
                }
            }

            ifrTimeText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    viewModel.setIfrTime(ifrTimeText.text.toString())
                }
            }

            /***************************************************************************************
             * onClicks for toggles
             ***************************************************************************************/

            augmentedTextView.setOnClickListener {
                supportFragmentManager.commit {
                    add(R.id.mainActivityLayout, AugmentedCrewDialog())
                    addToBackStack(null)
                }
            }

            picTextView.setOnClickListener {
                viewModel.togglePic()
            }

            coPilotTextView.setOnClickListener {
                viewModel.toggleCopilot()
            }

            dualTextview.setOnClickListener {
                viewModel.toggleDual()
            }

            instructorTextView.setOnClickListener {
                viewModel.toggleInstructor()
            }


            /**
             * Hide softKeyboard on pressing Enter
             */
            ifrTimeText.setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val imm: InputMethodManager =
                        v.ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                    v.clearFocus()
                }
                true
            }


            /**
             * If cancelled or clicked outside dialog, undo changes and close Fragment
             */
            timePickerDialogBackground.setOnClickListener {
                viewModel.undo()
                closeFragment()
            }
            cancelTimeDialog.setOnClickListener {
                viewModel.undo()
                closeFragment()
            }

            /**
             * No need to save anything as viewModel is updated realtime
             */
            saveTimeDialog.setOnClickListener {
                closeFragment()
            }

            //empty onClickListener to block clicks on lower layers
            headerLayout.setOnClickListener { }
            bodyLayout.setOnClickListener { }


            /**
             * observers:
             */

            viewModel.feedbackEvent.observe(viewLifecycleOwner) {
                when (it.getEvent()) {
                    TimePickerEvents.NOT_IMPLEMENTED -> context?.toast("Not implemented")
                    TimePickerEvents.TOTAL_TIME_GREATER_THAN_DURATION -> context?.toast(R.string.total_time_too_large)
                    TimePickerEvents.INVALID_TOTAL_TIME -> context?.toast(R.string.invalid_total_time)
                    //TODO handle other events
                }
            }

            viewModel.totalTime.observe(viewLifecycleOwner) { ttofText.setText(it) }

            viewModel.ifrTime.observe(viewLifecycleOwner) { ifrTimeText.setText(it) }

            viewModel.nightTime.observe(viewLifecycleOwner) { nightTimeText.setText(it) }

            viewModel.augmentedCrew.crewSize.observe(viewLifecycleOwner) {
                augmentedTextView.showIfActive(it > 2)
            }

            viewModel.pic.observe(viewLifecycleOwner) {
                picTextView.showIfActive(it)
            }
            viewModel.coPilot.observe(viewLifecycleOwner) {
                coPilotTextView.showIfActive(it)
            }
            viewModel.dual.observe(viewLifecycleOwner) {
                dualTextview.showIfActive(it)
            }
            viewModel.instructor.observe(viewLifecycleOwner) {
                instructorTextView.showIfActive(it)
            }
        }.root

    /*
    private val paddedMinutes = IntArray(60) { it }.map { v -> v.toString().padStart(2, '0') }.toTypedArray()
    private val paddedHours = IntArray(24) { it }.map { v -> v.toString().padStart(2, '0') }.toTypedArray()

    private fun NumberPicker.setSpinnervaluesForMinutes() {
        minValue = 0
        maxValue = 59
        displayedValues = paddedMinutes
    }

    private fun NumberPicker.setSpinnervaluesForHours() {
        minValue = 0
        maxValue = 23
        displayedValues = paddedHours
    }
    */

    companion object {

    }
}
