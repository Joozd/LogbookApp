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
import nl.joozd.logbookapp.R

import nl.joozd.logbookapp.databinding.DialogTimesInOutBinding
import nl.joozd.logbookapp.extensions.ctx
import nl.joozd.logbookapp.extensions.showIfActive
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DialogTimesInOutBinding.bind(inflater.inflate(R.layout.dialog_times_in_out, container, false))
        with (binding) {
            //Set dialog title background color
            timesDialogTopHalf.joozdLogSetBackgroundColor()




            /***************************************************************************************
             * onFocusChangedListeners for EditTexts
             ***************************************************************************************/

            ttofText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    viewModel.setTotalTimeOfFlight(nightTimeText.text.toString())
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
                Log.d("meh", "XOXO")
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
            ifrTimeText.setOnEditorActionListener { thisview, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val imm: InputMethodManager =
                        thisview.ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
            timesDialogLayout.setOnClickListener { }


            /**
             * observers:
             */

            viewModel.feedbackEvent.observe(viewLifecycleOwner, Observer {
                when (it.getEvent()) {
                    TimePickerEvents.NOT_IMPLEMENTED -> toast("Not implemented")
                    //TODO handle other events
                }
            })

            viewModel.totalTime.observe(viewLifecycleOwner, Observer { ttofText.setText(it)})

            viewModel.ifrTime.observe(viewLifecycleOwner, Observer { ifrTimeText.setText(it) })

            viewModel.nightTime.observe(viewLifecycleOwner, Observer { nightTimeText.setText(it)})

            viewModel.augmentedCrew.observe(viewLifecycleOwner, Observer {
                augmentedTextView.showIfActive(it)
            })

            viewModel.pic.observe(viewLifecycleOwner, Observer {
                picTextView.showIfActive(it)
            })
            viewModel.coPilot.observe(viewLifecycleOwner, Observer {
                coPilotTextView.showIfActive(it)
            })
            viewModel.dual.observe(viewLifecycleOwner, Observer {
                dualTextview.showIfActive(it)
            })
            viewModel.instructor.observe(viewLifecycleOwner, Observer {
                instructorTextView.showIfActive(it)
            })
        }


        return binding.root


    }


    /**
     * Sets layout to sim
     */

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
