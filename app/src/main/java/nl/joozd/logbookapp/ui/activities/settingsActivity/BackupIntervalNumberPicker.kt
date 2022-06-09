package nl.joozd.logbookapp.ui.activities.settingsActivity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
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
        Prefs.backupInterval(pickedNumber)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        lifecycleScope.launch {
            selectedValue = Prefs.backupInterval()
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override val formatter = NumberPicker.Formatter{
        when (it) {
            0 -> App.instance.getString(R.string.never)
            1 -> App.instance.getString(R.string.day)
            else -> App.instance.getStringWithMakeup(R.string.n_days, it.toString()).toString()
        }
    }
}