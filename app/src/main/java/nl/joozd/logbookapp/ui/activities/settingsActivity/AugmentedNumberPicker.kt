package nl.joozd.logbookapp.ui.activities.settingsActivity

import android.widget.NumberPicker
import nl.joozd.logbookapp.data.sharedPrefs.Preferences
import nl.joozd.logbookapp.model.helpers.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.ui.dialogs.NumberPickerDialog

class AugmentedNumberPicker: NumberPickerDialog() {
    fun setValue(value: Int) {
        selectedValue = when (value) {
            in (0..THIRTY) -> value
            in (THIRTY..NINETY) -> THIRTY + (value - 30) / 5
            else -> maxOf(NINETY + (value - 90) / 15, 0)
        }
    }

    /**
     * Override this to actually do something with the picked number
     */
    override fun onNumberPicked(pickedNumber: Int) {
        Preferences.standardTakeoffLandingTimes = unFormat(pickedNumber)
    }

    override val formatter = NumberPicker.Formatter{
        format(it)
    }

    private fun format(value: Int): String = when (value) {
        in (0..THIRTY) -> value.minutesToHoursAndMinutesString()
        in (THIRTY..NINETY) -> (30 + (value- THIRTY)*5).minutesToHoursAndMinutesString()
        else -> maxOf(90 + (value- NINETY)*15, 0).minutesToHoursAndMinutesString()
    }

    private fun unFormat(value: Int) = when (value) {
        in (0..THIRTY) -> value
        in (THIRTY..NINETY) -> 30 + (value- THIRTY)*5
        else -> 90 + (value- NINETY) * 15
    }

    companion object {
        private const val THIRTY: Int = 30
        private const val NINETY: Int = 30 + (90-30) / 5
        const val EIGHT_HOURS = NINETY + (8*60 - 90) / 15
    }
}