package nl.joozd.logbookapp.ui.activities.settingsActivity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.model.helpers.minutesToHoursAndMinutesString
import nl.joozd.logbookapp.ui.dialogs.NumberPickerDialog

class AugmentedTakeoffLandingTimesPicker: NumberPickerDialog() {
    fun setValue(value: Int) {
        selectedValue = when (value) {
            in (0..THIRTY) -> value
            in (THIRTY..NINETY) -> THIRTY + (value - 30) / 5
            else -> maxOf(NINETY + (value - 90) / 15, 0)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        lifecycleScope.launch {
            setValue(Prefs.augmentedTakeoffLandingTimes())
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    /**
     * Override this to actually do something with the picked number
     */
    override fun onNumberPicked(pickedNumber: Int) {
        Prefs.augmentedTakeoffLandingTimes(unFormat(pickedNumber))
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