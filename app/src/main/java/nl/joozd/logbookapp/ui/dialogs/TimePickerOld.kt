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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.android.synthetic.main.dialog_times_in_out.view.*
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.dataclasses.Airport
import nl.joozd.logbookapp.data.dataclasses.Flight

import nl.joozd.logbookapp.data.miscClasses.Crew
import nl.joozd.logbookapp.data.viewmodel.JoozdlogViewModel
import nl.joozd.logbookapp.extensions.ctx
import nl.joozd.logbookapp.extensions.getColorFromAttr
import nl.joozd.logbookapp.ui.utils.toast
import nl.joozd.logbookapp.utils.TwilightCalculator
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Deprecated ("Switch to now viewModel-using TimePicker Fragment")
class TimePickerOld: Fragment() {
    companion object{
        private val paddedMinutes = IntArray(60) {it}.map{v -> v.toString().padStart(2,'0')}.toTypedArray()
        private val paddedHours = IntArray(24) {it}.map{v -> v.toString().padStart(2,'0')}.toTypedArray()

    }
    private val viewModel: JoozdlogViewModel by viewModels()
    private var thisDialog: View? = null
    private var twilightCalculator: TwilightCalculator? = null
    private var ori: Airport? = null
    private var des: Airport? = null

    class OnSaveListener(private val f: (flight: Flight) -> Unit){
        fun onSave(flight: Flight){
            f(flight)
        }
    }

    var onSaveListener: OnSaveListener? = null

    fun setOnSaveListener(f: (flight: Flight) -> Unit){
        onSaveListener = OnSaveListener(f)
    }

    var currentFlight: Flight? = null
    set(flight){
        flight?.let { f ->
            field = f
            thisDialog?.let { view ->
                view.totalTimeView.text =
                    if (f.sim) f.simTimeNoHrs else f.correctedTotalTimeNoHrs
                view.nightTimeText?.setText(timeFromMinutes(f.nightTime))
                view.ifrTimeText?.setText(timeFromMinutes(f.ifrTime))
                view.augmentedCrewCheckbox.isChecked = Crew.of(f.augmentedCrew).crewSize > 2
            }
            if (f.ifrTime > f.duration.toMinutes()) currentFlight = f.copy(ifrTime = f.duration.toMinutes().toInt())
        }
    }
    var autoTimesChanged = false
    // DEPRECATED var airportDb: AirportDb? = null
    // var aircraftDb: AircraftDb? = null

    // variable to store previous text on any selected field
    var previousText: String = ""

    //used in changing an amount of time (ie. "1:15") to a LocalTime so I can easily take hours and minutes
    //don't forget to padStart hours to HH in stead of H.
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view: View = inflater.inflate(R.layout.dialog_times_in_out, container, false)
        thisDialog = view

        (view.timesDialogTopHalf.background as GradientDrawable).colorFilter = PorterDuffColorFilter(requireActivity().getColorFromAttr(android.R.attr.colorPrimary), PorterDuff.Mode.SRC_IN)

// define values for pickers
        view.hoursOutPicker.minValue=0
        view.hoursOutPicker.maxValue=23
        view.hoursOutPicker.displayedValues=paddedHours
        view.minutesOutPicker.minValue=0
        view.minutesOutPicker.maxValue=59
        view.minutesOutPicker.displayedValues= paddedMinutes
        view.hoursInPicker.minValue=0
        view.hoursInPicker.maxValue=23
        view.hoursInPicker.displayedValues=paddedHours
        view.minutesInPicker.minValue=0
        view.minutesInPicker.maxValue=59
        view.minutesInPicker.displayedValues= paddedMinutes


        // Fill with data from current Flight
        lifecycleScope.launch {
            currentFlight?.let {
                if (it.sim) {
                    view.hoursOutPicker.value = it.simTime / 60
                    view.minutesOutPicker.value = it.simTime % 60
                    view.tOutText.text = getString(R.string.simtTime)
                    view.tInText.visibility = View.INVISIBLE
                    view.hoursInPicker.visibility = View.INVISIBLE
                    view.minutesInPicker.visibility = View.INVISIBLE
                    view.nightTimeLayout.visibility = View.GONE
                    view.ifrTimeLayout.visibility = View.GONE
                    view.augmentedCrewCheckbox.visibility = View.GONE
                    view.autoValuesCheckbox.visibility = View.GONE
                    ori = null
                    des = null
                } else {
                    twilightCalculator = TwilightCalculator(
                        LocalDateTime.of(
                            it.tOut.toLocalDate(),
                            it.tOut.toLocalTime()
                        )
                    )
                    view.hoursOutPicker.value = it.tOut.hour
                    view.minutesOutPicker.value = it.tOut.minute
                    view.hoursInPicker.value = it.tIn.hour
                    view.minutesInPicker.value = it.tIn.minute
                    view.autoValuesCheckbox.isChecked = it.autoFill > 0
                    view.augmentedCrewCheckbox.isChecked =
                        Crew.of(it.augmentedCrew).crewSize > 2
                    ori = null // viewModel.searchAirport(it.orig) // deprecated anyway
                    des = null // viewModel.searchAirport(it.dest) // deprecated anyway
                }
                currentFlight = autoValue(it) // set self to trigger setter
            }
        }


        view.timesDialogBackground.setOnClickListener { fragmentManager?.popBackStack() }
        view.hoursOutPicker.setOnValueChangedListener { _, _, newVal ->
            currentFlight?.let {
                val timeOut = LocalDateTime.of(
                    it.tOut.toLocalDate(),
                    LocalTime.of(newVal, it.tOut.minute)
                )
                val timeIn =
                    when {
                        it.tIn.minusDays(1) > timeOut -> it.tIn.minusDays(1)
                        it.tIn > timeOut -> it.tIn
                        else -> it.tIn.plusDays(1)
                    }
                currentFlight = autoValue(it.copy(timeOut = timeOut.toInstant(ZoneOffset.UTC).epochSecond, timeIn = timeIn.toInstant(ZoneOffset.UTC).epochSecond))

            }
        }
        view.minutesOutPicker.setOnValueChangedListener { _, _, newVal ->
            currentFlight?.let {
                val timeOut = LocalDateTime.of(
                    it.tOut.toLocalDate(),
                    LocalTime.of(it.tOut.hour, newVal)
                )
                val timeIn =
                    when {
                        it.tIn.minusDays(1) > timeOut -> it.tIn.minusDays(1)
                        it.tIn > timeOut -> it.tIn
                        else -> it.tIn.plusDays(1)
                    }
                currentFlight = autoValue(it.copy(timeOut = timeOut.toInstant(ZoneOffset.UTC).epochSecond, timeIn = timeIn.toInstant(ZoneOffset.UTC).epochSecond))
            }
        }
        view.hoursInPicker.setOnValueChangedListener { _, _, newVal ->
            currentFlight?.let {
                val timeToCheck = LocalDateTime.of(
                    it.tOut.toLocalDate(),
                    LocalTime.of(newVal, it.tIn.minute)
                )
                val timeIn = when {
                    timeToCheck.minusDays(1) > it.tOut -> timeToCheck.minusDays(1)
                    timeToCheck > it.tOut -> timeToCheck
                    else -> timeToCheck.plusDays(1)
                }

                currentFlight = autoValue(it.copy(timeIn = timeIn.toInstant(ZoneOffset.UTC).epochSecond))
            }
        }
        view.minutesInPicker.setOnValueChangedListener { _, _, newVal ->
            currentFlight?.let {
                val timeToCheck = LocalDateTime.of(
                    it.tOut.toLocalDate(),
                    LocalTime.of(it.tIn.hour, newVal)
                )
                val timeIn = when {
                    timeToCheck.minusDays(1) > it.tOut -> timeToCheck.minusDays(1)
                    timeToCheck > it.tOut -> timeToCheck
                    else -> timeToCheck.plusDays(1)
                }

                currentFlight = autoValue(it.copy(timeIn = timeIn.toInstant(ZoneOffset.UTC).epochSecond))
            }
        }
        
        view.autoValuesCheckbox.setOnCheckedChangeListener { _, b ->
            if (b) {
                currentFlight?.let {
                    currentFlight = autoValue(it.copy(autoFill = 1))
                }
            } else {
                currentFlight?.let {
                    currentFlight = (it.copy(autoFill = 0))
                }
            }
        }

        view.augmentedCrewCheckbox.setOnClickListener {
            Log.d("meh", "XOXO")
            currentFlight?.let {
                val augmentedCrewDialog = AugmentedCrewDialogOld()
                augmentedCrewDialog.crewValue = it.crew
                augmentedCrewDialog.setOnSaveListener { crewValue -> currentFlight = it.copy(augmentedCrew = crewValue.toInt()) }
                fragmentManager?.beginTransaction()
                    ?.add(R.id.mainActivityLayout, augmentedCrewDialog)
                    ?.addToBackStack(null)
                    ?.commit()
            }

        }
        
        view.cancelTimeDialog.setOnClickListener { fragmentManager?.popBackStack() }
        view.saveTimeDialog.setOnClickListener { 
            currentFlight?.let { onSaveListener?.onSave(it) }
            fragmentManager?.popBackStack()
        }

        view.ifrTimeText.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                with(v as TextInputEditText) {
                    previousText = text.toString()
                }
            } else {
                currentFlight?.let { f ->
                    with(v as EditText) {
                        if (text.toString() != previousText) { // if something changed
                            val currentText = this.text.toString()
                            //TODO change "100" to "1:00"

                            // check if not longer than 5 characters (hh:mm is 5 characters)
                            if (!(("([01]\\d|2[0-3]):[0-5]\\d".toRegex().containsMatchIn(currentText) && currentText.length == 5)
                                        || ("([01]\\d|2[0-3])[0-5]\\d".toRegex().containsMatchIn(
                                    currentText.padStart(
                                        4,
                                        '0'
                                    )
                                ) && currentText.length <= 4))
                                || (currentText == "")
                            ){
                                context.toast("Invalid entry")
                                v.setText(previousText)
                            } else {
                                val len = currentText.padStart(3, '0').length
                                this.setText(if (len < 4)
                                    "${currentText.padStart(3,'0')
                                        .slice(0..(len-3))}:${currentText.padStart(3, '0').slice(len-2 until len)}"
                                    else currentText)

                                val t = LocalTime.parse(
                                    v.text.toString().padStart(5, '0'),
                                    timeFormatter
                                )
                                currentFlight = f.copy(ifrTime = t.hour * 60 + t.minute, autoFill = 0,  changed = 1)
                            }

                        }
                    }
                }
            }
        }

        view.ifrTimeText.setOnEditorActionListener { thisview, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE){
                val imm: InputMethodManager = thisview.ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(thisview.windowToken, 0)
                thisview.clearFocus()
            }
            true
        }

        view.nightTimeText.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                with(v as TextInputEditText) {
                    previousText = text.toString()
                }
            } else {
                currentFlight?.let { f ->
                    with(v as EditText) {
                        if (text.toString() != previousText) { // if something changed
                            val currentText = this.text.toString()
                            //TODO change "100" to "1:00"

                            // check if not longer than 5 characters (hh:mm is 5 characters)
                            if (!(("([01]\\d|2[0-3]):[0-5]\\d".toRegex().containsMatchIn(currentText) && currentText.length == 5)
                                        || ("([01]\\d|2[0-3])[0-5]\\d".toRegex().containsMatchIn(
                                    currentText.padStart(
                                        4,
                                        '0'
                                    )
                                ) && currentText.length <= 4))
                                || (currentText == "")
                            ){
                                context.toast("Invalid entry")
                                v.setText(previousText)
                            } else {
                                val len = currentText.padStart(3, '0').length
                                this.setText(if (len < 4)
                                    "${currentText.padStart(3,'0')
                                        .slice(0..(len-3))}:${currentText.padStart(3, '0').slice(len-2 until len)}"
                                else currentText)

                                val t = LocalTime.parse(
                                    v.text.toString().padStart(5, '0'),
                                    timeFormatter
                                )
                                currentFlight = f.copy(nightTime = t.hour * 60 + t.minute, autoFill = 0,  changed = 1)
                            }

                        }
                    }
                }
            }
        }


        //empty onClickListener to block clicks on lower layers
        view.timesDialogLayout.setOnClickListener {  }

        return view
    }

    private fun autoValue(flight: Flight): Flight {
        if (thisDialog?.autoValuesCheckbox?.isChecked == true && !flight.sim){
            with (flight) {

                //ratio is what augmentedFactor is in EditFlightNew
                val ratio = correctedDuration.toMinutes()/duration.toMinutes().toDouble()
                val nightTime =
                    if (ori != null && des != null && twilightCalculator!= null) (twilightCalculator!!.minutesOfNight(ori, des, tOut, tIn)*ratio).toInt()
                    else 0
                val ifrTime = 3 // if ((aircraftDb?.searchRegAndType(reg = registration)?.firstOrNull()
                       // ?.isIfr ?: 0) > 0) correctedDuration.toMinutes().toInt() else 0
                return flight.copy(ifrTime = ifrTime, nightTime = nightTime)
            }
        }
        else return flight
    }

    private fun timeFromMinutes(minutes: Int): String = "${minutes/60}:${(minutes%60).toString().padStart(2,'0')}"
}
