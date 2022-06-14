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

import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.children
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.databinding.DialogDatepickerBinding
import nl.joozd.logbookapp.extensions.getColorFromAttr
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.*

/**
 * Usage:
 * LocalDatePickerFragment.create{ pickedDate -> /* do something with picked date */ }
 * TODO: Save state on rotation
 * TODO make horizontal layout
 */
abstract class LocalDatePickerFragment: JoozdlogFragment() {
    /**
     * Selected day
     */
    var selectedDate: LocalDate?
        get() = mSelectedDate
        set(it) {
            mSelectedDate = it
            it?.let {
                selectedMonth = it.withDayOfMonth(1)
                mBinding?.selectDate(it)

            } ?: previousSelected?.unselect()
        }


    /**
     * This function will be called when "OK" is clicked.
     * @param date: Selected date, null if no date selected
     *      LocalDate.now() is auto-selected so that  probably won't happen
     */
    abstract fun onDateSelectedListener(date: LocalDate?)

    private var mSelectedDate = selectedDate
    private var mBinding: DialogDatepickerBinding? = null
    private var mDaysArray: Array<Array<TextView>>? = null



    //previously selected day
    private var previousSelected: TextView? = null

    private val today = LocalDate.now()

    /**
     * The month we are showing dates for.
     * This is a LocalDate so a year is included, day will be ignored.
     */
    private var selectedMonth: LocalDate = today.withDayOfMonth(1)
    set(it){
        field = it
        mBinding?.let { binding ->
            binding.monthText.text = it.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            populateDays(layoutInflater, binding.daysLayout, it)
            mSelectedDate?.let { sd ->
                if (it.month == sd.month) binding.selectDate(sd)
            }
        }
    }

    private val colorAccent
        get() = requireActivity().getColorFromAttr(R.attr.colorAccent)
    private val textColor
        get() = requireActivity().getColorFromAttr(android.R.attr.textColorSecondary)
    private val colorOnAccent
        get() = requireActivity().getColorFromAttr(R.attr.colorOnAccent)



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DialogDatepickerBinding.bind(inflater.inflate(R.layout.dialog_datepicker, container, false)).apply {
            mBinding = this
            populateDays(inflater, daysLayout, selectedMonth)
            selectedDate?.let{ selectDate(it) }
                ?: run { selectedDate = today } // this will select today

            prevMonth.setOnClickListener {
                selectedMonth = selectedMonth.minusMonths(1)
            }
            nextMonth.setOnClickListener {
                selectedMonth = selectedMonth.plusMonths(1)
            }

            /**
             * Exit buttons:
             */
            dialogBackground.setOnClickListener {
                //onCancel()
                supportFragmentManager.popBackStack()
            }
            cancelButton.setOnClickListener {
                //onCancel()
                supportFragmentManager.popBackStack()
            }

            continueButton.setOnClickListener {
                onDateSelectedListener(mSelectedDate)
                supportFragmentManager.popBackStack()
            }

            headerLayout.setOnClickListener {  } // catch clicks
            bodyLayout.setOnClickListener {  } // catch clicks
        }.root

    /**
     * Add day number TextViews to [container]
     * @param container:  an empty [ConstraintLayout] that will be filled.
     */
    private fun populateDays(inflater: LayoutInflater, container: ConstraintLayout, dayInMonth: LocalDate) = with(container) {
        removeAllViews() // clear layout soa  new one can be populated
        val firstDayOfMonth = dayInMonth.withDayOfMonth(1)
        val locale = Locale.getDefault()
        val firstDayWeekday = firstDayOfMonth.dayOfWeek.get(WeekFields.of(locale).dayOfWeek())


        val weekDays = (0..6L).map { dayInWeek ->
            (WeekFields.of(locale).firstDayOfWeek + dayInWeek)                  // get day of week
                        .getDisplayName(TextStyle.SHORT_STANDALONE, locale)             // get it's name
                        .first().uppercaseChar()                                          // get first letter of that name as Uppercase
                .let { c -> makeCharacterTextView(inflater, container, c) }      // put that into a TextView
                .also { addView(it) }                                            // add that textView to layout
        }

        val daysArray = Array(6) { row ->
            Array(7) { column ->
                val today = 7 * row + column + 2 - firstDayWeekday // offset is firstdayWeekday - 1, weekday starts at 1 and column starts at 0 so that cancels each other
                makeDayTextView(inflater, container, today, dayInMonth.month.length(dayInMonth.isLeapYear), onDayClickedListener)   // make textView
                    .also { addView(it) }                                                                                           // add that textview to layout
            }
        }

        //Now, to connect all the views:
        ConstraintSet().apply {
            clone(container)
            weekDays.forEachIndexed { i, v ->
                connect(v.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                if (i == 0)
                    connect(v.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                else
                    connect(v.id, ConstraintSet.START, weekDays[i - 1].id, ConstraintSet.END)
                // don't think I need to connect end to parent.end as size matches exactly
            }
            daysArray.forEachIndexed { lineIndex, line ->
                line.forEachIndexed { i, v ->
                    val prevLine = if (lineIndex == 0) weekDays.toTypedArray() else daysArray[lineIndex - 1]
                    connect(v.id, ConstraintSet.TOP, prevLine[i].id, ConstraintSet.BOTTOM)
                    if (i == 0)
                        connect(v.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                    else
                        connect(v.id, ConstraintSet.START, weekDays[i - 1].id, ConstraintSet.END)
                }
                // connect to bottom so we can use match_constraint for height
                if (line === daysArray.last())
                    connect(line.first().id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            }
            applyTo(container)
        }
        mDaysArray = daysArray
    }


    /**
     * Make a TextView with a day number in it.
     * If day is between 1 and [lastDay] it will put that as text, otherwise it will put an empty string
     * @param day: number of that day
     * @param lastDay: Last day of the month
     * @return: Filled TextView
     */
    private fun makeDayTextView(inflater: LayoutInflater, container: ViewGroup?, day: Int, lastDay: Int, onClick: OnDateClickedListener): TextView =
        (inflater.inflate(R.layout.datepicker_dialog_day, container, false) as TextView).apply {
            if (day in 1..lastDay) {
                text = day.toString()
                setOnClickListener { onClick.onDateClicked(this, day) }
            } else text = ""

            if (day == today.dayOfMonth && selectedMonth.month == today.month && selectedMonth.year == today.year) setTextColor(colorAccent)

            id = View.generateViewId()
        }

    private fun makeCharacterTextView(inflater: LayoutInflater, container: ViewGroup?, letter: Char): TextView =
        (inflater.inflate(R.layout.datepicker_dialog_day, container, false) as TextView).apply {
            text = letter.toString()
            id = View.generateViewId()
        }

    private fun TextView.unselect() {
        setBackgroundResource(0)
        setTextColor(if (text == today.dayOfMonth.toString()) this@LocalDatePickerFragment.colorAccent else this@LocalDatePickerFragment.textColor)
    }

    private fun TextView.select(): TextView {
        background = backgroundCircle
        setTextColor(colorOnAccent)
        return this
    }

    private fun ConstraintLayout.selectDay(day: Int): TextView =
        (children.firstOrNull { it is TextView && it.text == day.toString() } as TextView).select()



    private val onDayClickedListener = OnDateClickedListener { _, day ->
        selectedDate = selectedMonth.withDayOfMonth(day)
    }

    private val backgroundCircle: ShapeDrawable by lazy {
        ShapeDrawable(OvalShape()).apply {
            intrinsicHeight = DAY_VIEW_SIZE_DP.dpToPixels().toInt()
            intrinsicWidth = intrinsicHeight
            paint.color = colorAccent
        }
    }

    private fun DialogDatepickerBinding.selectDate(date: LocalDate){
        previousSelected?.unselect()
        previousSelected = daysLayout.selectDay(date.dayOfMonth)
        monthText.text = date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        dateText.text = date.format(DateTimeFormatter.ofPattern("dd MMMM"))
        yearText.text = date.format(DateTimeFormatter.ofPattern("yyyy"))
        dayText.text = date.format(DateTimeFormatter.ofPattern("EEEE"))
    }

    /**
     * Interface for constructing onclicks for days
     */
    private fun interface OnDateClickedListener {
        fun onDateClicked(v: TextView, date: Int)
    }

    companion object {
        private const val DAY_VIEW_SIZE_DP = 30
    }
}



/*
class DatePickerFragment: DialogFragment(), DatePickerDialog.OnDateSetListener {
    private val effViewModel: NewEditFlightFragmentViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use the current date as the default date in the picker
        effViewModel.localDate?.let{
        return DatePickerDialog(requireContext(), R.style.DatePicker, this, it.year, it.month.value-1, it.dayOfMonth)
        }
        //This is only reached if effViewModel.localDate.value == null
        val c = Calendar.getInstance()
        val year = c.get(effViewModel.localDate?.year ?: Calendar.YEAR)
        val month = c.get(effViewModel.localDate?.monthValue?.let { it - 1 } ?: Calendar.MONTH)
        val day = c.get(effViewModel.localDate?.dayOfMonth ?: Calendar.DAY_OF_MONTH)
        return DatePickerDialog(requireContext(), R.style.DatePicker, this, year, month, day)

        // Create a new instance of DatePickerDialog and return it

    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        effViewModel.setDate(LocalDate.of(year, month+1, day))
        // Do something with the date chosen by the user
    }
}
*/