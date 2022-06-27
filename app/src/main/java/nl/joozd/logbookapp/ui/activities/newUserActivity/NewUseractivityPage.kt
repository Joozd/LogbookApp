package nl.joozd.logbookapp.ui.activities.newUserActivity

import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import nl.joozd.logbookapp.R
import nl.joozd.logbookapp.ui.utils.JoozdlogFragment

abstract class NewUseractivityPage: JoozdlogFragment() {
    val newUserActivity: NewUserActivity get() = requireActivity() as NewUserActivity

    private fun continueClicked(){
        newUserActivity.continueClicked()
    }
    private fun previousClicked(){
        newUserActivity.previousClicked()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.continueButton)?.setOnClickListener {
            continueClicked()
        }
        view.findViewById<TextView>(R.id.backButton)?.setOnClickListener {
            previousClicked()
        }
    }
}