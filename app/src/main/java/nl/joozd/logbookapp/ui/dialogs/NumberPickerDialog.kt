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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.DialogNumberPickerBinding

/**
 * Override onNumberPicked
 */
abstract class NumberPickerDialog: DialogFragment() {
    private val selectedValueLiveData = MutableLiveData(0)

    var title: String? = null
    var minValue: Int = 0
    var maxValue: Int = Int.MAX_VALUE
    var selectedValue: Int
        get() = selectedValueLiveData.value ?: 0
        set(newVal){
            selectedValueLiveData.value = newVal
        }
    var wrapSelectorWheel: Boolean? = false

    /**
     * Override this value to set formatting for numberpicker
     */
    open val formatter: NumberPicker.Formatter? = null
    private var mNumberPicker: NumberPicker? = null



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        with(DialogNumberPickerBinding.bind(requireActivity().layoutInflater.inflate(R.layout.dialog_number_picker, container, false))) {

            // Restore instance state

            restoreInstanceState(savedInstanceState)
            onRestoreInstanceState(savedInstanceState)

            mNumberPicker = numberPicker

            //set title text, or hide it when none set
            title?.let {
                numberPickerTitle.text = it
            } ?: run { numberPickerTitle.visibility = View.GONE }




            numberPicker.minValue = minValue
            numberPicker.maxValue = maxValue
            // numberPicker.value = selectedValue should be taken care of by observer
            numberPicker.setOnValueChangedListener { _, _, newVal ->
                selectedValue = newVal
            }
            formatter?.let {
                numberPicker.setFormatter(it)

            }
            wrapSelectorWheel?.let { numberPicker.wrapSelectorWheel = it }


            cancelTextView.setOnClickListener {
                dismiss()
            }
            saveTextView.setOnClickListener {
                onNumberPicked(selectedValue)
                dismiss()
            }

            selectedValueLiveData.observe(viewLifecycleOwner){
                numberPicker.value = it
            }

            return root
        }
    }


    /**
     * if overriding, do call super.onRestoreInstanceState() or values will not be saved
     */
    open fun onRestoreInstanceState(savedInstanceState: Bundle?) {

    }

    /**
     * Restores set and currently picked values on recreate
     */
    private fun restoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.let { bundle ->
            title = bundle.getString(SAVED_INSTANCE_STATE_TITLE_TAG)
            minValue = bundle.getInt(SAVED_INSTANCE_STATE_MIN_VALUE_TAG, Int.MIN_VALUE)
            maxValue = bundle.getInt(SAVED_INSTANCE_STATE_MAX_VALUE_TAG, Int.MAX_VALUE)
            selectedValue = bundle.getInt(SAVED_INSTANCE_STATE_SELECTED_VALUE_TAG, 0)
            wrapSelectorWheel = when (bundle.getInt(SAVED_INSTANCE_STATE_WRAP_SELECTOR_WHEEL_TAG)){
                1 -> true
                0 -> false
                else -> null
            }
        }
    }

    /**
     * Call super.onSaveInstanceState(outState) if overriding
     */
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(SAVED_INSTANCE_STATE_TITLE_TAG, title)
        outState.putInt(SAVED_INSTANCE_STATE_MIN_VALUE_TAG, minValue)
        outState.putInt(SAVED_INSTANCE_STATE_MAX_VALUE_TAG, maxValue)
        outState.putInt(SAVED_INSTANCE_STATE_SELECTED_VALUE_TAG, selectedValue)
        outState.putInt(SAVED_INSTANCE_STATE_WRAP_SELECTOR_WHEEL_TAG, when (wrapSelectorWheel){
            true -> 1
            false -> 0
            null -> -1337
        })

        super.onSaveInstanceState(outState)
    }


    /**
     * Override this to actually do something with the picked number
     */
    abstract fun onNumberPicked(pickedNumber: Int)


    companion object {
        const val SAVED_INSTANCE_STATE_TITLE_TAG = "SAVED_INSTANCE_STATE_TITLE_TAG"
        const val SAVED_INSTANCE_STATE_MIN_VALUE_TAG = "SAVED_INSTANCE_STATE_MIN_VALUE_TAG"
        const val SAVED_INSTANCE_STATE_MAX_VALUE_TAG = "SAVED_INSTANCE_STATE_MAX_VALUE_TAG"
        const val SAVED_INSTANCE_STATE_SELECTED_VALUE_TAG = "SAVED_INSTANCE_STATE_SELECTED_VALUE_TAG"
        const val SAVED_INSTANCE_STATE_WRAP_SELECTOR_WHEEL_TAG = "SAVED_INSTANCE_STATE_WRAP_SELECTOR_WHEEL_TAG"


    }
}