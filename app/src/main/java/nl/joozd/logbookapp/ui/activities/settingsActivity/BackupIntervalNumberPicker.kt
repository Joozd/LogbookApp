package nl.joozd.logbookapp.ui.activities.settingsActivity

import android.widget.NumberPicker
import nl.joozd.logbookapp.core.App
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.data.sharedPrefs.Prefs
import nl.joozd.logbookapp.extensions.getStringWithMakeup
import nl.joozd.logbookapp.ui.dialogs.NumberPickerDialog

class BackupIntervalNumberPicker: NumberPickerDialog() {
    /**
     * Override this to actually do something with the picked number
     */
    override fun onNumberPicked(pickedNumber: Int) {
        Prefs.backupInterval = pickedNumber
    }

    override val formatter = NumberPicker.Formatter{
        when (it) {
            0 -> App.instance.getString(R.string.never)
            1 -> App.instance.getString(R.string.day)
            else -> App.instance.getStringWithMakeup(R.string.n_days, it.toString()).toString()
        }
    }
}