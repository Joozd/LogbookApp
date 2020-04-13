/*
 * JoozdLog Pilot's Logbook
 * Copyright (C) 2020 Joost Welle
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see https://www.gnu.org/licenses
 */

/*
 * JoozdLog Pilot's Logbook
 * Copyright (C) 2020 Joost Welle
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see https://www.gnu.org/licenses
 */

package nl.joozd.logbookapp.ui.dialogs

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.textfield.TextInputEditText
import kotlinx.android.synthetic.main.dialog_times_in_out.view.*
import kotlinx.coroutines.*
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.dataclasses.Flight

import nl.joozd.logbookapp.data.miscClasses.Crew
import nl.joozd.logbookapp.data.room.Repository
import nl.joozd.logbookapp.data.viewmodel.JoozdlogViewModel
import nl.joozd.logbookapp.extensions.ctx
import nl.joozd.logbookapp.extensions.getColorFromAttr
import nl.joozd.logbookapp.extensions.toBoolean
import nl.joozd.logbookapp.extensions.toInt
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment
import nl.joozd.logbookapp.ui.utils.toast
import nl.joozd.logbookapp.utils.TwilightCalculator
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class TimePicker: JoozdlogFragment() {
    companion object{
        private val paddedMinutes = IntArray(60) {it}.map{v -> v.toString().padStart(2,'0')}.toTypedArray()
        private val paddedHours = IntArray(24) {it}.map{v -> v.toString().padStart(2,'0')}.toTypedArray()
    }
    // variable to store previous text on any selected field
    private var previousText: String = ""

    //used in changing an amount of time (ie. "1:15") to a LocalTime so I can easily take hours and minutes
    //don't forget to padStart hours to HH in stead of H.
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    lateinit var orig: Deferred<Airport?>
    lateinit var dest: Deferred<Airport?>
    override fun onAttach(context: Context) {
        super.onAttach(context)
        orig = async { repository.searchAirport(flight.orig) }
        dest = async { repository.searchAirport(flight.dest) }
    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val twilightCalculator = TwilightCalculator(flight.tOut)


        unchangedFlight = flight
        val view = inflater.inflate(R.layout.dialog_times_in_out, container, false).apply {

            //Set dialog title background color
            (timesDialogTopHalf.background as GradientDrawable).colorFilter = PorterDuffColorFilter(
                requireActivity().getColorFromAttr(android.R.attr.colorPrimary),
                PorterDuff.Mode.SRC_IN
            )

            // define values for pickers
            hoursOutPicker.minValue = 0
            hoursOutPicker.maxValue = 23
            hoursOutPicker.displayedValues =
                paddedHours
            minutesOutPicker.minValue = 0
            minutesOutPicker.maxValue = 59
            minutesOutPicker.displayedValues =
                paddedMinutes
            hoursInPicker.minValue = 0
            hoursInPicker.maxValue = 23
            hoursInPicker.displayedValues =
                paddedHours
            minutesInPicker.minValue = 0
            minutesInPicker.maxValue = 59
            minutesInPicker.displayedValues =
                paddedMinutes

            // Fill with data from current Flight
            if (flight.sim) {
                hoursOutPicker.value = flight.simTime / 60
                minutesOutPicker.value = flight.simTime % 60
                tOutText.text = getString(R.string.simtTime)
                tInText.visibility = View.INVISIBLE
                hoursInPicker.visibility = View.INVISIBLE
                minutesInPicker.visibility = View.INVISIBLE
                nightTimeLayout.visibility = View.GONE
                ifrTimeLayout.visibility = View.GONE
                augmentedCrewCheckbox.visibility = View.GONE
                autoValuesCheckbox.visibility = View.GONE
            } else {
                hoursOutPicker.value = flight.tOut.hour
                minutesOutPicker.value = flight.tOut.minute
                hoursInPicker.value = flight.tIn.hour
                minutesInPicker.value = flight.tIn.minute
                autoValuesCheckbox.isChecked = flight.autoFill > 0
                augmentedCrewCheckbox.isChecked =
                    Crew.of(flight.augmentedCrew).crewSize > 2
            }

            /**
             * Set pickers listeners
             */
            //Date of timeOut stays the same, if needed, adjust timeIn. Flights with length >24 hours are not supported.
            hoursOutPicker.setOnValueChangedListener { _, _, newVal ->
                val timeOut = LocalDateTime.of(
                    flight.tOut.toLocalDate(),
                    LocalTime.of(newVal, flight.tOut.minute)
                )
                val timeIn =
                    when {
                        flight.tIn.minusDays(1) > timeOut -> flight.tIn.minusDays(1)
                        flight.tIn > timeOut -> flight.tIn
                        else -> flight.tIn.plusDays(1)
                    }
                flight = flight.copy(
                    timeOut = timeOut.toInstant(ZoneOffset.UTC).epochSecond,
                    timeIn = timeIn.toInstant(ZoneOffset.UTC).epochSecond
                )
            }
            minutesOutPicker.setOnValueChangedListener { _, _, newVal ->
                val timeOut = LocalDateTime.of(
                    flight.tOut.toLocalDate(),
                    LocalTime.of(flight.tOut.hour, newVal)
                )
                val timeIn =
                    when {
                        flight.tIn.minusDays(1) > timeOut -> flight.tIn.minusDays(1)
                        flight.tIn > timeOut -> flight.tIn
                        else -> flight.tIn.plusDays(1)
                    }
                flight = flight.copy(
                    timeOut = timeOut.toInstant(ZoneOffset.UTC).epochSecond,
                    timeIn = timeIn.toInstant(ZoneOffset.UTC).epochSecond
                )

            }
            hoursInPicker.setOnValueChangedListener { _, _, newVal ->
                val timeToCheck = LocalDateTime.of(
                    flight.tOut.toLocalDate(),
                    LocalTime.of(newVal, flight.tIn.minute)
                )
                val timeIn = when {
                    timeToCheck.minusDays(1) > flight.tOut -> timeToCheck.minusDays(1)
                    timeToCheck > flight.tOut -> timeToCheck
                    else -> timeToCheck.plusDays(1)
                }
                flight = flight.copy(timeIn = timeIn.toInstant(ZoneOffset.UTC).epochSecond)
            }
            minutesInPicker.setOnValueChangedListener { _, _, newVal ->

                val timeToCheck = LocalDateTime.of(
                    flight.tOut.toLocalDate(),
                    LocalTime.of(flight.tIn.hour, newVal)
                )
                val timeIn = when {
                    timeToCheck.minusDays(1) > flight.tOut -> timeToCheck.minusDays(1)
                    timeToCheck > flight.tOut -> timeToCheck
                    else -> timeToCheck.plusDays(1)
                }
                flight = flight.copy(timeIn = timeIn.toInstant(ZoneOffset.UTC).epochSecond)
            }

            autoValuesCheckbox.setOnCheckedChangeListener { _, isChecked ->
                flight = if (isChecked)
                    flight.copy(autoFill = true.toInt())
                else
                    flight.copy(autoFill = false.toInt())
            }

            augmentedCrewCheckbox.setOnClickListener {
                Log.d("meh", "XOXO")
                supportFragmentManager.beginTransaction()
                    .add(R.id.mainActivityLayout,
                        AugmentedCrewDialog()
                    )
                    .addToBackStack(null)
                    .commit()
            }

            /**
             * Make IFR Input field do it's magic
             */
            ifrTimeText.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    with(v as TextInputEditText) {
                        previousText = text.toString()
                    }
                } else {
                    with(v as EditText) {
                        if (text.toString() != previousText) { // if something changed
                            val currentText = this.text.toString()
                            //TODO change "100" to "1:00"

                            // check if not longer than 5 characters (hh:mm is 5 characters)
                            if (!(("([01]\\d|2[0-3]):[0-5]\\d".toRegex().containsMatchIn(currentText) && currentText.length == 5)
                                        || ("([01]\\d|2[0-3])[0-5]\\d".toRegex().containsMatchIn(currentText.padStart(4,'0')) && currentText.length <= 4))
                                || (currentText == "")
                            ){
                                context.toast("Invalid entry")
                                v.setText(previousText)
                            } else {
                                val len = currentText.padStart(3, '0').length
                                this.setText(if (len < 4)
                                    "${currentText.padStart(3,'0').slice(0..(len-3))}:${currentText.padStart(3, '0').slice(len-2 until len)}"
                                else currentText
                                )

                                val t = LocalTime.parse(v.text.toString().padStart(5, '0'), timeFormatter)
                                flight = flight.copy(ifrTime = t.hour * 60 + t.minute, autoFill = 0,  changed = 1)
                            }
                        }
                    }
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
             * Make NightTime Input field do it's magic
             */
            nightTimeText.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    with(v as TextInputEditText) {
                        previousText = text.toString()
                    }
                } else {
                    with(v as EditText) {
                        if (text.toString() != previousText) { // if something changed
                            val currentText = this.text.toString()
                            //TODO change "100" to "1:00"

                            // check if not longer than 5 characters (hh:mm is 5 characters)
                            if (!(("([01]\\d|2[0-3]):[0-5]\\d".toRegex().containsMatchIn(currentText) && currentText.length == 5)
                                        || ("([01]\\d|2[0-3])[0-5]\\d".toRegex().containsMatchIn(currentText.padStart(4,'0')) && currentText.length <= 4))
                                || (currentText == "")){
                                context.toast("Invalid entry")
                                v.setText(previousText)
                            } else {
                                val len = currentText.padStart(3, '0').length
                                this.setText(if (len < 4)
                                    "${currentText.padStart(3,'0')
                                        .slice(0..(len-3))}:${currentText.padStart(3, '0').slice(len-2 until len)}"
                                else currentText)

                                val t = LocalTime.parse(v.text.toString().padStart(5, '0'), timeFormatter)
                                flight = flight.copy(nightTime = t.hour * 60 + t.minute, autoFill = 0,  changed = 1)
                            }
                        }
                    }
                }
            }


            /**
             * If cancelled or clicked outside dialog, undo changes and close Fragment
             */
            timesDialogBackground.setOnClickListener {
                unchangedFlight?.let { flight = it }  // undo all changes made in this dialog
                supportFragmentManager.popBackStack()
            }
            cancelTimeDialog.setOnClickListener {
                unchangedFlight?.let { flight = it }  // undo all changes made in this dialog
                supportFragmentManager.popBackStack()
            }

            /**
             * No need to save anything as viewModel is updated realtime
             */
            saveTimeDialog.setOnClickListener {
                supportFragmentManager.popBackStack()
            }

            //empty onClickListener to block clicks on lower layers
            timesDialogLayout.setOnClickListener {  }
        }

        viewModel.distinctWorkingFlight.observe(viewLifecycleOwner, Observer {f ->
            view.totalTimeView.text = if (f.sim) f.simTimeNoHrs else f.correctedTotalTimeNoHrs
            view.nightTimeText?.setText(timeFromMinutes(f.nightTime))
            view.ifrTimeText?.setText(timeFromMinutes(f.ifrTime))
            view.augmentedCrewCheckbox.isChecked = Crew.of(f.augmentedCrew).crewSize > 2
            autoValue(twilightCalculator)
        })

        return view

    }


    /**
     * This will do some magic, but it will do that delayed, so some changes (ifrTime and nightTime might be showing too late. Check this?)
     */
    private fun autoValue(twilightCalculator: TwilightCalculator) {
        launch {
            if (flight.autoFill.toBoolean() && !flight.sim) {
                //ratio is what augmentedFactor is in EditFlightNew
                val ratio =
                    flight.correctedDuration.toMinutes() / flight.duration.toMinutes().toDouble()
                val orig = orig.await()
                val dest = dest.await()
                val nightTime =
                    if (orig != null && dest != null) (twilightCalculator.minutesOfNight(orig, dest, flight.tOut, flight.tIn) * ratio).toInt()
                    else 0
                //TODO this used deprecated AircraftDb and touches that on main thread
                val ifrTime = 0 // if ((aircraftDb?.searchRegAndType(reg = flight.registration)?.firstOrNull()?.isIfr ?: 0) > 0) flight.correctedDuration.toMinutes().toInt() else 0
                flight = flight.copy(ifrTime = ifrTime, nightTime = nightTime)
            }
        }


    }

    private fun timeFromMinutes(minutes: Int): String = "${minutes/60}:${(minutes%60).toString().padStart(2,'0')}"
}
