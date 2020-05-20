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

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.util.Log
import com.google.android.material.textfield.TextInputEditText
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.dialog_add_balance_forward.view.*
import nl.joozd.logbookapp.R
import nl.joozd.joozdlogcommon.BalanceForward
import nl.joozd.logbookapp.extensions.getColorFromAttr
import nl.joozd.logbookapp.extensions.toInt
import nl.joozd.logbookapp.ui.activities.BalanceForwardActivity.Companion.TAG

@Deprecated ("Make new")
class AddBalanceForwardDialog: Fragment() {
    companion object{
        private val onlyDigitsRegex = """\d+""".toRegex()
        private val digitsWithColonRegex = """\d+:\d{1,2}""".toRegex()
    }
    var balanceForwardId: Int = -1
    class OnSave(private val f: (balanceForward: BalanceForward) -> Unit){
        fun save (balanceForward: BalanceForward) {
            f(balanceForward)
        }
    }
    var onSave: OnSave? = null
    fun setOnSave(f: (balanceForward: BalanceForward) -> Unit){
        onSave = OnSave(f)
    }
    class OnClose(private val f: () -> Unit){
        fun closing () {
            f()
        }
    }
    var onClose: OnClose? = null
    fun setOnClose(f: () -> Unit){
        onClose = OnClose(f)
    }


    private var thisView: View? = null
    var balanceForward= BalanceForward(
        "",
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        -1
    )
    set(value){
        field=value
        thisView?.let{v ->
            v.logbookNameText.setText(value.logbookName)
            v.aircraftTimeText.setText(value.aircraftTime.minutesToTimeString())
            v.simulatorTimeText.setText(value.simTime.minutesToTimeString())
            v.takeoffDayText.setText(value.takeOffDay.toString())
            v.takeoffNightText.setText(value.takeOffNight.toString())
            v.landingDayText.setText(value.landingDay.toString())
            v.landingNightText.setText(value.landingNight.toString())
            v.nightTimeText.setText(value.nightTime.minutesToTimeString())
            v.ifrTimeText.setText(value.ifrTime.minutesToTimeString())
            v.picText.setText(value.picTime.minutesToTimeString())
            v.copilotText.setText(value.copilotTime.minutesToTimeString())
            v.dualText.setText(value.dualTime.minutesToTimeString())
            v.instructorText.setText(value.instructortime.minutesToTimeString())
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        thisView = inflater.inflate(R.layout.dialog_add_balance_forward, container, false)
        thisView?.let {thisView ->
            balanceForward = balanceForward.copy(id = balanceForwardId) // reset self to set all fields and correct ID


            if (balanceForward.logbookName.isEmpty()) balanceForward =
                balanceForward.copy(logbookName = resources.getString(R.string.paperLogbook))

            (thisView.flightInfoText.background as GradientDrawable).colorFilter =
                PorterDuffColorFilter(
                    requireActivity().getColorFromAttr(android.R.attr.colorPrimary),
                    PorterDuff.Mode.SRC_IN
                ) // set background color to bakground with rounded corners


            thisView.logbookNameText.onFocusChangeListener =
                View.OnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        (v as TextInputEditText).selectAll()

                    } else {
                        balanceForward =
                            balanceForward.copy(logbookName = thisView.logbookNameText.text.toString())
                    }
                }

            thisView.aircraftTimeText.onFocusChangeListener =
                View.OnFocusChangeListener { vw, hasFocus ->
                    val v = vw as TextInputEditText
                    if (hasFocus) {
                        v.selectAll()

                    } else {
                        val text = v.text?.addColonForTime() ?: "00:00"
                        balanceForward = balanceForward.copy(aircraftTime = text.toMinutes())
                    }
                }

            thisView.simulatorTimeText.onFocusChangeListener =
                View.OnFocusChangeListener { vw, hasFocus ->
                    val v = vw as TextInputEditText
                    if (hasFocus) {
                        v.selectAll()

                    } else {
                        val text = v.text?.addColonForTime() ?: "00:00"
                        balanceForward = balanceForward.copy(simTime = text.toMinutes())
                    }
                }

            thisView.nightTimeText.onFocusChangeListener =
                View.OnFocusChangeListener { vw, hasFocus ->
                    val v = vw as TextInputEditText
                    if (hasFocus) {
                        v.selectAll()

                    } else {
                        val text = v.text?.addColonForTime() ?: "00:00"
                        balanceForward = balanceForward.copy(nightTime = text.toMinutes())
                    }
                }

            thisView.ifrTimeText.onFocusChangeListener =
                View.OnFocusChangeListener { vw, hasFocus ->
                    val v = vw as TextInputEditText
                    if (hasFocus) {
                        v.selectAll()

                    } else {
                        val text = v.text?.addColonForTime() ?: "00:00"
                        balanceForward = balanceForward.copy(ifrTime = text.toMinutes())
                    }
                }

            thisView.picText.onFocusChangeListener = View.OnFocusChangeListener { vw, hasFocus ->
                val v = vw as TextInputEditText
                if (hasFocus) {
                    v.selectAll()

                } else {
                    val text = v.text?.addColonForTime() ?: "00:00"
                    balanceForward = balanceForward.copy(picTime = text.toMinutes())
                }
            }

            thisView.copilotText.onFocusChangeListener =
                View.OnFocusChangeListener { vw, hasFocus ->
                    val v = vw as TextInputEditText
                    if (hasFocus) {
                        v.selectAll()

                    } else {
                        val text = v.text?.addColonForTime() ?: "00:00"
                        balanceForward = balanceForward.copy(copilotTime = text.toMinutes())
                    }
                }

            thisView.dualText.onFocusChangeListener = View.OnFocusChangeListener { vw, hasFocus ->
                val v = vw as TextInputEditText
                if (hasFocus) {
                    v.selectAll()

                } else {
                    val text = v.text?.addColonForTime() ?: "00:00"
                    balanceForward = balanceForward.copy(dualTime = text.toMinutes())
                }
            }

            thisView.instructorText.onFocusChangeListener =
                View.OnFocusChangeListener { vw, hasFocus ->
                    val v = vw as TextInputEditText
                    if (hasFocus) {
                        v.selectAll()

                    } else {
                        val text = v.text?.addColonForTime() ?: "00:00"
                        balanceForward = balanceForward.copy(instructortime = text.toMinutes())
                    }
                }

            thisView.takeoffDayText.onFocusChangeListener =
                View.OnFocusChangeListener { vw, hasFocus ->
                    val v = vw as TextInputEditText
                    if (hasFocus) {
                        v.selectAll()

                    } else {
                        balanceForward = balanceForward.copy(takeOffDay = v.text.toInt())
                    }
                }

            thisView.takeoffNightText.onFocusChangeListener =
                View.OnFocusChangeListener { vw, hasFocus ->
                    val v = vw as TextInputEditText
                    if (hasFocus) {
                        v.selectAll()

                    } else {
                        balanceForward = balanceForward.copy(takeOffNight = v.text.toInt())
                    }
                }

            thisView.landingDayText.onFocusChangeListener =
                View.OnFocusChangeListener { vw, hasFocus ->
                    val v = vw as TextInputEditText
                    if (hasFocus) {
                        v.selectAll()

                    } else {
                        balanceForward = balanceForward.copy(landingDay = v.text.toInt())
                    }
                }

            thisView.landingNightText.onFocusChangeListener =
                View.OnFocusChangeListener { vw, hasFocus ->
                    val v = vw as TextInputEditText
                    if (hasFocus) {
                        v.selectAll()

                    } else {
                        balanceForward = balanceForward.copy(landingNight = v.text.toInt())
                    }
                }


            thisView.backgroundLayout.setOnClickListener { fragmentManager?.popBackStack() }
            thisView.cancelButton.setOnClickListener { fragmentManager?.popBackStack() }

            thisView.saveButton.setOnClickListener {
                activity?.currentFocus?.clearFocus()
                onSave?.save(balanceForward)
                fragmentManager?.popBackStack()
            }
        }
        return thisView
    }

    override fun onDestroyView() {
        onClose?.closing()
        super.onDestroyView()
    }


    /**
     * This will change "1230 to 12:30 and 17 to 0:17 and 123456 to 1234:56
     * It will leave all other things the way they were
     *
     */
    private fun String.addColonForTime(): String{
        // drop anything after and including a possible second ':'
        var string = this
        while (string.split(':').size > 2) {
            string = string.slice(0 until string.lastIndexOf(":"))
        }

        if (":" in string)
            string = string.removeRange(string.indexOf(":"), string.indexOf(":")+1)

        string = string.padStart(3,'0')

        //add a colon if none provided
        if (string.matches(onlyDigitsRegex)){
            Log.d(TAG, "string is $string")
                // string is only digits
                return  if (string.takeLast(2).toInt() < 60) "${string.dropLast(2)}:${string.takeLast(2)}"
                        else "${(string.dropLast(2).toInt()+1)}:${(string.takeLast(2).toInt()-60).toString().padStart(2,'0')}"
                }
        return string
    }

    private fun Editable.addColonForTime(): String = this.toString().addColonForTime()

    private fun String.toMinutes(): Int =
        if (!this.matches(digitsWithColonRegex)) 0
        else (this.slice(0 until this.indexOf(":")).toInt()) * 60 + this.takeLast(2).toInt()

    private fun Int.minutesToTimeString():String = "${this / 60}:${(this % 60).toString().padStart(
        2,
        '0'
    )}"

}