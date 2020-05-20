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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.NumberPicker
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.dialog_times_in_out.view.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.extensions.ctx
import nl.joozd.logbookapp.model.helpers.FeedbackEvents.TimePickerEvents
import nl.joozd.logbookapp.model.viewmodels.dialogs.TimePickerViewModel
import nl.joozd.logbookapp.ui.fragments.JoozdlogFragment
import nl.joozd.logbookapp.ui.utils.toast

class TimePicker: JoozdlogFragment() {
    companion object{
        private val paddedMinutes = IntArray(60) {it}.map{v -> v.toString().padStart(2,'0')}.toTypedArray()
        private val paddedHours = IntArray(24) {it}.map{v -> v.toString().padStart(2,'0')}.toTypedArray()
    }
    private val viewModel: TimePickerViewModel by viewModels()

    // variable to store previous text on any selected field
    private var previousText: String = ""

    //used in changing an amount of time (ie. "1:15") to a LocalTime so I can easily take hours and minutes
    //don't forget to padStart hours to HH in stead of H.

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.dialog_times_in_out, container, false).apply {

            //Set dialog title background color
            timesDialogTopHalf.joozdLogSetBackgroundColor()

            // define values for pickers
            hoursOutPicker.setSpinnervaluesForHours()
            minutesOutPicker.setSpinnervaluesForMinutes()

            hoursInPicker.setSpinnervaluesForHours()
            minutesInPicker.setSpinnervaluesForMinutes()

            /**
             * Set pickers listeners
             */
            //Date of timeOut stays the same, if needed, adjust timeIn. Flights with length >24 hours are not supported.
            hoursOutPicker.setOnValueChangedListener { _, _, newVal -> viewModel.timeOutPicked(hours = newVal) }
            minutesOutPicker.setOnValueChangedListener { _, _, newVal -> viewModel.timeOutPicked(hours = newVal) }

            hoursInPicker.setOnValueChangedListener { _, _, newVal -> viewModel.timeInPicked(hours = newVal) }
            minutesInPicker.setOnValueChangedListener { _, _, newVal -> viewModel.timeInPicked(hours = newVal) }

            autoValuesCheckbox.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setAutoValues(isChecked)
            }

            augmentedCrewCheckbox.setOnClickListener {
                Log.d("meh", "XOXO")
                supportFragmentManager.commit {
                    add(R.id.mainActivityLayout, AugmentedCrewDialog())
                    addToBackStack(null)
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

            /**
             * Hide softKeyboard on pressing Enter
             */
            ifrTimeText.setOnEditorActionListener { thisview, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE){
                    val imm: InputMethodManager = thisview.ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(thisview.windowToken, 0)
                    thisview.clearFocus()
                }
                true
            }



            /**
             * If cancelled or clicked outside dialog, undo changes and close Fragment
             */
            timesDialogBackground.setOnClickListener {
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
            timesDialogLayout.setOnClickListener {  }


            /**
             * observers:
             */

            viewModel.sim.observe(viewLifecycleOwner, Observer {
                setSimLayoutIfNeeded(this, it)
            })

            viewModel.feedbackEvent.observe(viewLifecycleOwner, Observer{
                when(it.getEvent()){
                    TimePickerEvents.NOT_IMPLEMENTED -> toast("Not implemented")
                    //TODO handle other events
                }
            })
            viewModel.hourIn.observe(viewLifecycleOwner, Observer{ hoursInPicker.value = it })
            viewModel.minuteIn.observe(viewLifecycleOwner, Observer{ minutesInPicker.value = it })
            viewModel.hourOut.observe(viewLifecycleOwner, Observer{ hoursOutPicker.value = it })
            viewModel.minuteOut.observe(viewLifecycleOwner, Observer{ minutesOutPicker.value = it })

            viewModel.ifrTime.observe(viewLifecycleOwner, Observer{ ifrTimeText.setText(it) })

        } // end of return


    }


    /**
     * Sets layout to sim
     */
    private fun  setSimLayoutIfNeeded(view: View, sim: Boolean){
        with(view) {
            if(sim) {
                tInText.visibility = View.INVISIBLE
                hoursInPicker.visibility = View.INVISIBLE
                minutesInPicker.visibility = View.INVISIBLE
                nightTimeLayout.visibility = View.GONE
                ifrTimeLayout.visibility = View.GONE
                augmentedCrewCheckbox.visibility = View.GONE
                autoValuesCheckbox.visibility = View.GONE
            } else {
                tInText.visibility = View.VISIBLE
                hoursInPicker.visibility = View.VISIBLE
                minutesInPicker.visibility = View.VISIBLE
                nightTimeLayout.visibility = View.VISIBLE
                ifrTimeLayout.visibility = View.VISIBLE
                augmentedCrewCheckbox.visibility = View.VISIBLE
                autoValuesCheckbox.visibility = View.VISIBLE
            }
        }
    }

    private fun NumberPicker.setSpinnervaluesForMinutes(){
        minValue = 0
        maxValue = 59
        displayedValues = paddedMinutes
    }
    private fun NumberPicker.setSpinnervaluesForHours(){
        minValue = 0
        maxValue = 23
        displayedValues = paddedHours
    }

    private fun timeFromMinutes(minutes: Int): String = "${minutes/60}:${(minutes%60).toString().padStart(2,'0')}"
}
