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

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.DatePicker
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.model.viewmodels.fragments.EditFlightFragmentViewModel
import java.time.LocalDate
import java.util.*


class DatePickerFragment: DialogFragment(), DatePickerDialog.OnDateSetListener {
    private val effViewModel: EditFlightFragmentViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use the current date as the default date in the picker
        effViewModel.getLocalDate()?.let{
            Log.d("datethingy", "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX - $it - XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX")
        return DatePickerDialog(requireContext(), R.style.DatePicker, this, it.year, it.month.value, it.dayOfMonth)
        }
        //This is only reached if effViewMOdel.localDate.value == null
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)
        return DatePickerDialog(requireContext(), R.style.DatePicker, this, year, month, day)

        // Create a new instance of DatePickerDialog and return it

    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        effViewModel.setDate(LocalDate.of(year, month+1, day))
        // Do something with the date chosen by the user
    }
}